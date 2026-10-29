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

package org.springframework.web.cors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@code CorsConfigurationSource}，使用 URL 路径模式为请求选择
 * {@code CorsConfiguration}。
 *
 * <p>模式匹配既可以使用 {@link PathMatcher}，也可以使用预先解析的
 * {@link PathPattern}。两者的语法大体相同，后者更适合 Web 场景且效率更高。
 * 具体选择取决于是否存在已
 * {@link UrlPathHelper#resolveAndCacheLookupPath resolved} 的 String
 * 查找路径，或已 {@link ServletRequestPathUtils#parseAndCache parsed} 的
 * {@code RequestPath}，否则会回退到 {@link PathMatcher}，但可以禁用该回退。
 * 更多详情请参阅 {@link #setAllowInitLookupPath(boolean)}。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see PathPattern
 * @see AntPathMatcher
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private static PathMatcher defaultPathMatcher = new AntPathMatcher();


	private final PathPatternParser patternParser;

	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	private PathMatcher pathMatcher = defaultPathMatcher;

	@Nullable
	private String lookupPathAttributeName;

	private boolean allowInitLookupPath = true;

	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();


	/**
	 * 使用 {@link PathPatternParser#defaultInstance} 的默认构造函数。
	 */
	public UrlBasedCorsConfigurationSource() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * 使用 {@link PathPatternParser} 解析模式的构造函数。
	 * @param parser the parser to use
	 * @since 5.3
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser parser) {
		Assert.notNull(parser, "PathPatternParser must not be null");
		this.patternParser = parser;
	}


	/**
	 * 对已配置的 {@code UrlPathHelper} 上
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 * 同名属性}的快捷方式。
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		initUrlPathHelper();
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 对已配置的 {@code UrlPathHelper} 上
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode 同名属性}
	 * 的快捷方式。
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
		initUrlPathHelper();
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 对已配置的 {@code UrlPathHelper} 上
	 * {@link org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent
	 * 同名属性}的快捷方式。
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		initUrlPathHelper();
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	private void initUrlPathHelper() {
		if (this.urlPathHelper == UrlPathHelper.defaultInstance) {
			this.urlPathHelper = new UrlPathHelper();
		}
	}

	/**
	 * 配置用于解析 lookupPath 的 {@code UrlPathHelper}。如果预期 lookupPath
	 * 已被预先解析，或者改用已解析的 {@code PathPattern}，则可能不需要设置此项。
	 * 有关更多详情，请参阅 {@link #setAllowInitLookupPath(boolean)}。
	 * <p>默认值为 {@link UrlPathHelper#defaultInstance}。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 启用后，如果既没有
	 * {@link UrlPathHelper#resolveAndCacheLookupPath esolved} 的 String 查找路径，
	 * 也没有 {@link ServletRequestPathUtils#parseAndCache parsed} 的
	 * {@code RequestPath}，则使用{@link #setUrlPathHelper 已配置}的
	 * {@code UrlPathHelper} 解析出 String 查找路径。这进而决定是使用
	 * {@link PathMatcher} 进行 URL 模式匹配，还是使用已解析的
	 * {@link PathPattern} 进行匹配。
	 * <p>在 Spring MVC 中，{@code DispatcherServlet} 处理过程中始终可以使用
	 * 已解析的 String 查找路径或已解析的 {@code RequestPath}。但在诸如
	 * {@code CorsFilter} 的 Servlet {@code Filter} 中，情况可能并非如此。
	 * <p>默认值为 {@code true}，即允许延迟初始化 lookupPath。当应用程序使用
	 * 已解析的 {@code PathPattern} 时，可将此项设置为 {@code false}，
	 * 此时可以通过
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 更早地解析 {@code RequestPath}。
	 * @param allowInitLookupPath whether to disable lazy initialization
	 * and fail if not already resolved
	 * @since 5.3
	 */
	public void setAllowInitLookupPath(boolean allowInitLookupPath) {
		this.allowInitLookupPath = allowInitLookupPath;
	}

	/**
	 * 配置用于保存通过
	 * {@link UrlPathHelper#getLookupPathForRequest(HttpServletRequest)}
	 * 提取的 lookupPath 的属性名。
	 * <p>默认值为 {@link UrlPathHelper#PATH_ATTRIBUTE}。
	 * @param name the request attribute to check
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of {@link UrlPathHelper#PATH_ATTRIBUTE}.
	 */
	@Deprecated
	public void setLookupPathAttributeName(String name) {
		this.lookupPathAttributeName = name;
	}

	/**
	 * 配置用于模式匹配的 {@code PathMatcher}。
	 * <p>这是一个高级属性，仅在需要自定义的 {@link AntPathMatcher} 或自定义
	 * PathMatcher 时才应使用。
	 * <p>默认值为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>设置 {@code PathMatcher} 会强制使用 String
	 * 模式匹配，即使已有
	 * {@link ServletRequestPathUtils#parseAndCache parsed} 的
	 * {@code RequestPath} 也是如此。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 设置 CORS 配置映射。
	 * <p>有关模式语法，请参阅 {@link AntPathMatcher} 和 {@link PathPattern}，
	 * 以及类级 Javadoc，以了解实际使用哪一种。通常两者的语法大体相同，
	 * 而 {@link PathPattern} 更适合 Web 场景。
	 * @param corsConfigurations the mappings to use
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			corsConfigurations.forEach(this::registerCorsConfiguration);
		}
	}

	/**
	 * {@link #setCorsConfigurations(Map)} 的变体，用于一次注册一个映射。
	 * @param pattern the mapping pattern
	 * @param config the CORS configuration to use for the pattern
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void registerCorsConfiguration(String pattern, CorsConfiguration config) {
		this.corsConfigurations.put(this.patternParser.parse(pattern), config);
	}

	/**
	 * 返回所有已配置的 CORS 映射。
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> result = CollectionUtils.newHashMap(this.corsConfigurations.size());
		this.corsConfigurations.forEach((pattern, config) -> result.put(pattern.getPatternString(), config));
		return Collections.unmodifiableMap(result);
	}


	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		Object path = resolvePath(request);
		boolean isPathContainer = (path instanceof PathContainer);
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (match(path, isPathContainer, entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private Object resolvePath(HttpServletRequest request) {
		if (this.allowInitLookupPath && !ServletRequestPathUtils.hasCachedPath(request)) {
			return (this.lookupPathAttributeName != null ?
					this.urlPathHelper.getLookupPathForRequest(request, this.lookupPathAttributeName) :
					this.urlPathHelper.getLookupPathForRequest(request));
		}
		Object lookupPath = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			lookupPath = lookupPath.toString();
		}
		return lookupPath;
	}

	private boolean match(Object path, boolean isPathContainer, PathPattern pattern) {
		return (isPathContainer ?
				pattern.matches((PathContainer) path) :
				this.pathMatcher.match(pattern.getPatternString(), (String) path));
	}

}
