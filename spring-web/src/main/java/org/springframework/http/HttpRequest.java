/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http;

import java.net.URI;

import org.springframework.lang.Nullable;

/**
 * 表示 HTTP 请求消息，由 {@linkplain #getMethod() method} 和 {@linkplain #getURI() uri} 组成。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface HttpRequest extends HttpMessage {

	/**
	 * 返回请求的 HTTP 方法。
	 * @return the HTTP method as an HttpMethod enum value, or {@code null}
	 * if not resolvable (e.g. in case of a non-standard HTTP method)
	 * @see #getMethodValue()
	 * @see HttpMethod#resolve(String)
	 */
	@Nullable
	default HttpMethod getMethod() {
		return HttpMethod.resolve(getMethodValue());
	}

	/**
	 * 以字符串值的形式返回请求的 HTTP 方法。
	 * @return the HTTP method as a plain String
	 * @since 5.0
	 * @see #getMethod()
	 */
	String getMethodValue();

	/**
	 * 返回请求的 URI（如果存在查询字符串也会包含在内，但前提是它符合 URI 表示形式的规范）。
	 * @return the URI of the request (never {@code null})
	 */
	URI getURI();

}
