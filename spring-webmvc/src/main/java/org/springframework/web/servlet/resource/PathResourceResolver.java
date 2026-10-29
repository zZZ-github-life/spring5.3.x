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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 简单的 {@code ResourceResolver}，它尝试在给定
 * 位置下查找与请求路径匹配的资源。
 *
 * <p>此解析器不会委托给 {@code ResourceResolverChain}，
 * 预期配置在解析器链的末尾。
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class PathResourceResolver extends AbstractResourceResolver {

	@Nullable
	private Resource[] allowedLocations;

	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	@Nullable
	private UrlPathHelper urlPathHelper;


	/**
	 * 默认情况下，找到 Resource 时，会将已解析资源的路径
	 * 与输入位置进行比较，以确保它位于找到它的输入位置之下。
	 * 但有时情况并非如此，例如当
	 * {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}
	 * 解析其包含链接的公共 URL 时，CSS 文件就是位置，
	 * 而被解析的资源是位于相邻或父目录中的 CSS 文件、图片、字体等。
	 * <p>此属性允许配置一个完整的位置列表，资源必须位于这些位置之下，
	 * 这样如果资源不在相对于其被发现的位置之下，
	 * 也可以检查此列表。
	 * <p>默认情况下，{@link ResourceHttpRequestHandler} 会将此属性
	 * 初始化为与其位置列表一致。
	 * @param locations the list of allowed locations
	 * @since 4.1.2
	 * @see ResourceHttpRequestHandler#initAllowedLocations()
	 */
	public void setAllowedLocations(@Nullable Resource... locations) {
		this.allowedLocations = locations;
	}

	@Nullable
	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}

	/**
	 * 配置与位置关联的字符集。如果在
	 * {@link org.springframework.core.io.UrlResource URL 资源}位置下找到静态资源，
	 * 则使用该字符集对相对路径进行编码。
	 * <p><strong>注意：</strong>仅当同时配置了
	 * {@link #setUrlPathHelper urlPathHelper} 属性且其 {@code urlDecode} 属性设为 true 时，
	 * 才会使用该字符集。
	 * @since 4.3.13
	 */
	public void setLocationCharsets(Map<Resource, Charset> locationCharsets) {
		this.locationCharsets.clear();
		this.locationCharsets.putAll(locationCharsets);
	}

	/**
	 * 返回与静态资源位置关联的字符集。
	 * @since 4.3.13
	 */
	public Map<Resource, Charset> getLocationCharsets() {
		return Collections.unmodifiableMap(this.locationCharsets);
	}

	/**
	 * 提供对用于将请求映射到静态资源的 {@link UrlPathHelper} 的引用。
	 * 这有助于推导查找路径的信息，例如它是否已被解码。
	 * @since 4.3.13
	 */
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 已配置的 {@link UrlPathHelper}。
	 * @since 4.3.13
	 */
	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, request, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return (StringUtils.hasText(resourcePath) &&
				getResource(resourcePath, null, locations) != null ? resourcePath : null);
	}

	@Nullable
	private Resource getResource(String resourcePath, @Nullable HttpServletRequest request,
			List<? extends Resource> locations) {

		for (Resource location : locations) {
			try {
				String pathToUse = encodeOrDecodeIfNecessary(resourcePath, request, location);
				Resource resource = getResource(pathToUse, location);
				if (resource != null) {
					return resource;
				}
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					String error = "Skip location [" + location + "] due to error";
					if (logger.isTraceEnabled()) {
						logger.trace(error, ex);
					}
					else {
						logger.debug(error + ": " + ex.getMessage());
					}
				}
			}
		}
		return null;
	}

	/**
	 * 在给定位置下查找资源。
	 * <p>默认实现会检查给定路径是否相对于该位置存在
	 * 可读的 {@code Resource}。
	 * @param resourcePath the path to the resource
	 * @param location the location to check
	 * @return the resource, or {@code null} if none found
	 */
	@Nullable
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource resource = location.createRelative(resourcePath);
		if (resource.isReadable()) {
			if (checkResource(resource, location)) {
				return resource;
			}
			else if (logger.isWarnEnabled()) {
				Resource[] allowed = getAllowedLocations();
				logger.warn(LogFormatUtils.formatValue(
						"Resource path \"" + resourcePath + "\" was successfully resolved " +
								"but resource \"" + resource.getURL() + "\" is neither under " +
								"the current location \"" + location.getURL() + "\" nor under any of " +
								"the allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
			}
		}
		return null;
	}

	/**
	 * 对已解析的资源执行附加检查，而不仅仅是检查资源
	 * 是否存在且可读。默认实现还会验证
	 * 资源位于找到它的位置之下，或者
	 * 位于 {@linkplain #setAllowedLocations allowed locations} 之一。
	 * @param resource the resource to check
	 * @param location the location relative to which the resource was found
	 * @return "true" if resource is in a valid location, "false" otherwise.
	 * @since 4.1.2
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}
		Resource[] allowedLocations = getAllowedLocations();
		if (allowedLocations != null) {
			for (Resource current : allowedLocations) {
				if (isResourceUnderLocation(resource, current)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
		if (resource.getClass() != location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		}
		else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		}
		else if (resource instanceof ServletContextResource) {
			resourcePath = ((ServletContextResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
		}
		else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}

		if (locationPath.equals(resourcePath)) {
			return true;
		}
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
	}

	private String encodeOrDecodeIfNecessary(String path, @Nullable HttpServletRequest request, Resource location) {
		if (request != null) {
			boolean usesPathPattern = (
					ServletRequestPathUtils.hasCachedPath(request) &&
					ServletRequestPathUtils.getCachedPath(request) instanceof PathContainer);

			if (shouldDecodeRelativePath(location, usesPathPattern)) {
				return UriUtils.decode(path, StandardCharsets.UTF_8);
			}
			else if (shouldEncodeRelativePath(location, usesPathPattern)) {
				Charset charset = this.locationCharsets.getOrDefault(location, StandardCharsets.UTF_8);
				StringBuilder sb = new StringBuilder();
				StringTokenizer tokenizer = new StringTokenizer(path, "/");
				while (tokenizer.hasMoreTokens()) {
					String value = UriUtils.encode(tokenizer.nextToken(), charset);
					sb.append(value);
					sb.append('/');
				}
				if (!path.endsWith("/")) {
					sb.setLength(sb.length() - 1);
				}
				return sb.toString();
			}
		}
		return path;
	}

	/**
	 * 当 {@code HandlerMapping} 设置为不解码 URL 路径时，
	 * 对于非 {@code UrlResource} 位置，需要对路径进行解码。
	 */
	private boolean shouldDecodeRelativePath(Resource location, boolean usesPathPattern) {
		return (!(location instanceof UrlResource) &&
				(usesPathPattern || (this.urlPathHelper != null && !this.urlPathHelper.isUrlDecode())));
	}

	/**
	 * 当 {@code HandlerMapping} 设置为解码 URL 路径时，
	 * 对于 {@code UrlResource} 位置，需要对路径进行编码。
	 */
	private boolean shouldEncodeRelativePath(Resource location, boolean usesPathPattern) {
		return (location instanceof UrlResource && !usesPathPattern &&
				this.urlPathHelper != null && this.urlPathHelper.isUrlDecode());
	}

	private boolean isInvalidEncodedPath(String resourcePath) {
		if (resourcePath.contains("%")) {
			// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars...
			try {
				String decodedPath = URLDecoder.decode(resourcePath, "UTF-8");
				if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
					logger.warn(LogFormatUtils.formatValue(
							"Resolved resource path contains encoded \"../\" or \"..\\\": " + resourcePath, -1, true));
					return true;
				}
			}
			catch (IllegalArgumentException ex) {
				// May not be possible to decode...
			}
			catch (UnsupportedEncodingException ex) {
				// Should never happen...
			}
		}
		return false;
	}

}
