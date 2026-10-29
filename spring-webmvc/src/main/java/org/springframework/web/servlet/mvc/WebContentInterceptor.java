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

package org.springframework.web.servlet.mvc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 处理器拦截器，用于检查请求是否使用受支持的方法、是否具有
 * 必需的会话，并通过应用已配置的缓存设置来准备响应。
 *
 * <p>可以通过路径模式使用
 * {@link #addCacheMapping(CacheControl, String...)} 和
 * {@link #setCacheMappings(Properties)} 为特定 URL 配置缓存设置，
 * 并通过 {@link #setCacheControl(CacheControl)} 为所有 URL
 * 回退到默认设置。
 *
 * <p>模式匹配可以使用 {@link PathMatcher}，也可以使用解析后的
 * {@link PathPattern}。两者的语法基本相同，后者更
 * 针对 Web 用途且更高效。选择哪一种取决于是否存在由
 * {@link UrlPathHelper#resolveAndCacheLookupPath resolved}
 * 的 {@code String} lookupPath，或者是由
 * {@link ServletRequestPathUtils#parseAndCache parsed} 的
 * {@code RequestPath}，而这又取决于匹配当前请求的
 * {@link HandlerMapping}。
 *
 * <p>此拦截器支持的所有设置也可以在
 * {@link AbstractController} 上设置。此拦截器主要用于对
 * 由 HandlerMapping 映射的一组控制器应用检查和准备工作。
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 27.11.2003
 * @see PathMatcher
 * @see AntPathMatcher
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

	private static PathMatcher defaultPathMatcher = new AntPathMatcher();


	private final PathPatternParser patternParser;

	private PathMatcher pathMatcher = defaultPathMatcher;

	private Map<PathPattern, Integer> cacheMappings = new HashMap<>();

	private Map<PathPattern, CacheControl> cacheControlMappings = new HashMap<>();


	/**
	 * 使用 {@link PathPatternParser#defaultInstance} 的默认构造函数。
	 */
	public WebContentInterceptor() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * 使用 {@link PathPatternParser} 解析模式的构造函数。
	 * @since 5.3
	 */
	public WebContentInterceptor(PathPatternParser parser) {
		// No restriction of HTTP methods by default,
		// in particular for use with annotated controllers...
		super(false);
		this.patternParser = parser;
	}


	/**
	 * 对已配置的 {@code UrlPathHelper} 上
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 * same property} 的快捷设置。
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
	}

	/**
	 * 对已配置的 {@code UrlPathHelper} 上
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode
	 * same property} 的快捷设置。
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
	}

	/**
	 * 设置用于解析查找路径的 UrlPathHelper。
	 * @deprecated as of 5.3, the path is resolved externally and obtained with
	 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
	 */
	@Deprecated
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
	}

	/**
	 * 配置用于将 URL 路径与已注册 URL 模式进行匹配的 PathMatcher，
	 * 以便为请求选择缓存设置。
	 * <p>这是一个高级属性，仅在需要自定义的
	 * {@link AntPathMatcher} 或自定义 PathMatcher 时才应使用。
	 * <p>默认值为 {@link AntPathMatcher}。
	 * <p><strong>注意：</strong>设置 {@code PathMatcher} 会强制使用
	 * 字符串模式匹配，即使存在已
	 * {@link ServletRequestPathUtils#parseAndCache parsed} 的 {@code RequestPath}
	 * 也是如此。
	 * @see #addCacheMapping
	 * @see #setCacheMappings
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 通过模式将缓存秒数设置映射到特定的 URL 路径。
	 * <p>覆盖此拦截器的默认缓存秒数设置。
	 * 可以指定 "-1" 以将某个 URL 路径排除在默认缓存之外。
	 * <p>有关模式语法以及各自的使用场景，请参见
	 * {@link AntPathMatcher} 和 {@link PathPattern} 以及类级 Javadoc。
	 * 两者语法基本相同，{@link PathPattern} 更针对
	 * Web 用途。
	 * <p><b>注意：</b>路径模式不应重叠。如果某个请求
	 * 匹配多个映射，实际上无法确定哪个映射会生效
	 * （因为 {@code java.util.Properties} 不保证键的顺序）。
	 * @param cacheMappings a mapping between URL paths (as keys) and
	 * cache seconds (as values, need to be integer-parsable)
	 * @see #setCacheSeconds
	 */
	public void setCacheMappings(Properties cacheMappings) {
		this.cacheMappings.clear();
		Enumeration<?> propNames = cacheMappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String path = (String) propNames.nextElement();
			int cacheSeconds = Integer.parseInt(cacheMappings.getProperty(path));
			this.cacheMappings.put(this.patternParser.parse(path), cacheSeconds);
		}
	}

	/**
	 * 将特定 URL 路径映射到特定的 {@link org.springframework.http.CacheControl}。
	 * <p>覆盖此拦截器的默认缓存秒数设置。
	 * 可以指定一个空的 {@link org.springframework.http.CacheControl} 实例
	 * 以将某个 URL 路径排除在默认缓存之外。
	 * <p>有关模式语法以及各自的使用场景，请参见
	 * {@link AntPathMatcher} 和 {@link PathPattern} 以及类级 Javadoc。
	 * 两者语法基本相同，{@link PathPattern} 更针对
	 * Web 用途。
	 * <p><b>注意：</b>路径模式不应重叠。如果某个请求
	 * 匹配多个映射，实际上无法确定哪个映射会生效
	 * （因为底层 {@code java.util.HashMap} 不保证键的顺序）。
	 * @param cacheControl the {@code CacheControl} to use
	 * @param paths the URL paths that will map to the given {@code CacheControl}
	 * @since 4.2
	 * @see #setCacheSeconds
	 */
	public void addCacheMapping(CacheControl cacheControl, String... paths) {
		for (String path : paths) {
			this.cacheControlMappings.put(this.patternParser.parse(path), cacheControl);
		}
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		checkRequest(request);

		Object path = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			path = path.toString();
		}

		if (!ObjectUtils.isEmpty(this.cacheControlMappings)) {
			CacheControl control = (path instanceof PathContainer ?
					lookupCacheControl((PathContainer) path) : lookupCacheControl((String) path));
			if (control != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Applying " + control);
				}
				applyCacheControl(response, control);
				return true;
			}
		}

		if (!ObjectUtils.isEmpty(this.cacheMappings)) {
			Integer cacheSeconds = (path instanceof PathContainer ?
					lookupCacheSeconds((PathContainer) path) : lookupCacheSeconds((String) path));
			if (cacheSeconds != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Applying cacheSeconds " + cacheSeconds);
				}
				applyCacheSeconds(response, cacheSeconds);
				return true;
			}
		}

		prepareResponse(response);
		return true;
	}

	/**
	 * 为给定已解析的 {@link PathContainer path} 查找
	 * {@link org.springframework.http.CacheControl} 实例。当
	 * {@code HandlerMapping} 使用解析后的 {@code PathPatterns} 时会使用此方法。
	 * @param path the path to match to
	 * @return the matched {@code CacheControl}, or {@code null} if no match
	 * @since 5.3
	 */
	@Nullable
	protected CacheControl lookupCacheControl(PathContainer path) {
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 为给定的字符串 lookupPath 查找
	 * {@link org.springframework.http.CacheControl} 实例。当 {@code HandlerMapping}
	 * 依赖 {@link PathMatcher} 进行字符串模式匹配时会使用此方法。
	 * @param lookupPath the path to match to
	 * @return the matched {@code CacheControl}, or {@code null} if no match
	 */
	@Nullable
	protected CacheControl lookupCacheControl(String lookupPath) {
		for (Map.Entry<PathPattern, CacheControl> entry : this.cacheControlMappings.entrySet()) {
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 为给定已解析的 {@link PathContainer path} 查找 cacheSeconds 值。
	 * 当 {@code HandlerMapping} 使用解析后的 {@code PathPatterns} 时会使用此方法。
	 * @param path the path to match to
	 * @return the matched cacheSeconds, or {@code null} if there is no match
	 * @since 5.3
	 */
	@Nullable
	protected Integer lookupCacheSeconds(PathContainer path) {
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			if (entry.getKey().matches(path)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 为给定的字符串 lookupPath 查找 cacheSeconds 实例。
	 * 当 {@code HandlerMapping} 依赖 {@link PathMatcher} 进行字符串模式
	 * 匹配时会使用此方法。
	 * @param lookupPath the path to match to
	 * @return the matched cacheSeconds, or {@code null} if there is no match
	 */
	@Nullable
	protected Integer lookupCacheSeconds(String lookupPath) {
		for (Map.Entry<PathPattern, Integer> entry : this.cacheMappings.entrySet()) {
			if (this.pathMatcher.match(entry.getKey().getPatternString(), lookupPath)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}
