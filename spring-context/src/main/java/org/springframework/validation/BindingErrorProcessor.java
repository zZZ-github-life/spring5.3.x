/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.validation;

import org.springframework.beans.PropertyAccessException;

/**
 * 用于处理 {@code DataBinder} 的缺失字段错误，以及将 {@code PropertyAccessException} 转换为
 * {@code FieldError} 的策略。
 *
 * <p>错误处理器是可插拔的，因此您可以根据需要以不同方式处理错误。为典型需求提供了默认实现。
 *
 * <p>注意：从 Spring 2.0 起，此接口作用于给定的 BindingResult，以兼容任何绑定策略（Bean 属性、直接
 * 字段访问等）。它仍然可以接收 BindException 作为参数（因为 BindException 也实现了 BindingResult
 * 接口），但不再直接作用于它。
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 1.2
 * @see DataBinder#setBindingErrorProcessor
 * @see DefaultBindingErrorProcessor
 * @see BindingResult
 * @see BindException
 */
public interface BindingErrorProcessor {

	/**
	 * 将缺失字段错误应用到给定的 BindException。<p>通常，会为缺失的必填字段创建字段错误。
	 * @param missingField the field that was missing during binding
	 * @param bindingResult the errors object to add the error(s) to.
	 * You can add more than just one error or maybe even ignore it.
	 * The {@code BindingResult} object features convenience utils such as
	 * a {@code resolveMessageCodes} method to resolve an error code.
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processMissingFieldError(String missingField, BindingResult bindingResult);

	/**
	 * 将给定的 {@code PropertyAccessException} 转换为注册在给定 {@code Errors} 实例上的适当错误。<p>请
	 * 注意，有两种可用的错误类型：{@code FieldError} 和 {@code ObjectError}。通常会创建字段错误，但在
	 * 某些情况下可能希望改为创建全局的 {@code ObjectError}。
	 * @param ex the {@code PropertyAccessException} to translate
	 * @param bindingResult the errors object to add the error(s) to.
	 * You can add more than just one error or maybe even ignore it.
	 * The {@code BindingResult} object features convenience utils such as
	 * a {@code resolveMessageCodes} method to resolve an error code.
	 * @see Errors
	 * @see FieldError
	 * @see ObjectError
	 * @see MessageCodesResolver
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult);

}
