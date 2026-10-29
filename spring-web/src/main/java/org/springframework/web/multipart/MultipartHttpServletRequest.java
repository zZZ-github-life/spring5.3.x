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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * 提供额外的方法来处理 Servlet 请求中的 multipart 内容，从而可以访问上传的文件。
 * 实现类还需要重写用于参数访问的标准
 * {@link javax.servlet.ServletRequest} 方法，使 multipart 参数可用。
 *
 * <p>具体实现是
 * {@link org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest}。
 * 作为中间步骤，可以继承
 * {@link org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest}。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see MultipartResolver
 * @see MultipartFile
 * @see javax.servlet.http.HttpServletRequest#getParameter
 * @see javax.servlet.http.HttpServletRequest#getParameterNames
 * @see javax.servlet.http.HttpServletRequest#getParameterMap
 * @see org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
 * @see org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest
 */
public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

	/**
	 * 以便捷的 HttpMethod 实例形式返回此请求的方法。
	 */
	@Nullable
	HttpMethod getRequestMethod();

	/**
	 * 以便捷的 HttpHeaders 实例形式返回此请求的请求头。
	 */
	HttpHeaders getRequestHeaders();

	/**
	 * 返回 multipart 请求中指定部分的请求头。
	 * <p>如果底层实现支持访问部分（part）的请求头，则返回所有请求头。
	 * 否则（例如对于文件上传），返回的请求头可能只包含可用的 'Content-Type'。
	 */
	@Nullable
	HttpHeaders getMultipartHeaders(String paramOrFileName);

}
