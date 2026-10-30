/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * 使用 {@link MessageConverter} 提取并转换消息载荷的解析器。
 * 如果参数带有校验注解，它还会使用 {@link Validator} 校验载荷。
 *
 * <p>此 {@link HandlerMethodArgumentResolver} 应排在最后，因为它支持所有类型，
 * 且不要求存在 {@link Payload} 注解。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 5.2
 */
public class PayloadMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	@Nullable
	private final Validator validator;

	private final boolean useDefaultResolution;


	/**
	 * 使用给定 {@link MessageConverter} 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter 要使用的 MessageConverter（必需）
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * 使用给定 {@link MessageConverter} 和 {@link Validator}
	 * 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter 要使用的 MessageConverter（必需）
	 * @param validator 要使用的 Validator（可选）
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator) {
		this(messageConverter, validator, true);
	}

	/**
	 * 使用给定 {@link MessageConverter} 和 {@link Validator}
	 * 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter 要使用的 MessageConverter（必需）
	 * @param validator 要使用的 Validator（可选）
	 * @param useDefaultResolution 如果为 "true"（默认值），则该解析器支持所有参数；
	 * 如果为 "false"，则仅支持带有 {@code @Payload} 注解的参数。
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator,
			boolean useDefaultResolution) {

		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
		this.validator = validator;
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(Payload.class) || this.useDefaultResolution);
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Payload ann = parameter.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.expression())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver");
		}

		Object payload = message.getPayload();
		if (isEmptyPayload(payload)) {
			if (ann == null || ann.required()) {
				String paramName = getParameterName(parameter);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload, paramName);
				bindingResult.addError(new ObjectError(paramName, "Payload value must not be empty"));
				throw new MethodArgumentNotValidException(message, parameter, bindingResult);
			}
			else {
				return null;
			}
		}

		Class<?> targetClass = resolveTargetClass(parameter, message);
		Class<?> payloadClass = payload.getClass();
		if (ClassUtils.isAssignable(targetClass, payloadClass)) {
			validate(message, parameter, payload);
			return payload;
		}
		else {
			if (this.converter instanceof SmartMessageConverter) {
				SmartMessageConverter smartConverter = (SmartMessageConverter) this.converter;
				payload = smartConverter.fromMessage(message, targetClass, parameter);
			}
			else {
				payload = this.converter.fromMessage(message, targetClass);
			}
			if (payload == null) {
				throw new MessageConversionException(message, "Cannot convert from [" +
						payloadClass.getName() + "] to [" + targetClass.getName() + "] for " + message);
			}
			validate(message, parameter, payload);
			return payload;
		}
	}

	private String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return (paramName != null ? paramName : "Arg " + param.getParameterIndex());
	}

	/**
	 * 指定给定 {@code payload} 是否为空。
	 * @param payload 要检查的载荷（可以为 {@code null}）
	 */
	protected boolean isEmptyPayload(@Nullable Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[]) {
			return ((byte[]) payload).length == 0;
		}
		else if (payload instanceof String) {
			return !StringUtils.hasText((String) payload);
		}
		else {
			return false;
		}
	}

	/**
	 * 解析要将载荷转换为的目标类。
	 * <p>默认情况下，这只是 {@link MethodParameter#getParameterType()}，
	 * 但可以重写此方法，以便在同时考虑 "Content-Type" 后选择更具体的目标类型，
	 * 例如当目标类型为 {@code Object} 且 {@code "Content-Type:text/**"} 时返回 {@code String}。
	 * @param parameter 目标方法参数
	 * @param message 正在处理的消息
	 * @return 要使用的目标类型
	 * @since 5.2
	 */
	protected Class<?> resolveTargetClass(MethodParameter parameter, Message<?> message) {
		return parameter.getParameterType();
	}

	/**
	 * 在适用时校验载荷。
	 * <p>默认实现会检查 {@code @javax.validation.Valid}、Spring 的 {@link Validated}
	 * 以及名称以 "Valid" 开头的自定义注解。
	 * @param message 当前正在处理的消息
	 * @param parameter 方法参数
	 * @param target 目标载荷对象
	 * @throws MethodArgumentNotValidException 如果发生绑定错误
	 */
	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				BeanPropertyBindingResult bindingResult =
						new BeanPropertyBindingResult(target, getParameterName(parameter));
				if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
				}
				else {
					this.validator.validate(target, bindingResult);
				}
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter, bindingResult);
				}
				break;
			}
		}
	}

}
