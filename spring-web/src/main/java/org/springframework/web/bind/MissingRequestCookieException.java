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

package org.springframework.web.bind;

import org.springframework.core.MethodParameter;

/**
 * {@link ServletRequestBindingException} 的子类，表示
 * {@code @RequestMapping} 方法参数中所期望的请求 Cookie 不存在。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see MissingRequestHeaderException
 */
@SuppressWarnings("serial")
public class MissingRequestCookieException extends MissingRequestValueException {

	private final String cookieName;

	private final MethodParameter parameter;


	/**
	 * MissingRequestCookieException 的构造函数。
	 * @param cookieName 缺失的请求 Cookie 名称
	 * @param parameter 方法参数
	 */
	public MissingRequestCookieException(String cookieName, MethodParameter parameter) {
		this(cookieName, parameter, false);
	}

	/**
	 * 当值存在但被转换为 {@code null} 时使用的构造函数。
	 * @param cookieName 缺失的请求 Cookie 名称
	 * @param parameter 方法参数
	 * @param missingAfterConversion 转换后值是否变为 null
	 * @since 5.3.6
	 */
	public MissingRequestCookieException(
			String cookieName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.cookieName = cookieName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Required cookie '" + this.cookieName + "' for method parameter type " +
				this.parameter.getNestedParameterType().getSimpleName() + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回请求 Cookie 的预期名称。
	 */
	public final String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 返回绑定到该请求 Cookie 的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
