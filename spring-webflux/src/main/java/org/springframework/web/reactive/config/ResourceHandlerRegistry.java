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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.ResourceTransformer;
import org.springframework.web.reactive.resource.ResourceTransformerSupport;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.server.WebHandler;

/**
 * 存储用于通过 Spring WebFlux 提供静态资源（如图像、css 文件等）的资源处理器注册信息，包括设置针对 Web 浏览器高效加载而优化的缓存头。资源可以从 Web 应用根目录、类路径等位置提供。
 *
 * <p>要创建资源处理器，请使用 {@link #addResourceHandler(String...)} 提供应调用该处理器以提供静态资源的 URL 路径模式（例如 {@code "/resources/**"}）。
 *
 * <p>然后使用返回的 {@link ResourceHandlerRegistration} 上的其他方法添加一个或多个提供静态内容的位置（例如 {@code "/"}、{@code "classpath:/META-INF/public-web-resources/"}），或为所提供的资源指定缓存时长。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceHandlerRegistry {

	private final ResourceLoader resourceLoader;

	private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 为给定的资源加载器（通常是应用上下文）创建新的资源处理器注册表。
	 * @param resourceLoader 要使用的资源加载器
	 */
	public ResourceHandlerRegistry(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 配置可供 {@link org.springframework.web.reactive.resource.ResourceTransformer} 实例使用的 {@link ResourceUrlProvider}。
	 * @param resourceUrlProvider 要使用的资源 URL 提供者
	 * @since 5.0.11
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}



	/**
	 * 添加基于指定 URL 路径模式提供静态资源的资源处理器。对于每个匹配指定路径模式之一的传入请求，都会调用该处理器。
	 * <p>允许使用 {@code "/static/**"} 或 {@code "/css/{filename:\\w+\\.css}"} 等模式。有关语法的更多详情，请参见 {@link org.springframework.web.util.pattern.PathPattern}。
	 * @return 用于进一步配置所注册资源处理器的
	 * {@link ResourceHandlerRegistration}
	 */
	public ResourceHandlerRegistration addResourceHandler(String... patterns) {
		ResourceHandlerRegistration registration = new ResourceHandlerRegistration(this.resourceLoader, patterns);
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
	 * 指定资源处理相对于 Spring 配置中其他 {@code HandlerMapping} 的顺序。
	 * <p>使用的默认值为 {@code Integer.MAX_VALUE-1}。
	 */
	public ResourceHandlerRegistry setOrder(int order) {
		this.order = order;
		return this;
	}

	/**
	 * 返回包含所映射资源处理器的处理器映射；如果没有任何注册，则返回 {@code null}。
	 */
	@Nullable
	protected AbstractUrlHandlerMapping getHandlerMapping() {
		if (this.registrations.isEmpty()) {
			return null;
		}
		Map<String, WebHandler> urlMap = new LinkedHashMap<>();
		for (ResourceHandlerRegistration registration : this.registrations) {
			ResourceWebHandler handler = getRequestHandler(registration);
			for (String pathPattern : registration.getPathPatterns()) {
				urlMap.put(pathPattern, handler);
			}
		}
		return new SimpleUrlHandlerMapping(urlMap, this.order);
	}

	private ResourceWebHandler getRequestHandler(ResourceHandlerRegistration registration) {
		ResourceWebHandler handler = registration.getRequestHandler();
		for (ResourceTransformer transformer : handler.getResourceTransformers()) {
			if (transformer instanceof ResourceTransformerSupport) {
				((ResourceTransformerSupport) transformer).setResourceUrlProvider(this.resourceUrlProvider);
			}
		}
		try {
			handler.afterPropertiesSet();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
		}
		return handler;
	}

}
