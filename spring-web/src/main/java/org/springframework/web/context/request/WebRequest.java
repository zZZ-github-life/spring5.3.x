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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 通用的 Web 请求接口。主要面向通用的 Web 请求拦截器，使其能够访问通用的请求元数据，而不用于实际
 * 处理请求。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.0
 * @see WebRequestInterceptor
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * 返回给定名称的请求头，如果没有则返回 {@code null}。
	 * <p>对于多值请求头，返回第一个头值。
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 */
	@Nullable
	String getHeader(String headerName);

	/**
	 * 返回给定请求头名称的请求头值，如果没有则返回 {@code null}。
	 * <p>单值请求头将以包含单个元素的数组形式暴露。
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 */
	@Nullable
	String[] getHeaderValues(String headerName);

	/**
	 * 返回请求头名称的 Iterator。
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	Iterator<String> getHeaderNames();

	/**
	 * 返回给定名称的请求参数，如果没有则返回 {@code null}。
	 * <p>对于多值参数，返回第一个参数值。
	 * @see javax.servlet.http.HttpServletRequest#getParameter(String)
	 */
	@Nullable
	String getParameter(String paramName);

	/**
	 * 返回给定参数名称的请求参数值，如果没有则返回 {@code null}。
	 * <p>单值参数将以包含单个元素的数组形式暴露。
	 * @see javax.servlet.http.HttpServletRequest#getParameterValues(String)
	 */
	@Nullable
	String[] getParameterValues(String paramName);

	/**
	 * 返回请求参数名称的 Iterator。
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames()
	 */
	Iterator<String> getParameterNames();

	/**
	 * 返回请求参数的不可变 Map，参数名称作为 Map 键，参数值作为 Map 值。Map 值的类型为 String 数组。
	 * <p>单值参数将以包含单个元素的数组形式暴露。
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap()
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * 返回此请求的主要 Locale。
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	Locale getLocale();

	/**
	 * 返回此请求的上下文路径（通常是当前 Web 应用映射到的基路径）。
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	String getContextPath();

	/**
	 * 返回此请求的远程用户（如果有）。
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Nullable
	String getRemoteUser();

	/**
	 * 返回此请求的用户主体（如果有）。
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	Principal getUserPrincipal();

	/**
	 * 判断用户在此请求中是否属于给定角色。
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	boolean isUserInRole(String role);

	/**
	 * 返回此请求是否通过安全传输机制（如 SSL）发送。
	 * @see javax.servlet.http.HttpServletRequest#isSecure()
	 */
	boolean isSecure();

	/**
	 * 检查在给定最后修改时间戳（由应用程序确定）的情况下，请求的资源是否已被修改。
	 * <p>在适用时，这还会透明地设置 "Last-Modified" 响应头和 HTTP 状态码。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>你可以使用此 {@code #checkNotModified(long)} 方法，或者使用
	 * {@link #checkNotModified(String)}。如果希望按照 HTTP 规范的建议同时强制使用强实体标签和
	 * Last-Modified 值，则应使用 {@link #checkNotModified(String, long)}。
	 * <p>如果设置了 "If-Modified-Since" 头但无法解析为日期值，此方法将忽略该头，并继续在响应上设置最
	 * 后修改时间戳。
	 * @param lastModifiedTimestamp the last-modified timestamp in
	 * milliseconds that the application determined for the underlying
	 * resource
	 * @return whether the request qualifies as not modified,
	 * allowing to abort request processing and relying on the response
	 * telling the client that the content has not been modified
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * 检查在给定 {@code ETag}（实体标签，由应用程序确定）的情况下，请求的资源是否已被修改。
	 * <p>在适用时，这还会透明地设置 "ETag" 响应头和 HTTP 状态码。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // application-specific calculation
	 *   if (request.checkNotModified(eTag)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>注意：</strong>你可以使用此 {@code #checkNotModified(String)} 方法，或者使用
	 * {@link #checkNotModified(long)}。如果希望按照 HTTP 规范的建议同时强制使用强实体标签和
	 * Last-Modified 值，则应使用 {@link #checkNotModified(String, long)}。
	 * @param etag the entity tag that the application determined
	 * for the underlying resource. This parameter will be padded
	 * with quotes (") if necessary.
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(String etag);

	/**
	 * 检查在给定 {@code ETag}（实体标签）和最后修改时间戳（由应用程序确定）的情况下，请求的资源是否
	 * 已被修改。
	 * <p>在适用时，这还会透明地设置 "ETag" 和 "Last-Modified" 响应头以及 HTTP 状态码。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // application-specific calculation
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(eTag, lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>HTTP 规范建议同时设置 ETag 和 Last-Modified 值，
	 * 但你也可以使用 {@code #checkNotModified(String)} 或
	 * {@link #checkNotModified(long)}。
	 * @param etag the entity tag that the application determined
	 * for the underlying resource. This parameter will be padded
	 * with quotes (") if necessary.
	 * @param lastModifiedTimestamp the last-modified timestamp in
	 * milliseconds that the application determined for the underlying
	 * resource
	 * @return true if the request does not require further processing.
	 * @since 4.2
	 */
	boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp);

	/**
	 * 获取此请求的简短描述，通常包含请求 URI 和会话 id。
	 * @param includeClientInfo whether to include client-specific
	 * information such as session id and user name
	 * @return the requested description as String
	 */
	String getDescription(boolean includeClientInfo);

}
