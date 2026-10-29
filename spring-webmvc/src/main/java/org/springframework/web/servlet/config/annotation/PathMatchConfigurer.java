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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 配置路径匹配选项。这些选项将应用于以下内容：
 * <ul>
 * <li>{@link WebMvcConfigurationSupport#requestMappingHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#viewControllerHandlerMapping}</li>
 * <li>{@link WebMvcConfigurationSupport#resourceHandlerMapping}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @since 4.0.3
 */
public class PathMatchConfigurer {

	@Nullable
	private PathPatternParser patternParser;

	@Nullable
	private Boolean trailingSlashMatch;

	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;

	@Nullable
	private Boolean suffixPatternMatch;

	@Nullable
	private Boolean registeredSuffixPatternMatch;

	@Nullable
	private UrlPathHelper urlPathHelper;

	@Nullable
	private PathMatcher pathMatcher;

	@Nullable
	private PathPatternParser defaultPatternParser;

	@Nullable
	private UrlPathHelper defaultUrlPathHelper;

	@Nullable
	private PathMatcher defaultPathMatcher;


	/**
	 * 启用解析后的 {@link PathPattern} 的使用，具体如
	 * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)} 中所述。
	 * <p><strong>注意：</strong>这与使用
	 * {@link #setUrlPathHelper(UrlPathHelper)} 和
	 * {@link #setPathMatcher(PathMatcher)} 互斥。
	 * <p>默认不启用。
	 * @param patternParser the parser to pre-parse patterns with
	 * @since 5.3
	 */
	public PathMatchConfigurer setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
		return this;
	}

	/**
	 * 匹配 URL 时是否忽略尾部斜杠的存在。
	 * 如果启用，映射到 "/users" 的方法也会匹配 "/users/"。
	 * <p>默认值为 {@code true}。
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * 配置应用于匹配控制器方法的路径前缀。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，
	 * 这些方法的控制器类型需与相应的 {@code Predicate} 匹配。
	 * 使用第一个匹配的谓词所对应的前缀。
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} 对控制器进行分组。
	 * @param prefix the prefix to apply
	 * @param predicate a predicate for matching controller types
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}

	/**
	 * 将模式与请求匹配时是否使用后缀模式匹配（".*"）。
	 * 如果启用，映射到 "/users" 的方法也会匹配 "/users.*"。
	 * <p>默认设置为 {@code false}。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)}
	 * 互斥，在设置后者时将被忽略。
	 * @deprecated 自 5.2.4 起。请参见 {@link RequestMappingHandlerMapping}
	 * 类级别关于路径扩展配置选项弃用的说明。由于此方法没有替代方案，
	 * 在 5.2.x 中必须将其设置为 {@code false}。在 5.3 中默认值变为
	 * {@code false}，使用此属性变得不再必要。
	 */
	@Deprecated
	public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
		this.suffixPatternMatch = suffixPatternMatch;
		return this;
	}

	/**
	 * 后缀模式匹配是否应仅针对在
	 * {@link WebMvcConfigurer#configureContentNegotiation 配置内容协商}时
	 * 显式注册的路径扩展生效。通常建议这样做，以减少歧义并避免诸如
	 * "." 因其他原因出现在路径中时的问题。
	 * <p>默认设置为 "false"。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)}
	 * 互斥，在设置后者时将被忽略。
	 * @deprecated 自 5.2.4 起。请参见 {@link RequestMappingHandlerMapping}
	 * 类级别关于路径扩展配置选项弃用的说明。
	 */
	@Deprecated
	public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
		this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
		return this;
	}

	/**
	 * 设置用于解析应用映射路径的 UrlPathHelper。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)}
	 * 互斥，在设置后者时将被忽略。
	 */
	public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
		return this;
	}

	/**
	 * 设置用于字符串模式匹配的 PathMatcher。
	 * <p>默认为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)}
	 * 互斥，在设置后者时将被忽略。
	 */
	public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}


	/**
	 * 返回要使用的 {@link PathPatternParser}（如果已配置）。
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	@Nullable
	@Deprecated
	public Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 * @deprecated 自 5.2.4 起，请参见
	 * {@link #setUseRegisteredSuffixPatternMatch(Boolean)} 上的弃用说明。
	 */
	@Nullable
	@Deprecated
	public Boolean isUseRegisteredSuffixPatternMatch() {
		return this.registeredSuffixPatternMatch;
	}

	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 * @deprecated 自 5.2.4 起，请参见
	 * {@link #setUseSuffixPatternMatch(Boolean)} 上的弃用说明。
	 */
	@Nullable
	@Deprecated
	public Boolean isUseSuffixPatternMatch() {
		return this.suffixPatternMatch;
	}

	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	@Nullable
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 返回已配置的 UrlPathHelper，否则返回默认的共享实例。
	 * @since 5.3
	 */
	protected UrlPathHelper getUrlPathHelperOrDefault() {
		if (this.urlPathHelper != null) {
			return this.urlPathHelper;
		}
		if (this.defaultUrlPathHelper == null) {
			this.defaultUrlPathHelper = new UrlPathHelper();
		}
		return this.defaultUrlPathHelper;
	}

	/**
	 * 返回已配置的 PathMatcher，否则返回默认的共享实例。
	 * @since 5.3
	 */
	protected PathMatcher getPathMatcherOrDefault() {
		if (this.pathMatcher != null) {
			return this.pathMatcher;
		}
		if (this.defaultPathMatcher == null) {
			this.defaultPathMatcher = new AntPathMatcher();
		}
		return this.defaultPathMatcher;
	}

	/**
	 * 返回已配置的 PathPatternParser，否则返回默认的共享实例。
	 * @since 5.3.4
	 */
	public PathPatternParser getPatternParserOrDefault() {
		if (this.patternParser != null) {
			return this.patternParser;
		}
		if (this.defaultPatternParser == null) {
			this.defaultPatternParser = new PathPatternParser();
		}
		return this.defaultPatternParser;
	}
}
