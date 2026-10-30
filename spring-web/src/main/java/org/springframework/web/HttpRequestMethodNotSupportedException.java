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

package org.springframework.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 当请求处理器不支持某个特定请求方法时抛出该异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException {

	private final String method;

	@Nullable
	private final String[] supportedMethods;


	/**
	 * 创建一个新的 HttpRequestMethodNotSupportedException。
	 * @param method 不受支持的 HTTP 请求方法
	 */
	public HttpRequestMethodNotSupportedException(String method) {
		this(method, (String[]) null);
	}

	/**
	 * 创建一个新的 HttpRequestMethodNotSupportedException。
	 * @param method 不受支持的 HTTP 请求方法
	 * @param msg 详细消息
	 */
	public HttpRequestMethodNotSupportedException(String method, String msg) {
		this(method, null, msg);
	}

	/**
	 * 创建一个新的 HttpRequestMethodNotSupportedException。
	 * @param method 不受支持的 HTTP 请求方法
	 * @param supportedMethods 实际支持的 HTTP 方法（可能为 {@code null}）
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
		this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
	}

	/**
	 * 创建一个新的 HttpRequestMethodNotSupportedException。
	 * @param method 不受支持的 HTTP 请求方法
	 * @param supportedMethods 实际支持的 HTTP 方法（可能为 {@code null}）
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
		this(method, supportedMethods, "Request method '" + method + "' not supported");
	}

	/**
	 * 创建一个新的 HttpRequestMethodNotSupportedException。
	 * @param method 不受支持的 HTTP 请求方法
	 * @param supportedMethods 实际支持的 HTTP 方法
	 * @param msg 详细消息
	 */
	public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods, String msg) {
		super(msg);
		this.method = method;
		this.supportedMethods = supportedMethods;
	}


	/**
	 * 返回导致失败的 HTTP 请求方法。
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回实际支持的 HTTP 方法，如果未知则返回 {@code null}。
	 */
	@Nullable
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

	/**
	 * 以 {@link HttpMethod} 实例形式返回实际支持的 HTTP 方法，
	 * 如果未知则返回 {@code null}。
	 * @since 3.2
	 */
	@Nullable
	public Set<HttpMethod> getSupportedHttpMethods() {
		if (this.supportedMethods == null) {
			return null;
		}
		List<HttpMethod> supportedMethods = new ArrayList<>(this.supportedMethods.length);
		for (String value : this.supportedMethods) {
			HttpMethod resolved = HttpMethod.resolve(value);
			if (resolved != null) {
				supportedMethods.add(resolved);
			}
		}
		return EnumSet.copyOf(supportedMethods);
	}

}
