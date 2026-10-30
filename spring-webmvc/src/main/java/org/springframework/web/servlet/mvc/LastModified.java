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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;

/**
 * 支持最后修改（last-modified）HTTP 请求，以促进内容缓存。
 * 约定与 Servlet API 的 {@code getLastModified} 方法相同。
 *
 * <p>由 {@link org.springframework.web.servlet.HandlerAdapter#getLastModified}
 * 实现进行委托调用。默认情况下，Spring 默认框架中的任何 Controller 或
 * HttpRequestHandler 都可以实现此接口以启用最后修改检查。
 *
 * <p><b>注意：</b>替代的处理器实现方式具有不同的最后修改处理风格。
 * 例如，Spring 2.5 基于注解的控制器方式（使用 {@code @RequestMapping}）
 * 通过 {@link org.springframework.web.context.request.WebRequest#checkNotModified}
 * 方法提供最后修改支持，允许在主处理方法中执行最后修改检查。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see javax.servlet.http.HttpServlet#getLastModified
 * @see Controller
 * @see SimpleControllerHandlerAdapter
 * @see org.springframework.web.HttpRequestHandler
 * @see HttpRequestHandlerAdapter
 * @deprecated as of 5.3.9 in favor of using the {@code checkNotModified} methods
 * in {@link org.springframework.web.context.request.WebRequest}, or from an
 * annotated controller method, returning a
 * {@link org.springframework.http.ResponseEntity} with an "ETag" and/or
 * "Last-Modified" headers set.
 */
@Deprecated
public interface LastModified {

	/**
	 * 约定与 HttpServlet 的 {@code getLastModified} 方法相同。
	 * 在请求处理<b>之前</b>调用。
	 * <p>返回值将作为 Last-Modified 头发送给 HTTP 客户端，
	 * 并与客户端回传的 If-Modified-Since 头进行比较。
	 * 只有当内容发生修改时才会重新生成。
	 * @param request 当前 HTTP 请求
	 * @return 底层资源最后修改的时间，或 -1 表示内容必须始终重新生成
	 * @see org.springframework.web.servlet.HandlerAdapter#getLastModified
	 * @see javax.servlet.http.HttpServlet#getLastModified
	 */
	long getLastModified(HttpServletRequest request);

}
