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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@code HttpRequestHandler}，按照 Page Speed、YSlow 等指南以优化方式提供静态资源。
 *
 * <p>{@linkplain #setLocations "locations"} 和 {@linkplain #setLocationValues "locationValues"} 属性接受此处理器可从中提供静态资源的位置。这些位置可以相对于 Web 应用根目录，也可以来自类路径，例如 "classpath:/META-INF/public-web-resources/"，从而便于打包和提供 jar 文件中的 .js、.css 等资源。
 *
 * <p>此请求处理器还可以配置 {@link #setResourceResolvers(List) resourcesResolver} 和 {@link #setResourceTransformers(List) resourceTransformer} 链，以支持对所提供资源进行任意的解析和转换。默认情况下，{@link PathResourceResolver} 仅根据所配置的 "locations" 查找资源。应用可以配置额外的解析器和转换器，例如 {@link VersionResourceResolver}，它能够解析并准备 URL 中带有版本的资源 URL。
 *
 * <p>此处理器还会正确评估 {@code Last-Modified} 头（如果存在），从而在适当情况下返回 {@code 304} 状态码，避免对客户端已缓存的资源产生不必要的开销。
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
		implements HttpRequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";


	private final List<String> locationValues = new ArrayList<>(4);

	private final List<Resource> locationResources = new ArrayList<>(4);

	private final List<Resource> locationsToUse = new ArrayList<>(4);

	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	@Nullable
	private ResourceResolverChain resolverChain;

	@Nullable
	private ResourceTransformerChain transformerChain;

	@Nullable
	private ResourceHttpMessageConverter resourceHttpMessageConverter;

	@Nullable
	private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

	@Nullable
	private CorsConfiguration corsConfiguration;

	@Nullable
	private UrlPathHelper urlPathHelper;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;

	@Nullable
	private StringValueResolver embeddedValueResolver;


	public ResourceHttpRequestHandler() {
		super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}


	/**
	 * 配置基于字符串的资源位置，以从中提供资源。
	 * <p>例如，{{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}} 允许同时从 Web 应用根目录和类路径上任何包含 {@code /META-INF/public-web-resources/} 目录的 JAR 中提供资源，其中 Web 应用根目录中的资源优先。
	 * <p>对于 {@link org.springframework.core.io.UrlResource 基于 URL 的资源}（例如文件、HTTP URL 等），此方法支持一个特殊前缀来指示与该 URL 关联的字符集，以便追加到其后的相对路径能够被正确编码，例如 {@code "[charset=Windows-31J]https://example.org/path"}。
	 * @since 4.3.13
	 * @see #setLocations(List)
	 */
	public void setLocationValues(List<String> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locations);
	}

	/**
	 * 以预先解析的 Resource 形式配置用于提供资源的位置。
	 * @see #setLocationValues(List)
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationResources.clear();
		this.locationResources.addAll(locations);
	}

	/**
	 * 返回已配置的 {@code Resource} 位置 {@code List}，其中包括通过 {@link #setLocationValues(List) setLocationValues} 提供的基于字符串的位置，以及通过 {@link #setLocations(List) setLocations} 提供的预先解析的 {@code Resource} 位置。
	 * <p>请注意，只有在通过 {@link #afterPropertiesSet()} 初始化之后，返回的列表才是完全初始化的。
	 * <p><strong>注意：</strong>从 5.3.11 开始，位置列表可能会被过滤以排除实际不存在的位置，因此此方法返回的列表可能是所有给定位置的子集。请参阅 {@link #setOptimizeLocations}。
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
	 * <p>默认配置了 {@link PathResourceResolver}。如果使用此属性，建议将 {@link PathResourceResolver} 添加为最后一个解析器。
	 */
	public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * 返回已配置的资源解析器列表。
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * 配置要使用的 {@link ResourceTransformer ResourceTransformers} 列表。
	 * <p>默认不配置任何转换器以供使用。
	 */
	public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
		this.resourceTransformers.clear();
		if (resourceTransformers != null) {
			this.resourceTransformers.addAll(resourceTransformers);
		}
	}

	/**
	 * 返回已配置的资源转换器列表。
	 */
	public List<ResourceTransformer> getResourceTransformers() {
		return this.resourceTransformers;
	}

	/**
	 * 配置要使用的 {@link ResourceHttpMessageConverter}。
	 * <p>默认将配置一个 {@link ResourceHttpMessageConverter}。
	 * @since 4.3
	 */
	public void setResourceHttpMessageConverter(@Nullable ResourceHttpMessageConverter messageConverter) {
		this.resourceHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回已配置的资源转换器。
	 * @since 4.3
	 */
	@Nullable
	public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
		return this.resourceHttpMessageConverter;
	}

	/**
	 * 配置要使用的 {@link ResourceRegionHttpMessageConverter}。
	 * <p>默认将配置一个 {@link ResourceRegionHttpMessageConverter}。
	 * @since 4.3
	 */
	public void setResourceRegionHttpMessageConverter(@Nullable ResourceRegionHttpMessageConverter messageConverter) {
		this.resourceRegionHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回已配置的资源区域转换器。
	 * @since 4.3
	 */
	@Nullable
	public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
		return this.resourceRegionHttpMessageConverter;
	}

	/**
	 * 配置 {@code ContentNegotiationManager}，以帮助确定所提供资源的媒体类型。如果该管理器包含路径扩展名策略，则会检查其中注册的文件扩展名。
	 * @since 4.3
	 * @deprecated as of 5.2.4 in favor of using {@link #setMediaTypes(Map)}
	 * with mappings possibly obtained from
	 * {@link ContentNegotiationManager#getMediaTypeMappings()}.
	 */
	@Deprecated
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回已配置的内容协商管理器。
	 * @since 4.3
	 * @deprecated as of 5.2.4
	 */
	@Nullable
	@Deprecated
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 添加从静态 {@link Resource} 的文件名中提取的文件扩展名与要在响应中设置的相应媒体类型之间的映射。
	 * <p>通常不需要使用此方法，因为映射会通过 {@link javax.servlet.ServletContext#getMimeType(String)} 或 {@link MediaTypeFactory#getMediaType(Resource)} 来确定。
	 * @param mediaTypes 媒体类型映射
	 * @since 5.2.4
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((ext, mediaType) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), mediaType));
	}

	/**
	 * 返回{@link #setMediaTypes(Map) 已配置的}媒体类型。
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * 指定此处理器所提供资源的 CORS 配置。
	 * <p>默认情况下未设置此项，此时允许跨源请求。
	 */
	public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	/**
	 * 返回指定的 CORS 配置。
	 */
	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		return this.corsConfiguration;
	}

	/**
	 * 提供用于将请求映射到静态资源的 {@link UrlPathHelper} 引用。这有助于推导关于查找路径的信息，例如它是否已解码。
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

	/**
	 * 设置提供资源时是否应检查 {@link Resource#lastModified()}，并使用此信息来驱动 {@code "Last-Modified"} HTTP 响应头。
	 * <p>此选项默认启用；如果应忽略静态文件的元数据，则应将其关闭。
	 * @since 5.3
	 */
	public void setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
	}

	/**
	 * 返回在提供静态资源时是否使用 {@link Resource#lastModified()} 信息来驱动 HTTP 响应。
	 * @since 5.3
	 */
	public boolean isUseLastModified() {
		return this.useLastModified;
	}

	/**
	 * 设置是否在启动时通过存在性检查来优化指定的位置，提前过滤掉不存在的目录，从而无需在每次访问资源时都进行检查。
	 * <p>默认值为 {@code false}，以便防御那些没有目录条目的 zip 文件——这些文件无法提前暴露目录是否存在。如果 jar 布局一致且包含目录条目，可将此标志切换为 {@code true} 以获得优化的访问性能。
	 * @since 5.3.13
	 */
	public void setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
	}

	/**
	 * 返回是否在启动时通过存在性检查来优化指定的位置，提前过滤掉不存在的目录，从而无需在每次访问资源时都进行检查。
	 * @since 5.3.13
	 */
	public boolean isOptimizeLocations() {
		return this.optimizeLocations;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		// Initialize immutable resolver and transformer chains
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);

		if (this.resourceHttpMessageConverter == null) {
			this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
		}
		if (this.resourceRegionHttpMessageConverter == null) {
			this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
		}

		ContentNegotiationManager manager = getContentNegotiationManager();
		if (manager != null) {
			setMediaTypes(manager.getMediaTypeMappings());
		}

		@SuppressWarnings("deprecation")
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy =
				initContentNegotiationStrategy();
		if (strategy != null) {
			setMediaTypes(strategy.getMediaTypes());
		}
	}

	private void resolveResourceLocations() {
		List<Resource> result = new ArrayList<>();
		if (!this.locationValues.isEmpty()) {
			ApplicationContext applicationContext = obtainApplicationContext();
			for (String location : this.locationValues) {
				if (this.embeddedValueResolver != null) {
					String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
					if (resolvedLocation == null) {
						throw new IllegalArgumentException("Location resolved to null: " + location);
					}
					location = resolvedLocation;
				}
				Charset charset = null;
				location = location.trim();
				if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
					int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
					if (endIndex == -1) {
						throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
					}
					String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
					charset = Charset.forName(value);
					location = location.substring(endIndex + 1);
				}
				Resource resource = applicationContext.getResource(location);
				if (location.equals("/") && !(resource instanceof ServletContextResource)) {
					throw new IllegalStateException(
							"The String-based location \"/\" should be relative to the web application root " +
							"but resolved to a Resource of type: " + resource.getClass() + ". " +
							"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
				}
				result.add(resource);
				if (charset != null) {
					if (!(resource instanceof UrlResource)) {
						throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
					}
					this.locationCharsets.put(resource, charset);
				}
			}
		}

		result.addAll(this.locationResources);
		if (isOptimizeLocations()) {
			result = result.stream().filter(Resource::exists).collect(Collectors.toList());
		}

		this.locationsToUse.clear();
		this.locationsToUse.addAll(result);
	}

	/**
	 * 在已配置的资源解析器中查找 {@code PathResourceResolver}，并将其 {@code allowedLocations} 属性（如果为空）设置为与此类上配置的 {@link #setLocations 位置}相匹配。
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(getLocations())) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
					pathResolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				if (this.urlPathHelper != null) {
					pathResolver.setLocationCharsets(this.locationCharsets);
					pathResolver.setUrlPathHelper(this.urlPathHelper);
				}
				break;
			}
		}
	}

	/**
	 * 初始化用于确定资源媒体类型的策略。
	 * @deprecated as of 5.2.4 this method returns {@code null}, and if a
	 * subclass returns an actual instance, the instance is used only as a
	 * source of media type mappings, if it contains any. Please, use
	 * {@link #setMediaTypes(Map)} instead, or if you need to change behavior,
	 * you can override {@link #getMediaType(HttpServletRequest, Resource)}.
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	protected org.springframework.web.accept.PathExtensionContentNegotiationStrategy initContentNegotiationStrategy() {
		return null;
	}


	/**
	 * 处理资源请求。
	 * <p>在已配置的位置列表中检查所请求的资源是否存在。如果资源不存在，将向客户端返回 {@code 404} 响应。如果资源存在，则将检查请求中是否存在 {@code Last-Modified} 头，并将其值与给定资源的最后修改时间戳进行比较；如果 {@code Last-Modified} 值更大，则返回 {@code 304} 状态码。如果资源比 {@code Last-Modified} 值更新，或该头不存在，则将资源的内容写入响应，并将缓存头设置为一年后过期。
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// For very general mappings (e.g. "/") we need to check 404 first
		Resource resource = getResource(request);
		if (resource == null) {
			logger.debug("Resource not found");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return;
		}

		// Supported methods and required session
		checkRequest(request);

		// Header phase
		if (isUseLastModified() && new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.trace("Resource not modified");
			return;
		}

		// Apply cache settings, if any
		prepareResponse(response);

		// Check the media type for the resource
		MediaType mediaType = getMediaType(request, resource);
		setHeaders(response, resource, mediaType);

		// Content phase
		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		if (request.getHeader(HttpHeaders.RANGE) == null) {
			Assert.state(this.resourceHttpMessageConverter != null, "Not initialized");
			this.resourceHttpMessageConverter.write(resource, mediaType, outputMessage);
		}
		else {
			Assert.state(this.resourceRegionHttpMessageConverter != null, "Not initialized");
			ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
			try {
				List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				this.resourceRegionHttpMessageConverter.write(
						HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
			}
			catch (IllegalArgumentException ex) {
				response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		}
	}

	@Nullable
	protected Resource getResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		if (isInvalidEncodedPath(path)) {
			return null;
		}

		Assert.notNull(this.resolverChain, "ResourceResolverChain not initialized.");
		Assert.notNull(this.transformerChain, "ResourceTransformerChain not initialized.");

		Resource resource = this.resolverChain.resolveResource(request, path, getLocations());
		if (resource != null) {
			resource = this.transformerChain.transform(request, resource);
		}
		return resource;
	}

	/**
	 * 处理给定的资源路径。
	 * <p>默认实现会替换：
	 * <ul>
	 * <li>反斜杠替换为正斜杠。
	 * <li>重复出现的斜杠替换为单个斜杠。
	 * <li>前导斜杠与控制字符（00-1F 和 7F）的任意组合替换为单个 "/" 或 ""。例如 {@code "  / // foo/bar"} 会变为 {@code "/foo/bar"}。
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
				if ((curr == '/') && (prev == '/')) {
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
		return sb != null ? sb.toString() : path;
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
	 * @param path 要校验的路径
	 * @return 如果路径无效则返回 {@code true}，否则返回 {@code false}
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
	 * 识别无效的资源路径。默认情况下，会拒绝：
	 * <ul>
	 * <li>包含 "WEB-INF" 或 "META-INF" 的路径
	 * <li>在调用 {@link org.springframework.util.StringUtils#cleanPath} 之后包含 "../" 的路径。
	 * <li>表示{@link org.springframework.util.ResourceUtils#isUrl 有效 URL} 的路径，或在移除前导斜杠后将表示有效 URL 的路径。
	 * </ul>
	 * <p><strong>注意：</strong>此方法假定前导斜杠、重复的 '/' 或控制字符（例如空白字符）已被去除，因此路径可以预期地以单个 '/' 开头或不以斜杠开头。
	 * @param path 要校验的路径
	 * @return 如果路径无效则返回 {@code true}，否则返回 {@code false}
	 * @since 3.0.6
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

	/**
	 * 确定给定请求以及与其匹配的资源的媒体类型。此实现尝试根据资源文件名及其路径扩展名，通过以下查找之一来确定 MediaType：
	 * <ol>
	 * <li>{@link javax.servlet.ServletContext#getMimeType(String)}
	 * <li>{@link #getMediaTypes()}
	 * <li>{@link MediaTypeFactory#getMediaType(String)}
	 * </ol>
	 * @param request 当前请求
	 * @param resource 要检查的资源
	 * @return 对应的媒体类型；如果未找到，则为 {@code null}
	 */
	@Nullable
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		MediaType result = null;
		String mimeType = request.getServletContext().getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			result = MediaType.parseMediaType(mimeType);
		}
		if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
			MediaType mediaType = null;
			String filename = resource.getFilename();
			String ext = StringUtils.getFilenameExtension(filename);
			if (ext != null) {
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
			}
			if (mediaType == null) {
				List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
				if (!CollectionUtils.isEmpty(mediaTypes)) {
					mediaType = mediaTypes.get(0);
				}
			}
			if (mediaType != null) {
				result = mediaType;
			}
		}
		return result;
	}

	/**
	 * 在给定的 servlet 响应上设置头。对 GET 请求和 HEAD 请求都会调用。
	 * @param response 当前 servlet 响应
	 * @param resource 已确定的资源（永不为 {@code null}）
	 * @param mediaType 资源的媒体类型（永不为 {@code null}）
	 * @throws IOException 在设置头信息时发生错误的情况下
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}

		if (resource instanceof HttpResource) {
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			resourceHeaders.forEach((headerName, headerValues) -> {
				boolean first = true;
				for (String headerValue : headerValues) {
					if (first) {
						response.setHeader(headerName, headerValue);
					}
					else {
						response.addHeader(headerName, headerValue);
					}
					first = false;
				}
			});
		}

		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
	}


	@Override
	public String toString() {
		return "ResourceHttpRequestHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
