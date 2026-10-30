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

package org.springframework.web.socket.server.standard;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * 检测类型为 {@link javax.websocket.server.ServerEndpointConfig} 的 Bean，
 * 并注册到标准 Java WebSocket 运行时。同时也会检测带有
 * {@link ServerEndpoint} 注解的 Bean 并注册它们。虽然不是必需的，但被注解的端点
 * 通常应将其 {@code configurator} 属性设置为
 * {@link SpringConfigurator}。
 *
 * <p>使用此类时，通过在 Spring 配置中声明它，应该可以关闭 Servlet 容器对
 * WebSocket 端点的扫描。这可以借助 {@code web.xml} 中的
 * {@code <absolute-ordering>} 元素来完成。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @see ServerEndpointRegistration
 * @see SpringConfigurator
 * @see ServletServerContainerFactoryBean
 */
public class ServerEndpointExporter extends WebApplicationObjectSupport
		implements InitializingBean, SmartInitializingSingleton {

	@Nullable
	private List<Class<?>> annotatedEndpointClasses;

	@Nullable
	private ServerContainer serverContainer;


	/**
	 * 显式列出应在启动时注册的被注解端点类型。如果你希望关闭 Servlet 容器
	 * 对端点的扫描（该扫描会遍历类路径中的所有第三方 jar），
	 * 转而依赖 Spring 配置，则可以使用此方法。
	 * @param annotatedEndpointClasses 带有 {@link ServerEndpoint} 注解的类型
	 */
	public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
		this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
	}

	/**
	 * 设置用于端点注册的 JSR-356 {@link ServerContainer}。
	 * 如果未设置，则将通过 {@code ServletContext} 获取容器。
	 */
	public void setServerContainer(@Nullable ServerContainer serverContainer) {
		this.serverContainer = serverContainer;
	}

	/**
	 * 返回用于端点注册的 JSR-356 {@link ServerContainer}。
	 */
	@Nullable
	protected ServerContainer getServerContainer() {
		return this.serverContainer;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.serverContainer == null) {
			this.serverContainer =
					(ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");
		}
	}

	@Override
	protected boolean isContextRequired() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(getServerContainer() != null, "javax.websocket.server.ServerContainer not available");
	}

	@Override
	public void afterSingletonsInstantiated() {
		registerEndpoints();
	}


	/**
	 * 实际注册端点。由 {@link #afterSingletonsInstantiated()} 调用。
	 */
	protected void registerEndpoints() {
		Set<Class<?>> endpointClasses = new LinkedHashSet<>();
		if (this.annotatedEndpointClasses != null) {
			endpointClasses.addAll(this.annotatedEndpointClasses);
		}

		ApplicationContext context = getApplicationContext();
		if (context != null) {
			String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
			for (String beanName : endpointBeanNames) {
				endpointClasses.add(context.getType(beanName));
			}
		}

		for (Class<?> endpointClass : endpointClasses) {
			registerEndpoint(endpointClass);
		}

		if (context != null) {
			Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
			for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
				registerEndpoint(endpointConfig);
			}
		}
	}

	private void registerEndpoint(Class<?> endpointClass) {
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null,
				"No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
				"has not run yet. Was the Spring ApplicationContext refreshed through a " +
				"org.springframework.web.context.ContextLoaderListener, " +
				"i.e. after the ServletContext has been fully initialized?");
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering @ServerEndpoint class: " + endpointClass);
			}
			serverContainer.addEndpoint(endpointClass);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
		}
	}

	private void registerEndpoint(ServerEndpointConfig endpointConfig) {
		ServerContainer serverContainer = getServerContainer();
		Assert.state(serverContainer != null, "No ServerContainer set");
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering ServerEndpointConfig: " + endpointConfig);
			}
			serverContainer.addEndpoint(endpointConfig);
		}
		catch (DeploymentException ex) {
			throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
		}
	}

}
