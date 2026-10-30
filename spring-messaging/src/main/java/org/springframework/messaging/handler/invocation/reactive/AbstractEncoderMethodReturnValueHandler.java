/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 返回值处理器的基类，通过配置的 {@link Encoder} 将返回值编码为
 * {@code Flux<DataBuffer>}。
 *
 * <p>子类必须实现抽象方法 {@link #handleEncodedContent}，
 * 以处理生成的编码内容。
 *
 * <p>此处理器应排在最后，因为对于任何方法参数类型，
 * 它的 {@link #supportsReturnType} 都返回 {@code true}。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class AbstractEncoderMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final ResolvableType VOID_RESOLVABLE_TYPE = ResolvableType.forClass(Void.class);

	private static final ResolvableType OBJECT_RESOLVABLE_TYPE = ResolvableType.forClass(Object.class);

	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";


	protected final Log logger = LogFactory.getLog(getClass());

	private final List<Encoder<?>> encoders;

	private final ReactiveAdapterRegistry adapterRegistry;


	protected AbstractEncoderMethodReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		Assert.notEmpty(encoders, "At least one Encoder is required");
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		this.encoders = Collections.unmodifiableList(encoders);
		this.adapterRegistry = registry;
	}


	/**
	 * 已配置的编码器。
	 */
	public List<Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * 已配置的适配器注册表。
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// We could check canEncode but we're probably last in order anyway
		return true;
	}

	@Override
	public Mono<Void> handleReturnValue(
			@Nullable Object returnValue, MethodParameter returnType, Message<?> message) {

		if (returnValue == null) {
			return handleNoContent(returnType, message);
		}

		DataBufferFactory bufferFactory = (DataBufferFactory) message.getHeaders()
				.getOrDefault(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER,
						DefaultDataBufferFactory.sharedInstance);

		MimeType mimeType = (MimeType) message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		Flux<DataBuffer> encodedContent = encodeContent(
				returnValue, returnType, bufferFactory, mimeType, Collections.emptyMap());

		return new ChannelSendOperator<>(encodedContent, publisher ->
				handleEncodedContent(Flux.from(publisher), returnType, message));
	}

	private Flux<DataBuffer> encodeContent(
			@Nullable Object content, MethodParameter returnType, DataBufferFactory bufferFactory,
			@Nullable MimeType mimeType, Map<String, Object> hints) {

		ResolvableType returnValueType = ResolvableType.forMethodParameter(returnType);
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(returnValueType.resolve(), content);

		Publisher<?> publisher;
		ResolvableType elementType;
		if (adapter != null) {
			publisher = adapter.toPublisher(content);
			Method method = returnType.getMethod();
			boolean isUnwrapped = (method != null && KotlinDetector.isSuspendingFunction(method) &&
					!COROUTINES_FLOW_CLASS_NAME.equals(returnValueType.toClass().getName()));
			ResolvableType genericType = (isUnwrapped ? returnValueType : returnValueType.getGeneric());
			elementType = getElementType(adapter, genericType);
		}
		else {
			publisher = Mono.justOrEmpty(content);
			elementType = (returnValueType.toClass() == Object.class && content != null ?
					ResolvableType.forInstance(content) : returnValueType);
		}

		if (elementType.resolve() == void.class || elementType.resolve() == Void.class) {
			return Flux.from(publisher).cast(DataBuffer.class);
		}

		Encoder<?> encoder = getEncoder(elementType, mimeType);
		return Flux.from(publisher).map(value ->
				encodeValue(value, elementType, encoder, bufferFactory, mimeType, hints));
	}

	private ResolvableType getElementType(ReactiveAdapter adapter, ResolvableType type) {
		if (adapter.isNoValue()) {
			return VOID_RESOLVABLE_TYPE;
		}
		else if (type != ResolvableType.NONE) {
			return type;
		}
		else {
			return OBJECT_RESOLVABLE_TYPE;
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> Encoder<T> getEncoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : getEncoders()) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> DataBuffer encodeValue(
			Object element, ResolvableType elementType, @Nullable Encoder<T> encoder,
			DataBufferFactory bufferFactory, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		if (encoder == null) {
			encoder = getEncoder(ResolvableType.forInstance(element), mimeType);
			if (encoder == null) {
				throw new MessagingException(
						"No encoder for " + elementType + ", current value type is " + element.getClass());
			}
		}
		return encoder.encodeValue((T) element, bufferFactory, elementType, mimeType, hints);
	}

	/**
	 * 子类实现此方法以某种方式处理编码值，例如创建并发送消息。
	 * @param encodedContent 编码后的内容；每个 {@code DataBuffer}
	 * 表示 HandlerMethod 返回的一个值（即载荷）的
	 * 完全聚合的编码内容。
	 * @param returnType 生成该数据的处理器方法的返回类型
	 * @param message 处理器方法处理的输入消息
	 * @return 用于表示处理完成的 {@code Mono<Void>}
	 */
	protected abstract Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message);

	/**
	 * 当返回值为 {@code null} 时调用，这可能表示 void 方法，
	 * 或返回以 void 参数化的异步类型的方法。
	 * @param returnType 生成该数据的处理器方法的返回类型
	 * @param message 处理器方法处理的输入消息
	 * @return 用于表示处理完成的 {@code Mono<Void>}
	 */
	protected abstract Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message);

}
