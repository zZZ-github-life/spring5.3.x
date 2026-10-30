/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 存储资源处理器的注册信息，用于通过 Spring MVC 提供静态资源
 * （如图片、css 文件等），包括设置针对浏览器中高效加载而优化的缓存标头。
 * 资源可以从 Web 应用根目录下、类路径及其他位置提供。
 *
 * <p>要创建资源处理器，请使用 {@link #addResourceHandler(String...)}，
 * 并提供应调用该处理器以提供静态资源的 URL 路径模式（例如 {@code "/resources/**"}）。
 *
 * <p>然后使用返回的 {@link ResourceHandlerRegistration} 上的其他方法
 * 添加一个或多个提供静态内容的位置（例如 {{@code "/"}、
 * {@code "classpath:/META-INF/public-web-resources/"}}），
 * 或为所提供的资源指定缓存周期。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see DefaultServletHandlerConfigurer
 */
public class ResourceHandlerRegistry {

	private final ServletContext servletContext;

	private final ApplicationContext applicationContext;

	@Nullable
	private final ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private final UrlPathHelper pathHelper;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	private int order = Ordered.LOWEST_PRECEDENCE - 1;


	/**
	 * 为给定的应用上下文创建新的资源处理器注册表。
	 * @param applicationContext Spring 应用上下文
	 * @param servletContext 对应的 Servlet 上下文
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
		this(applicationContext, servletContext, null);
	}

	/**
	 * 为给定的应用上下文创建新的资源处理器注册表。
	 * @param applicationContext Spring 应用上下文
	 * @param servletContext 对应的 Servlet 上下文
	 * @param contentNegotiationManager 要使用的内容协商管理器
	 * @since 4.3
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			@Nullable ContentNegotiationManager contentNegotiationManager) {

		this(applicationContext, servletContext, contentNegotiationManager, null);
	}

	/**
	 * {@link #ResourceHandlerRegistry(ApplicationContext, ServletContext, ContentNegotiationManager)}
	 * 的一个变体，同时还接受用于将请求映射到静态资源的 {@link UrlPathHelper}。
	 * @since 4.3.13
	 */
	public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext,
			@Nullable ContentNegotiationManager contentNegotiationManager, @Nullable UrlPathHelper pathHelper) {

		Assert.notNull(applicationContext, "ApplicationContext is required");
		this.applicationContext = applicationContext;
		this.servletContext = servletContext;
		this.contentNegotiationManager = contentNegotiationManager;
		this.pathHelper = pathHelper;
	}


	/**
	 * 添加用于提供静态资源的资源处理器。对于匹配指定 URL 路径模式之一的请求，
	 * 将调用该处理器。
	 * <p>支持诸如 {@code "/static/**"} 或 {@code "/css/{filename:\\w+\\.css}"} 之类的模式。
	 * <p>关于模式语法，当解析模式被
	 * {@link PathMatchConfigurer#setPatternParser 启用}时请参见 {@link PathPattern}，
	 * 否则请参见 {@link AntPathMatcher}。两者语法大体相同，
	 * 但 {@link PathPattern} 更贴合 Web 用途且更高效。
	 */
	public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 是否已为给定的路径模式注册了资源处理器。
	 */
	public boolean hasMappingForPattern(String pathPattern) {
		for (ResourceHandlerRegistration registration : this.registrations) {
			if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定资源处理相对于 Spring MVC 应用上下文中配置的其他
	 * {@link HandlerMapping HandlerMappings} 的排序。
	 * <p>使用的默认值为 {@code Integer.MAX_VALUE-1}。
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回包含已映射资源处理器的处理器映射；如果没有注册，则返回 {@code null}。
	 */
	@Nullable
	protected AbstractHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}
		Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			ResourceHttpRequestHandler handler = getRequestHandler(registration);
			for (String pathPattern : registration.getPathPatterns()) {
				urlMap.put(pathPattern, handler);
			}
		}
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

	@SuppressWarnings("deprecation")
	private ResourceHttpRequestHandler getRequestHandler(ResourceHandlerRegistration registration) {
		ResourceHttpRequestHandler handler = registration.getRequestHandler();
		if (this.pathHelper != null) {
			handler.setUrlPathHelper(this.pathHelper);
		}
		if (this.contentNegotiationManager != null) {
			handler.setContentNegotiationManager(this.contentNegotiationManager);
		}
		handler.setServletContext(this.servletContext);
		handler.setApplicationContext(this.applicationContext);
		try {
			handler.afterPropertiesSet();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}
		return handler;
	}

}
