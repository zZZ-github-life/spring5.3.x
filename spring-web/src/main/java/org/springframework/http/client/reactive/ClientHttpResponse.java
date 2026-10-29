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

package org.springframework.http.client.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 表示客户端响应式 HTTP 响应。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpResponse extends ReactiveHttpInputMessage {

	/**
	 * 返回表示底层连接的 id（如果可用），
	 * 或返回请求，以便关联日志消息。
	 * @since 5.3.5
	 */
	default String getId() {
		return ObjectUtils.getIdentityHexString(this);
	}

	/**
	 * 以 {@link HttpStatus} 枚举值形式返回 HTTP 状态码。
	 * @return the HTTP status as an HttpStatus enum value (never {@code null})
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus getStatusCode();

	/**
	 * 以整数形式返回 HTTP 状态码（可能非标准且
	 * 无法通过 {@link HttpStatus} 枚举解析）。
	 * @return the HTTP status as an integer value
	 * @since 5.0.6
	 * @see #getStatusCode()
	 * @see HttpStatus#resolve(int)
	 */
	int getRawStatusCode();

	/**
	 * 返回从服务器接收的响应 Cookie 的只读映射。
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

}
