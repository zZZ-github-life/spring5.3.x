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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;

/**
 * 简单的 {@code ResourceResolver}，尝试在给定位置下查找与请求路径匹配的资源。
 *
 * <p>此解析器不委托给 {@code ResourceResolverChain}，应配置在解析器链的末尾。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class PathResourceResolver extends AbstractResourceResolver {

	@Nullable
	private Resource[] allowedLocations;


	/**
	 * 默认情况下，当找到 Resource 时，会比较所解析资源的路径，以确保它位于发现它的输入位置之下。但有时可能并非如此，例如当 {@link CssLinkResourceTransformer} 解析其包含链接的公共 URL 时，CSS 文件是位置，而所解析的资源是位于相邻或父目录中的 css 文件、图像、字体等。
	 * <p>此属性允许配置完整的资源必须位于其下的位置列表，这样如果资源不在发现它的相对位置之下，也可以检查此列表。
	 * <p>默认情况下，{@link ResourceWebHandler} 会将此属性初始化为与其位置列表匹配。
	 * @param locations the list of allowed locations
	 */
	public void setAllowedLocations(@Nullable Resource... locations) {
		this.allowedLocations = locations;
	}

	@Nullable
	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, locations);
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String path, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		if (StringUtils.hasText(path)) {
			return getResource(path, locations).map(resource -> path);
		}
		else {
			return Mono.empty();
		}
	}

	private Mono<Resource> getResource(String resourcePath, List<? extends Resource> locations) {
		return Flux.fromIterable(locations)
				.concatMap(location -> getResource(resourcePath, location))
				.next();
	}

	/**
	 * 在给定位置下查找资源。
	 * <p>默认实现检查相对于该位置的给定路径是否存在可读的 {@code Resource}。
	 * @param resourcePath the path to the resource
	 * @param location the location to check
	 * @return the resource, or empty {@link Mono} if none found
	 */
	protected Mono<Resource> getResource(String resourcePath, Resource location) {
		try {
			if (!(location instanceof UrlResource)) {
				resourcePath = UriUtils.decode(resourcePath, StandardCharsets.UTF_8);
			}
			Resource resource = location.createRelative(resourcePath);
			if (resource.isReadable()) {
				if (checkResource(resource, location)) {
					return Mono.just(resource);
				}
				else if (logger.isWarnEnabled()) {
					Resource[] allowed = getAllowedLocations();
					logger.warn(LogFormatUtils.formatValue(
							"Resource path \"" + resourcePath + "\" was successfully resolved " +
									"but resource \"" + resource.getURL() + "\" is neither under the " +
									"current location \"" + location.getURL() + "\" nor under any of the " +
									"allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
				}
			}
			return Mono.empty();
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
			return Mono.error(ex);
		}
	}

	/**
	 * 对已解析资源执行除检查资源是否存在且可读之外的其他检查。默认实现还会验证资源要么位于发现它的相对位置之下，要么位于{@link #setAllowedLocations 允许的位置}之一。
	 * @param resource the resource to check
	 * @param location the location relative to which the resource was found
	 * @return "true" if resource is in a valid location, "false" otherwise.
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}
		if (getAllowedLocations() != null) {
			for (Resource current : getAllowedLocations()) {
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
