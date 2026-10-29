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

package org.springframework.jca.support;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.FactoryBean}，以 "非管理" 模式
 * （由 Java 连接器架构规范定义）创建本地 JCA 连接工厂。
 * 这是 {@link org.springframework.jndi.JndiObjectFactoryBean} 定义的直接替代方案，
 * 后者从 Java EE 服务器的命名环境获取连接工厂句柄。
 *
 * <p>连接工厂的类型取决于实际的连接器：连接器可以暴露其原生 API
 * （如 JDBC {@link javax.sql.DataSource} 或 JMS {@link javax.jms.ConnectionFactory}），
 * 也可以遵循 JCA 规范定义的标准公共客户端接口（CCI）。
 * CCI 情况下暴露的接口为 {@link javax.resource.cci.ConnectionFactory}。
 *
 * <p>要使用此 FactoryBean，必须指定连接器的
 * {@link #setManagedConnectionFactory "managedConnectionFactory"}（通常配置为单独的 JavaBean），
 * 它将用于创建暴露给应用程序的实际连接工厂引用。或者，你还可以指定
 * {@link #setConnectionManager "connectionManager"}，以使用自定义的 ConnectionManager
 * 而不是连接器的默认实现。
 *
 * <p><b>注意：</b>在非管理模式下，连接器不部署在应用服务器上，
 * 更具体地说，不与应用服务器交互。因此，它无法使用 Java EE 服务器的系统契约：
 * 连接管理、事务管理和安全管理。必须使用自定义的 ConnectionManager 实现
 * 来结合独立的事务协调器等应用这些服务。
 *
 * <p>连接器默认使用本地 ConnectionManager（包含在连接器中），
 * 由于缺少 XA 登记，无法参与全局事务。你需要指定一个支持 XA 的 ConnectionManager，
 * 以使连接器与 XA 事务协调器交互。或者，简单地使用暴露 API 的原生本地事务设施
 * （例如 CCI 本地事务），或使用 Spring 的 PlatformTransactionManager SPI 的相应实现
 * （例如 {@link org.springframework.jca.cci.connection.CciLocalTransactionManager}）
 * 来驱动本地事务。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedConnectionFactory
 * @see #setConnectionManager
 * @see javax.resource.cci.ConnectionFactory
 * @see javax.resource.cci.Connection#getLocalTransaction
 * @see org.springframework.jca.cci.connection.CciLocalTransactionManager
 */
public class LocalConnectionFactoryBean implements FactoryBean<Object>, InitializingBean {

	@Nullable
	private ManagedConnectionFactory managedConnectionFactory;

	@Nullable
	private ConnectionManager connectionManager;

	@Nullable
	private Object connectionFactory;


	/**
	 * 设置用于创建所需连接工厂的 JCA ManagerConnectionFactory。
	 * <p>ManagerConnectionFactory 通常设置为单独的 bean（可能作为内部 bean），
	 * 使用 JavaBean 属性进行填充：JCA 规范鼓励 ManagerConnectionFactory 遵循 JavaBean 模式，
	 * 类似于 JDBC DataSource 和 JPA EntityManagerFactory。
	 * <p>注意，ManagerConnectionFactory 实现可能期望通过
	 * {@link javax.resource.spi.ResourceAdapterAssociation} 接口引用其 JCA 1.7 ResourceAdapter。
	 * 在这种情况下，在将 ManagerConnectionFactory 传入此 LocalConnectionFactoryBean 之前，
	 * 只需将相应的 ResourceAdapter 实例注入其 "resourceAdapter" bean 属性即可。
	 * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
	 */
	public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
		this.managedConnectionFactory = managedConnectionFactory;
	}

	/**
	 * 设置用于创建所需连接工厂的 JCA ConnectionManager。
	 * <p>用于本地使用的 ConnectionManager 实现通常包含在 JCA 连接器中。
	 * 此类包含的 ConnectionManager 可以设置为默认值，无需显式指定。
	 * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
	 */
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.managedConnectionFactory == null) {
			throw new IllegalArgumentException("Property 'managedConnectionFactory' is required");
		}
		if (this.connectionManager != null) {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory(this.connectionManager);
		}
		else {
			this.connectionFactory = this.managedConnectionFactory.createConnectionFactory();
		}
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.connectionFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.connectionFactory != null ? this.connectionFactory.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
