/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * JMS ConnectionFactory 适配器，对所有 {@link #createConnection()} 调用
 * 返回同一个 Connection，并忽略对 {@link javax.jms.Connection#close()} 的调用。
 * 根据 JMS Connection 模型，这完全是线程安全的（与 JDBC 等不同）。
 * 共享的 Connection 在发生异常时可以自动恢复。
 *
 * <p>你可以直接传入一个特定的 JMS Connection，也可以让此工厂通过给定的
 * 目标 ConnectionFactory 延迟创建 Connection。此工厂通常既适用于
 * JMS 1.1，也适用于 JMS 1.0.2 API。
 *
 * <p>注意，使用 JMS 1.0.2 API 时，此 ConnectionFactory 会根据运行时使用的
 * JMS API 方法切换到队列/主题模式：{@code createQueueConnection} 和
 * {@code createTopicConnection} 分别对应队列模式和主题模式；通用的
 * {@code createConnection} 调用会得到一个能够同时服务两种模式的 JMS 1.1 连接。
 *
 * <p>从 Spring Framework 5 开始，此类支持 JMS 2.0 {@code JMSContext} 调用，
 * 因此要求运行时存在 JMS 2.0 API。不过，只要应用设置不触发实际的 JMS 2.0 调用，
 * 它仍然可以运行在 JMS 1.1 驱动之上（绑定到 JMS 2.0 API）。
 *
 * <p>适用于测试和独立环境，以便在多次
 * {@link org.springframework.jms.core.JmsTemplate} 调用中持续使用同一个
 * Connection，而无需在底层使用池化的 ConnectionFactory。
 * 这可以跨越任意数量的事务，甚至是并发执行的事务。
 *
 * <p>注意，Spring 的消息监听器容器支持在每个监听器容器实例内使用共享的
 * Connection。只有<i>跨多个监听器容器</i>共享单个 JMS Connection 时，
 * 结合使用 SingleConnectionFactory 才真正有意义。
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate
 * @see org.springframework.jms.listener.SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setCacheLevel
 */
public class SingleConnectionFactory implements ConnectionFactory, QueueConnectionFactory,
		TopicConnectionFactory, ExceptionListener, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConnectionFactory targetConnectionFactory;

	@Nullable
	private String clientId;

	@Nullable
	private ExceptionListener exceptionListener;

	private boolean reconnectOnException = false;

	/** 目标 Connection。 */
	@Nullable
	private Connection connection;

	/** 提示应创建队列连接还是主题连接。 */
	@Nullable
	private Boolean pubSubMode;

	/** 内部聚合器，允许为每个连接提供 ExceptionListener。 */
	@Nullable
	private AggregatedExceptionListener aggregatedExceptionListener;

	/** 共享 Connection 是否已启动。 */
	private int startedCount = 0;

	/** 共享 Connection 的同步监视器。 */
	private final Object connectionMonitor = new Object();


	/**
	 * 创建一个用于 bean 风格用法的新 SingleConnectionFactory。
	 * @see #setTargetConnectionFactory
	 */
	public SingleConnectionFactory() {
	}

	/**
	 * 创建一个始终返回给定 Connection 的新 SingleConnectionFactory。
	 * @param targetConnection the single Connection
	 */
	public SingleConnectionFactory(Connection targetConnection) {
		Assert.notNull(targetConnection, "Target Connection must not be null");
		this.connection = targetConnection;
	}

	/**
	 * 创建一个新的 SingleConnectionFactory，它始终返回同一个 Connection，
	 * 并会通过给定的目标 ConnectionFactory 延迟创建该 Connection。
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * 设置目标 ConnectionFactory，将用于延迟创建单个 Connection。
	 */
	public void setTargetConnectionFactory(@Nullable ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回目标 ConnectionFactory（如果有），将用于延迟创建单个 Connection。
	 */
	@Nullable
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * 为由此 ConnectionFactory 创建并公开的单个 Connection 指定 JMS 客户端 ID。
	 * <p>请注意，客户端 ID 在底层 JMS 提供者的所有活动 Connection 中必须唯一。
	 * 此外，只有在原始 ConnectionFactory 尚未分配客户端 ID 时才能分配。
	 * @see javax.jms.Connection#setClientID
	 * @see #setTargetConnectionFactory
	 */
	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	/**
	 * 返回由此 ConnectionFactory 创建并公开的单个 Connection 的 JMS 客户端 ID
	 * （如果有）。
	 */
	@Nullable
	protected String getClientId() {
		return this.clientId;
	}

	/**
	 * 指定一个 JMS ExceptionListener 实现，应将其注册到此工厂创建的单个 Connection。
	 * @see #setReconnectOnException
	 */
	public void setExceptionListener(@Nullable ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * 返回应注册到此工厂创建的单个 Connection 的 JMS ExceptionListener 实现
	 * （如果有）。
	 */
	@Nullable
	protected ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * 指定当底层 Connection 报告 JMSException 时，是否应重置单个 Connection
	 * （以便随后重新获取）。
	 * <p>默认值为 "false"。将其切换为 "true" 可根据 JMS 提供者的异常通知
	 * 自动触发恢复。
	 * <p>在内部，这会导致向底层 Connection 注册一个特殊的 JMS ExceptionListener
	 * （即此 SingleConnectionFactory 本身）。如有需要，这也可以与用户指定的
	 * ExceptionListener 组合使用。
	 * @see #setExceptionListener
	 */
	public void setReconnectOnException(boolean reconnectOnException) {
		this.reconnectOnException = reconnectOnException;
	}

	/**
	 * 返回当底层 Connection 报告 JMSException 时是否应更新单个 Connection。
	 */
	protected boolean isReconnectOnException() {
		return this.reconnectOnException;
	}

	/**
	 * 确保已设置 Connection 或 ConnectionFactory。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Target Connection or ConnectionFactory is required");
		}
	}


	@Override
	public Connection createConnection() throws JMSException {
		return getSharedConnectionProxy(getConnection());
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.FALSE;
			con = createConnection();
		}
		if (!(con instanceof QueueConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a QueueConnection but rather: " + con);
		}
		return ((QueueConnection) con);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.TRUE;
			con = createConnection();
		}
		if (!(con instanceof TopicConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a TopicConnection but rather: " + con);
		}
		return ((TopicConnection) con);
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public JMSContext createContext() {
		return obtainTargetConnectionFactory().createContext();
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return obtainTargetConnectionFactory().createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return obtainTargetConnectionFactory().createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return obtainTargetConnectionFactory().createContext(sessionMode);
	}

	private ConnectionFactory obtainTargetConnectionFactory() {
		ConnectionFactory target = getTargetConnectionFactory();
		Assert.state(target != null, "'targetConnectionFactory' is required");
		return target;
	}


	/**
	 * 获取一个已初始化的共享 Connection。
	 * @return the Connection (never {@code null})
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @see #initConnection()
	 */
	protected Connection getConnection() throws JMSException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	/**
	 * 初始化底层的共享 Connection。
	 * <p>如果底层 Connection 已存在，则关闭并重新初始化它。
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @see #prepareConnection
	 */
	public void initConnection() throws JMSException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			// Create new (method local) connection, which is later assigned to instance connection
			//  - prevention to hold instance connection without exception listener, in case when
			//    some subsequent methods (after creation of connection) throws JMSException
			Connection con = doCreateConnection();
			try {
				prepareConnection(con);
				this.connection = con;
			}
			catch (JMSException ex) {
				// Attempt to close new (not used) connection to release possible resources
				try {
					con.close();
				}
				catch(Throwable th) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not close newly obtained JMS Connection that failed to prepare", th);
					}
				}
				throw ex;
			}
			if (this.startedCount > 0) {
				this.connection.start();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Established shared JMS Connection: " + this.connection);
			}
		}
	}

	/**
	 * 异常监听器回调，用于更新底层的单个 Connection。
	 * @see #resetConnection()
	 */
	@Override
	public void onException(JMSException ex) {
		logger.info("Encountered a JMSException - resetting the underlying JMS Connection", ex);
		resetConnection();
	}

	/**
	 * 关闭底层的共享连接。
	 * 此 ConnectionFactory 的提供者需要负责正确关闭。
	 * <p>由于此 bean 实现了 DisposableBean，bean 工厂将在销毁其缓存的单例时
	 * 自动调用此方法。
	 * @see #resetConnection()
	 */
	@Override
	public void destroy() {
		resetConnection();
	}

	/**
	 * 重置底层的共享 Connection，以便在下次访问时重新初始化。
	 * @see #closeConnection
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			this.connection = null;
		}
	}

	/**
	 * 通过此模板的 ConnectionFactory 创建 JMS Connection。
	 * @return the new JMS Connection
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection doCreateConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (Boolean.FALSE.equals(this.pubSubMode) && cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection();
		}
		else {
			return obtainTargetConnectionFactory().createConnection();
		}
	}

	/**
	 * 在公开之前准备给定的 Connection。
	 * <p>默认实现会应用 ExceptionListener 和客户端 ID。可在子类中重写。
	 * @param con the Connection to prepare
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setExceptionListener
	 * @see #setReconnectOnException
	 */
	protected void prepareConnection(Connection con) throws JMSException {
		if (getClientId() != null) {
			con.setClientID(getClientId());
		}
		if (this.aggregatedExceptionListener != null) {
			con.setExceptionListener(this.aggregatedExceptionListener);
		}
		else if (getExceptionListener() != null || isReconnectOnException()) {
			ExceptionListener listenerToUse = getExceptionListener();
			if (isReconnectOnException()) {
				this.aggregatedExceptionListener = new AggregatedExceptionListener();
				this.aggregatedExceptionListener.delegates.add(this);
				if (listenerToUse != null) {
					this.aggregatedExceptionListener.delegates.add(listenerToUse);
				}
				listenerToUse = this.aggregatedExceptionListener;
			}
			con.setExceptionListener(listenerToUse);
		}
	}

	/**
	 * 用于获取（可能已缓存的）Session 的模板方法。
	 * <p>默认实现始终返回 {@code null}。子类可以重写此方法以公开特定的
	 * Session 句柄，也可以委托给 {@link #createSession} 创建原始 Session 对象，
	 * 然后在此处包装并返回。
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * ({@code Session.TRANSACTED} or one of the common modes)
	 * @return the Session to use, or {@code null} to indicate
	 * creation of a raw standard Session
	 * @throws JMSException if thrown by the JMS API
	 */
	@Nullable
	protected Session getSession(Connection con, Integer mode) throws JMSException {
		return null;
	}

	/**
	 * 为此 ConnectionFactory 创建默认 Session，
	 * 必要时适配 JMS 1.0.2 风格的队列/主题模式。
	 * @param con the JMS Connection to operate on
	 * @param mode the Session acknowledgement mode
	 * ({@code Session.TRANSACTED} or one of the common modes)
	 * @return the newly created Session
	 * @throws JMSException if thrown by the JMS API
	 */
	protected Session createSession(Connection con, Integer mode) throws JMSException {
		// Determine JMS API arguments...
		boolean transacted = (mode == Session.SESSION_TRANSACTED);
		int ackMode = (transacted ? Session.AUTO_ACKNOWLEDGE : mode);
		// Now actually call the appropriate JMS factory method...
		if (Boolean.FALSE.equals(this.pubSubMode) && con instanceof QueueConnection) {
			return ((QueueConnection) con).createQueueSession(transacted, ackMode);
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && con instanceof TopicConnection) {
			return ((TopicConnection) con).createTopicSession(transacted, ackMode);
		}
		else {
			return con.createSession(transacted, ackMode);
		}
	}

	/**
	 * 关闭给定的 Connection。
	 * @param con the Connection to close
	 */
	protected void closeConnection(Connection con) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing shared JMS Connection: " + con);
		}
		try {
			try {
				if (this.startedCount > 0) {
					con.stop();
				}
			}
			finally {
				con.close();
			}
		}
		catch (javax.jms.IllegalStateException ex) {
			logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
		}
		catch (Throwable ex) {
			logger.warn("Could not close shared JMS Connection", ex);
		}
	}

	/**
	 * 用代理包装给定的 Connection，该代理将每个方法调用都委托给它，
	 * 但会抑制 close 调用。这对于允许应用代码像处理来自 JMS ConnectionFactory
	 * 的普通 Connection 一样处理特殊的框架 Connection 很有用。
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getSharedConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new SharedConnectionInvocationHandler());
	}


	/**
	 * 缓存的 JMS Connection 代理的调用处理器。
	 */
	private class SharedConnectionInvocationHandler implements InvocationHandler {

		@Nullable
		private ExceptionListener localExceptionListener;

		private boolean locallyStarted = false;

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					Object other = args[0];
					if (proxy == other) {
						return true;
					}
					if (other == null || !Proxy.isProxyClass(other.getClass())) {
						return false;
					}
					InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
					return (otherHandler instanceof SharedConnectionInvocationHandler &&
							factory() == ((SharedConnectionInvocationHandler) otherHandler).factory());
				case "hashCode":
					// Use hashCode of containing SingleConnectionFactory.
					return System.identityHashCode(factory());
				case "toString":
					return "Shared JMS Connection: " + getConnection();
				case "setClientID":
					// Handle setClientID method: throw exception if not compatible.
					String currentClientId = getConnection().getClientID();
					if (currentClientId != null && currentClientId.equals(args[0])) {
						return null;
					}
					else {
						throw new javax.jms.IllegalStateException(
								"setClientID call not supported on proxy for shared Connection. " +
								"Set the 'clientId' property on the SingleConnectionFactory instead.");
					}
				case "setExceptionListener":
					// Handle setExceptionListener method: add to the chain.
					synchronized (connectionMonitor) {
						if (aggregatedExceptionListener != null) {
							ExceptionListener listener = (ExceptionListener) args[0];
							if (listener != this.localExceptionListener) {
								if (this.localExceptionListener != null) {
									aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
								}
								if (listener != null) {
									aggregatedExceptionListener.delegates.add(listener);
								}
								this.localExceptionListener = listener;
							}
							return null;
						}
						else {
							throw new javax.jms.IllegalStateException(
									"setExceptionListener call not supported on proxy for shared Connection. " +
									"Set the 'exceptionListener' property on the SingleConnectionFactory instead. " +
									"Alternatively, activate SingleConnectionFactory's 'reconnectOnException' feature, " +
									"which will allow for registering further ExceptionListeners to the recovery chain.");
						}
					}
				case "getExceptionListener":
					synchronized (connectionMonitor) {
						if (this.localExceptionListener != null) {
							return this.localExceptionListener;
						}
						else {
							return getExceptionListener();
						}
					}
				case "start":
					localStart();
					return null;
				case "stop":
					localStop();
					return null;
				case "close":
					localStop();
					synchronized (connectionMonitor) {
						if (this.localExceptionListener != null) {
							if (aggregatedExceptionListener != null) {
								aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
							}
							this.localExceptionListener = null;
						}
					}
					return null;
				case "createSession":
				case "createQueueSession":
				case "createTopicSession":
					// Default: JMS 2.0 createSession() method
					Integer mode = Session.AUTO_ACKNOWLEDGE;
					if (!ObjectUtils.isEmpty(args)) {
						if (args.length == 1) {
							// JMS 2.0 createSession(int) method
							mode = (Integer) args[0];
						}
						else if (args.length == 2) {
							// JMS 1.1 createSession(boolean, int) method
							boolean transacted = (Boolean) args[0];
							Integer ackMode = (Integer) args[1];
							mode = (transacted ? Session.SESSION_TRANSACTED : ackMode);
						}
					}
					Session session = getSession(getConnection(), mode);
					if (session != null) {
						if (!method.getReturnType().isInstance(session)) {
							String msg = "JMS Session does not implement specific domain: " + session;
							try {
								session.close();
							}
							catch (Throwable ex) {
								logger.trace("Failed to close newly obtained JMS Session", ex);
							}
							throw new javax.jms.IllegalStateException(msg);
						}
						return session;
					}
			}

			try {
				return method.invoke(getConnection(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private void localStart() throws JMSException {
			synchronized (connectionMonitor) {
				if (!this.locallyStarted) {
					this.locallyStarted = true;
					if (startedCount == 0 && connection != null) {
						connection.start();
					}
					startedCount++;
				}
			}
		}

		private void localStop() throws JMSException {
			synchronized (connectionMonitor) {
				if (this.locallyStarted) {
					this.locallyStarted = false;
					if (startedCount == 1 && connection != null) {
						connection.stop();
					}
					if (startedCount > 0) {
						startedCount--;
					}
				}
			}
		}

		private SingleConnectionFactory factory() {
			return SingleConnectionFactory.this;
		}
	}


	/**
	 * 内部聚合的 ExceptionListener，用于将内部恢复监听器与用户指定的监听器组合起来。
	 */
	private class AggregatedExceptionListener implements ExceptionListener {

		final Set<ExceptionListener> delegates = new LinkedHashSet<>(2);

		@Override
		public void onException(JMSException ex) {
			// Iterate over temporary copy in order to avoid ConcurrentModificationException,
			// since listener invocations may in turn trigger registration of listeners...
			Set<ExceptionListener> copy;
			synchronized (connectionMonitor) {
				copy = new LinkedHashSet<>(this.delegates);
			}
			for (ExceptionListener listener : copy) {
				listener.onException(ex);
			}
		}
	}

}
