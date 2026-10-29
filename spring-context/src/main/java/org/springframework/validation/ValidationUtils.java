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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 提供便捷方法的工具类，用于调用 {@link Validator} 以及拒绝空字段。
 *
 * <p>使用 {@link #rejectIfEmpty} 或 {@link #rejectIfEmptyOrWhitespace} 时，{@code Validator} 实现
 * 中的空字段检查可以变成一行代码。
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 06.05.2003
 * @see Validator
 * @see Errors
 */
public abstract class ValidationUtils {

	private static final Log logger = LogFactory.getLog(ValidationUtils.class);


	/**
	 * 为所提供的对象和 {@link Errors} 实例调用给定的 {@link Validator}。
	 * @param validator the {@code Validator} to be invoked
	 * @param target the object to bind the parameters to
	 * @param errors the {@link Errors} instance that should store the errors
	 * @throws IllegalArgumentException if either of the {@code Validator} or {@code Errors}
	 * arguments is {@code null}, or if the supplied {@code Validator} does not
	 * {@link Validator#supports(Class) support} the validation of the supplied object's type
	 */
	public static void invokeValidator(Validator validator, Object target, Errors errors) {
		invokeValidator(validator, target, errors, (Object[]) null);
	}

	/**
	 * 为所提供的对象和 {@link Errors} 实例调用给定的 {@link Validator}/{@link SmartValidator}。
	 * @param validator the {@code Validator} to be invoked
	 * @param target the object to bind the parameters to
	 * @param errors the {@link Errors} instance that should store the errors
	 * @param validationHints one or more hint objects to be passed to the validation engine
	 * @throws IllegalArgumentException if either of the {@code Validator} or {@code Errors}
	 * arguments is {@code null}, or if the supplied {@code Validator} does not
	 * {@link Validator#supports(Class) support} the validation of the supplied object's type
	 */
	public static void invokeValidator(
			Validator validator, Object target, Errors errors, @Nullable Object... validationHints) {

		Assert.notNull(validator, "Validator must not be null");
		Assert.notNull(target, "Target object must not be null");
		Assert.notNull(errors, "Errors object must not be null");

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking validator [" + validator + "]");
		}
		if (!validator.supports(target.getClass())) {
			throw new IllegalArgumentException(
					"Validator [" + validator.getClass() + "] does not support [" + target.getClass() + "]");
		}

		if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
			((SmartValidator) validator).validate(target, errors, validationHints);
		}
		else {
			validator.validate(target, errors);
		}

		if (logger.isDebugEnabled()) {
			if (errors.hasErrors()) {
				logger.debug("Validator found " + errors.getErrorCount() + " errors");
			}
			else {
				logger.debug("Validator found no errors");
			}
		}
	}


	/**
	 * 如果值为空，则用给定的错误代码拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null} 或空字符串 ""。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
		rejectIfEmpty(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空，则用给定的错误代码和默认消息拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null} 或空字符串 ""。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode error code, interpretable as message key
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
		rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空，则用给定的错误代码和错误参数拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null} 或空字符串 ""。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs) {
		rejectIfEmpty(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空，则用给定的错误代码、错误参数和默认消息拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null} 或空字符串 ""。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null || !StringUtils.hasLength(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

	/**
	 * 如果值为空或仅包含空白字符，则用给定的错误代码拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null}、空字符串 ""，或完全由空白字符组成。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 */
	public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空或仅包含空白字符，则用给定的错误代码和默认消息拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null}、空字符串 ""，或完全由空白字符组成。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, String defaultMessage) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空或仅包含空白字符，则用给定的错误代码和错误参数拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null}、空字符串 ""，或完全由空白字符组成。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, @Nullable Object[] errorArgs) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空或仅包含空白字符，则用给定的错误代码、错误参数和默认消息拒绝给定字段。
	 *
	 * <p>此上下文中的“空”值指的是 {@code null}、空字符串 ""，或完全由空白字符组成。
	 *
	 * <p>无需传入正在校验其字段的对象，因为 {@link Errors} 实例可以自行解析字段值（它通常会持有对目标
	 * 对象的内部引用）。
	 * @param errors the {@code Errors} instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null ||!StringUtils.hasText(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

}
