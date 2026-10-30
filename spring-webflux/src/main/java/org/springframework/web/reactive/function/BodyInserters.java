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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 用于 {@link BodyInserter} 实现的静态工厂方法。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class BodyInserters {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private static final ResolvableType SSE_TYPE = ResolvableType.forClass(ServerSentEvent.class);

	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Object.class);

	private static final BodyInserter<Void, ReactiveHttpOutputMessage> EMPTY_INSERTER =
			(response, context) -> response.setComplete();

	private static final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();


	/**
	 * 不写入任何内容的插入器。
	 * @return 插入器
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> empty() {
		return (BodyInserter<T, ReactiveHttpOutputMessage>) EMPTY_INSERTER;
	}

	/**
	 * 用于写入给定值的插入器。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code bodyValue(Object)} 快捷方法。
	 * @param body 要写入的值
	 * @param <T> 主体的类型
	 * @return 用于写入单个值的插入器
	 * @throws IllegalArgumentException 如果 {@code body} 是 {@link Publisher}，或者是受
	 * {@link ReactiveAdapterRegistry#getSharedInstance()} 支持的类型的实例，
	 * 此时应使用 {@link #fromPublisher(Publisher, Class)} 或
	 * {@link #fromProducer(Object, Class)}。
	 * @see #fromPublisher(Publisher, Class)
	 * @see #fromProducer(Object, Class)
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromValue(T body) {
		Assert.notNull(body, "'body' must not be null");
		Assert.isNull(registry.getAdapter(body.getClass()), "'body' should be an object, for reactive types use a variant specifying a publisher/producer and its related element type");
		return (message, context) ->
				writeWithMessageWriters(message, context, Mono.just(body), ResolvableType.forInstance(body), null);
	}

	/**
	 * 用于写入给定对象的插入器。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code bodyValue(Object)} 快捷方法。
	 * @param body 要写入响应的主体
	 * @param <T> 主体的类型
	 * @return 用于写入单个对象的插入器
	 * @throws IllegalArgumentException 如果 {@code body} 是 {@link Publisher}，或者是受
	 * {@link ReactiveAdapterRegistry#getSharedInstance()} 支持的类型的实例，
	 * 此时应使用 {@link #fromPublisher(Publisher, Class)} 或
	 * {@link #fromProducer(Object, Class)}。
	 * @see #fromPublisher(Publisher, Class)
	 * @see #fromProducer(Object, Class)
	 * @deprecated As of Spring Framework 5.2, in favor of {@link #fromValue(Object)}
	 */
	@Deprecated
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromObject(T body) {
		return fromValue(body);
	}

	/**
	 * 用于写入给定值生产者的插入器，该生产者必须是 {@link Publisher} 或可通过 {@link ReactiveAdapterRegistry} 适配为 {@code Publisher} 的其他生产者。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方法。
	 * @param <T> 主体的类型
	 * @param producer 主体值的来源。
	 * @param elementClass 要生成的值的类
	 * @return 用于写入 producer 的插入器
	 * @since 5.2
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromProducer(T producer, Class<?> elementClass) {
		Assert.notNull(producer, "'producer' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(producer.getClass());
		Assert.notNull(adapter, "'producer' type is unknown to ReactiveAdapterRegistry");
		return (message, context) ->
				writeWithMessageWriters(message, context, producer, ResolvableType.forClass(elementClass), adapter);
	}

	/**
	 * 用于写入给定值生产者的插入器，该生产者必须是 {@link Publisher} 或可通过 {@link ReactiveAdapterRegistry} 适配为 {@code Publisher} 的其他生产者。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方法。
	 * @param <T> 主体的类型
	 * @param producer 主体值的来源。
	 * @param elementTypeRef 要生成的值的类型
	 * @return 用于写入 producer 的插入器
	 * @since 5.2
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromProducer(
			T producer, ParameterizedTypeReference<?> elementTypeRef) {

		Assert.notNull(producer, "'producer' must not be null");
		Assert.notNull(elementTypeRef, "'elementTypeRef' must not be null");
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(producer.getClass());
		Assert.notNull(adapter, "'producer' type is unknown to ReactiveAdapterRegistry");
		return (message, context) ->
				writeWithMessageWriters(message, context, producer, ResolvableType.forType(elementTypeRef), adapter);
	}

	/**
	 * 用于写入给定 {@link Publisher} 的插入器。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方法。
	 * @param publisher 用于写入的 publisher
	 * @param elementClass publisher 中元素的类
	 * @param <T> publisher 中包含的元素的类型
	 * @param <P> {@code Publisher} 类型
	 * @return 用于写入 {@code Publisher} 的插入器
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forClass(elementClass), null);
	}

	/**
	 * 用于写入给定 {@link Publisher} 的插入器。
	 * <p>或者，可考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方法。
	 * @param publisher 用于写入的 publisher
	 * @param elementTypeRef publisher 中包含的元素的类型
	 * @param <T> publisher 中包含的元素的类型
	 * @param <P> {@code Publisher} 类型
	 * @return 用于写入 {@code Publisher} 的插入器
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, ParameterizedTypeReference<T> elementTypeRef) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementTypeRef, "'elementTypeRef' must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forType(elementTypeRef.getType()), null);
	}

	/**
	 * 用于写入给定 {@code Resource} 的插入器。
	 * <p>如果资源可以解析为{@linkplain Resource#getFile() 文件}，则将使用<a href="https://en.wikipedia.org/wiki/Zero-copy">零拷贝</a>方式进行复制。
	 * @param resource 要写入输出消息的资源
	 * @param <T> {@code Resource} 的类型
	 * @return 用于写入 {@code Publisher} 的插入器
	 */
	public static <T extends Resource> BodyInserter<T, ReactiveHttpOutputMessage> fromResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return (outputMessage, context) -> {
			ResolvableType elementType = RESOURCE_TYPE;
			HttpMessageWriter<Resource> writer = findWriter(context, elementType, null);
			MediaType contentType = outputMessage.getHeaders().getContentType();
			return write(Mono.just(resource), elementType, contentType, outputMessage, context, writer);
		};
	}

	/**
	 * 用于写入给定 {@code ServerSentEvent} 发布者的插入器。
	 * <p>或者，可以通过 {@link #fromPublisher(Publisher, Class)} 或 {@link #fromProducer(Object, Class)} 提供事件数据对象，并将 "Content-Type" 设置为 {@link MediaType#TEXT_EVENT_STREAM text/event-stream}。
	 * @param eventsPublisher 要写入响应体的 {@code ServerSentEvent} publisher
	 * @param <T> {@link ServerSentEvent} 中数据元素的类型
	 * @return 用于写入 {@code ServerSentEvent} publisher 的插入器
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	// Parameterized for server-side use
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return (serverResponse, context) -> {
			ResolvableType elementType = SSE_TYPE;
			MediaType mediaType = MediaType.TEXT_EVENT_STREAM;
			HttpMessageWriter<ServerSentEvent<T>> writer = findWriter(context, elementType, mediaType);
			return write(eventsPublisher, elementType, mediaType, serverResponse, context, writer);
		};
	}

	/**
	 * 返回用于将给定 {@code MultiValueMap} 写入为 URL 编码表单数据的 {@link FormInserter}。返回的插入器允许通过 {@link FormInserter#with(String, Object)} 添加更多条目。
	 * <p>请注意，也可以使用 {@code WebClient} 和 {@code WebTestClient} 请求构建器中的 {@code bodyValue(Object)} 方法。在这种情况下也无需设置请求内容类型，只需确保映射中仅包含 String 值，否则它将被解释为 multipart 请求。
	 * @param formData 要写入输出消息的表单数据
	 * @return 允许添加更多表单数据的插入器
	 */
	public static FormInserter<String> fromFormData(MultiValueMap<String, String> formData) {
		return new DefaultFormInserter().with(formData);
	}

	/**
	 * 返回用于将给定键值对写入为 URL 编码表单数据的 {@link FormInserter}。返回的插入器允许通过 {@link FormInserter#with(String, Object)} 添加更多条目。
	 * @param name 要添加到表单的键
	 * @param value 要添加到表单的值
	 * @return 允许添加更多表单数据的插入器
	 */
	public static FormInserter<String> fromFormData(String name, String value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultFormInserter().with(name, value);
	}

	/**
	 * 返回用于将给定 {@code MultiValueMap} 写入为 multipart 数据的 {@link MultipartInserter}。映射中的值可以是 Object 或 {@link HttpEntity}。
	 * <p>请注意，也可以使用 {@link MultipartBodyBuilder} 在外部构建 multipart 数据，并将生成的映射直接传给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 * @param multipartData 要写入输出消息的表单数据
	 * @return 允许添加更多部分的插入器
	 * @see MultipartBodyBuilder
	 */
	public static MultipartInserter fromMultipartData(MultiValueMap<String, ?> multipartData) {
		Assert.notNull(multipartData, "'multipartData' must not be null");
		return new DefaultMultipartInserter().withInternal(multipartData);
	}

	/**
	 * 返回用于将给定分片写入为 multipart 数据的 {@link MultipartInserter}。映射中的值可以是 Object 或 {@link HttpEntity}。
	 * <p>请注意，也可以使用 {@link MultipartBodyBuilder} 在外部构建 multipart 数据，并将生成的映射直接传给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 * @param name 部分名称
	 * @param value 部分值，为 Object 或 {@code HttpEntity}
	 * @return 允许添加更多部分的插入器
	 */
	public static MultipartInserter fromMultipartData(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultMultipartInserter().with(name, value);
	}

	/**
	 * 返回用于将给定异步分片写入为 multipart 数据的 {@link MultipartInserter}。
	 * <p>请注意，也可以使用 {@link MultipartBodyBuilder} 在外部构建 multipart 数据，并将生成的映射直接传给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 * @param name 部分名称
	 * @param publisher 构成部分值的 publisher
	 * @param elementClass {@code publisher} 中包含的类
	 * @return 允许添加更多部分的插入器
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, Class<T> elementClass) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, elementClass);
	}

	/**
	 * {@link #fromMultipartAsyncData(String, Publisher, Class)} 的变体，接受元素类型的 {@link ParameterizedTypeReference}，从而可以指定泛型类型信息。
	 * <p>请注意，也可以使用 {@link MultipartBodyBuilder} 在外部构建 multipart 数据，并将生成的映射直接传给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 * @param name 部分名称
	 * @param publisher 构成部分值的 publisher
	 * @param typeReference {@code publisher} 中包含的类型
	 * @return 允许添加更多部分的插入器
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, typeReference);
	}

	/**
	 * 用于将给定 {@code Publisher<DataBuffer>} 写入正文的插入器。
	 * @param publisher 要写入的数据缓冲区 publisher
	 * @param <T> publisher 的类型
	 * @return 直接写入主体的插入器
	 * @see ReactiveHttpOutputMessage#writeWith(Publisher)
	 */
	public static <T extends Publisher<DataBuffer>> BodyInserter<T, ReactiveHttpOutputMessage> fromDataBuffers(
			T publisher) {

		Assert.notNull(publisher, "'publisher' must not be null");
		return (outputMessage, context) -> outputMessage.writeWith(publisher);
	}


	private static <M extends ReactiveHttpOutputMessage> Mono<Void> writeWithMessageWriters(
			M outputMessage, BodyInserter.Context context, Object body, ResolvableType bodyType, @Nullable ReactiveAdapter adapter) {

		Publisher<?> publisher;
		if (body instanceof Publisher) {
			publisher = (Publisher<?>) body;
		}
		else if (adapter != null) {
			publisher = adapter.toPublisher(body);
		}
		else {
			publisher = Mono.just(body);
		}
		MediaType mediaType = outputMessage.getHeaders().getContentType();
		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(bodyType, mediaType))
				.findFirst()
				.map(BodyInserters::cast)
				.map(writer -> write(publisher, bodyType, mediaType, outputMessage, context, writer))
				.orElseGet(() -> Mono.error(unsupportedError(bodyType, context, mediaType)));
	}

	private static UnsupportedMediaTypeException unsupportedError(ResolvableType bodyType,
			BodyInserter.Context context, @Nullable MediaType mediaType) {

		List<MediaType> supportedMediaTypes = context.messageWriters().stream()
				.flatMap(reader -> reader.getWritableMediaTypes(bodyType).stream())
				.collect(Collectors.toList());

		return new UnsupportedMediaTypeException(mediaType, supportedMediaTypes, bodyType);
	}

	private static <T> Mono<Void> write(Publisher<? extends T> input, ResolvableType type,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message,
			BodyInserter.Context context, HttpMessageWriter<T> writer) {

		return context.serverRequest()
				.map(request -> {
					ServerHttpResponse response = (ServerHttpResponse) message;
					return writer.write(input, type, type, mediaType, request, response, context.hints());
				})
				.orElseGet(() -> writer.write(input, type, mediaType, message, context.hints()));
	}

	private static <T> HttpMessageWriter<T> findWriter(
			BodyInserter.Context context, ResolvableType elementType, @Nullable MediaType mediaType) {

		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(elementType, mediaType))
				.findFirst()
				.map(BodyInserters::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"No HttpMessageWriter for \"" + mediaType + "\" and \"" + elementType + "\""));
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}


	/**
	 * 允许添加表单数据或 multipart 表单数据的 {@link BodyInserter} 扩展。
	 * @param <T> 值类型
	 */
	public interface FormInserter<T> extends BodyInserter<MultiValueMap<String, T>, ClientHttpRequest> {

		// FormInserter is parameterized to ClientHttpRequest (for client-side use only)

		/**
		 * 将指定的键值对添加到表单中。
		 * @param key 要添加的键
		 * @param value 要添加的值
		 * @return 此插入器，用于添加更多部分
		 */
		FormInserter<T> with(String key, T value);

		/**
		 * 将指定的值添加到表单中。
		 * @param values 要添加的值
		 * @return 此插入器，用于添加更多部分
		 */
		FormInserter<T> with(MultiValueMap<String, T> values);

	}


	/**
	 * 允许添加异步分片的 {@link FormInserter} 扩展。
	 */
	public interface MultipartInserter extends FormInserter<Object> {

		/**
		 * 添加基于 {@link Publisher} 内容的异步分片。
		 * @param name 要添加的部分的名称
		 * @param publisher 部分内容
		 * @param elementClass publisher 中包含的元素的类型
		 * @return 此插入器，用于添加更多部分
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
				Class<T> elementClass);

		/**
		 * {@link #withPublisher(String, Publisher, Class)} 的变体，接受元素类型的 {@link ParameterizedTypeReference}，从而可以指定泛型类型信息。
		 * @param name 要添加的键
		 * @param publisher 要作为值添加的 publisher
		 * @param typeReference {@code publisher} 中包含的元素的类型
		 * @return 此插入器，用于添加更多部分
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
				ParameterizedTypeReference<T> typeReference);

	}


	private static class DefaultFormInserter implements FormInserter<String> {

		private final MultiValueMap<String, String> data = new LinkedMultiValueMap<>();

		@Override
		public FormInserter<String> with(String key, @Nullable String value) {
			this.data.add(key, value);
			return this;
		}

		@Override
		public FormInserter<String> with(MultiValueMap<String, String> values) {
			this.data.addAll(values);
			return this;
		}

		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			HttpMessageWriter<MultiValueMap<String, String>> messageWriter =
					findWriter(context, FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
			return messageWriter.write(Mono.just(this.data), FORM_DATA_TYPE,
					MediaType.APPLICATION_FORM_URLENCODED,
					outputMessage, context.hints());
		}
	}


	private static class DefaultMultipartInserter implements MultipartInserter {

		private final MultipartBodyBuilder builder = new MultipartBodyBuilder();

		@Override
		public MultipartInserter with(String key, Object value) {
			this.builder.part(key, value);
			return this;
		}

		@Override
		public MultipartInserter with(MultiValueMap<String, Object> values) {
			return withInternal(values);
		}

		@SuppressWarnings("unchecked")
		private MultipartInserter withInternal(MultiValueMap<String, ?> values) {
			values.forEach((key, valueList) -> {
				for (Object value : valueList) {
					this.builder.part(key, value);
				}
			});
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, Class<T> elementClass) {

			this.builder.asyncPart(name, publisher, elementClass);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, ParameterizedTypeReference<T> typeReference) {

			this.builder.asyncPart(name, publisher, typeReference);
			return this;
		}

		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			HttpMessageWriter<MultiValueMap<String, HttpEntity<?>>> messageWriter =
					findWriter(context, MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA);
			MultiValueMap<String, HttpEntity<?>> body = this.builder.build();
			return messageWriter.write(Mono.just(body), MULTIPART_DATA_TYPE,
					MediaType.MULTIPART_FORM_DATA, outputMessage, context.hints());
		}
	}

}
