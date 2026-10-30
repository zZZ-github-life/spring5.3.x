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

package org.springframework.jca.context;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * JCA 1.7 {@link javax.resource.spi.ResourceAdapter} 实现，
 * 加载 Spring {@link org.springframework.context.ApplicationContext}，
 * 作为 ResourceAdapter 生命周期的一部分启动和停止 Spring 管理的 bean。
 *
 * <p>适用于不需要任何 HTTP 入口点，而仅由消息端点和调度任务等组成的应用程序上下文。
 * 此类上下文中的 bean 可以使用应用服务器资源，如 JTA 事务管理器和 JNDI 绑定的
 * JDBC DataSource 及 JMS ConnectionFactory 实例，并可以通过 Spring 的标准事务管理
 * 以及 JNDI 和 JMX 支持设施向平台的 JMX 服务器注册。
 *
 * <p>如果需要调度异步工作，考虑使用 Spring 的
 * {@link org.springframework.jca.work.WorkManagerTaskExecutor} 作为标准 bean 定义，
 * 通过依赖注入到应用程序 bean 中。此 WorkManagerTaskExecutor 将自动使用
 * 提供给此 ResourceAdapter 的 BootstrapContext 中的 JCA WorkManager。
 *
 * <p>JCA {@link javax.resource.spi.BootstrapContext} 也可以通过实现
 * {@link BootstrapContextAware} 接口的应用程序组件直接访问。
 * 使用此 ResourceAdapter 部署时，BootstrapContext 保证被传递给此类组件。
 *
 * <p>此 ResourceAdapter 应在 Java EE ".rar" 部署单元中的 "META-INF/ra.xml" 文件中定义，如下所示：
 *
 * <pre class="code">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;connector xmlns="http://java.sun.com/xml/ns/j2ee"
 *		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *		 xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee https://java.sun.com/xml/ns/j2ee/connector_1_5.xsd"
 *		 version="1.5"&gt;
 *	 &lt;vendor-name&gt;Spring Framework&lt;/vendor-name&gt;
 *	 &lt;eis-type&gt;Spring Connector&lt;/eis-type&gt;
 *	 &lt;resourceadapter-version&gt;1.0&lt;/resourceadapter-version&gt;
 *	 &lt;resourceadapter&gt;
 *		 &lt;resourceadapter-class&gt;org.springframework.jca.context.SpringContextResourceAdapter&lt;/resourceadapter-class&gt;
 *		 &lt;config-property&gt;
 *			 &lt;config-property-name&gt;ContextConfigLocation&lt;/config-property-name&gt;
 *			 &lt;config-property-type&gt;java.lang.String&lt;/config-property-type&gt;
 *			 &lt;config-property-value&gt;META-INF/applicationContext.xml&lt;/config-property-value&gt;
 *		 &lt;/config-property&gt;
 *	 &lt;/resourceadapter&gt;
 * &lt;/connector&gt;</pre>
 *
 * 注意 "META-INF/applicationContext.xml" 是默认的上下文配置位置，所以除非你打算指定
 * 不同的/额外的配置文件，否则不需要指定。因此在默认情况下，你可以移除上面的整个
 * {@code config-property} 部分。
 *
 * <p><b>对于简单的部署需求，你需要做的只是以下步骤：</b>
 * 将所有应用程序类打包到 RAR 文件中（这只是具有不同文件扩展名的标准 JAR 文件），
 * 将所有必需的库 jar 添加到 RAR 归档的根目录，添加如上所示的 "META-INF/ra.xml"
 * 部署描述符以及相应的 Spring XML bean 定义文件（通常为 "META-INF/applicationContext.xml"），
 * 然后将生成的 RAR 文件放入应用程序服务器的部署目录即可！
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setContextConfigLocation
 * @see #loadBeanDefinitions
 * @see ResourceAdapterApplicationContext
 */
public class SpringContextResourceAdapter implements ResourceAdapter {

