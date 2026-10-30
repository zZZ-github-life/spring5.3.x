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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 表示响应式服务器端 HTTP 响应。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

	/**
	 * 设置响应的 HTTP 状态码。
	 * @param status 作为 {@link HttpStatus} 枚举值的 HTTP 状态
	 * @return 如果因 HTTP 响应已提交而未处理状态码更改则返回 {@code false}，
	 * 如果成功设置则返回 {@code true}。
	 */
	boolean setStatusCode(@Nullable HttpStatus status);

	/**
	 * 返回已设置的状态码，否则回退到底层服务器的响应状态。
	 * 如果状态码值超出 {@link HttpStatus} 枚举范围，或者底层服务器
	 * 没有默认值，则返回值可能为 {@code null}。
	 */
	@Nullable
	HttpStatus getStatusCode();

	/**
	 * 将 HTTP 状态码设置为给定的整数值（可能为非标准值，
	 * 无法通过 {@link HttpStatus} 枚举解析）。
	 * @param value 状态码值
	 * @return 如果因 HTTP 响应已提交而未处理状态码更改则返回 {@code false}，
	 * 如果成功设置则返回 {@code true}。
	 * @since 5.2.4
	 */
	default boolean setRawStatusCode(@Nullable Integer value) {
		if (value == null) {
			return setStatusCode(null);
		}
		else {
			HttpStatus httpStatus = HttpStatus.resolve(value);
			if (httpStatus == null) {
				throw new IllegalStateException(
						"Unresolvable HttpStatus for general ServerHttpResponse: " + value);
			}
			return setStatusCode(httpStatus);
		}
	}

	/**
	 * 返回已设置的状态码，否则回退到底层服务器的响应状态。
	 * 如果底层服务器没有默认值，则返回值可能为 {@code null}。
	 * @since 5.2.4
	 */
	@Nullable
	default Integer getRawStatusCode() {
		HttpStatus httpStatus = getStatusCode();
		return (httpStatus != null ? httpStatus.value() : null);
	}

	/**
	 * 返回要发送到服务器的 cookie 的可变 Map。
	 */
	MultiValueMap<String, ResponseCookie> getCookies();

	/**
	 * 添加给定的 {@code ResponseCookie}。
	 * @param cookie 要添加的 Cookie
	 * @throws IllegalStateException 如果响应已被提交则抛出
	 */
	void addCookie(ResponseCookie cookie);

}
