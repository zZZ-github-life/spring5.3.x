/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link javax.jms.ConnectionFactory} 实现，将所有调用委托给给定的目标
 * {@link javax.jms.ConnectionFactory}，并在必要时将特定的
 * {@code create(Queue/Topic)Connection} 调用适配到目标 ConnectionFactory
 * （例如，基于 JMS 1.0.2 API 的代码运行在通用 JMS 1.1 ConnectionFactory
 * 之上时，如 ActiveMQ 的 PooledConnectionFactory）。
 *
 * <p>从 Spring Framework 5 开始，此类支持 JMS 2.0 {@code JMSContext} 调用，
 * 因此要求运行时存在 JMS 2.0 API。不过，只要应用设置不触发实际的 JMS 2.0 调用，
 * 它仍然可以运行在 JMS 1.1 驱动之上（绑定到 JMS 2.0 API）。
 *
 * <p>此类允许被继承，子类只需重写那些不应简单委托给目标 ConnectionFactory
 * 的方法（如 {@link #createConnection()}）。
 *
 * <p>也可以按原样定义，用于包装特定的目标 ConnectionFactory，
 * 并通过 "shouldStopConnections" 标志指示从目标工厂获取的 Connection
 * 是否应在关闭前停止。对于某些连接池来说，后者可能是必要的，
 * 因为它们只是将释放的连接归还到池中，而不会在连接驻留池中时停止它们。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #createConnection()
 * @see #setShouldStopConnections
 * @see ConnectionFactoryUtils#releaseConnection
 */
public class DelegatingConnectionFactory
		implements SmartConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	@Nullable
	private ConnectionFactory targetConnectionFactory;

	private boolean shouldStopConnections = false;


	/**
	 * 设置此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	public void setTargetConnectionFactory(@Nullable ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此 ConnectionFactory 委托的目标 ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	private ConnectionFactory obtainTargetConnectionFactory() {
		ConnectionFactory target = getTargetConnectionFactory();
		Assert.state(target != null, "No 'targetConnectionFactory' set");
		return target;
	}

	/**
	 * 指示从目标工厂获取的 Connection 是否应在关闭前停止（"true"），还是仅关闭（"false"）。
	 * 对于某些连接池来说，额外调用 stop 可能是必要的，因为它们只是将释放的连接
	 * 归还到池中，而不会在连接驻留池中时停止它们。
	 * <p>默认值为 "false"，即仅关闭 Connection。
	 * @see ConnectionFactoryUtils#releaseConnection
	 */
	public void setShouldStopConnections(boolean shouldStopConnections) {
		this.shouldStopConnections = shouldStopConnections;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection createConnection() throws JMSException {
		return obtainTargetConnectionFactory().createConnection();
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return obtainTargetConnectionFactory().createConnection(username, password);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (target instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) target).createQueueConnection();
		}
		else {
			Connection con = target.createConnection();
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (target instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) target).createQueueConnection(username, password);
		}
		else {
			Connection con = target.createConnection(username, password);
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (target instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) target).createTopicConnection();
		}
		else {
			Connection con = target.createConnection();
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (target instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) target).createTopicConnection(username, password);
		}
		else {
			Connection con = target.createConnection(username, password);
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
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

	@Override
	public boolean shouldStop(Connection con) {
		return this.shouldStopConnections;
	}

}
