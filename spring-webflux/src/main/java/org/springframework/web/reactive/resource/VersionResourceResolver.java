/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析包含版本字符串的请求路径，可用作 HTTP 缓存策略的一部分：资源以较远未来的日期（例如 1 年）缓存，并一直缓存到版本（因而 URL）更改。
 *
 * <p>存在不同的版本策略，此解析器必须配置一个或多个此类策略以及路径映射，以指示哪种策略适用于哪些资源。
 *
 * <p>{@code ContentVersionStrategy} 是一个良好的默认选择，除非无法使用。最值得注意的是 {@code ContentVersionStrategy} 不能与 JavaScript 模块加载器结合使用。对于这种情况，{@code FixedVersionStrategy} 是更好的选择。
 *
 * <p>请注意，使用此解析器提供 CSS 文件意味着还应使用 {@link CssLinkResourceTransformer}，以便修改 CSS 文件内的链接，使其也包含此解析器生成的适当版本。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionStrategy
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/** 从路径模式到 VersionStrategy 的 Map。 */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<>();


	/**
	 * 设置以 URL 路径为键、以 {@code VersionStrategy} 为值的 Map。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。语法详情请参见 {@link AntPathMatcher} 的 javadoc。
	 * @param map a map with URLs as keys and version strategies as values
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * 返回以路径模式为键的版本策略映射。
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * 在匹配给定路径模式的资源 URL 中插入基于内容的版本。版本根据文件内容计算，例如 {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。这是一个良好的默认策略，除非无法使用，例如使用 JavaScript 模块加载器时，请改用 {@link #addFixedVersionStrategy} 来提供 JavaScript 文件。
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
	 * @return the current instance for chained method invocation
	 * @see ContentVersionStrategy
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * 在匹配给定路径模式的资源 URL 中插入固定的、基于前缀的版本，例如：<code>"{version}/js/main.js"</code>。在使用 JavaScript 模块加载器时，这比基于内容的版本更有用。
	 * <p>版本可以是随机数、当前日期，或从 git 提交 sha、属性文件或环境变量获取的值，并在配置中使用 SpEL 表达式设置（例如参见 Java 配置中的 {@code @Value}）。
	 * <p>如果尚未这样做，还会配置以 {@code version} 为前缀的给定 {@code pathPatterns} 的变体。例如，添加 {@code "/js/**"} 路径模式还会自动配置 {@code "/v1.0.0/js/**"}，其中 {@code "v1.0.0"} 是作为参数给定的 {@code version} 字符串。
	 * @param version a version string
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
	 * @return the current instance for chained method invocation
	 * @see FixedVersionStrategy
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		List<String> patternsList = Arrays.asList(pathPatterns);
		List<String> prefixedPatterns = new ArrayList<>(pathPatterns.length);
		String versionPrefix = "/" + version;
		for (String pattern : patternsList) {
			prefixedPatterns.add(pattern);
			if (!pattern.startsWith(versionPrefix) && !patternsList.contains(versionPrefix + pattern)) {
				prefixedPatterns.add(versionPrefix + pattern);
			}
		}
		return addVersionStrategy(new FixedVersionStrategy(version), StringUtils.toStringArray(prefixedPatterns));
	}

	/**
	 * 注册自定义 VersionStrategy，以应用于匹配给定路径模式的资源 URL。
	 * @param strategy the custom strategy
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
	 * @return the current instance for chained method invocation
	 * @see VersionStrategy
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations)
				.switchIfEmpty(Mono.defer(() ->
						resolveVersionedResource(exchange, requestPath, locations, chain)));
	}

	private Mono<Resource> resolveVersionedResource(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		VersionStrategy versionStrategy = getStrategyForPath(requestPath);
		if (versionStrategy == null) {
			return Mono.empty();
		}

		String candidate = versionStrategy.extractVersion(requestPath);
		if (!StringUtils.hasLength(candidate)) {
			return Mono.empty();
		}

		String simplePath = versionStrategy.removeVersion(requestPath, candidate);
		return chain.resolveResource(exchange, simplePath, locations)
				.filterWhen(resource -> versionStrategy.getResourceVersion(resource)
						.map(actual -> {
							if (candidate.equals(actual)) {
								return true;
							}
							else {
								if (logger.isTraceEnabled()) {
									String logPrefix = exchange != null ? exchange.getLogPrefix() : "";
									logger.trace(logPrefix + "Found resource for \"" + requestPath +
											"\", but version [" + candidate + "] does not match");
								}
								return false;
							}
						}))
				.map(resource -> new FileNameVersionedResource(resource, candidate));
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations)
				.flatMap(baseUrl -> {
					if (StringUtils.hasText(baseUrl)) {
						VersionStrategy strategy = getStrategyForPath(resourceUrlPath);
						if (strategy == null) {
							return Mono.just(baseUrl);
						}
						return chain.resolveResource(null, baseUrl, locations)
								.flatMap(resource -> strategy.getResourceVersion(resource)
										.map(version -> strategy.addVersion(baseUrl, version)));
					}
					return Mono.empty();
				});
	}

	/**
	 * 为所请求资源的请求路径查找 {@code VersionStrategy}。
	 * @return an instance of a {@code VersionStrategy} or null if none matches that request path
	 */
	@Nullable
	protected VersionStrategy getStrategyForPath(String requestPath) {
		String path = "/".concat(requestPath);
		List<String> matchingPatterns = new ArrayList<>();
		for (String pattern : this.versionStrategyMap.keySet()) {
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}
		if (!matchingPatterns.isEmpty()) {
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			matchingPatterns.sort(comparator);
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}
		return null;
	}


	private static class FileNameVersionedResource extends AbstractResource implements HttpResource {

		private final Resource original;

		private final String version;

		public FileNameVersionedResource(Resource original, String version) {
			this.original = original;
			this.version = version;
		}

		@Override
		public boolean exists() {
			return this.original.exists();
		}

		@Override
		public boolean isReadable() {
			return this.original.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.original.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.original.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.original.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.original.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.original.getFile();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.original.getInputStream();
		}

		@Override
		public ReadableByteChannel readableChannel() throws IOException {
			return this.original.readableChannel();
		}

		@Override
		public long contentLength() throws IOException {
			return this.original.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.original.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.original.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.original.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers = (this.original instanceof HttpResource ?
					((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
			headers.setETag("W/\"" + this.version + "\"");
			return headers;
		}
	}

}
