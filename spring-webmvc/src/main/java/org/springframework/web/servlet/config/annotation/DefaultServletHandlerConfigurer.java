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

package org.springframework.web.servlet.config.annotation;

import java.util.Collections;

import javax.servlet.ServletContext;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * 配置一个请求处理器，通过将请求转发到 Servlet 容器的 "default" Servlet
 * 来提供静态资源。当 Spring MVC 的 {@link DispatcherServlet} 映射到 "/"，
 * 从而覆盖 Servlet 容器对静态资源的默认处理时，就应使用此配置。
 *
 * <p>由于此处理器以最低优先级配置，实际上它允许所有其他处理器映射先处理请求，
 * 如果它们都无法处理，此处理器可以将请求转发到 "default" Servlet。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 * @see DefaultServletHttpRequestHandler
 */
public class DefaultServletHandlerConfigurer {

	private final ServletContext servletContext;

	@Nullable
	private DefaultServletHttpRequestHandler handler;


	/**
	 * 创建一个 {@link DefaultServletHandlerConfigurer} 实例。
	 * @param servletContext the ServletContext to use.
	 */
	public DefaultServletHandlerConfigurer(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 启用向 "default" Servlet 的转发。
	 * <p>使用此方法时，{@link DefaultServletHttpRequestHandler}
	 * 将尝试自动检测 "default" Servlet 的名称。或者，你也可以
	 * 通过 {@link #enable(String)} 指定默认 Servlet 的名称。
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable() {
		enable(null);
	}

	/**
	 * 启用向具有给定名称的 "default" Servlet 的转发。
	 * <p>当无法自动检测默认 Servlet 时（例如它已被手动配置），
	 * 此方法很有用。
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable(@Nullable String defaultServletName) {
		this.handler = new DefaultServletHttpRequestHandler();
		if (defaultServletName != null) {
			this.handler.setDefaultServletName(defaultServletName);
		}
		this.handler.setServletContext(this.servletContext);
	}


	/**
	 * 返回一个排序值为 {@link Ordered#LOWEST_PRECEDENCE} 的处理器映射实例，
	 * 其中包含映射到 {@code "/**"} 的 {@link DefaultServletHttpRequestHandler}；
	 * 如果未启用默认 Servlet 处理，则返回 {@code null}。
	 * @since 4.3.12
	 */
	@Nullable
	protected SimpleUrlHandlerMapping buildHandlerMapping() {
		if (this.handler == null) {
			return null;
		}
		return new SimpleUrlHandlerMapping(Collections.singletonMap("/**", this.handler),
				Ordered.LOWEST_PRECEDENCE);
	}

}
