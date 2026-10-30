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

package org.springframework.web.server;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 与特定 HTTP 响应状态码相关联的异常的基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ResponseStatusException extends NestedRuntimeException {

	private final int status;

	@Nullable
	private final String reason;


	/**
	 * 带有响应状态的构造函数。
	 * @param status HTTP 状态（必需）
	 */
	public ResponseStatusException(HttpStatus status) {
		this(status, null);
	}

	/**
	 * 带有响应状态以及添加到异常消息中作为说明的原因的构造函数。
	 * @param status HTTP 状态（必需）
	 * @param reason 关联的原因（可选）
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason) {
		super("");
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * 带有响应状态、添加到异常消息中作为说明的原因，
	 * 以及嵌套异常的构造函数。
	 * @param status HTTP 状态（必需）
	 * @param reason 关联的原因（可选）
	 * @param cause 嵌套异常（可选）
	 */
	public ResponseStatusException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		Assert.notNull(status, "HttpStatus is required");
		this.status = status.value();
		this.reason = reason;
	}

	/**
	 * 带有响应状态、添加到异常消息中作为说明的原因，
	 * 以及嵌套异常的构造函数。
	 * @param rawStatusCode HTTP 状态码值
	 * @param reason 关联的原因（可选）
	 * @param cause 嵌套异常（可选）
	 * @since 5.3
	 */
	public ResponseStatusException(int rawStatusCode, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		this.status = rawStatusCode;
		this.reason = reason;
	}


	/**
	 * 返回与此异常关联的 HTTP 状态。
	 * @throws IllegalArgumentException 如果 HTTP 状态码未知
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	public HttpStatus getStatus() {
		return HttpStatus.valueOf(this.status);
	}

	/**
	 * 以整数形式返回 HTTP 状态码（可能是非标准状态码，
	 * 无法通过 {@link HttpStatus} 枚举解析）。
	 * @return 以整数值表示的 HTTP 状态
	 * @since 5.3
	 * @see #getStatus()
	 * @see HttpStatus#resolve(int)
	 */
	public int getRawStatusCode() {
		return this.status;
	}

	/**
	 * 返回与此异常关联、应添加到错误响应中的头部，例如 "Allow"、"Accept" 等。
	 * <p>此类中的默认实现返回空 Map。
	 * @since 5.1.11
	 * @deprecated as of 5.1.13 in favor of {@link #getResponseHeaders()}
	 */
	@Deprecated
	public Map<String, String> getHeaders() {
		return Collections.emptyMap();
	}

	/**
	 * 返回与此异常关联、应添加到错误响应中的头部，例如 "Allow"、"Accept" 等。
	 * <p>此类中的默认实现返回空的头部集合。
	 * @since 5.1.13
	 */
	public HttpHeaders getResponseHeaders() {
		Map<String, String> headers = getHeaders();
		if (headers.isEmpty()) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders result = new HttpHeaders();
		getHeaders().forEach(result::add);
		return result;
	}

	/**
	 * 解释此异常的原因（可能为 {@code null} 或空）。
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}


	@Override
	public String getMessage() {
		HttpStatus code = HttpStatus.resolve(this.status);
		String msg = (code != null ? code : this.status) + (this.reason != null ? " \"" + this.reason + "\"" : "");
		return NestedExceptionUtils.buildMessage(msg, getCause());
	}

}