	/**
	 * 任意数量的这些字符都被视为单个 String 值中多个上下文配置路径之间的分隔符。
	 * @see #setContextConfigLocation
	 */
	public static final String CONFIG_LOCATION_DELIMITERS = ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;

	/**
	 * 默认的 {@code applicationContext.xml} 位置。
	 */
	public static final String DEFAULT_CONTEXT_CONFIG_LOCATION = "META-INF/applicationContext.xml";


	protected final Log logger = LogFactory.getLog(getClass());

	private String contextConfigLocation = DEFAULT_CONTEXT_CONFIG_LOCATION;

	@Nullable
	private ConfigurableApplicationContext applicationContext;


	/**
	 * 设置上下文配置文件的位置，在资源适配器的部署单元内。
	 * 这可以是一个由多个资源位置组成的分隔字符串，
	 * 使用逗号、分号、空格或换行符分隔。
	 * <p>可以在 {@code ra.xml} 部署描述符中指定为 "ContextConfigLocation" 配置属性。
	 * <p>默认为 "classpath:META-INF/applicationContext.xml"。
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回指定的上下文配置文件。
	 */
	protected String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 返回新的 {@link StandardEnvironment}。
	 * <p>子类可以重写此方法以提供自定义的 {@link ConfigurableEnvironment} 实现。
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * 此实现通过 {@link #createApplicationContext} 模板方法加载 Spring ApplicationContext。
	 */
	@Override
	public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
		if (logger.isDebugEnabled()) {
			logger.debug("Starting SpringContextResourceAdapter with BootstrapContext: " + bootstrapContext);
		}
		this.applicationContext = createApplicationContext(bootstrapContext);
	}

	/**
	 * 为给定的 JCA BootstrapContext 构建 Spring ApplicationContext。
	 * <p>默认实现构建 {@link ResourceAdapterApplicationContext}，
	 * 并委托 {@link #loadBeanDefinitions} 实际解析指定的配置文件。
	 * @param bootstrapContext 此 ResourceAdapter 的 BootstrapContext
	 * @return Spring ApplicationContext 实例
	 */
	protected ConfigurableApplicationContext createApplicationContext(BootstrapContext bootstrapContext) {
		ResourceAdapterApplicationContext applicationContext =
				new ResourceAdapterApplicationContext(bootstrapContext);

		// Set ResourceAdapter's ClassLoader as bean class loader.
		applicationContext.setClassLoader(getClass().getClassLoader());

		// Extract individual config locations.
		String[] configLocations =
				StringUtils.tokenizeToStringArray(getContextConfigLocation(), CONFIG_LOCATION_DELIMITERS);

		loadBeanDefinitions(applicationContext, configLocations);
		applicationContext.refresh();

		return applicationContext;
	}

	/**
	 * 根据指定的配置文件将 bean 定义加载到给定的注册表中。
	 * @param registry 要加载到的注册表
	 * @param configLocations 解析后的配置位置
	 * @see #setContextConfigLocation
	 */
	protected void loadBeanDefinitions(BeanDefinitionRegistry registry, String[] configLocations) {
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(configLocations);
	}

	/**
	 * 此实现关闭 Spring ApplicationContext。
	 */
	@Override
	public void stop() {
		logger.debug("Stopping SpringContextResourceAdapter");
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}


	/**
	 * 此实现始终抛出 NotSupportedException。
	 */
	@Override
	public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec)
			throws ResourceException {

		throw new NotSupportedException("SpringContextResourceAdapter does not support message endpoints");
	}

	/**
	 * 此实现不做任何操作。
	 */
	@Override
	public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	/**
	 * 此实现始终返回 {@code null}。
	 */
	@Override
	@Nullable
	public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
		return null;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SpringContextResourceAdapter &&
				ObjectUtils.nullSafeEquals(getContextConfigLocation(),
						((SpringContextResourceAdapter) other).getContextConfigLocation())));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getContextConfigLocation());
	}

}
