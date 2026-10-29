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

package org.springframework.validation.beanvalidation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;

/**
 * 适配器，接收 JSR-303 {@code javax.validator.Validator} 并将其暴露为 Spring
 * {@link org.springframework.validation.Validator}，同时也暴露原始的 JSR-303 Validator 接口本身。
 *
 * <p>可用作编程式包装器。同时作为 {@link CustomValidatorBean} 和 {@link LocalValidatorFactoryBean}
 * 的基类，并作为 {@link SmartValidator} 接口的主要实现。
 *
 * <p>从 Spring Framework 5.0 起，此适配器完全兼容 Bean Validation 1.1 以及 2.0。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see SmartValidator
 * @see CustomValidatorBean
 * @see LocalValidatorFactoryBean
 */
public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {

	private static final Set<String> internalAnnotationAttributes = new HashSet<>(4);

	static {
		internalAnnotationAttributes.add("message");
		internalAnnotationAttributes.add("groups");
		internalAnnotationAttributes.add("payload");
	}

	@Nullable
	private javax.validation.Validator targetValidator;


	/**
	 * 使用给定的 JSR-303 Validator 创建一个新的 SpringValidatorAdapter。
	 * @param targetValidator the JSR-303 Validator to wrap
	 */
	public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
		Assert.notNull(targetValidator, "Target Validator must not be null");
		this.targetValidator = targetValidator;
	}

	SpringValidatorAdapter() {
	}

	void setTargetValidator(javax.validation.Validator targetValidator) {
		this.targetValidator = targetValidator;
	}


	//---------------------------------------------------------------------
	// Implementation of Spring Validator interface
	//---------------------------------------------------------------------

	@Override
	public boolean supports(Class<?> clazz) {
		return (this.targetValidator != null);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (this.targetValidator != null) {
			processConstraintViolations(this.targetValidator.validate(target), errors);
		}
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		if (this.targetValidator != null) {
			processConstraintViolations(
					this.targetValidator.validate(target, asValidationGroups(validationHints)), errors);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void validateValue(
			Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {

		if (this.targetValidator != null) {
			processConstraintViolations(this.targetValidator.validateValue(
					(Class) targetType, fieldName, value, asValidationGroups(validationHints)), errors);
		}
	}

	/**
	 * 将指定的校验提示转换为 JSR-303 校验分组。
	 * @since 5.1
	 */
	private Class<?>[] asValidationGroups(Object... validationHints) {
		Set<Class<?>> groups = new LinkedHashSet<>(4);
		for (Object hint : validationHints) {
			if (hint instanceof Class) {
				groups.add((Class<?>) hint);
			}
		}
		return ClassUtils.toClassArray(groups);
	}

	/**
	 * 处理给定的 JSR-303 ConstraintViolations，并将相应的错误添加到提供的 Spring {@link Errors} 对象中。
	 * @param violations the JSR-303 ConstraintViolation results
	 * @param errors the Spring errors object to register to
	 */
	@SuppressWarnings("serial")
	protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
		for (ConstraintViolation<Object> violation : violations) {
			String field = determineField(violation);
			FieldError fieldError = errors.getFieldError(field);
			if (fieldError == null || !fieldError.isBindingFailure()) {
				try {
					ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
					String errorCode = determineErrorCode(cd);
					Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
					if (errors instanceof BindingResult) {
						// Can do custom FieldError registration with invalid value from ConstraintViolation,
						// as necessary for Hibernate Validator compatibility (non-indexed set path in field)
						BindingResult bindingResult = (BindingResult) errors;
						String nestedField = bindingResult.getNestedPath() + field;
						if (nestedField.isEmpty()) {
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
							ObjectError error = new ViolationObjectError(
									errors.getObjectName(), errorCodes, errorArgs, violation, this);
							bindingResult.addError(error);
						}
						else {
							Object rejectedValue = getRejectedValue(field, violation, bindingResult);
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
							FieldError error = new ViolationFieldError(errors.getObjectName(), nestedField,
									rejectedValue, errorCodes, errorArgs, violation, this);
							bindingResult.addError(error);
						}
					}
					else {
						// Got no BindingResult - can only do standard rejectValue call
						// with automatic extraction of the current field value
						errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
					}
				}
				catch (NotReadablePropertyException ex) {
					throw new IllegalStateException("JSR-303 validated property '" + field +
							"' does not have a corresponding accessor for Spring data binding - " +
							"check your DataBinder's configuration (bean property versus direct field access)", ex);
				}
			}
		}
	}

	/**
	 * 确定给定约束违反对应的字段。
	 * <p>默认实现返回字符串形式的属性路径。
	 * @param violation the current JSR-303 ConstraintViolation
	 * @return the Spring-reported field (for use with {@link Errors})
	 * @since 4.2
	 * @see javax.validation.ConstraintViolation#getPropertyPath()
	 * @see org.springframework.validation.FieldError#getField()
	 */
	protected String determineField(ConstraintViolation<Object> violation) {
		Path path = violation.getPropertyPath();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Path.Node node : path) {
			if (node.isInIterable()) {
				sb.append('[');
				Object index = node.getIndex();
				if (index == null) {
					index = node.getKey();
				}
				if (index != null) {
					sb.append(index);
				}
				sb.append(']');
			}
			String name = node.getName();
			if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
				if (!first) {
					sb.append('.');
				}
				first = false;
				sb.append(name);
			}
		}
		return sb.toString();
	}

	/**
	 * 确定给定约束描述符的由 Spring 报告的错误码。
	 * <p>默认实现返回该描述符注解类型的简单类名。请注意，配置的
	 * {@link org.springframework.validation.MessageCodesResolver} 会自动生成包含对象名和字段名的错误码
	 * 变体。
	 * @param descriptor the JSR-303 ConstraintDescriptor for the current violation
	 * @return a corresponding error code (for use with {@link Errors})
	 * @since 4.2
	 * @see javax.validation.metadata.ConstraintDescriptor#getAnnotation()
	 * @see org.springframework.validation.MessageCodesResolver
	 */
	protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
		return descriptor.getAnnotation().annotationType().getSimpleName();
	}

	/**
	 * 返回给定字段上校验错误的 FieldError 参数。对每个被违反的约束调用。
	 * <p>默认实现返回一个指示字段名的第一个参数（参见 {@link #getResolvableField}）。随后，它按属性名
	 * 的字母顺序添加所有实际的约束注解属性（即排除 "message"、"groups" 和 "payload"）。
	 * <p>可以重写此方法，例如从约束描述符中添加更多属性。
	 * @param objectName the name of the target object
	 * @param field the field that caused the binding error
	 * @param descriptor the JSR-303 constraint descriptor
	 * @return the Object array that represents the FieldError arguments
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 * @see org.springframework.validation.DefaultBindingErrorProcessor#getArgumentsForBindError
	 */
	protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
		List<Object> arguments = new ArrayList<>();
		arguments.add(getResolvableField(objectName, field));
		// Using a TreeMap for alphabetical ordering of attribute names
		Map<String, Object> attributesToExpose = new TreeMap<>();
		descriptor.getAttributes().forEach((attributeName, attributeValue) -> {
			if (!internalAnnotationAttributes.contains(attributeName)) {
				if (attributeValue instanceof String) {
					attributeValue = new ResolvableAttribute(attributeValue.toString());
				}
				attributesToExpose.put(attributeName, attributeValue);
			}
		});
		arguments.addAll(attributesToExpose.values());
		return arguments.toArray();
	}

	/**
	 * 为指定字段构建可解析的包装器，以便在 {@code MessageSource} 中解析该字段的名称。
	 * <p>默认实现返回一个指示字段的第一个参数：类型为 {@code DefaultMessageSourceResolvable}，以
	 * "objectName.field" 和 "field" 作为代码，并以普通字段名作为默认消息。
	 * @param objectName the name of the target object
	 * @param field the field that caused the binding error
	 * @return a corresponding {@code MessageSourceResolvable} for the specified field
	 * @since 4.3
	 * @see #getArgumentsForConstraint
	 */
	protected MessageSourceResolvable getResolvableField(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new DefaultMessageSourceResolvable(codes, field);
	}

	/**
	 * 提取给定约束违反背后的被拒绝值，以便通过 Spring 错误表示形式暴露出来。
	 * @param field the field that caused the binding error
	 * @param violation the corresponding JSR-303 ConstraintViolation
	 * @param bindingResult a Spring BindingResult for the backing object
	 * which contains the current field's value
	 * @return the invalid value to expose as part of the field error
	 * @since 4.2
	 * @see javax.validation.ConstraintViolation#getInvalidValue()
	 * @see org.springframework.validation.FieldError#getRejectedValue()
	 */
	@Nullable
	protected Object getRejectedValue(String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
		Object invalidValue = violation.getInvalidValue();
		if (!field.isEmpty() && !field.contains("[]") &&
				(invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
			// Possibly a bean constraint with property path: retrieve the actual property value.
			// However, explicitly avoid this for "address[]" style paths that we can't handle.
			invalidValue = bindingResult.getRawFieldValue(field);
		}
		return invalidValue;
	}

	/**
	 * 指示此违反的插值消息是否仍含占位符，因而需要对其应用 {@link java.text.MessageFormat}。当 Bean
	 * Validation 定义的消息（来自 {@code ValidationMessages.properties}）在 Spring 的 MessageSource 中
	 * 作为默认消息呈现时会调用此方法。
	 * <p>默认实现将字段名的 Spring 风格 "{0}" 占位符视为需要使用 {@link java.text.MessageFormat} 的标
	 * 志。任何其他占位符或转义语法的出现通常都是不匹配的，来自正则模式值之类。请注意，标准 Bean
	 * Validation 完全不支持 "{0}" 风格的占位符；这是通常用于 Spring MessageSource 资源包的特性。
	 * @param violation the Bean Validation constraint violation, including
	 * BV-defined interpolation of named attribute references in its message
	 * @return {@code true} if {@code java.text.MessageFormat} is to be applied,
	 * or {@code false} if the violation's message should be used as-is
	 * @since 5.1.8
	 * @see #getArgumentsForConstraint
	 */
	protected boolean requiresMessageFormat(ConstraintViolation<?> violation) {
		return containsSpringStylePlaceholder(violation.getMessage());
	}

	private static boolean containsSpringStylePlaceholder(@Nullable String message) {
		return (message != null && message.contains("{0}"));
	}


	//---------------------------------------------------------------------
	// Implementation of JSR-303 Validator interface
	//---------------------------------------------------------------------

	@Override
	public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validate(object, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateProperty(object, propertyName, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateValue(
			Class<T> beanType, String propertyName, Object value, Class<?>... groups) {

		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateValue(beanType, propertyName, value, groups);
	}

	@Override
	public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.getConstraintsForClass(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(@Nullable Class<T> type) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		try {
			return (type != null ? this.targetValidator.unwrap(type) : (T) this.targetValidator);
		}
		catch (ValidationException ex) {
			// Ignore if just being asked for plain JSR-303 Validator
			if (javax.validation.Validator.class == type) {
				return (T) this.targetValidator;
			}
			throw ex;
		}
	}

	@Override
	public ExecutableValidator forExecutables() {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.forExecutables();
	}


	/**
	 * String 属性的包装器，可通过 {@code MessageSource} 解析，否则回退到原始属性作为默认值。
	 */
	@SuppressWarnings("serial")
	private static class ResolvableAttribute implements MessageSourceResolvable, Serializable {

		private final String resolvableString;

		public ResolvableAttribute(String resolvableString) {
			this.resolvableString = resolvableString;
		}

		@Override
		public String[] getCodes() {
			return new String[] {this.resolvableString};
		}

		@Override
		@Nullable
		public Object[] getArguments() {
			return null;
		}

		@Override
		public String getDefaultMessage() {
			return this.resolvableString;
		}

		@Override
		public String toString() {
			return this.resolvableString;
		}
	}


	/**
	 * {@code ObjectError} 的子类，采用 Spring 风格的默认消息呈现。
	 */
	@SuppressWarnings("serial")
	private static class ViolationObjectError extends ObjectError implements Serializable {

		@Nullable
		private transient SpringValidatorAdapter adapter;

		@Nullable
		private transient ConstraintViolation<?> violation;

		public ViolationObjectError(String objectName, String[] codes, Object[] arguments,
				ConstraintViolation<?> violation, SpringValidatorAdapter adapter) {

			super(objectName, codes, arguments, violation.getMessage());
			this.adapter = adapter;
			this.violation = violation;
			wrap(violation);
		}

		@Override
		public boolean shouldRenderDefaultMessage() {
			return (this.adapter != null && this.violation != null ?
					this.adapter.requiresMessageFormat(this.violation) :
					containsSpringStylePlaceholder(getDefaultMessage()));
		}
	}


	/**
	 * {@code FieldError} 的子类，采用 Spring 风格的默认消息呈现。
	 */
	@SuppressWarnings("serial")
	private static class ViolationFieldError extends FieldError implements Serializable {

		@Nullable
		private transient SpringValidatorAdapter adapter;

		@Nullable
		private transient ConstraintViolation<?> violation;

		public ViolationFieldError(String objectName, String field, @Nullable Object rejectedValue, String[] codes,
				Object[] arguments, ConstraintViolation<?> violation, SpringValidatorAdapter adapter) {

			super(objectName, field, rejectedValue, false, codes, arguments, violation.getMessage());
			this.adapter = adapter;
			this.violation = violation;
			wrap(violation);
		}

		@Override
		public boolean shouldRenderDefaultMessage() {
			return (this.adapter != null && this.violation != null ?
					this.adapter.requiresMessageFormat(this.violation) :
					containsSpringStylePlaceholder(getDefaultMessage()));
		}
	}

}
