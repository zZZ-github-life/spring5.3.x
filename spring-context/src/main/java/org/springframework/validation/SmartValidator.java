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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * {@link Validator} 接口的扩展变体，增加了对校验“提示”的支持。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public interface SmartValidator extends Validator {

	/**
	 * 校验所提供的 {@code target} 对象，该对象必须是某个 {@link Class} 所表示的类型，并且对于该类型，
	 * {@link #supports(Class)} 方法通常返回 {@code true}。
	 *
	 * <p>所提供的 {@link Errors errors} 实例可用于报告任何产生的校验错误。
	 *
	 * <p><b>此 {@code validate()} 变体支持校验提示，例如针对 JSR-303 提供程序的校验组</b>（此时，所提
	 * 供的提示对象需要是类型为 {@code Class} 的注解参数）。
	 *
	 * <p>注意：校验提示可能会被实际的目标 {@code Validator} 忽略，此时此方法的行为应与其常规的
	 * {@link #validate(Object, Errors)} 同级方法相同。
	 * @param target 要校验的对象。
	 * @param errors 关于校验过程的上下文状态。
	 * @param validationHints 要传递给校验引擎的一个或多个提示对象。
	 * @see javax.validation.Validator#validate(Object, Class[])
	 */
	void validate(Object target, Errors errors, Object... validationHints);

	/**
	 * 校验目标类型上指定字段的所提供值，报告与将该值绑定到目标类实例上的该字段时相同的校验错误。
	 * @param targetType 目标类型。
	 * @param fieldName 字段的名称。
	 * @param value 候选值。
	 * @param errors 关于校验过程的上下文状态。
	 * @param validationHints 要传递给校验引擎的一个或多个提示对象。
	 * @since 5.1
	 * @see javax.validation.Validator#validateValue(Class, String, Object, Class[])
	 */
	default void validateValue(
			Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {

		throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
	}

}
