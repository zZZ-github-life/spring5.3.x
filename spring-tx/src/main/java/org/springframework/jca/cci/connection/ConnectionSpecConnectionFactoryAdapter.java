/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jca.cci.connection;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 目标 CCI {@link javax.resource.cci.ConnectionFactory} 的适配器，
 * 将给定的 ConnectionSpec 应用于每次标准 {@code getConnection()} 调用，
 * 即隐式调用目标上的 {@code getConnection(ConnectionSpec)}。
 * 所有其他方法简单地委托给目标 ConnectionFactory 的对应方法。
 *
 * <p>可用于代理未配置 ConnectionSpec 的目标 JNDI ConnectionFactory。
 * 客户端代码可以操作 ConnectionFactory，而无需在每次
 * {@code getConnection()} 调用时传入 ConnectionSpec。
 *
 * <p>在以下示例中，客户端代码可以简单地透明地使用预配置的 "myConnectionFactory"，
 * 隐式使用指定的用户凭据访问 "myTargetConnectionFactory"。
 *
 * <pre class="code">
 * &lt;bean id="myTargetConnectionFactory" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/cci/mycf"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myConnectionFactory" class="org.springframework.jca.cci.connection.ConnectionSpecConnectionFactoryAdapter"&gt;
 *   &lt;property name="targetConnectionFactory" ref="myTargetConnectionFactory"/&gt;
 *   &lt;property name="connectionSpec"&gt;
 *     &lt;bean class="your.resource.adapter.ConnectionSpecImpl"&gt;
 *       &lt;property name="username" value="myusername"/&gt;
 *       &lt;property name="password" value="mypassword"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果 "connectionSpec" 为空，此代理将简单地委托给目标 ConnectionFactory 的标准
 * {@code getConnection()} 方法。这可以用于保留 UserCredentialsConnectionFactoryAdapter bean 定义，
 * 仅作为在特定目标 ConnectionFactory 需要时隐式传入 ConnectionSpec 的<i>选项</i>。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class ConnectionSpecConnectionFactoryAdapter extends DelegatingConnectionFactory {

	@Nullable
	private ConnectionSpec connectionSpec;

	private final ThreadLocal<ConnectionSpec> threadBoundSpec =
			new NamedThreadLocal<>("Current CCI ConnectionSpec");


	/**
	 * 设置此适配器用于获取连接的 ConnectionSpec。
	 * 默认为无。
	 */
	public void setConnectionSpec(ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * 为此代理和当前线程设置 ConnectionSpec。
	 * 给定的 ConnectionSpec 将应用于此 ConnectionFactory 代理后续的
	 * 所有 {@code getConnection()} 调用。
	 * <p>这将覆盖任何静态指定的 "connectionSpec" 属性。
	 * @param spec 要应用的 ConnectionSpec
	 * @see #removeConnectionSpecFromCurrentThread
	 */
	public void setConnectionSpecForCurrentThread(ConnectionSpec spec) {
		this.threadBoundSpec.set(spec);
	}

	/**
	 * 从当前线程移除此代理的任何 ConnectionSpec。
	 * 之后将重新应用静态指定的 ConnectionSpec。
	 * @see #setConnectionSpecForCurrentThread
	 */
	public void removeConnectionSpecFromCurrentThread() {
		this.threadBoundSpec.remove();
	}


	/**
	 * 确定当前是否存在线程绑定的 ConnectionSpec，
	 * 如果存在则使用它，否则回退到静态指定的 "connectionSpec" 属性。
	 * @see #doGetConnection
	 */
	@Override
	public final Connection getConnection() throws ResourceException {
		ConnectionSpec threadSpec = this.threadBoundSpec.get();
		if (threadSpec != null) {
			return doGetConnection(threadSpec);
		}
		else {
			return doGetConnection(this.connectionSpec);
		}
	}

	/**
	 * 此实现委托给目标 ConnectionFactory 的
	 * {@code getConnection(ConnectionSpec)} 方法，传入指定的用户凭据。
	 * 如果指定的 ConnectionSpec 为空，将简单地委托给目标 ConnectionFactory 的
	 * 标准 {@code getConnection()} 方法。
	 * @param spec 要应用的 ConnectionSpec
	 * @return 该 Connection
	 * @see javax.resource.cci.ConnectionFactory#getConnection(javax.resource.cci.ConnectionSpec)
	 * @see javax.resource.cci.ConnectionFactory#getConnection()
	 */
	protected Connection doGetConnection(@Nullable ConnectionSpec spec) throws ResourceException {
		ConnectionFactory connectionFactory = getTargetConnectionFactory();
		Assert.state(connectionFactory != null, "No 'targetConnectionFactory' set");
		return (spec != null ? connectionFactory.getConnection(spec) : connectionFactory.getConnection());
	}

}
