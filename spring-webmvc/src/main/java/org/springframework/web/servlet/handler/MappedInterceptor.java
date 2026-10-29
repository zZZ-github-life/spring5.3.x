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

package org.springframework.web.servlet.handler;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

/**
 * 包装一个 {@link HandlerInterceptor}，并使用 URL 模式来确定
 * 它是否适用于给定请求。
 *
 * <p>模式匹配可以使用 {@link PathMatcher} 或解析后的
 * {@link PathPattern} 完成。两者语法大致相同，后者更
 * 针对 Web 用途定制且更高效。选择取决于是否存在
 * {@link UrlPathHelper#resolveAndCacheLookupPath 已解析}的
 * {@code String} lookupPath，或者
 * {@link ServletRequestPathUtils#parseAndCache 已解析}的 {@code RequestPath}，
 * 这又取决于匹配当前请求的 {@link HandlerMapping}。
 *
 * <p>{@code MappedInterceptor} 由
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} 的子类支持，这些子类会检测
 * {@code MappedInterceptor} 类型的 bean，并检查直接注册到其中的
 * 拦截器是否属于此类型。
 *
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.0
 */
public final class MappedInterceptor implements HandlerInterceptor {

	private static PathMatcher defaultPathMatcher = new AntPathMatcher();


	@Nullable
	private final PatternAdapter[] includePatterns;

	@Nullable
	private final PatternAdapter[] excludePatterns;

	private PathMatcher pathMatcher = defaultPathMatcher;

	private final HandlerInterceptor interceptor;


	/**
	 * 使用给定的包含和排除模式以及用于映射的目标拦截器
	 * 创建实例。
	 * @param includePatterns patterns to which requests must match, or null to
	 * match all paths
	 * @param excludePatterns patterns to which requests must not match
	 * @param interceptor the target interceptor
	 * @param parser a parser to use to pre-parse patterns into {@link PathPattern};
	 * when not provided, {@link PathPatternParser#defaultInstance} is used.
	 * @since 5.3
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {

		this.includePatterns = PatternAdapter.initPatterns(includePatterns, parser);
		this.excludePatterns = PatternAdapter.initPatterns(excludePatterns, parser);
		this.interceptor = interceptor;
	}


	/**
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * 的变体，仅使用包含模式。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * 的变体，未提供解析器。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			HandlerInterceptor interceptor) {

		this(includePatterns, excludePatterns, interceptor, null);
	}

	/**
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * 的变体，以 {@link WebRequestInterceptor} 为目标。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * {@link #MappedInterceptor(String[], String[], HandlerInterceptor, PathPatternParser)}
	 * 的变体，以 {@link WebRequestInterceptor} 为目标。
	 */
	public MappedInterceptor(@Nullable String[] includePatterns, @Nullable String[] excludePatterns,
			WebRequestInterceptor interceptor) {

		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * 返回此拦截器所映射的模式。
	 */
	@Nullable
	public String[] getPathPatterns() {
		return (!ObjectUtils.isEmpty(this.includePatterns) ?
				Arrays.stream(this.includePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
				null);
	}

	/**
	 * 匹配时要调用的目标 {@link HandlerInterceptor}。
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}

	/**
	 * 配置用于将 URL 路径与包含和排除模式进行匹配的 PathMatcher。
	 * <p>这是一个高级属性，仅在需要
	 * 自定义 {@link AntPathMatcher} 或自定义 PathMatcher 时才应使用。
	 * <p>默认情况下为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>设置 {@code PathMatcher} 会强制使用
	 * 字符串模式匹配，即使有
	 * {@link ServletRequestPathUtils#parseAndCache 已解析}的 {@code RequestPath}
	 * 可用也是如此。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 通过 {@link #setPathMatcher(PathMatcher) 配置}的 PathMatcher。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	/**
	 * 检查此拦截器是否映射到该请求。
	 * <p>预期请求映射路径已在外部解析。
	 * 另请参阅类级 Javadoc。
	 * @param request the request to match to
	 * @return {@code true} if the interceptor should be applied to the request
	 */
	public boolean matches(HttpServletRequest request) {
		Object path = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			path = path.toString();
		}
		boolean isPathContainer = (path instanceof PathContainer);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (PatternAdapter adapter : this.includePatterns) {
			if (adapter.match(path, isPathContainer, this.pathMatcher)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定查找路径的匹配结果。
	 * @param lookupPath the current request path
	 * @param pathMatcher a path matcher for path pattern matching
	 * @return {@code true} if the interceptor applies to the given request path
	 * @deprecated 自 5.3 起，建议使用 {@link #matches(HttpServletRequest)}
	 */
	@Deprecated
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		pathMatcher = (this.pathMatcher != defaultPathMatcher ? this.pathMatcher : pathMatcher);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (PatternAdapter adapter : this.excludePatterns) {
				if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (PatternAdapter adapter : this.includePatterns) {
			if (pathMatcher.match(adapter.getPatternString(), lookupPath)) {
				return true;
			}
		}
		return false;
	}


	// HandlerInterceptor delegation

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return this.interceptor.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {

		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {

		this.interceptor.afterCompletion(request, response, handler, ex);
	}


	/**
	 * 同时包含解析后的 {@link PathPattern} 和原始字符串模式，
	 * 当缓存的路径为 {@link PathContainer} 时使用前者，否则使用
	 * 后者。如果由于不支持的语法而无法解析模式，
	 * 则对所有请求都使用 {@link PathMatcher}。
	 * @since 5.3.6
	 */
	private static class PatternAdapter {

		private final String patternString;

		@Nullable
		private final PathPattern pathPattern;


		public PatternAdapter(String pattern, @Nullable PathPatternParser parser) {
			this.patternString = pattern;
			this.pathPattern = initPathPattern(pattern, parser);
		}

		@Nullable
		private static PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
			try {
				return (parser != null ? parser : PathPatternParser.defaultInstance).parse(pattern);
			}
			catch (PatternParseException ex) {
				return null;
			}
		}

		public String getPatternString() {
			return this.patternString;
		}

		public boolean match(Object path, boolean isPathContainer, PathMatcher pathMatcher) {
			if (isPathContainer) {
				PathContainer pathContainer = (PathContainer) path;
				if (this.pathPattern != null) {
					return this.pathPattern.matches(pathContainer);
				}
				String lookupPath = pathContainer.value();
				path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
			}
			return pathMatcher.match(this.patternString, (String) path);
		}

		@Nullable
		public static PatternAdapter[] initPatterns(
				@Nullable String[] patterns, @Nullable PathPatternParser parser) {

			if (ObjectUtils.isEmpty(patterns)) {
				return null;
			}
			return Arrays.stream(patterns)
					.map(pattern -> new PatternAdapter(pattern, parser))
					.toArray(PatternAdapter[]::new);
		}
	}

}
