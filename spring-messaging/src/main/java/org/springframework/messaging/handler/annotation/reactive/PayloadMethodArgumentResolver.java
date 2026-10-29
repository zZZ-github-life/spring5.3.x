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

package org.springframework.messaging.handler.annotation.reactive;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * 使用 {@link Decoder} 提取并解码消息载荷的解析器，其中载荷应为
 * {@link DataBuffer DataBuffer} 的 {@link Publisher}。
 *
 * <p>如果方法参数使用 {@code @javax.validation.Valid} 或
 * {@link org.springframework.validation.annotation.Validated} 注解，则会应用校验。
 * 校验失败会导致 {@link MethodArgumentNotValidException}。
 *
 * <p>如果 {@link #useDefaultResolution} 设置为 {@code true}，
 * 则应将此解析器排在最后，因为此时它支持所有类型，且不要求存在 {@link Payload}。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class PayloadMethodArgumentResolver implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());


	private final List<Decoder<?>> decoders;

	@Nullable
	private final Validator validator;

	private final ReactiveAdapterRegistry adapterRegistry;

	private final boolean useDefaultResolution;


	public PayloadMethodArgumentResolver(List<? extends Decoder<?>> decoders, @Nullable Validator validator,
			@Nullable ReactiveAdapterRegistry registry, boolean useDefaultResolution) {

		Assert.isTrue(!CollectionUtils.isEmpty(decoders), "At least one Decoder is required");
		this.decoders = Collections.unmodifiableList(new ArrayList<>(decoders));
		this.validator = validator;
		this.adapterRegistry = registry != null ? registry : ReactiveAdapterRegistry.getSharedInstance();
		this.useDefaultResolution = useDefaultResolution;
	}


	/**
	 * 返回已配置解码器的只读列表。
	 */
	public List<Decoder<?>> getDecoders() {
		return this.decoders;
	}

	/**
	 * 返回已配置的校验器（如果有）。
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * 返回已配置的 {@link ReactiveAdapterRegistry}。
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}

	/**
	 * 此解析器是否配置为使用默认解析，即无论是否存在 {@code @Payload}，
	 * 都能处理任何参数类型。
	 */
	public boolean isUseDefaultResolution() {
		return this.useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Payload.class) || this.useDefaultResolution;
	}


	/**
	 * 通过兼容的 {@link Decoder} 解码给定消息载荷的内容。
	 *
	 * <p>如果方法参数使用 {@code @javax.validation.Valid} 或
	 * {@link org.springframework.validation.annotation.Validated} 注解，则会应用校验。
	 * 校验失败会导致 {@link MethodArgumentNotValidException}。
	 * @param parameter the target method argument that we are decoding to
	 * @param message the message from which the content was extracted
	 * @return a Mono with the result of argument resolution
	 * @see #extractContent(MethodParameter, Message)
	 * @see #getMimeType(Message)
	 */
	@Override
	public final Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {

		Payload ann = parameter.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.expression())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver");
		}

		MimeType mimeType = getMimeType(message);
		mimeType = mimeType != null ? mimeType : MimeTypeUtils.APPLICATION_OCTET_STREAM;

		Flux<DataBuffer> content = extractContent(parameter, message);
		return decodeContent(parameter, message, ann == null || ann.required(), content, mimeType);
	}

	@SuppressWarnings("unchecked")
	private Flux<DataBuffer> extractContent(MethodParameter parameter, Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof DataBuffer) {
			return Flux.just((DataBuffer) payload);
		}
		if (payload instanceof Publisher) {
			return Flux.from((Publisher<?>) payload).map(value -> {
				if (value instanceof DataBuffer) {
					return (DataBuffer) value;
				}
				String className = value.getClass().getName();
				throw getUnexpectedPayloadError(message, parameter, "Publisher<" + className + ">");
			});
		}
		return Flux.error(getUnexpectedPayloadError(message, parameter, payload.getClass().getName()));
	}

	private MethodArgumentResolutionException getUnexpectedPayloadError(
			Message<?> message, MethodParameter parameter, String actualType) {

		return new MethodArgumentResolutionException(message, parameter,
				"Expected DataBuffer or Publisher<DataBuffer> for the Message payload, actual: " + actualType);
	}

	/**
	 * 返回内容的 MIME 类型。默认情况下，此方法检查
	 * {@link MessageHeaders#CONTENT_TYPE} 头，期望找到 {@link MimeType} 值
	 * 或可解析为 {@link MimeType} 的 String。
	 * @param message the input message
	 */
	@Nullable
	protected MimeType getMimeType(Message<?> message) {
		Object headerValue = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		if (headerValue == null) {
			return null;
		}
		else if (headerValue instanceof String) {
			return MimeTypeUtils.parseMimeType((String) headerValue);
		}
		else if (headerValue instanceof MimeType) {
			return (MimeType) headerValue;
		}
		else {
			throw new IllegalArgumentException("Unexpected MimeType value: " + headerValue);
		}
	}

	private Mono<Object> decodeContent(MethodParameter parameter, Message<?> message,
			boolean isContentRequired, Flux<DataBuffer> content, MimeType mimeType) {

		ResolvableType targetType = ResolvableType.forMethodParameter(parameter);
		Class<?> resolvedType = targetType.resolve();
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		ResolvableType elementType = (adapter != null ? targetType.getGeneric() : targetType);
		isContentRequired = isContentRequired || (adapter != null && !adapter.supportsEmpty());
		Consumer<Object> validator = getValidator(message, parameter);

		Map<String, Object> hints = Collections.emptyMap();

		for (Decoder<?> decoder : this.decoders) {
			if (decoder.canDecode(elementType, mimeType)) {
				if (adapter != null && adapter.isMultiValue()) {
					Flux<?> flux = content
							.filter(this::nonEmptyDataBuffer)
							.map(buffer -> decoder.decode(buffer, elementType, mimeType, hints))
							.onErrorResume(ex -> Flux.error(handleReadError(parameter, message, ex)));
					if (isContentRequired) {
						flux = flux.switchIfEmpty(Flux.error(() -> handleMissingBody(parameter, message)));
					}
					if (validator != null) {
						flux = flux.doOnNext(validator);
					}
					return Mono.just(adapter.fromPublisher(flux));
				}
				else {
					// Single-value (with or without reactive type wrapper)
					Mono<?> mono = content.next()
							.filter(this::nonEmptyDataBuffer)
							.map(buffer -> decoder.decode(buffer, elementType, mimeType, hints))
							.onErrorResume(ex -> Mono.error(handleReadError(parameter, message, ex)));
					if (isContentRequired) {
						mono = mono.switchIfEmpty(Mono.error(() -> handleMissingBody(parameter, message)));
					}
					if (validator != null) {
						mono = mono.doOnNext(validator);
					}
					return (adapter != null ? Mono.just(adapter.fromPublisher(mono)) : Mono.from(mono));
				}
			}
		}

		return Mono.error(new MethodArgumentResolutionException(
				message, parameter, "Cannot decode to [" + targetType + "]" + message));
	}

	private boolean nonEmptyDataBuffer(DataBuffer buffer) {
		if (buffer.readableByteCount() > 0) {
			return true;
		}
		DataBufferUtils.release(buffer);
		return false;
	}

	private Throwable handleReadError(MethodParameter parameter, Message<?> message, Throwable ex) {
		return ex instanceof DecodingException ?
				new MethodArgumentResolutionException(message, parameter, "Failed to read HTTP message", ex) : ex;
	}

	private MethodArgumentResolutionException handleMissingBody(MethodParameter param, Message<?> message) {
		return new MethodArgumentResolutionException(message, param,
				"Payload content is missing: " + param.getExecutable().toGenericString());
	}

	@Nullable
	private Consumer<Object> getValidator(Message<?> message, MethodParameter parameter) {
		if (this.validator == null) {
			return null;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				String name = Conventions.getVariableNameForParameter(parameter);
				return target -> {
					BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, name);
					if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
						((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
					}
					else {
						this.validator.validate(target, bindingResult);
					}
					if (bindingResult.hasErrors()) {
						throw new MethodArgumentNotValidException(message, parameter, bindingResult);
					}
				};
			}
		}
		return null;
	}

}
