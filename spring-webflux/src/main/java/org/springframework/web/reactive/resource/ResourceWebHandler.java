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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * {@code HttpRequestHandler}，根据 Page Speed、YSlow 等指南以优化方式提供静态资源。
 *
 * <p>{@linkplain #setLocations "locations"} 属性接受一个 Spring {@link Resource} 位置列表，此处理器允许从这些位置提供静态资源。资源可以从类路径位置提供，例如 "classpath:/META-INF/public-web-resources/"，从而便于打包和提供 jar 文件中的 .js、.css 等资源。
 *
 * <p>此请求处理器还可以配置 {@link #setResourceResolvers(List) resourcesResolver} 和 {@link #setResourceTransformers(List) resourceTransformer} 链，以支持对正在提供的资源进行任意解析和转换。默认情况下，{@link PathResourceResolver} 仅根据配置的 "locations" 查找资源。应用可以配置额外的解析器和转换器，例如 {@link VersionResourceResolver}，它可以在 URL 中带有版本的情况下解析和准备资源 URL。
 *
 * <p>此处理器还会正确评估 {@code Last-Modified} 头（如果存在），以便酌情返回 {@code 304} 状态码，避免对客户端已缓存的资源产生不必要的开销。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.0
 */
public class ResourceWebHandler implements WebHandler, InitializingBean {

	private static final Set<HttpMethod> SUPPORTED_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

	private static final Log logger = LogFactory.getLog(ResourceWebHandler.class);


	@Nullable
	private ResourceLoader resourceLoader;

	private final List<String> locationValues = new ArrayList<>(4);

	private final List<Resource> locationResources = new ArrayList<>(4);

	private final List<Resource> locationsToUse = new ArrayList<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	@Nullable
	private ResourceResolverChain resolverChain;

	@Nullable
	private ResourceTransformerChain transformerChain;

	@Nullable
	private CacheControl cacheControl;

	@Nullable
	private ResourceHttpMessageWriter resourceHttpMessageWriter;

	@Nullable
	private Map<String, MediaType> mediaTypes;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;


	/**
	 * 提供用于加载{@link #setLocationValues 位置值}的 ResourceLoader。
	 * @since 5.1
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 接受要解析为 {@link Resource} 位置的基于字符串的位置值列表。
	 * @since 5.1
	 */
	public void setLocationValues(List<String> locationValues) {
		Assert.notNull(locationValues, "Location values list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locationValues);
	}

	/**
	 * 返回配置的位置值。
	 * @since 5.1
	 */
	public List<String> getLocationValues() {
		return this.locationValues;
	}

	/**
	 * 设置用作提供静态资源来源的 {@code Resource} 路径 {@code List}。
	 */
	public void setLocations(@Nullable List<Resource> locations) {
		this.locationResources.clear();
		if (locations != null) {
			this.locationResources.addAll(locations);
		}
	}

	/**
	 * 返回用作提供静态资源来源的 {@code Resource} 路径 {@code List}。
	 * <p>请注意，如果提供了{@link #setLocationValues(List) locationValues}，而不是加载基于 Resource 的位置，则在通过 {@link #afterPropertiesSet()} 初始化之前，此方法将返回空。
	 * <p><strong>注意：</strong>从 5.3.11 开始，位置列表可能会被过滤以排除实际不存在的位置，因此从此方法返回的列表可能是所有给定位置的子集。请参见 {@link #setOptimizeLocations}。
	 * @see #setLocationValues
	 * @see #setLocations
	 */
	public List<Resource> getLocations() {
		if (this.locationsToUse.isEmpty()) {
			// Possibly not yet initialized, return only what we have so far
			return this.locationResources;
		}
		return this.locationsToUse;
	}

	/**
	 * 配置要使用的 {@link ResourceResolver ResourceResolvers} 列表。
	 * <p>默认配置 {@link PathResourceResolver}。如果使用此属性，建议将 {@link PathResourceResolver} 添加为最后一个解析器。
	 */
	public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * 返回已配置资源解析器的列表。
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * 配置要使用的 {@link ResourceTransformer ResourceTransformers} 列表。
	 * <p>默认不配置任何转换器。
	 */
	public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
		this.resourceTransformers.clear();
		if (resourceTransformers != null) {
			this.resourceTransformers.addAll(resourceTransformers);
		}
	}

	/**
	 * 返回已配置资源转换器的列表。
	 */
	public List<ResourceTransformer> getResourceTransformers() {
		return this.resourceTransformers;
	}

	/**
	 * 配置要使用的 {@link ResourceHttpMessageWriter}。
	 * <p>默认将配置 {@link ResourceHttpMessageWriter}。
	 */
	public void setResourceHttpMessageWriter(@Nullable ResourceHttpMessageWriter httpMessageWriter) {
		this.resourceHttpMessageWriter = httpMessageWriter;
	}

	/**
	 * 返回配置的资源消息写出器。
	 */
	@Nullable
	public ResourceHttpMessageWriter getResourceHttpMessageWriter() {
		return this.resourceHttpMessageWriter;
	}

	/**
	 * 设置用于构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 */
	public void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * 返回用于构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 */
	@Nullable
	public CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * 设置提供资源时是否应查看 {@link Resource#lastModified()}，并使用此信息驱动 {@code "Last-Modified"} HTTP 响应头。
	 * <p>此选项默认启用；如果应忽略静态文件的元数据，则应关闭。
	 * @since 5.3
	 */
	public void setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
	}

	/**
	 * 返回提供静态资源时是否使用 {@link Resource#lastModified()} 信息驱动 HTTP 响应。
	 * @since 5.3
	 */
	public boolean isUseLastModified() {
		return this.useLastModified;
	}

	/**
	 * 设置是否在启动时通过存在性检查来优化指定的位置，预先过滤不存在的目录，从而无需在每次访问资源时都进行检查。
	 * <p>默认值为 {@code false}，以便防御没有目录条目的 zip 文件，此类文件无法预先暴露目录的存在性。如果 JAR 布局一致且包含目录条目，可将此标志切换为 {@code true} 以优化访问。
	 * @since 5.3.13
	 */
	public void setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
	}

	/**
	 * 返回是否在启动时通过存在性检查来优化指定的位置，预先过滤不存在的目录，从而无需在每次访问资源时都进行检查。
	 * @since 5.3.13
	 */
	public boolean isOptimizeLocations() {
		return this.optimizeLocations;
	}

	/**
	 * 添加从静态 {@link Resource} 的文件名中提取的文件扩展名与响应用媒体类型之间的映射。
	 * <p>通常不需要使用此方法，因为也可以通过 {@link MediaTypeFactory#getMediaType(Resource)} 确定映射。
	 * @param mediaTypes media type mappings
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		mediaTypes.forEach((ext, type) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), type));
	}

	/**
	 * 返回{@link #setMediaTypes(Map) 已配置}的媒体类型映射。
	 * @since 5.3.2
	 */
	public Map<String, MediaType> getMediaTypes() {
		return (this.mediaTypes != null ? this.mediaTypes : Collections.emptyMap());
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		if (getResourceHttpMessageWriter() == null) {
			this.resourceHttpMessageWriter = new ResourceHttpMessageWriter();
		}

		// Initialize immutable resolver and transformer chains
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);
	}

	private void resolveResourceLocations() {
		List<Resource> result = new ArrayList<>(this.locationResources);

		if (!this.locationValues.isEmpty()) {
			Assert.notNull(this.resourceLoader,
					"ResourceLoader is required when \"locationValues\" are configured.");
			Assert.isTrue(CollectionUtils.isEmpty(this.locationResources), "Please set " +
					"either Resource-based \"locations\" or String-based \"locationValues\", but not both.");
			for (String location : this.locationValues) {
				result.add(this.resourceLoader.getResource(location));
			}
		}

		if (isOptimizeLocations()) {
			result = result.stream().filter(Resource::exists).collect(Collectors.toList());
		}

		this.locationsToUse.clear();
		this.locationsToUse.addAll(result);
	}

	/**
	 * 在已配置的资源解析器中查找 {@code PathResourceResolver}，并将其 {@code allowedLocations} 属性（如果为空）设置为与此类上配置的{@link #setLocations 位置}匹配。
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(getLocations())) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver resolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(resolver.getAllowedLocations())) {
					resolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				break;
			}
		}
	}


	/**
	 * 处理资源请求。
	 * <p>检查所请求资源是否存在于配置的位置列表中。如果资源不存在，将向客户端返回 {@code 404} 响应。如果资源存在，将检查请求中是否存在 {@code Last-Modified} 头，并将其值与给定资源的最后修改时间戳进行比较；如果 {@code Last-Modified} 值更大，则返回 {@code 304} 状态码。如果资源比 {@code Last-Modified} 值更新，或者不存在该头，则将资源的内容写入响应，并将缓存头设置为一年后过期。
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return getResource(exchange)
				.switchIfEmpty(Mono.defer(() -> {
					logger.debug(exchange.getLogPrefix() + "Resource not found");
					return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
				}))
				.flatMap(resource -> {
					try {
						if (HttpMethod.OPTIONS.matches(exchange.getRequest().getMethodValue())) {
							exchange.getResponse().getHeaders().add("Allow", "GET,HEAD,OPTIONS");
							return Mono.empty();
						}

						// Supported methods and required session
						HttpMethod httpMethod = exchange.getRequest().getMethod();
						if (!SUPPORTED_METHODS.contains(httpMethod)) {
							return Mono.error(new MethodNotAllowedException(
									exchange.getRequest().getMethodValue(), SUPPORTED_METHODS));
						}

						// Header phase
						if (isUseLastModified() && exchange.checkNotModified(Instant.ofEpochMilli(resource.lastModified()))) {
							logger.trace(exchange.getLogPrefix() + "Resource not modified");
							return Mono.empty();
						}

						// Apply cache settings, if any
						CacheControl cacheControl = getCacheControl();
						if (cacheControl != null) {
							exchange.getResponse().getHeaders().setCacheControl(cacheControl);
						}

						// Check the media type for the resource
						MediaType mediaType = getMediaType(resource);
						setHeaders(exchange, resource, mediaType);

						// Content phase
						ResourceHttpMessageWriter writer = getResourceHttpMessageWriter();
						Assert.state(writer != null, "No ResourceHttpMessageWriter");
						return writer.write(Mono.just(resource),
								null, ResolvableType.forClass(Resource.class), mediaType,
								exchange.getRequest(), exchange.getResponse(),
								Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix()));
					}
					catch (IOException ex) {
						return Mono.error(ex);
					}
				});
	}

	protected Mono<Resource> getResource(ServerWebExchange exchange) {
		String name = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
		PathContainer pathWithinHandler = exchange.getRequiredAttribute(name);

		String path = processPath(pathWithinHandler.value());
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return Mono.empty();
		}
		if (isInvalidEncodedPath(path)) {
			return Mono.empty();
		}

		Assert.state(this.resolverChain != null, "ResourceResolverChain not initialized");
		Assert.state(this.transformerChain != null, "ResourceTransformerChain not initialized");

		return this.resolverChain.resolveResource(exchange, path, getLocations())
				.flatMap(resource -> this.transformerChain.transform(exchange, resource));
	}

	/**
	 * 处理给定的资源路径。
	 * <p>默认实现会替换：
	 * <ul>
	 * <li>反斜杠替换为正斜杠。
	 * <li>重复出现的斜杠替换为单个斜杠。
	 * <li>任意组合的前导斜杠和控制字符（00-1F 和 7F）替换为单个 "/" 或 ""。例如 {@code "  / // foo/bar"} 变为 {@code "/foo/bar"}。
	 * </ul>
	 * @since 3.2.12
	 */
	protected String processPath(String path) {
		path = StringUtils.replace(path, "\\", "/");
		path = cleanDuplicateSlashes(path);
		return cleanLeadingSlash(path);
	}

	private String cleanDuplicateSlashes(String path) {
		StringBuilder sb = null;
		char prev = 0;
		for (int i = 0; i < path.length(); i++) {
			char curr = path.charAt(i);
			try {
				if (curr == '/' && prev == '/') {
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					continue;
				}
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			}
			finally {
				prev = curr;
			}
		}
		return (sb != null ? sb.toString() : path);
	}

	private String cleanLeadingSlash(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				return (slash ? "/" + path.substring(i) : path.substring(i));
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * 检查给定路径是否包含无效的转义序列。
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	private boolean isInvalidEncodedPath(String path) {
		if (path.contains("%")) {
			try {
				// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				if (isInvalidPath(decodedPath)) {
					return true;
				}
				decodedPath = processPath(decodedPath);
				if (isInvalidPath(decodedPath)) {
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

	/**
	 * 识别无效的资源路径。默认拒绝：
	 * <ul>
	 * <li>包含 "WEB-INF" 或 "META-INF" 的路径
	 * <li>调用 {@link StringUtils#cleanPath} 后包含 "../" 的路径。
	 * <li>表示{@link ResourceUtils#isUrl 有效 URL}的路径，或在移除前导斜杠后将表示有效 URL 的路径。
	 * </ul>
	 * <p><strong>注意：</strong>此方法假定前导的、重复的 '/' 或控制字符（例如空白）已被修剪，因此路径可预测地以单个 '/' 开头，或者没有 '/'。
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	protected boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
			}
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				if (logger.isWarnEnabled()) {
					logger.warn(LogFormatUtils.formatValue(
							"Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
				}
				return true;
			}
		}
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
			}
			return true;
		}
		return false;
	}

	@Nullable
	private MediaType getMediaType(Resource resource) {
		MediaType mediaType = null;
		String filename = resource.getFilename();
		if (!CollectionUtils.isEmpty(this.mediaTypes)) {
			String ext = StringUtils.getFilenameExtension(filename);
			if (ext != null) {
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
			}
		}
		if (mediaType == null) {
			List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
			if (!CollectionUtils.isEmpty(mediaTypes)) {
				mediaType = mediaTypes.get(0);
			}
		}
		return mediaType;
	}

	/**
	 * 在响应上设置头。对 GET 和 HEAD 请求都会调用。
	 * @param exchange current exchange
	 * @param resource the identified resource (never {@code null})
	 * @param mediaType the resource's media type (never {@code null})
	 */
	protected void setHeaders(ServerWebExchange exchange, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		HttpHeaders headers = exchange.getResponse().getHeaders();

		long length = resource.contentLength();
		headers.setContentLength(length);

		if (mediaType != null) {
			headers.setContentType(mediaType);
		}

		if (resource instanceof HttpResource) {
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			exchange.getResponse().getHeaders().putAll(resourceHeaders);
		}
	}


	@Override
	public String toString() {
		return "ResourceWebHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
