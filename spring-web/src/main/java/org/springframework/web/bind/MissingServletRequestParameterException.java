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

/**
 * {@link ServletRequestBindingException} 的子类，表示缺少某个参数。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
@SuppressWarnings("serial")
public class MissingServletRequestParameterException extends MissingRequestValueException {

	private final String parameterName;

	private final String parameterType;


	/**
	 * MissingServletRequestParameterException 的构造函数。
	 * @param parameterName 缺失的参数名称
	 * @param parameterType 缺失参数的预期类型
	 */
	public MissingServletRequestParameterException(String parameterName, String parameterType) {
		this(parameterName, parameterType, false);
	}

	/**
	 * 当值存在但被转换为 {@code null} 时使用的构造函数。
	 * @param parameterName 缺失的参数名称
	 * @param parameterType 缺失参数的预期类型
	 * @param missingAfterConversion 转换后值是否变为 null
	 * @since 5.3.6
	 */
	public MissingServletRequestParameterException(
			String parameterName, String parameterType, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.parameterName = parameterName;
		this.parameterType = parameterType;
	}


	@Override
	public String getMessage() {
		return "Required request parameter '" + this.parameterName + "' for method parameter type " +
				this.parameterType + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回出错参数的名称。
	 */
	public final String getParameterName() {
		return this.parameterName;
	}

	/**
	 * 返回出错参数的预期类型。
	 */
	public final String getParameterType() {
		return this.parameterType;
	}

}
