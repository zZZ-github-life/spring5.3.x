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
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 目标 JMS {@link javax.jms.ConnectionFactory} 的适配器，为每次标准的
 * {@code createConnection()} 调用应用给定的用户凭据，即在目标上隐式调用
 * {@code createConnection(username, password)}。所有其他方法只是委托给目标
 * ConnectionFactory 的相应方法。
 *
 * <p>可用于代理未配置用户凭据的目标 JNDI ConnectionFactory。
 * 客户端代码可以使用该 ConnectionFactory，而无需在每次
 * {@code createConnection()} 调用时传入用户名和密码。
 *
 * <p>在下面的示例中，客户端代码可以直接透明地使用预配置的
 * "myConnectionFactory"，并以指定的用户凭据隐式访问
 * "myTargetConnectionFactory"。
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jms/mycf"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果 "username" 为空，此代理将仅委托给目标 ConnectionFactory 的标准
 * {@code createConnection()} 方法。这样可以将 UserCredentialsConnectionFactoryAdapter
 * bean 定义仅保留为<i>可选</i>地隐式传递用户凭据，以备特定目标
 * ConnectionFactory 需要时使用。
 *
 * <p>从 Spring Framework 5 开始，此类委托 JMS 2.0 {@code JMSContext} 调用，
 * 因此要求运行时存在 JMS 2.0 API。不过，只要应用设置不触发实际的 JMS 2.0 调用，
 * 它仍然可以运行在 JMS 1.1 驱动之上（绑定到 JMS 2.0 API）。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #createConnection
 * @see #createQueueConnection
 * @see #createTopicConnection
 */
public class UserCredentialsConnectionFactoryAdapter
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	@Nullable
	private ConnectionFactory targetConnectionFactory;

	@Nullable
	private String username;

	@Nullable
	private String password;

	private final ThreadLocal<JmsUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<>("Current JMS user credentials");


	/**
	 * 设置此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 设置此适配器用于获取 Connection 的用户名。
	 * 默认不指定特定用户。
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 设置此适配器用于获取 Connection 的密码。
	 * 默认不指定特定密码。
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	/**
	 * 为此代理和当前线程设置用户凭据。
	 * 给定的用户名和密码将应用于此后在此 ConnectionFactory 代理上的
	 * 所有 {@code createConnection()} 调用。
	 * <p>这将覆盖任何静态指定的用户凭据，即 "username" 和 "password"
	 * bean 属性的值。
	 * @param username the username to apply
	 * @param password the password to apply
	 * @see #removeCredentialsFromCurrentThread
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JmsUserCredentials(username, password));
	}

	/**
	 * 从当前线程移除此代理的任何用户凭据。
	 * 此后将重新应用静态指定的用户凭据。
	 * @see #setCredentialsForCurrentThread
	 */
	public void removeCredentialsFromCurrentThread() {
		this.threadBoundCredentials.remove();
	}


	/**
	 * 判断当前是否存在线程绑定的凭据，如果存在则使用它们，
	 * 否则回退到静态指定的用户名和密码（即 bean 属性的值）。
	 * @see #doCreateConnection
	 */
	@Override
	public final Connection createConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标 ConnectionFactory。
	 */
	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return doCreateConnection(username, password);
	}

	/**
	 * 此实现委托给目标 ConnectionFactory 的
	 * {@code createConnection(username, password)} 方法，并传入指定的用户凭据。
	 * 如果指定的用户名为空，则仅委托给目标 ConnectionFactory 的标准
	 * {@code createConnection()} 方法。
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see javax.jms.ConnectionFactory#createConnection(String, String)
	 * @see javax.jms.ConnectionFactory#createConnection()
	 */
	protected Connection doCreateConnection(@Nullable String username, @Nullable String password) throws JMSException {
		ConnectionFactory target = obtainTargetConnectionFactory();
		if (StringUtils.hasLength(username)) {
			return target.createConnection(username, password);
		}
		else {
			return target.createConnection();
		}
	}

	/**
	 * 判断当前是否存在线程绑定的凭据，如果存在则使用它们，
	 * 否则回退到静态指定的用户名和密码（即 bean 属性的值）。
	 * @see #doCreateQueueConnection
	 */
	@Override
	public final QueueConnection createQueueConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateQueueConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateQueueConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标 QueueConnectionFactory。
	 */
	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		return doCreateQueueConnection(username, password);
	}

	/**
	 * 此实现委托给目标 QueueConnectionFactory 的
	 * {@code createQueueConnection(username, password)} 方法，并传入指定的用户凭据。
	 * 如果指定的用户名为空，则仅委托给目标 ConnectionFactory 的标准
	 * {@code createQueueConnection()} 方法。
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see javax.jms.QueueConnectionFactory#createQueueConnection(String, String)
	 * @see javax.jms.QueueConnectionFactory#createQueueConnection()
	 */
	protected QueueConnection doCreateQueueConnection(
			@Nullable String username, @Nullable String password) throws JMSException {

		ConnectionFactory target = obtainTargetConnectionFactory();
		if (!(target instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
		}
		QueueConnectionFactory queueFactory = (QueueConnectionFactory) target;
		if (StringUtils.hasLength(username)) {
			return queueFactory.createQueueConnection(username, password);
		}
		else {
			return queueFactory.createQueueConnection();
		}
	}

	/**
	 * 判断当前是否存在线程绑定的凭据，如果存在则使用它们，
	 * 否则回退到静态指定的用户名和密码（即 bean 属性的值）。
	 * @see #doCreateTopicConnection
	 */
	@Override
	public final TopicConnection createTopicConnection() throws JMSException {
		JmsUserCredentials threadCredentials = this.threadBoundCredentials.get();
		if (threadCredentials != null) {
			return doCreateTopicConnection(threadCredentials.username, threadCredentials.password);
		}
		else {
			return doCreateTopicConnection(this.username, this.password);
		}
	}

	/**
	 * 将调用直接委托给目标 TopicConnectionFactory。
	 */
	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		return doCreateTopicConnection(username, password);
	}

	/**
	 * 此实现委托给目标 TopicConnectionFactory 的
	 * {@code createTopicConnection(username, password)} 方法，并传入指定的用户凭据。
	 * 如果指定的用户名为空，则仅委托给目标 ConnectionFactory 的标准
	 * {@code createTopicConnection()} 方法。
	 * @param username the username to use
	 * @param password the password to use
	 * @return the Connection
	 * @see javax.jms.TopicConnectionFactory#createTopicConnection(String, String)
	 * @see javax.jms.TopicConnectionFactory#createTopicConnection()
	 */
	protected TopicConnection doCreateTopicConnection(
			@Nullable String username, @Nullable String password) throws JMSException {

		ConnectionFactory target = obtainTargetConnectionFactory();
		if (!(target instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
		}
		TopicConnectionFactory queueFactory = (TopicConnectionFactory) target;
		if (StringUtils.hasLength(username)) {
			return queueFactory.createTopicConnection(username, password);
		}
		else {
			return queueFactory.createTopicConnection();
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

	private ConnectionFactory obtainTargetConnectionFactory() {
		Assert.state(this.targetConnectionFactory != null, "'targetConnectionFactory' is required");
		return this.targetConnectionFactory;
	}


	/**
	 * 用作 ThreadLocal 值的内部类。
	 */
	private static final class JmsUserCredentials {

		public final String username;

		public final String password;

		public JmsUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JmsUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}

}
