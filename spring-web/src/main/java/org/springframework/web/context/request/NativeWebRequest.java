/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 对 {@link WebRequest} 接口的扩展，以通用方式暴露原生请求对象和响应对象。
 *
 * <p>主要用于框架内部，特别是通用的参数解析代码。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface NativeWebRequest extends WebRequest {

	/**
	 * 返回底层的原生请求对象。
	 * @see javax.servlet.http.HttpServletRequest
	 */
	Object getNativeRequest();

	/**
	 * 返回底层的原生响应对象（如果有）。
	 * @see javax.servlet.http.HttpServletResponse
	 */
	@Nullable
	Object getNativeResponse();

	/**
	 * 返回底层的原生请求对象（如果可用）。
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 * @see javax.servlet.http.HttpServletRequest
	 */
	@Nullable
	<T> T getNativeRequest(@Nullable Class<T> requiredType);

	/**
	 * 返回底层的原生响应对象（如果可用）。
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 * @see javax.servlet.http.HttpServletResponse
	 */
	@Nullable
	<T> T getNativeResponse(@Nullable Class<T> requiredType);

}
