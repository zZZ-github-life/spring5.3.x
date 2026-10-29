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

package org.springframework.web.servlet.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * UriComponentsBuilder，提供额外的静态工厂方法以基于当前的 HttpServletRequest 创建链接。
 *
 * <p><strong>注意：</strong>从 5.1 开始，此类中的方法不会提取指定客户端原始地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。请使用 {@link org.springframework.web.filter.ForwardedHeaderFilter ForwardedHeaderFilter} 或底层服务器中的类似机制来提取和使用此类头，或将其丢弃。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

	@Nullable
	private String originalPath;


	/**
	 * 默认构造函数。设为 protected 以防止直接实例化。
	 * @see #fromContextPath(HttpServletRequest)
	 * @see #fromServletMapping(HttpServletRequest)
	 * @see #fromRequest(HttpServletRequest)
	 * @see #fromCurrentContextPath()
	 * @see #fromCurrentServletMapping()
 	 * @see #fromCurrentRequest()
	 */
	protected ServletUriComponentsBuilder() {
	}

	/**
	 * 创建给定 ServletUriComponentsBuilder 的深拷贝。
	 * @param other the other builder to copy from
	 */
	protected ServletUriComponentsBuilder(ServletUriComponentsBuilder other) {
		super(other);
		this.originalPath = other.originalPath;
	}


	// Factory methods based on an HttpServletRequest

	/**
	 * 根据给定 HttpServletRequest 的主机、端口、协议和上下文路径准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.replacePath(request.getContextPath());
		return builder;
	}

	/**
	 * 根据给定 HttpServletRequest 的主机、端口、协议、上下文路径和 servlet 映射准备一个构建器。
	 * <p>如果 servlet 按名称映射，例如 {@code "/main/*"}，则路径将以 "/main" 结尾。如果 servlet 以其他方式映射，例如 {@code "/"} 或 {@code "*.do"}，则结果与调用 {@link #fromContextPath(HttpServletRequest)} 相同。
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromContextPath(request);
		if (StringUtils.hasText(UrlPathHelper.defaultInstance.getPathWithinServletMapping(request))) {
			builder.path(request.getServletPath());
		}
		return builder;
	}

	/**
	 * 根据 HttpServletRequest 的主机、端口、协议和路径（但不包括查询字符串）准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(request.getRequestURI());
		return builder;
	}

	/**
	 * 通过复制 HttpServletRequest 的协议、主机、端口、路径和查询字符串来准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(request.getRequestURI());
		builder.query(request.getQueryString());
		return builder;
	}

	/**
	 * 使用协议、主机和端口（但不包括路径和查询字符串）初始化一个构建器。
	 */
	private static ServletUriComponentsBuilder initFromRequest(HttpServletRequest request) {
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();

		ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
		builder.scheme(scheme);
		builder.host(host);
		if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
			builder.port(port);
		}
		return builder;
	}


	// Alternative methods relying on RequestContextHolder to find the request

	/**
	 * 与 {@link #fromContextPath(HttpServletRequest)} 相同，只是请求是通过 {@link RequestContextHolder} 获取的。
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromServletMapping(HttpServletRequest)} 相同，只是请求是通过 {@link RequestContextHolder} 获取的。
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromRequestUri(HttpServletRequest)} 相同，只是请求是通过 {@link RequestContextHolder} 获取的。
	 */
	public static ServletUriComponentsBuilder fromCurrentRequestUri() {
		return fromRequestUri(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromRequest(HttpServletRequest)} 相同，只是请求是通过 {@link RequestContextHolder} 获取的。
	 */
	public static ServletUriComponentsBuilder fromCurrentRequest() {
		return fromRequest(getCurrentRequest());
	}

	/**
	 * 通过 {@link RequestContextHolder} 获取当前请求。
	 */
	protected static HttpServletRequest getCurrentRequest() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		return ((ServletRequestAttributes) attrs).getRequest();
	}


	private void initPath(String path) {
		this.originalPath = path;
		replacePath(path);
	}

	/**
	 * 从 {@link HttpServletRequest#getRequestURI()
	 * requestURI} 中移除任何路径扩展名。必须在调用 {@link #path(String)}
	 * 或 {@link #pathSegment(String...)} 之前调用此方法。
	 * <pre>
	 * GET http://www.foo.example/rest/books/6.json
	 *
	 * ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
	 * String ext = builder.removePathExtension();
	 * String uri = builder.path("/pages/1.{ext}").buildAndExpand(ext).toUriString();
	 * assertEquals("http://www.foo.example/rest/books/6/pages/1.json", result);
	 * </pre>
	 * @return the removed path extension for possible re-use, or {@code null}
	 * @since 4.0
	 */
	@Nullable
	public String removePathExtension() {
		String extension = null;
		if (this.originalPath != null) {
			extension = UriUtils.extractFileExtension(this.originalPath);
			if (StringUtils.hasLength(extension)) {
				int end = this.originalPath.length() - (extension.length() + 1);
				replacePath(this.originalPath.substring(0, end));
			}
			this.originalPath = null;
		}
		return extension;
	}

	@Override
	public ServletUriComponentsBuilder cloneBuilder() {
		return new ServletUriComponentsBuilder(this);
	}

}
