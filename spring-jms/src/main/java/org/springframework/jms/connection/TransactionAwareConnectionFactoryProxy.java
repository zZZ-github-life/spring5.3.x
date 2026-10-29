/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TransactionInProgressException;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 目标 JMS {@link javax.jms.ConnectionFactory} 的代理，增加了对
 * Spring 管理事务的感知。类似于 Java EE 应用服务器提供的事务性 JNDI ConnectionFactory。
 *
 * <p>无需感知 Spring JMS 支持的消息代码可以使用此代理，无缝参与
 * Spring 管理的事务。请注意，事务管理器（例如 {@link JmsTransactionManager}）
 * 仍需要与底层 ConnectionFactory 协作，<i>而不要</i>与此外层代理协作。
 *
 * <p><b>请确保 TransactionAwareConnectionFactoryProxy 位于 ConnectionFactory
 * 代理/适配器链的最外层。</b>TransactionAwareConnectionFactoryProxy 可以直接委托给
 * 目标工厂，也可以委托给某些中间适配器，如
 * {@link UserCredentialsConnectionFactoryAdapter}。
 *
 * <p>委托给 {@link ConnectionFactoryUtils} 以自动参与线程绑定的事务，
 * 例如由 {@link JmsTransactionManager} 管理的事务。对返回的 Session 调用
 * {@code createSession} 和 {@code close} 时，在事务内会正确行事，即始终操作
 * 事务性 Session。如果不在事务中，则适用正常的 ConnectionFactory 行为。
 *
 * <p>请注意，事务性 JMS Session 将按 Connection 注册。要在事务中共享同一个
 * JMS Session，请确保操作的是同一个 JMS Connection 句柄——要么复用该句柄，
 * 要么在底层配置一个 {@link SingleConnectionFactory}。
 *
 * <p>返回的事务性 Session 代理将实现 {@link SessionProxy} 接口，
 * 以允许访问底层的目标 Session。这仅用于访问厂商特定的 Session API
 * 或测试目的（例如执行手动事务控制）。对于典型的应用用途，
 * 只需使用标准的 JMS Session 接口即可。
 *
 * <p>从 Spring Framework 5 开始，此类委托 JMS 2.0 {@code JMSContext} 调用，
 * 因此要求运行时存在 JMS 2.0 API。不过，只要应用设置不触发实际的 JMS 2.0 调用，
 * 它仍然可以运行在 JMS 1.1 驱动之上（绑定到 JMS 2.0 API）。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see UserCredentialsConnectionFactoryAdapter
 * @see SingleConnectionFactory
 */
public class TransactionAwareConnectionFactoryProxy
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {

	@Nullable
	private ConnectionFactory targetConnectionFactory;

	private boolean synchedLocalTransactionAllowed = false;


	/**
	 * 创建一个新的 TransactionAwareConnectionFactoryProxy。
	 */
	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * 创建一个新的 TransactionAwareConnectionFactoryProxy。
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
	}


	/**
	 * 设置此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	public final void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	protected ConnectionFactory getTargetConnectionFactory() {
		ConnectionFactory target = this.targetConnectionFactory;
		Assert.state(target != null, "'targetConnectionFactory' is required");
		return target;
	}

	/**
	 * 设置是否允许与 Spring 管理的事务同步的本地 JMS 事务
	 * （其中主事务可能是针对特定 DataSource 的基于 JDBC 的事务等），
	 * 该 JMS 事务在主事务之后立即提交。如果不允许，给定的 ConnectionFactory
	 * 需要在内部处理事务登记。
	 * <p>默认值为 "false"：如果不在涵盖底层 JMS ConnectionFactory 的托管事务中，
	 * 将返回标准 Session。启用此标志可允许参与任何 Spring 管理的事务，
	 * 并与主事务同步一个本地 JMS 事务。
	 */
	public void setSynchedLocalTransactionAllowed(boolean synchedLocalTransactionAllowed) {
		this.synchedLocalTransactionAllowed = synchedLocalTransactionAllowed;
	}

	/**
	 * 返回是否允许与 Spring 管理的事务同步的本地 JMS 事务。
	 */
	protected boolean isSynchedLocalTransactionAllowed() {
		return this.synchedLocalTransactionAllowed;
	}


	@Override
	public Connection createConnection() throws JMSException {
		Connection targetConnection = getTargetConnectionFactory().createConnection();
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		Connection targetConnection = getTargetConnectionFactory().createConnection(username, password);
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection = ((QueueConnectionFactory) target).createQueueConnection();
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection = ((QueueConnectionFactory) target).createQueueConnection(username, password);
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection = ((TopicConnectionFactory) target).createTopicConnection();
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		ConnectionFactory target = getTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection = ((TopicConnectionFactory) target).createTopicConnection(username, password);
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public JMSContext createContext() {
		return getTargetConnectionFactory().createContext();
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return getTargetConnectionFactory().createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return getTargetConnectionFactory().createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return getTargetConnectionFactory().createContext(sessionMode);
	}


	/**
	 * 用代理包装给定的 Connection，该代理将每个方法调用都委托给它，
	 * 但以事务感知的方式处理 Session 查找。
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new TransactionAwareConnectionInvocationHandler(target));
	}


	/**
	 * 为底层 Connection 公开事务性 Session 的调用处理器。
	 */
	private class TransactionAwareConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		public TransactionAwareConnectionInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
			}

			if (Session.class == method.getReturnType()) {
				Session session = ConnectionFactoryUtils.getTransactionalSession(
						getTargetConnectionFactory(), this.target, isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (QueueSession.class == method.getReturnType()) {
				QueueSession session = ConnectionFactoryUtils.getTransactionalQueueSession(
						(QueueConnectionFactory) getTargetConnectionFactory(), (QueueConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (TopicSession.class == method.getReturnType()) {
				TopicSession session = ConnectionFactoryUtils.getTransactionalTopicSession(
						(TopicConnectionFactory) getTargetConnectionFactory(), (TopicConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private Session getCloseSuppressingSessionProxy(Session target) {
			List<Class<?>> classes = new ArrayList<>(3);
			classes.add(SessionProxy.class);
			if (target instanceof QueueSession) {
				classes.add(QueueSession.class);
			}
			if (target instanceof TopicSession) {
				classes.add(TopicSession.class);
			}
			return (Session) Proxy.newProxyInstance(SessionProxy.class.getClassLoader(),
					ClassUtils.toClassArray(classes), new CloseSuppressingSessionInvocationHandler(target));
		}
	}


	/**
	 * 抑制事务性 JMS Session 的 close 调用的调用处理器。
	 */
	private static class CloseSuppressingSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingSessionInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on SessionProxy interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				case "commit":
					throw new TransactionInProgressException("Commit call not allowed within a managed transaction");
				case "rollback":
					throw new TransactionInProgressException("Rollback call not allowed within a managed transaction");
				case "close":
					// Handle close method: not to be closed within a transaction.
					return null;
				case "getTargetSession":
					// Handle getTargetSession method: return underlying Session.
					return this.target;
			}

			// Invoke method on target Session.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
