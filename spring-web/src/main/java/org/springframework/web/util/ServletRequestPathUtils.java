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

package org.springframework.web.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.MappingMatch;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于协助为请求映射准备和访问查找路径的实用工具类。启用
 * {@link org.springframework.web.util.pattern.PathPattern  解析模式}时，
 * 它可以是路径的已解析 {@link RequestPath} 表示；否则可以是用于
 * {@link org.springframework.util.PathMatcher} 的 String 路径。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public abstract class ServletRequestPathUtils {

	/** 保存已解析 {@link RequestPath} 的 Servlet 请求属性名称。 */
	public static final String PATH_ATTRIBUTE = ServletRequestPathUtils.class.getName() + ".PATH";


	/**
	 * 将{@link HttpServletRequest#getRequestURI() requestURI}解析为
	 * {@link RequestPath}，并将其保存到请求属性
	 * {@link #PATH_ATTRIBUTE} 中，供后续与
	 * {@link org.springframework.web.util.pattern.PathPattern 解析模式}一起使用。
	 * <p>返回的 {@code RequestPath} 在其公开的
	 * {@link RequestPath#pathWithinApplication() pathWithinApplication}
	 * 中将省略 contextPath 和任何 servletPath 前缀。
	 * <p>此方法通常由 {@code DispatcherServlet} 调用，以确定是否有任何
	 * {@code HandlerMapping} 表明其使用解析模式。之后可以通过
	 * {@link #getParsedRequestPath(ServletRequest)} 访问预解析并缓存的
	 * {@code RequestPath}。
	 */
	public static RequestPath parseAndCache(HttpServletRequest request) {
		RequestPath requestPath = ServletRequestPath.parse(request);
		request.setAttribute(PATH_ATTRIBUTE, requestPath);
		return requestPath;
	}

	/**
	 * 返回{@link #parseAndCache 先前}解析并缓存的 {@code RequestPath}。
	 * @throws IllegalArgumentException if not found
	 */
	public static RequestPath getParsedRequestPath(ServletRequest request) {
		RequestPath path = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		Assert.notNull(path, () -> "Expected parsed RequestPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
		return path;
	}

	/**
	 * 将缓存、已解析的 {@code RequestPath} 设置为给定值。
	 * @param requestPath the value to set to, or if {@code null} the cache
	 * value is cleared.
	 * @param request the current request
	 * @since 5.3.3
	 */
	public static void setParsedRequestPath(@Nullable RequestPath requestPath, ServletRequest request) {
		if (requestPath != null) {
			request.setAttribute(PATH_ATTRIBUTE, requestPath);
		}
		else {
			request.removeAttribute(PATH_ATTRIBUTE);
		}
	}

	/**
	 * 检查是否存在{@link #parseAndCache 先前}解析并缓存的 {@code RequestPath}。
	 */
	public static boolean hasParsedRequestPath(ServletRequest request) {
		return (request.getAttribute(PATH_ATTRIBUTE) != null);
	}

	/**
	 * 移除保存{@link #parseAndCache 先前}解析并缓存的
	 * {@code RequestPath} 的请求属性 {@link #PATH_ATTRIBUTE}。
	 */
	public static void clearParsedRequestPath(ServletRequest request) {
		request.removeAttribute(PATH_ATTRIBUTE);
	}


	// Methods to select either parsed RequestPath or resolved String lookupPath

	/**
	 * 返回{@link UrlPathHelper#resolveAndCacheLookupPath 预先解析}的
	 * String lookupPath，或{@link #parseAndCache(HttpServletRequest)
	 * 预先解析}的 {@code RequestPath}。
	 * <p>在 Spring MVC 中，当至少有一个 {@code HandlerMapping} 启用了
	 * 已解析的 {@code PathPatterns} 时，{@code DispatcherServlet} 会主动
	 * 解析并缓存 {@code RequestPath}，也可以更早地通过
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 完成同样的操作。在其他 {@code HandlerMapping}
	 * 使用 {@code PathMatcher} 进行 String 模式匹配的情况下，String
	 * lookupPath 由每个 {@code HandlerMapping} 分别解析。
	 * @param request the current request
	 * @return a String lookupPath or a {@code RequestPath}
	 * @throws IllegalArgumentException if neither is available
	 */
	public static Object getCachedPath(ServletRequest request) {

		// The RequestPath is pre-parsed if any HandlerMapping uses PathPatterns.
		// The lookupPath is re-resolved or cleared per HandlerMapping.
		// So check for lookupPath first.

		String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
		if (lookupPath != null) {
			return lookupPath;
		}
		RequestPath requestPath = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
		if (requestPath != null) {
			return requestPath.pathWithinApplication();
		}
		throw new IllegalArgumentException(
				"Neither a pre-parsed RequestPath nor a pre-resolved String lookupPath is available.");
	}

	/**
	 * {@link #getCachedPath(ServletRequest)} 的变体，以 String 形式返回
	 * 用于请求映射的路径。
	 * <p>如果缓存的路径是{@link #parseAndCache(HttpServletRequest)
	 * 预先解析}的 {@code RequestPath}，则返回的 String 路径值经过编码，
	 * 并移除了路径参数。
	 * <p>如果缓存的路径是 {@link UrlPathHelper#resolveAndCacheLookupPath
	 * 预先解析}的 String lookupPath，则返回的 String 路径值取决于
	 * 解析它的 {@link UrlPathHelper} 的配置。
	 * @param request the current request
	 * @return the full request mapping path as a String
	 */
	public static String getCachedPathValue(ServletRequest request) {
		Object path = getCachedPath(request);
		if (path instanceof PathContainer) {
			String value = ((PathContainer) path).value();
			path = UrlPathHelper.defaultInstance.removeSemicolonContent(value);
		}
		return (String) path;
	}

	/**
	 * 检查是否存在先前 {@link UrlPathHelper#resolveAndCacheLookupPath 解析}的
	 * String lookupPath 或先前 {@link #parseAndCache 解析}的 {@code RequestPath}。
	 * @param request the current request
	 * @return whether a pre-resolved or pre-parsed path is available
	 */
	public static boolean hasCachedPath(ServletRequest request) {
		return (request.getAttribute(PATH_ATTRIBUTE) != null ||
				request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) != null);
	}


	/**
	 * 默认 {@link RequestPath} 实现的简单包装器，支持将 servletPath
	 * 作为额外前缀从 {@link #pathWithinApplication()} 中省略。
	 */
	private static final class ServletRequestPath implements RequestPath {

		private final RequestPath requestPath;

		private final PathContainer contextPath;

		private ServletRequestPath(String rawPath, @Nullable String contextPath, String servletPathPrefix) {
			Assert.notNull(servletPathPrefix, "`servletPathPrefix` is required");
			this.requestPath = RequestPath.parse(rawPath, contextPath + servletPathPrefix);
			this.contextPath = PathContainer.parsePath(StringUtils.hasText(contextPath) ? contextPath : "");
		}

		@Override
		public String value() {
			return this.requestPath.value();
		}

		@Override
		public List<Element> elements() {
			return this.requestPath.elements();
		}

		@Override
		public PathContainer contextPath() {
			return this.contextPath;
		}

		@Override
		public PathContainer pathWithinApplication() {
			return this.requestPath.pathWithinApplication();
		}

		@Override
		public RequestPath modifyContextPath(String contextPath) {
			throw new UnsupportedOperationException();
		}


		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			return (this.requestPath.equals(((ServletRequestPath) other).requestPath));
		}

		@Override
		public int hashCode() {
			return this.requestPath.hashCode();
		}

		@Override
		public String toString() {
			return this.requestPath.toString();
		}


		public static RequestPath parse(HttpServletRequest request) {
			String requestUri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
			if (requestUri == null) {
				requestUri = request.getRequestURI();
			}
			if (UrlPathHelper.servlet4Present) {
				String servletPathPrefix = Servlet4Delegate.getServletPathPrefix(request);
				if (StringUtils.hasText(servletPathPrefix)) {
					return new ServletRequestPath(requestUri, request.getContextPath(), servletPathPrefix);
				}
			}
			return RequestPath.parse(requestUri, request.getContextPath());
		}
	}


	/**
	 * 内部类，用于避免在运行时对 Servlet 4 {@link HttpServletMapping}
	 * 和 {@link MappingMatch} 产生硬依赖。
	 */
	private static class Servlet4Delegate {

		@Nullable
		public static String getServletPathPrefix(HttpServletRequest request) {
			HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
			if (mapping == null) {
				mapping = request.getHttpServletMapping();
			}
			MappingMatch match = mapping.getMappingMatch();
			if (!ObjectUtils.nullSafeEquals(match, MappingMatch.PATH)) {
				return null;
			}
			String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
			servletPath = (servletPath != null ? servletPath : request.getServletPath());
			return UriUtils.encodePath(servletPath, StandardCharsets.UTF_8);
		}
	}

}
