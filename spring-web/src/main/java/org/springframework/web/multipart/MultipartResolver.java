/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * 按照 <a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
 * 解析 multipart 文件上传的策略接口。
 * 实现通常既可以在应用上下文中使用，也可以独立使用。
 *
 * <p>自 Spring 3.1 起，Spring 中包含两个具体实现：
 * <ul>
 * <li>{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}，
 * 用于 Apache Commons FileUpload
 * <li>{@link org.springframework.web.multipart.support.StandardServletMultipartResolver}，
 * 用于 Servlet 3.0+ Part API
 * </ul>
 *
 * <p>Spring 的 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlets}
 * 没有使用默认的解析器实现，因为应用程序可能选择自行解析其 multipart 请求。
 * 要定义实现，请在
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet} 的
 * 应用上下文中创建一个 id 为 "multipartResolver" 的 Bean。
 * 这样的解析器将应用于该
 * {@link org.springframework.web.servlet.DispatcherServlet} 处理的所有请求。
 *
 * <p>如果 {@link org.springframework.web.servlet.DispatcherServlet} 检测到
 * multipart 请求，它会通过配置的 {@link MultipartResolver} 进行解析，
 * 并传递一个包装后的 {@link javax.servlet.http.HttpServletRequest}。
 * 随后控制器可以将其请求强制转换为 {@link MultipartHttpServletRequest} 接口，
 * 从而可以访问任何 {@link MultipartFile MultipartFiles}。
 * 请注意，仅在实际的 multipart 请求中才支持此强制转换。
 *
 * <pre class="code">
 * public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
 *   MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 *
 * 除了直接访问之外，命令控制器或表单控制器还可以向其数据绑定器注册
 * {@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * 或 {@link org.springframework.web.multipart.support.StringMultipartFileEditor}，
 * 从而自动将 multipart 内容应用到表单 Bean 的属性上。
 *
 * <p>作为在 {@link org.springframework.web.servlet.DispatcherServlet}
 * 中使用 {@link MultipartResolver} 的替代方案，可以在 {@code web.xml} 中注册
 * {@link org.springframework.web.multipart.support.MultipartFilter}。
 * 它会委托给根应用上下文中相应的 {@link MultipartResolver} Bean。
 * 这主要面向未使用 Spring 自身 Web MVC 框架的应用程序。
 *
 * <p>注意：应用程序代码几乎不需要直接访问 {@link MultipartResolver} 本身。
 * 它只是在其背后默默完成工作，使控制器可以使用
 * {@link MultipartHttpServletRequest MultipartHttpServletRequests}。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see MultipartHttpServletRequest
 * @see MultipartFile
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 * @see org.springframework.web.multipart.support.ByteArrayMultipartFileEditor
 * @see org.springframework.web.multipart.support.StringMultipartFileEditor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public interface MultipartResolver {

	/**
	 * 判断给定请求是否包含 multipart 内容。
	 * <p>通常会检查内容类型是否为 "multipart/form-data"，
	 * 但实际接受的请求可能取决于解析器实现的能力。
	 * @param request 要评估的 Servlet 请求
	 * @return 该请求是否包含 multipart 内容
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * 将给定的 HTTP 请求解析为 multipart 文件和参数，
	 * 并将该请求包装在
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
	 * 对象中，该对象提供对文件描述符的访问，
	 * 并通过标准 ServletRequest 方法使所含参数可访问。
	 * @param request 要包装的 Servlet 请求（必须是 multipart 内容类型）
	 * @return 包装后的 Servlet 请求
	 * @throws MultipartException 如果 Servlet 请求不是 multipart，或遇到实现特定的问题
	 * （例如超出文件大小限制）
	 * @see MultipartHttpServletRequest#getFile
	 * @see MultipartHttpServletRequest#getFileNames
	 * @see MultipartHttpServletRequest#getFileMap
	 * @see javax.servlet.http.HttpServletRequest#getParameter
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * 清理 multipart 处理所使用的任何资源，
	 * 例如上传文件的存储。
	 * @param request 要为其清理资源的请求
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
