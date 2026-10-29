/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicSession;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link SingleConnectionFactory} 的子类，增加了 {@link Session} 缓存以及
 * {@link MessageProducer} 和 {@link MessageConsumer} 缓存。此 ConnectionFactory
 * 还将 {@link #setReconnectOnException "reconnectOnException" 属性}
 * 默认切换为 "true"，从而允许底层 Connection 自动恢复。
 *
 * <p>默认情况下只缓存一个 Session，进一步请求的 Session 将按需创建和销毁。
 * 在高并发环境中，请考虑提高
 * {@link #setSessionCacheSize "sessionCacheSize" 值}。
 *
 * <p>使用 JMS 1.0.2 API 时，此 ConnectionFactory 会根据运行时使用的 JMS API 方法
 * 切换到队列/主题模式：{@code createQueueConnection} 和 {@code createTopicConnection}
 * 分别对应队列模式和主题模式；通用的 {@code createConnection} 调用会得到一个
 * 能够同时服务两种模式的 JMS 1.1 连接。
 *
 * <p>从 Spring Framework 5 开始，此类支持 JMS 2.0 {@code JMSContext} 调用，
 * 因此要求运行时存在 JMS 2.0 API。不过，只要应用设置不触发实际的 JMS 2.0 调用，
 * 它仍然可以运行在 JMS 1.1 驱动之上（绑定到 JMS 2.0 API）。
 *
 * <p><b>注意：此 ConnectionFactory 要求显式关闭从其共享 Connection 获取的所有
 * Session。</b>无论如何，这都是使用原生 JMS 访问代码时的常见建议。
 * 但是，使用此 ConnectionFactory 时，为了真正实现 Session 复用，必须这样做。
 *
 * <p>另请注意，从缓存 Session 获取的 MessageConsumer 在 Session 最终从池中移除之前
 * 不会被关闭。这在某些情况下可能导致语义上的副作用。对于持久订阅者，
 * 逻辑上的 {@code Session.close()} 调用也会关闭订阅。不支持在同一个 Session
 * 句柄上为同一订阅重新注册持久消费者；请先关闭并重新获取一个缓存的 Session。
 *
 * <p>最后，临时队列和临时主题（TemporaryQueue/TemporaryTopic）的 MessageProducer
 * 和 MessageConsumer 永远不会被缓存。不幸的是，WebLogic JMS 恰好在其常规目标实现上
 * 实现了临时队列/主题接口，错误地表明其所有目标都无法缓存。在 WebLogic 上请使用
 * 不同的连接池/缓存，或为此目的自定义此类。
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see Connection
 * @see Session
 * @see MessageProducer
 * @see MessageConsumer
 */
public class CachingConnectionFactory extends SingleConnectionFactory {

	private int sessionCacheSize = 1;

	private boolean cacheProducers = true;

	private boolean cacheConsumers = true;

	private volatile boolean active = true;

	private final ConcurrentMap<Integer, Deque<Session>> cachedSessions = new ConcurrentHashMap<>();


	/**
	 * 创建一个用于 bean 风格用法的新 CachingConnectionFactory。
	 * @see #setTargetConnectionFactory
	 */
	public CachingConnectionFactory() {
		super();
		setReconnectOnException(true);
	}

	/**
	 * 为给定的目标 ConnectionFactory 创建一个新的 CachingConnectionFactory。
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public CachingConnectionFactory(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
		setReconnectOnException(true);
	}


	/**
	 * 指定 JMS Session 缓存所需的容量（每种 JMS Session 类型）。
	 * <p>此缓存大小是每种会话确认类型（auto、client、dups_ok、transacted）
	 * 可缓存 Session 数量的最大上限。因此，在混合使用不同确认类型的罕见情况下，
	 * 实际缓存的 Session 数量可能高达指定值的四倍。
	 * <p>默认值为 1：缓存单个 Session，并（重新）按需创建更多 Session。
	 * 如果希望提高缓存的 Session 数量，可指定诸如 10 之类的数值；
	 * 不过，对于低并发场景，1 可能已经足够。
	 * @see #setCacheProducers
	 */
	public void setSessionCacheSize(int sessionCacheSize) {
		Assert.isTrue(sessionCacheSize >= 1, "Session cache size must be 1 or higher");
		this.sessionCacheSize = sessionCacheSize;
	}

	/**
	 * 返回 JMS Session 缓存所需的容量（每种 JMS Session 类型）。
	 */
	public int getSessionCacheSize() {
		return this.sessionCacheSize;
	}

	/**
	 * 指定是否按 JMS Session 实例缓存 JMS MessageProducer
	 * （更具体地说：每个 Destination 和 Session 一个 MessageProducer）。
	 * <p>默认值为 "true"。将其切换为 "false" 可始终按需重新创建 MessageProducer。
	 */
	public void setCacheProducers(boolean cacheProducers) {
		this.cacheProducers = cacheProducers;
	}

	/**
	 * 返回是否按 JMS Session 实例缓存 JMS MessageProducer。
	 */
	public boolean isCacheProducers() {
		return this.cacheProducers;
	}

	/**
	 * 指定是否按 JMS Session 实例缓存 JMS MessageConsumer
	 * （更具体地说：每个 Destination、选择器字符串和 Session 一个 MessageConsumer）。
	 * 请注意，持久订阅者只会被缓存到 Session 句柄逻辑关闭为止。
	 * <p>默认值为 "true"。将其切换为 "false" 可始终按需重新创建 MessageConsumer。
	 */
	public void setCacheConsumers(boolean cacheConsumers) {
		this.cacheConsumers = cacheConsumers;
	}

	/**
	 * 返回是否按 JMS Session 实例缓存 JMS MessageConsumer。
	 */
	public boolean isCacheConsumers() {
		return this.cacheConsumers;
	}


	/**
	 * 返回当前会话数量，表示此连接工厂当前缓存的会话数。
	 * @since 5.3.7
	 */
	public int getCachedSessionCount() {
		int count = 0;
		synchronized (this.cachedSessions) {
			for (Deque<Session> sessionList : this.cachedSessions.values()) {
				synchronized (sessionList) {
					count += sessionList.size();
				}
			}
		}
		return count;
	}

	/**
	 * 同时重置 Session 缓存。
	 */
	@Override
	public void resetConnection() {
		this.active = false;

		synchronized (this.cachedSessions) {
			for (Deque<Session> sessionList : this.cachedSessions.values()) {
				synchronized (sessionList) {
					for (Session session : sessionList) {
						try {
							session.close();
						}
						catch (Throwable ex) {
							logger.trace("Could not close cached JMS Session", ex);
						}
					}
				}
			}
			this.cachedSessions.clear();
		}

		// Now proceed with actual closing of the shared Connection...
		super.resetConnection();

		this.active = true;
	}

	/**
	 * 检查给定模式下是否有缓存的 Session。
	 */
	@Override
	protected Session getSession(Connection con, Integer mode) throws JMSException {
		if (!this.active) {
			return null;
		}

		Deque<Session> sessionList = this.cachedSessions.computeIfAbsent(mode, k -> new ArrayDeque<>());
		Session session = null;
		synchronized (sessionList) {
			if (!sessionList.isEmpty()) {
				session = sessionList.removeFirst();
			}
		}
		if (session != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found cached JMS Session for mode " + mode + ": " +
						(session instanceof SessionProxy ? ((SessionProxy) session).getTargetSession() : session));
			}
		}
		else {
			Session targetSession = createSession(con, mode);
			if (logger.isDebugEnabled()) {
				logger.debug("Registering cached JMS Session for mode " + mode + ": " + targetSession);
			}
			session = getCachedSessionProxy(targetSession, sessionList);
		}
		return session;
	}

	/**
	 * 用代理包装给定的 Session，该代理将每个方法调用都委托给它，
	 * 但会适配 close 调用。这对于允许应用代码像处理普通 Session 一样
	 * 处理特殊的框架 Session 很有用。
	 * @param target the original Session to wrap
	 * @param sessionList the List of cached Sessions that the given Session belongs to
	 * @return the wrapped Session
	 */
	protected Session getCachedSessionProxy(Session target, Deque<Session> sessionList) {
		List<Class<?>> classes = new ArrayList<>(3);
		classes.add(SessionProxy.class);
		if (target instanceof QueueSession) {
			classes.add(QueueSession.class);
		}
		if (target instanceof TopicSession) {
			classes.add(TopicSession.class);
		}
		return (Session) Proxy.newProxyInstance(SessionProxy.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new CachedSessionInvocationHandler(target, sessionList));
	}


	/**
	 * 缓存的 JMS Session 代理的调用处理器。
	 */
	private class CachedSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		private final Deque<Session> sessionList;

		private final Map<DestinationCacheKey, MessageProducer> cachedProducers = new HashMap<>();

		private final Map<ConsumerCacheKey, MessageConsumer> cachedConsumers = new HashMap<>();

		private boolean transactionOpen = false;

		public CachedSessionInvocationHandler(Session target, Deque<Session> sessionList) {
			this.target = target;
			this.sessionList = sessionList;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (methodName.equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (methodName.equals("hashCode")) {
				// Use hashCode of Session proxy.
				return System.identityHashCode(proxy);
			}
			else if (methodName.equals("toString")) {
				return "Cached JMS Session: " + this.target;
			}
			else if (methodName.equals("close")) {
				// Handle close method: don't pass the call on.
				if (active) {
					synchronized (this.sessionList) {
						if (this.sessionList.size() < getSessionCacheSize()) {
							try {
								logicalClose((Session) proxy);
								// Remain open in the session list.
								return null;
							}
							catch (JMSException ex) {
								logger.trace("Logical close of cached JMS Session failed - discarding it", ex);
								// Proceed to physical close from here...
							}
						}
					}
				}
				// If we get here, we're supposed to shut down.
				physicalClose();
				return null;
			}
			else if (methodName.equals("getTargetSession")) {
				// Handle getTargetSession method: return underlying Session.
				return this.target;
			}
			else if (methodName.equals("commit") || methodName.equals("rollback")) {
				this.transactionOpen = false;
			}
			else if (methodName.startsWith("create")) {
				this.transactionOpen = true;
				if (isCacheProducers() && (methodName.equals("createProducer") ||
						methodName.equals("createSender") || methodName.equals("createPublisher"))) {
					// Destination argument being null is ok for a producer
					Destination dest = (Destination) args[0];
					if (!(dest instanceof TemporaryQueue || dest instanceof TemporaryTopic)) {
						return getCachedProducer(dest);
					}
				}
				else if (isCacheConsumers()) {
					// let raw JMS invocation throw an exception if Destination (i.e. args[0]) is null
					if ((methodName.equals("createConsumer") || methodName.equals("createReceiver") ||
							methodName.equals("createSubscriber"))) {
						Destination dest = (Destination) args[0];
						if (dest != null && !(dest instanceof TemporaryQueue || dest instanceof TemporaryTopic)) {
							return getCachedConsumer(dest,
									(args.length > 1 ? (String) args[1] : null),
									(args.length > 2 && (Boolean) args[2]),
									null,
									false);
						}
					}
					else if (methodName.equals("createDurableConsumer") || methodName.equals("createDurableSubscriber")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									(args.length > 3 && (Boolean) args[3]),
									(String) args[1],
									true);
						}
					}
					else if (methodName.equals("createSharedConsumer")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									null,
									(String) args[1],
									false);
						}
					}
					else if (methodName.equals("createSharedDurableConsumer")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									null,
									(String) args[1],
									true);
						}
					}
				}
			}
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private MessageProducer getCachedProducer(@Nullable Destination dest) throws JMSException {
			DestinationCacheKey cacheKey = (dest != null ? new DestinationCacheKey(dest) : null);
			MessageProducer producer = this.cachedProducers.get(cacheKey);
			if (producer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
			}
			else {
				producer = this.target.createProducer(dest);
				if (logger.isDebugEnabled()) {
					logger.debug("Registering cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
				this.cachedProducers.put(cacheKey, producer);
			}
			return new CachedMessageProducer(producer);
		}

		@SuppressWarnings("resource")
		private MessageConsumer getCachedConsumer(Destination dest, @Nullable String selector,
				@Nullable Boolean noLocal, @Nullable String subscription, boolean durable) throws JMSException {

			ConsumerCacheKey cacheKey = new ConsumerCacheKey(dest, selector, noLocal, subscription, durable);
			MessageConsumer consumer = this.cachedConsumers.get(cacheKey);
			if (consumer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
			}
			else {
				if (dest instanceof Topic) {
					if (noLocal == null) {
						consumer = (durable ?
								this.target.createSharedDurableConsumer((Topic) dest, subscription, selector) :
								this.target.createSharedConsumer((Topic) dest, subscription, selector));
					}
					else {
						consumer = (durable ?
								this.target.createDurableSubscriber((Topic) dest, subscription, selector, noLocal) :
								this.target.createConsumer(dest, selector, noLocal));
					}
				}
				else {
					consumer = this.target.createConsumer(dest, selector);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Registering cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
				this.cachedConsumers.put(cacheKey, consumer);
			}
			return new CachedMessageConsumer(consumer);
		}

		private void logicalClose(Session proxy) throws JMSException {
			// Preserve rollback-on-close semantics.
			if (this.transactionOpen && this.target.getTransacted()) {
				this.transactionOpen = false;
				this.target.rollback();
			}
			// Physically close durable subscribers at time of Session close call.
			for (Iterator<Map.Entry<ConsumerCacheKey, MessageConsumer>> it = this.cachedConsumers.entrySet().iterator(); it.hasNext();) {
				Map.Entry<ConsumerCacheKey, MessageConsumer> entry = it.next();
				if (entry.getKey().subscription != null) {
					entry.getValue().close();
					it.remove();
				}
			}
			// Allow for multiple close calls...
			boolean returned = false;
			synchronized (this.sessionList) {
				if (!this.sessionList.contains(proxy)) {
					this.sessionList.addLast(proxy);
					returned = true;
				}
			}
			if (returned && logger.isTraceEnabled()) {
				logger.trace("Returned cached Session: " + this.target);
			}
		}

		private void physicalClose() throws JMSException {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing cached Session: " + this.target);
			}
			// Explicitly close all MessageProducers and MessageConsumers that
			// this Session happens to cache...
			try {
				for (MessageProducer producer : this.cachedProducers.values()) {
					producer.close();
				}
				for (MessageConsumer consumer : this.cachedConsumers.values()) {
					consumer.close();
				}
			}
			finally {
				this.cachedProducers.clear();
				this.cachedConsumers.clear();
				// Now actually close the Session.
				this.target.close();
			}
		}
	}


	/**
	 * 围绕 Destination 引用的简单包装类。
	 * 在缓存 MessageProducer 对象时用作缓存键。
	 */
	private static class DestinationCacheKey implements Comparable<DestinationCacheKey> {

		private final Destination destination;

		@Nullable
		private String destinationString;

		public DestinationCacheKey(Destination destination) {
			Assert.notNull(destination, "Destination must not be null");
			this.destination = destination;
		}

		private String getDestinationString() {
			if (this.destinationString == null) {
				this.destinationString = this.destination.toString();
			}
			return this.destinationString;
		}

		protected boolean destinationEquals(DestinationCacheKey otherKey) {
			return (this.destination.getClass() == otherKey.destination.getClass() &&
					(this.destination.equals(otherKey.destination) ||
							getDestinationString().equals(otherKey.getDestinationString())));
		}

		@Override
		public boolean equals(@Nullable Object other) {
			// Effectively checking object equality as well as toString equality.
			// On WebSphere MQ, Destination objects do not implement equals...
			return (this == other || (other instanceof DestinationCacheKey &&
					destinationEquals((DestinationCacheKey) other)));
		}

		@Override
		public int hashCode() {
			// Can't use a more specific hashCode since we can't rely on
			// this.destination.hashCode() actually being the same value
			// for equivalent destinations... Thanks a lot, WebSphere MQ!
			return this.destination.getClass().hashCode();
		}

		@Override
		public String toString() {
			return getDestinationString();
		}

		@Override
		public int compareTo(DestinationCacheKey other) {
			return getDestinationString().compareTo(other.getDestinationString());
		}
	}


	/**
	 * 围绕 Destination 及其他消费者属性的简单包装类。
	 * 在缓存 MessageConsumer 对象时用作缓存键。
	 */
	private static class ConsumerCacheKey extends DestinationCacheKey {

		@Nullable
		private final String selector;

		@Nullable
		private final Boolean noLocal;

		@Nullable
		private final String subscription;

		private final boolean durable;

		public ConsumerCacheKey(Destination destination, @Nullable String selector, @Nullable Boolean noLocal,
				@Nullable String subscription, boolean durable) {

			super(destination);
			this.selector = selector;
			this.noLocal = noLocal;
			this.subscription = subscription;
			this.durable = durable;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ConsumerCacheKey)) {
				return false;
			}
			ConsumerCacheKey otherKey = (ConsumerCacheKey) other;
			return (destinationEquals(otherKey) &&
					ObjectUtils.nullSafeEquals(this.selector, otherKey.selector) &&
					ObjectUtils.nullSafeEquals(this.noLocal, otherKey.noLocal) &&
					ObjectUtils.nullSafeEquals(this.subscription, otherKey.subscription) &&
					this.durable == otherKey.durable);
		}

		@Override
		public int hashCode() {
			return (31 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.selector));
		}

		@Override
		public String toString() {
			return super.toString() + " [selector=" + this.selector + ", noLocal=" + this.noLocal +
					", subscription=" + this.subscription + ", durable=" + this.durable + "]";
		}
	}

}
