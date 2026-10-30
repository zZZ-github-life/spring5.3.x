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

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.web.servlet.resource.ResourceResolver}，它
 * 从 {@link org.springframework.cache.Cache} 解析资源，或者
 * 委托给解析器链并将结果保存到缓存中。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	/**
	 * 用于已解析资源缓存键的前缀。
	 */
	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	/**
	 * 用于已解析 URL 路径缓存键的前缀。
	 */
	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";


	private final Cache cache;

	private final List<String> contentCodings = new ArrayList<>(EncodedResourceResolver.DEFAULT_CODINGS);


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * 返回已配置的 {@code Cache}。
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * 配置要为其缓存资源变体的、来自
	 * {@literal "Accept-Encoding"} 头中受支持的内容编码。
	 * <p>此处配置的编码通常应与
	 * {@link EncodedResourceResolver#setContentCodings(List)} 上配置的编码一致。
	 * <p>默认情况下，此属性基于
	 * {@link EncodedResourceResolver#DEFAULT_CODINGS} 的值设置为 {@literal ["br", "gzip"]}。
	 * @param codings 一个或多个支持的内容编码
	 * @since 5.1
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回包含受支持内容编码的只读列表。
	 * @since 5.1
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = computeKey(request, requestPath);
		Resource resource = this.cache.get(key, Resource.class);

		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Resource resolved from cache");
			}
			return resource;
		}

		resource = chain.resolveResource(request, requestPath, locations);
		if (resource != null) {
			this.cache.put(key, resource);
		}

		return resource;
	}

	protected String computeKey(@Nullable HttpServletRequest request, String requestPath) {
		if (request != null) {
			String codingKey = getContentCodingKey(request);
			if (StringUtils.hasText(codingKey)) {
				return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=" + codingKey;
			}
		}
		return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath;
	}

	@Nullable
	private String getContentCodingKey(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
		if (!StringUtils.hasText(header)) {
			return null;
		}
		return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
				.map(token -> {
					int index = token.indexOf(';');
					return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase();
				})
				.filter(this.contentCodings::contains)
				.sorted()
				.collect(Collectors.joining(","));
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
		String resolvedUrlPath = this.cache.get(key, String.class);

		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Path resolved from cache");
			}
			return resolvedUrlPath;
		}

		resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
		if (resolvedUrlPath != null) {
			this.cache.put(key, resolvedUrlPath);
		}

		return resolvedUrlPath;
	}

}
