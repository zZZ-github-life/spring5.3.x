/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jmx.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.JmxException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 创建一个 JSR-160 {@link JMXConnectorServer} 的 {@link FactoryBean}，可选地将其注册到
 * {@link MBeanServer}，然后启动它。
 *
 * <p>通过将 {@code threaded} 属性设置为 {@code true}，可以在单独的线程中启动
 * {@code JMXConnectorServer}。通过将 {@code daemon} 属性设置为 {@code true}，可以将该线程配置为守
 * 护线程。
 *
 * <p>当此类的实例在所属 {@code ApplicationContext} 关闭时被销毁，{@code JMXConnectorServer} 会被正
 * 确关闭。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see JMXConnectorServer
 * @see MBeanServer
 */
public class ConnectorServerFactoryBean extends MBeanRegistrationSupport
		implements FactoryBean<JMXConnectorServer>, InitializingBean, DisposableBean {

	/** 默认服务 URL。 */
	public static final String DEFAULT_SERVICE_URL = "service:jmx:jmxmp://localhost:9875";


	private String serviceUrl = DEFAULT_SERVICE_URL;

	private Map<String, Object> environment = new HashMap<>();

	@Nullable
	private MBeanServerForwarder forwarder;

	@Nullable
	private ObjectName objectName;

	private boolean threaded = false;

	private boolean daemon = false;

	@Nullable
	private JMXConnectorServer connectorServer;


	/**
	 * 设置 {@code JMXConnectorServer} 的服务 URL。
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * 设置用于构造 {@code JMXConnectorServer} 的环境属性，形式为 {@code java.util.Properties}（字符串
	 * 键/值对）。
	 */
	public void setEnvironment(@Nullable Properties environment) {
		CollectionUtils.mergePropertiesIntoMap(environment, this.environment);
	}

	/**
	 * 设置用于构造 {@code JMXConnector} 的环境属性，形式为以字符串为键、任意对象为值的 {@code Map}。
	 */
	public void setEnvironmentMap(@Nullable Map<String, ?> environment) {
		if (environment != null) {
			this.environment.putAll(environment);
		}
	}

	/**
	 * 设置要应用到 {@code JMXConnectorServer} 的 MBeanServerForwarder。
	 */
	public void setForwarder(MBeanServerForwarder forwarder) {
		this.forwarder = forwarder;
	}

	/**
	 * 设置用于将 {@code JMXConnectorServer} 自身注册到 {@code MBeanServer} 的 {@code ObjectName}，可以
	 * 是 {@code ObjectName} 实例或 {@code String}。
	 * @throws MalformedObjectNameException 如果 {@code ObjectName} 格式错误
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置是否应在单独的线程中启动 {@code JMXConnectorServer}。
	 */
	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	/**
	 * 设置是否为 {@code JMXConnectorServer} 启动的任何线程都作为守护线程启动。
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}


	/**
	 * 启动连接器服务器。如果 {@code threaded} 标志设置为 {@code true}，{@code JMXConnectorServer} 将在
	 * 单独的线程中启动。如果 {@code daemon} 标志设置为 {@code true}，该线程将作为守护线程启动。
	 * @throws JMException 向 {@code MBeanServer} 注册连接器服务器时出现问题
	 * @throws IOException 启动连接器服务器时出现问题
	 */
	@Override
	public void afterPropertiesSet() throws JMException, IOException {
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}

		// Create the JMX service URL.
		JMXServiceURL url = new JMXServiceURL(this.serviceUrl);

		// Create the connector server now.
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);

		// Set the given MBeanServerForwarder, if any.
		if (this.forwarder != null) {
			this.connectorServer.setMBeanServerForwarder(this.forwarder);
		}

		// Do we want to register the connector with the MBean server?
		if (this.objectName != null) {
			doRegister(this.connectorServer, this.objectName);
		}

		try {
			if (this.threaded) {
				// Start the connector server asynchronously (in a separate thread).
				final JMXConnectorServer serverToStart = this.connectorServer;
				Thread connectorThread = new Thread() {
					@Override
					public void run() {
						try {
							serverToStart.start();
						}
						catch (IOException ex) {
							throw new JmxException("Could not start JMX connector server after delay", ex);
						}
					}
				};

				connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
				connectorThread.setDaemon(this.daemon);
				connectorThread.start();
			}
			else {
				// Start the connector server in the same thread.
				this.connectorServer.start();
			}

			if (logger.isInfoEnabled()) {
				logger.info("JMX connector server started: " + this.connectorServer);
			}
		}

		catch (IOException ex) {
			// Unregister the connector server if startup failed.
			unregisterBeans();
			throw ex;
		}
	}


	@Override
	@Nullable
	public JMXConnectorServer getObject() {
		return this.connectorServer;
	}

	@Override
	public Class<? extends JMXConnectorServer> getObjectType() {
		return (this.connectorServer != null ? this.connectorServer.getClass() : JMXConnectorServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 停止由此类实例管理的 {@code JMXConnectorServer}。会在 {@code ApplicationContext} 关闭时自动调用。
	 * @throws IOException 停止连接器服务器时出错
	 */
	@Override
	public void destroy() throws IOException {
		try {
			if (this.connectorServer != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping JMX connector server: " + this.connectorServer);
				}
				this.connectorServer.stop();
			}
		}
		finally {
			unregisterBeans();
		}
	}

}
