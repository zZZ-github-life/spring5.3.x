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

package org.springframework.web.server.adapter;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

/**
 * {@link org.springframework.web.WebApplicationInitializer} 的基类，
 * 用于在 Servlet 容器上安装 Spring 响应式 Web 应用。
 *
 * <p>Spring 配置会被加载并传递给
 * {@link WebHttpHandlerBuilder#applicationContext WebHttpHandlerBuilder}，
 * 后者会扫描上下文以查找特定的 Bean，并创建一个响应式
 * {@link HttpHandler}。生成的处理器通过 {@link ServletHttpHandlerAdapter}
 * 作为 Servlet 进行安装。
 *
 * @author Rossen Stoyanchev
 * @since 5.0.2
 */
public abstract class AbstractReactiveWebInitializer implements WebApplicationInitializer {

	/**
	 * 要使用的默认 Servlet 名称。请参阅 {@link #getServletName}。
	 */
	public static final String DEFAULT_SERVLET_NAME = "http-handler-adapter";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		ApplicationContext applicationContext = createApplicationContext();
		Assert.notNull(applicationContext, "createApplicationContext() must not return null");

		refreshApplicationContext(applicationContext);
		registerCloseListener(servletContext, applicationContext);

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(httpHandler);

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
		if (registration == null) {
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMapping());
		registration.setAsyncSupported(true);
	}

	/**
	 * 返回用于注册 {@link ServletHttpHandlerAdapter} 的名称。
	 * <p>默认值为 {@link #DEFAULT_SERVLET_NAME}。
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * 返回包含应用 Bean 的 Spring 配置，
	 * 其中包括由 {@link WebHttpHandlerBuilder#applicationContext} 检测到的 Bean。
	 */
	protected ApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Class<?>[] configClasses = getConfigClasses();
		Assert.notEmpty(configClasses, "No Spring configuration provided through getConfigClasses()");
		context.register(configClasses);
		return context;
	}

	/**
	 * 指定构成应用配置的
	 * {@link org.springframework.context.annotation.Configuration @Configuration}
	 * 和/或 {@link org.springframework.stereotype.Component @Component} 类。
	 * 这些配置类会被传递给 {@linkplain #createApplicationContext()}。
	 */
	protected abstract Class<?>[] getConfigClasses();

	/**
	 * 如有必要，刷新给定的应用上下文。
	 */
	protected void refreshApplicationContext(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			if (!cac.isActive()) {
				cac.refresh();
			}
		}
	}

	/**
	 * 注册一个 {@link ServletContextListener}，在 Servlet 上下文被销毁时
	 * 关闭给定的应用上下文。
	 * @param servletContext the servlet context to listen to
	 * @param applicationContext the application context that is to be
	 * closed when {@code servletContext} is destroyed
	 */
	protected void registerCloseListener(ServletContext servletContext, ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
			ServletContextDestroyedListener listener = new ServletContextDestroyedListener(cac);
			servletContext.addListener(listener);
		}
	}

	/**
	 * 返回要使用的 Servlet 映射。仅支持默认 Servlet 映射 '/' 以及
	 * 基于路径的 Servlet 映射，例如 '/api/*'。
	 * <p>默认值为 '/'。
	 */
	protected String getServletMapping() {
		return "/";
	}


	private static class ServletContextDestroyedListener implements ServletContextListener {

		private final ConfigurableApplicationContext applicationContext;

		public ServletContextDestroyedListener(ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			this.applicationContext.close();
		}
	}

}
