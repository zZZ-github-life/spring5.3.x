/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.resource.ResourceWebHandler;

/**
 * 用于协助创建和配置静态资源处理器。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceHandlerRegistration {

	private final ResourceLoader resourceLoader;

	private final String[] pathPatterns;

	private final List<String> locationValues = new ArrayList<>();

	@Nullable
	private CacheControl cacheControl;

	@Nullable
	private ResourceChainRegistration resourceChainRegistration;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;

	@Nullable
	private Map<String, MediaType> mediaTypes;



	/**
	 * 创建 {@link ResourceHandlerRegistration} 实例。
	 * @param resourceLoader 用于将 String 位置转换为 {@link Resource} 的
	 * 资源加载器
	 * @param pathPatterns 一个或多个资源 URL 路径模式
	 */
	public ResourceHandlerRegistration(ResourceLoader resourceLoader, String... pathPatterns) {
		Assert.notNull(resourceLoader, "ResourceLoader is required");
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling");
		this.resourceLoader = resourceLoader;
		this.pathPatterns = pathPatterns;
	}


	/**
	 * 添加一个或多个用于提供静态内容的资源位置。
	 * <p>每个位置都必须指向有效目录。可以逗号分隔列表的形式指定多个位置，将按指定顺序在这些位置中查找给定资源。
	 * <p>例如，{@code "/", "classpath:/META-INF/public-web-resources/"} 允许同时从 Web 应用根目录和类路径上任何包含 {@code /META-INF/public-web-resources/} 目录的 JAR 中提供资源，其中 Web 应用根目录中的资源优先。
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 */
	public ResourceHandlerRegistration addResourceLocations(String... resourceLocations) {
		this.locationValues.addAll(Arrays.asList(resourceLocations));
		return this;
	}

	/**
	 * 指定资源处理器应使用的 {@link CacheControl}。
	 * @param cacheControl 要使用的 CacheControl 配置
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * 设置是否应使用 {@link Resource#lastModified()} 信息来驱动 HTTP 响应。
	 * <p>此配置默认设置为 {@code true}。
	 * @param useLastModified 是否应使用“最后修改”资源信息
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 * @since 5.3
	 * @see ResourceWebHandler#setUseLastModified
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}

	/**
	 * 设置是否在启动时通过存在性检查来优化指定的位置，预先过滤不存在的目录，从而无需在每次访问资源时都进行检查。
	 * <p>默认值为 {@code false}，以便防御没有目录条目的 zip 文件，此类文件无法预先暴露目录的存在性。如果 JAR 布局一致且包含目录条目，可将此标志切换为 {@code true} 以优化访问。
	 * @param optimizeLocations 是否在启动时通过存在性检查来优化位置
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 * @since 5.3.13
	 * @see ResourceWebHandler#setOptimizeLocations
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
		return this;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，这对于为资源 URL 应用版本策略很有用。
	 * <p>如果未调用此方法，默认仅使用简单的 {@code PathResourceResolver} 将 URL 路径匹配到配置位置下的资源。
	 * @param cacheResources 是否缓存资源解析的结果；生产环境建议设为
	 * “true”（开发环境设为“false”，尤其是在应用
	 * 版本策略时）
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，这对于为资源 URL 应用版本策略很有用。
	 * <p>如果未调用此方法，默认仅使用简单的 {@code PathResourceResolver} 将 URL 路径匹配到配置位置下的资源。
	 * @param cacheResources 是否缓存资源解析的结果；生产环境建议设为
	 * “true”（开发环境设为“false”，尤其是在应用
	 * 版本策略
	 * @param cache 用于存储已解析和已转换资源的缓存；默认使用
	 * {@link org.springframework.cache.concurrent.ConcurrentMapCache}。
	 * 由于 Resource 不可序列化且可能依赖于应用主机，
	 * 因此不应使用分布式缓存，而应使用内存缓存。
	 * @return 同一个 {@link ResourceHandlerRegistration} 实例，用于链式方法调用
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}

	/**
	 * 添加从静态 {@link Resource} 的文件名中提取的文件扩展名与响应用媒体类型之间的映射。
	 * <p>通常不需要使用此方法，因为也可以通过 {@link MediaTypeFactory#getMediaType(Resource)} 确定映射。
	 * @param mediaTypes 媒体类型映射
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		this.mediaTypes.clear();
		this.mediaTypes.putAll(mediaTypes);
	}


	/**
	 * 返回资源处理器的 URL 路径模式。
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * 返回 {@link ResourceWebHandler} 实例。
	 */
	protected ResourceWebHandler getRequestHandler() {
		ResourceWebHandler handler = new ResourceWebHandler();
		handler.setResourceLoader(this.resourceLoader);
		handler.setLocationValues(this.locationValues);
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		handler.setUseLastModified(this.useLastModified);
		handler.setOptimizeLocations(this.optimizeLocations);
		if (this.mediaTypes != null) {
			handler.setMediaTypes(this.mediaTypes);
		}
		return handler;
	}

}
