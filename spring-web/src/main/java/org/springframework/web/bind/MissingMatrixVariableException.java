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
 * {@link ServletRequestBindingException} 的子类，表示 {@code @RequestMapping}
 * 方法参数中所期望的矩阵变量在从 URL 中提取的矩阵变量中不存在。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see MissingPathVariableException
 */
@SuppressWarnings("serial")
public class MissingMatrixVariableException extends MissingRequestValueException {

	private final String variableName;

	private final MethodParameter parameter;


	/**
	 * MissingMatrixVariableException 的构造函数。
	 * @param variableName the name of the missing matrix variable
	 * @param parameter the method parameter
	 */
	public MissingMatrixVariableException(String variableName, MethodParameter parameter) {
		this(variableName, parameter, false);
	}

	/**
	 * 当值存在但被转换为 {@code null} 时使用的构造函数。
	 * @param variableName the name of the missing matrix variable
	 * @param parameter the method parameter
	 * @param missingAfterConversion whether the value became null after conversion
	 * @since 5.3.6
	 */
	public MissingMatrixVariableException(
			String variableName, MethodParameter parameter, boolean missingAfterConversion) {

		super("", missingAfterConversion);
		this.variableName = variableName;
		this.parameter = parameter;
	}


	@Override
	public String getMessage() {
		return "Required matrix variable '" + this.variableName + "' for method parameter type " +
				this.parameter.getNestedParameterType().getSimpleName() + " is " +
				(isMissingAfterConversion() ? "present but converted to null" : "not present");
	}

	/**
	 * 返回矩阵变量的预期名称。
	 */
	public final String getVariableName() {
		return this.variableName;
	}

	/**
	 * 返回绑定到该矩阵变量的方法参数。
	 */
	public final MethodParameter getParameter() {
		return this.parameter;
	}

}
