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
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * 封装创建资源处理器所需的信息。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author Brian Clozel
 * @since 3.1
 */
public class ResourceHandlerRegistration {

	private final String[] pathPatterns;

	private final List<String> locationValues = new ArrayList<>();

	private final List<Resource> locationsResources = new ArrayList<>();

	@Nullable
	private Integer cachePeriod;

	@Nullable
	private CacheControl cacheControl;

	@Nullable
	private ResourceChainRegistration resourceChainRegistration;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;


	/**
	 * 创建 {@link ResourceHandlerRegistration} 实例。
	 * @param pathPatterns one or more resource URL path patterns
	 */
	public ResourceHandlerRegistration(String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.pathPatterns = pathPatterns;
	}


	/**
	 * 添加一个或多个用于提供静态内容的资源位置。
	 * 每个位置都必须指向一个有效目录。可以以逗号分隔列表的形式指定多个位置，
	 * 并将按指定的顺序在这些位置中查找给定资源。
	 * <p>例如，{{@code "/"}、{@code "classpath:/META-INF/public-web-resources/"}}
	 * 允许既从 Web 应用根目录提供资源，也从类路径上任何包含
	 * {@code /META-INF/public-web-resources/} 目录的 JAR 中提供资源，
	 * 其中 Web 应用根目录中的资源优先。
	 * <p>对于{@link org.springframework.core.io.UrlResource 基于 URL 的资源}
	 * （例如文件、HTTP URL 等），此方法支持一个特殊前缀来指定与该 URL 关联的字符集，
	 * 以便正确地编码追加到其后的相对路径，例如
	 * {@code [charset=Windows-31J]https://example.org/path}。
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 */
	public ResourceHandlerRegistration addResourceLocations(String... locations) {
		this.locationValues.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * 配置基于预先解析的 {@code Resource} 引用来提供静态资源的位置。
	 * @param locations the resource locations to use
	 * @return the same {@link ResourceHandlerRegistration} instance, for
	 * chained method invocation
	 * @since 5.3.3
	 */
	public ResourceHandlerRegistration addResourceLocations(Resource... locations) {
		this.locationsResources.addAll(Arrays.asList(locations));
		return this;
	}

	/**
	 * 指定资源处理器所服务资源的缓存周期，以秒为单位。默认不发送任何缓存标头，
	 * 而仅依赖最后修改时间戳。设置为 0 可发送禁止缓存的缓存标头，
	 * 或设置为正数秒数以发送带有给定 max-age 值的缓存标头。
	 * @param cachePeriod the time to cache resources in seconds
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * 指定资源处理器应使用的 {@link org.springframework.http.CacheControl}。
	 * <p>在此处设置自定义值将覆盖通过 {@link #setCachePeriod} 设置的配置。
	 * @param cacheControl the CacheControl configuration to use
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 4.2
	 */
	public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
		return this;
	}

	/**
	 * 设置是否应使用 {@link Resource#lastModified()} 信息来驱动 HTTP 响应。
	 * <p>此配置默认设置为 {@code true}。
	 * @param useLastModified whether the "last modified" resource information should be used
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 5.3
	 * @see ResourceHttpRequestHandler#setUseLastModified
	 */
	public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
		return this;
	}

	/**
	 * 设置是否在启动时通过存在性检查来优化指定的位置，提前过滤掉不存在的目录，
	 * 从而无需在每次资源访问时都进行检查。
	 * <p>默认值为 {@code false}，这是为了防范没有目录条目的 zip 文件——
	 * 这类文件无法提前暴露目录是否存在。如果 jar 布局一致且包含目录条目，
	 * 可将此标志切换为 {@code true} 以优化访问。
	 * @param optimizeLocations whether to optimize the locations through an existence check on startup
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 5.3.13
	 * @see ResourceHttpRequestHandler#setOptimizeLocations
	 */
	public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
		return this;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，这对于为资源 URL 应用版本策略很有用。
	 * <p>如果未调用此方法，默认仅使用一个简单的
	 * {@link PathResourceResolver} 来将 URL 路径与配置位置下的资源进行匹配。
	 * @param cacheResources whether to cache the result of resource resolution;
	 * setting this to "true" is recommended for production (and "false" for
	 * development, especially when applying a version strategy)
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
		return this.resourceChainRegistration;
	}

	/**
	 * 配置要使用的资源解析器和转换器链。例如，这对于为资源 URL 应用版本策略很有用。
	 * <p>如果未调用此方法，默认仅使用一个简单的
	 * {@link PathResourceResolver} 来将 URL 路径与配置位置下的资源进行匹配。
	 * @param cacheResources whether to cache the result of resource resolution;
	 * setting this to "true" is recommended for production (and "false" for
	 * development, especially when applying a version strategy
	 * @param cache the cache to use for storing resolved and transformed resources;
	 * by default a {@link org.springframework.cache.concurrent.ConcurrentMapCache}
	 * is used. Since Resources aren't serializable and can be dependent on the
	 * application host, one should not use a distributed cache but rather an
	 * in-memory cache.
	 * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
	 * @since 4.1
	 */
	public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
		this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
		return this.resourceChainRegistration;
	}


	/**
	 * 返回资源处理器的 URL 路径模式。
	 */
	protected String[] getPathPatterns() {
		return this.pathPatterns;
	}

	/**
	 * 返回 {@link ResourceHttpRequestHandler} 实例。
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		if (this.resourceChainRegistration != null) {
			handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
			handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
		}
		handler.setLocationValues(this.locationValues);
		handler.setLocations(this.locationsResources);
		if (this.cacheControl != null) {
			handler.setCacheControl(this.cacheControl);
		}
		else if (this.cachePeriod != null) {
			handler.setCacheSeconds(this.cachePeriod);
		}
		handler.setUseLastModified(this.useLastModified);
		handler.setOptimizeLocations(this.optimizeLocations);
		return handler;
	}

}
