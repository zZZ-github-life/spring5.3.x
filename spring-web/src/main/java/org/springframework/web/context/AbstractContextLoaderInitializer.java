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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

/**
 * 用于在 servlet 上下文中注册 {@link ContextLoaderListener} 的
 * {@link WebApplicationInitializer} 实现的便捷基类。
 *
 * <p>子类唯一需要实现的方法是 {@link #createRootApplicationContext()}，
 * 它由 {@link #registerContextLoaderListener(ServletContext)} 调用。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/** 供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
	 * 向给定的 servlet 上下文注册一个 {@link ContextLoaderListener}。
	 * {@code ContextLoaderListener} 使用 {@link #createRootApplicationContext()}
	 * 模板方法返回的应用上下文进行初始化。
	 * @param servletContext 用于注册该监听器的 Servlet 上下文
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			servletContext.addListener(listener);
		}
		else {
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
	 * 创建要提供给 {@code ContextLoaderListener} 的 "<strong>根</strong>" 应用上下文。
	 * <p>返回的上下文会被委托给
	 * {@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)}，
	 * 并将被建立为所有 {@code DispatcherServlet} 应用上下文的父上下文。
	 * 因此，它通常包含中间层服务、数据源等。
	 * @return 根应用上下文；如果不需要根上下文则为 {@code null}
	 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
	 */
	@Nullable
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * 指定要应用于 {@code ContextLoaderListener} 创建时所用根应用上下文的
	 * 应用上下文初始化器。
	 * @since 4.2
	 * @see #createRootApplicationContext()
	 * @see ContextLoaderListener#setContextInitializers
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
