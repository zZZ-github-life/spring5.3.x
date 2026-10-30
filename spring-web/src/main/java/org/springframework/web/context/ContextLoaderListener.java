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

package org.springframework.web.context;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 用于启动和关闭 Spring 根 {@link WebApplicationContext} 的引导监听器。
 * 简单地委托给 {@link ContextLoader} 以及 {@link ContextCleanupListener}。
 *
 * <p>自 Spring 3.1 起，{@code ContextLoaderListener} 支持通过
 * {@link #ContextLoaderListener(WebApplicationContext)} 构造函数注入根 Web 应用上下文，
 * 从而允许在 Servlet 3.0+ 环境中进行编程式配置。
 * 有关用法示例，请参见 {@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 17.02.2003
 * @see #setContextInitializers
 * @see org.springframework.web.WebApplicationInitializer
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	/**
	 * 创建一个新的 {@code ContextLoaderListener}，它将基于 "contextClass" 和
	 * "contextConfigLocation" servlet 上下文参数创建 Web 应用上下文。
	 * 有关二者默认值的详细信息，请参见 {@link ContextLoader} 超类文档。
	 * <p>此构造函数通常在 {@code web.xml} 中将 {@code ContextLoaderListener} 声明为
	 * {@code <listener>} 时使用，此时需要一个无参构造函数。
	 * <p>创建的应用上下文将以
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 属性名注册到 ServletContext 中，并且当在此监听器上调用
	 * {@link #contextDestroyed} 生命周期方法时，Spring 应用上下文将被关闭。
	 * @see ContextLoader
	 * @see #ContextLoaderListener(WebApplicationContext)
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener() {
	}

	/**
	 * 使用给定的应用上下文创建一个新的 {@code ContextLoaderListener}。
	 * 在 Servlet 3.0+ 环境中，可以通过
	 * {@link javax.servlet.ServletContext#addListener} API 进行基于实例的监听器注册，
	 * 此时此构造函数非常有用。
	 * <p>该上下文可能已经或尚未
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh() 刷新}。
	 * 如果它 (a) 是 {@link ConfigurableWebApplicationContext} 的实现，并且
	 * (b) <strong>尚未</strong>被刷新（推荐做法），那么将发生以下情况：
	 * <ul>
	 * <li>如果给定的上下文尚未被分配
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#setId id}，
	 * 则会为其分配一个</li>
	 * <li>{@code ServletContext} 和 {@code ServletConfig} 对象将被委托给应用上下文</li>
	 * <li>将调用 {@link #customizeContext}</li>
	 * <li>通过 "contextInitializerClasses" init-param 指定的任何
	 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer ApplicationContextInitializer ApplicationContextInitializers}
	 * 都将被应用。</li>
	 * <li>将调用 {@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已被刷新，或者未实现 {@code ConfigurableWebApplicationContext}，
	 * 则上述任何操作都不会发生，假设用户已根据其具体需求执行了（或未执行）这些操作。
	 * <p>有关用法示例，请参见 {@link org.springframework.web.WebApplicationInitializer}。
	 * <p>无论如何，给定的应用上下文都将以
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} 属性名
	 * 注册到 ServletContext 中，并且当在此监听器上调用 {@link #contextDestroyed}
	 * 生命周期方法时，Spring 应用上下文将被关闭。
	 * @param context 要管理的应用上下文
	 * @see #contextInitialized(ServletContextEvent)
	 * @see #contextDestroyed(ServletContextEvent)
	 */
	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}


	/**
	 * 初始化根 Web 应用上下文。
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}


	/**
	 * 关闭根 Web 应用上下文。
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		closeWebApplicationContext(event.getServletContext());
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}

}
