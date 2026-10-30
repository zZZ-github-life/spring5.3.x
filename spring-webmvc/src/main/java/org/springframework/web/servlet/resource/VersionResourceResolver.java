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

package org.springframework.web.servlet.resource;

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

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 解析包含版本字符串的请求路径，该版本字符串可作为
 * HTTP 缓存策略的一部分：在该策略中，资源会在很久以后的日期（例如 1 年）
 * 被缓存，并一直缓存到版本（因而 URL）发生变化为止。
 *
 * <p>存在不同的版本策略，此解析器必须配置
 * 一个或多个此类策略以及路径映射，以指示哪种
 * 策略适用于哪些资源。
 *
 * <p>{@code ContentVersionStrategy} 是不错的默认选择，
 * 除非确实无法使用。最值得注意的是，{@code ContentVersionStrategy}
 * 不能与 JavaScript 模块加载器结合使用。对于这种情况，
 * {@code FixedVersionStrategy} 是更好的选择。
 *
 * <p>请注意，使用此解析器提供 CSS 文件意味着还应使用
 * {@link CssLinkResourceTransformer}，以便修改
 * CSS 文件中的链接，使其也包含此解析器生成的
 * 相应版本。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionStrategy
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/** 从路径模式到 VersionStrategy 的映射。 */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<>();


	/**
	 * 设置一个 Map，以 URL 路径为键、{@code VersionStrategy} 为值。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。有关语法
	 * 详情，请参见 {@link org.springframework.util.AntPathMatcher} 的 javadoc。
	 * @param map 以 URL 为键、以版本策略为值的映射
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * 返回以路径模式为键的版本策略 map。
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * 在匹配给定路径模式的资源 URL 中插入基于内容的版本。
	 * 版本根据文件内容计算，例如
	 * {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。这是一个不错的
	 * 默认策略，除非无法使用；例如使用
	 * JavaScript 模块加载器时，请改用 {@link #addFixedVersionStrategy}
	 * 来提供 JavaScript 文件。
	 * @param pathPatterns 一个或多个资源 URL 路径模式，
	 * 相对于资源处理器中配置的模式
	 * @return 当前实例，用于链式方法调用
	 * @see ContentVersionStrategy
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * 在匹配给定路径模式的资源 URL 中插入基于前缀的固定版本，
	 * 例如：<code>"{version}/js/main.js"</code>。在使用 JavaScript 模块加载器时，
	 * 这比基于内容的版本更有用。
	 * <p>版本可以是随机数、当前日期，或者从 git 提交 sha、属性文件或环境变量
	 * 获取的值，并通过配置中的 SpEL 表达式设置（例如参见 Java 配置中的
	 * {@code @Value}）。
	 * <p>如果尚未这样做，还会配置给定 {@code pathPatterns} 的变体，
	 * 并以 {@code version} 作为前缀。例如，添加 {@code "/js/**"} 路径模式
	 * 还会自动配置 {@code "/v1.0.0/js/**"}，其中 {@code version} 字符串
	 * 为参数中给定的 {@code "v1.0.0"}。
	 * @param version 版本字符串
	 * @param pathPatterns 一个或多个资源 URL 路径模式，
	 * 相对于资源处理器中配置的模式
	 * @return 当前实例，用于链式方法调用
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
	 * 注册一个自定义 VersionStrategy，应用于匹配
	 * 给定路径模式的资源 URL。
	 * @param strategy 自定义策略
	 * @param pathPatterns 一个或多个资源 URL 路径模式，
	 * 相对于资源处理器中配置的模式
	 * @return 当前实例，用于链式方法调用
	 * @see VersionStrategy
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		VersionStrategy versionStrategy = getStrategyForPath(requestPath);
		if (versionStrategy == null) {
			return null;
		}

		String candidateVersion = versionStrategy.extractVersion(requestPath);
		if (!StringUtils.hasLength(candidateVersion)) {
			return null;
		}

		String simplePath = versionStrategy.removeVersion(requestPath, candidateVersion);
		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			return null;
		}

		String actualVersion = versionStrategy.getResourceVersion(baseResource);
		if (candidateVersion.equals(actualVersion)) {
			return new FileNameVersionedResource(baseResource, candidateVersion);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Found resource for \"" + requestPath + "\", but version [" +
						candidateVersion + "] does not match");
			}
			return null;
		}
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			VersionStrategy versionStrategy = getStrategyForPath(resourceUrlPath);
			if (versionStrategy == null) {
				return baseUrl;
			}
			Resource resource = chain.resolveResource(null, baseUrl, locations);
			Assert.state(resource != null, "Unresolvable resource");
			String version = versionStrategy.getResourceVersion(resource);
			return versionStrategy.addVersion(baseUrl, version);
		}
		return baseUrl;
	}

	/**
	 * 为所请求资源的请求路径查找 {@code VersionStrategy}。
	 * @return 一个 {@code VersionStrategy} 实例；如果没有与该请求路径匹配的实例，则为 null
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
