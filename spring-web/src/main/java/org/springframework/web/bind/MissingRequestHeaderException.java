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
 * {@code @RequestMapping} 方法参数中所期望的请求头不存在。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see MissingRequestCookieException
 */
@SuppressWarnings("serial")
public class MissingRequestHeaderException extends MissingRequestValueException {

	private final String headerName;

	private final MethodParameter parameter;


	/**
	 * MissingRequestHeaderException 的构造函数。
	 * @param headerName 缺失的请求头名称
	 * @param parameter 方法参数
	 */
	public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
		this(headerName, parameter, false);
	}

	/**
	 * 当值存在但被转换为 {@code null} 时使用的构造函数。
	 * @param headerName 缺失的请求头名称
	 * @param parameter 方法参数
	 * @param missingAfterConversion 转换后值是否变为 null
	 * @since 5.3.6
	 */
	public MissingRequestHeaderException(
			String headerName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.headerName = headerName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		String typeName = this.parameter.getNestedParameterType().getSimpleName();
		return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回请求头的预期名称。
	 */
	public final String getHeaderName() {
		return this.headerName;
	}

	/**
	 * 返回绑定到该请求头的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
