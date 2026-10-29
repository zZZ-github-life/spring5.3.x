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

package org.springframework.http.codec.multipart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link HttpMessageWriter}，用于将 {@code MultiValueMap<String, ?>}
 * 作为 multipart 表单数据（即 {@code "multipart/form-data"}）
 * 写入请求体。
 *
 * <p>各部分的序列化委托给其他写入器。默认仅支持 {@link String}
 * 和 {@link Resource} 部件，但可以通过构造函数参数配置其他写入器。
 *
 * <p>可以将此写入器配置为委托给一个 {@link FormHttpMessageWriter}。
 * 这是同时支持表单数据和 multipart 数据的首选方式（相比分别注册每个写入器），
 * 这样当未指定 {@link MediaType} 且目标元素类型上不存在泛型信息时，
 * 我们可以检查实际 Map 中的值，从而决定是写入纯表单数据（仅 String 值）
 * 还是采用其他方式。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see FormHttpMessageWriter
 */
public class MultipartHttpMessageWriter extends MultipartWriterSupport
		implements HttpMessageWriter<MultiValueMap<String, ?>> {

	/** 抑制各部件写入器的日志记录（在此级别记录完整的 Map）。 */
	private static final Map<String, Object> DEFAULT_HINTS = Hints.from(Hints.SUPPRESS_LOGGING_HINT, true);


	private final List<HttpMessageWriter<?>> partWriters;

	@Nullable
	private final HttpMessageWriter<MultiValueMap<String, String>> formWriter;


	/**
	 * 使用默认的部件写入器列表（String 和 Resource）的构造函数。
	 */
	public MultipartHttpMessageWriter() {
		this(Arrays.asList(
				new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()),
				new ResourceHttpMessageWriter()
		));
	}

	/**
	 * 使用显式的部件序列化写入器列表的构造函数。
	 */
	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters) {
		this(partWriters, new FormHttpMessageWriter());
	}

	/**
	 * 使用显式的部件序列化写入器列表以及纯表单数据写入器的构造函数，
	 * 后者在未指定媒体类型且实际 Map 仅包含 String 值时作为后备。
	 * @param partWriters the writers for serializing parts
	 * @param formWriter the fallback writer for form data, {@code null} by default
	 */
	public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters,
			@Nullable  HttpMessageWriter<MultiValueMap<String, String>> formWriter) {

		super(initMediaTypes(formWriter));
		this.partWriters = partWriters;
		this.formWriter = formWriter;
	}

	private static List<MediaType> initMediaTypes(@Nullable HttpMessageWriter<?> formWriter) {
		List<MediaType> result = new ArrayList<>(MultipartHttpMessageReader.MIME_TYPES);
		if (formWriter != null) {
			result.addAll(formWriter.getWritableMediaTypes());
		}
		return Collections.unmodifiableList(result);
	}


	/**
	 * 返回已配置的部件写入器。
	 * @since 5.0.7
	 */
	public List<HttpMessageWriter<?>> getPartWriters() {
		return Collections.unmodifiableList(this.partWriters);
	}


	/**
	 * 返回已配置的表单写入器。
	 * @since 5.1.13
	 */
	@Nullable
	public HttpMessageWriter<MultiValueMap<String, String>> getFormWriter() {
		return this.formWriter;
	}



	@Override
	public Mono<Void> write(Publisher<? extends MultiValueMap<String, ?>> inputStream,
			ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		return Mono.from(inputStream)
				.flatMap(map -> {
					if (this.formWriter == null || isMultipart(map, mediaType)) {
						return writeMultipart(map, outputMessage, mediaType, hints);
					}
					else {
						@SuppressWarnings("unchecked")
						Mono<MultiValueMap<String, String>> input = Mono.just((MultiValueMap<String, String>) map);
						return this.formWriter.write(input, elementType, mediaType, outputMessage, hints);
					}
				});
	}

	private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		if (contentType != null) {
			return contentType.getType().equalsIgnoreCase("multipart");
		}
		for (List<?> values : map.values()) {
			for (Object value : values) {
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	private Mono<Void> writeMultipart(MultiValueMap<String, ?> map,
			ReactiveHttpOutputMessage outputMessage, @Nullable MediaType mediaType, Map<String, Object> hints) {

		byte[] boundary = generateMultipartBoundary();

		mediaType = getMultipartMediaType(mediaType, boundary);
		outputMessage.getHeaders().setContentType(mediaType);

		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Encoding " +
				(isEnableLoggingRequestDetails() ?
						LogFormatUtils.formatValue(map, !traceOn) :
						"parts " + map.keySet() + " (content masked)"));

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();

		Flux<DataBuffer> body = Flux.fromIterable(map.entrySet())
				.concatMap(entry -> encodePartValues(boundary, entry.getKey(), entry.getValue(), bufferFactory))
				.concatWith(generateLastLine(boundary, bufferFactory))
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);

		if (logger.isDebugEnabled()) {
			body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
		}

		return outputMessage.writeWith(body);
	}

	private Flux<DataBuffer> encodePartValues(
			byte[] boundary, String name, List<?> values, DataBufferFactory bufferFactory) {

		return Flux.fromIterable(values)
				.concatMap(value -> encodePart(boundary, name, value, bufferFactory));
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodePart(byte[] boundary, String name, T value, DataBufferFactory factory) {
		MultipartHttpOutputMessage message = new MultipartHttpOutputMessage(factory);
		HttpHeaders headers = message.getHeaders();

		T body;
		ResolvableType resolvableType = null;
		if (value instanceof HttpEntity) {
			HttpEntity<T> httpEntity = (HttpEntity<T>) value;
			headers.putAll(httpEntity.getHeaders());
			body = httpEntity.getBody();
			Assert.state(body != null, "MultipartHttpMessageWriter only supports HttpEntity with body");
			if (httpEntity instanceof ResolvableTypeProvider) {
				resolvableType = ((ResolvableTypeProvider) httpEntity).getResolvableType();
			}
		}
		else {
			body = value;
		}
		if (resolvableType == null) {
			resolvableType = ResolvableType.forClass(body.getClass());
		}

		if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			if (body instanceof Resource) {
				headers.setContentDispositionFormData(name, ((Resource) body).getFilename());
			}
			else if (resolvableType.resolve() == Resource.class) {
				body = (T) Mono.from((Publisher<?>) body).doOnNext(o -> headers
						.setContentDispositionFormData(name, ((Resource) o).getFilename()));
			}
			else {
				headers.setContentDispositionFormData(name, null);
			}
		}

		MediaType contentType = headers.getContentType();

		final ResolvableType finalBodyType = resolvableType;
		Optional<HttpMessageWriter<?>> writer = this.partWriters.stream()
				.filter(partWriter -> partWriter.canWrite(finalBodyType, contentType))
				.findFirst();

		if (!writer.isPresent()) {
			return Flux.error(new CodecException("No suitable writer found for part: " + name));
		}

		Publisher<T> bodyPublisher =
				body instanceof Publisher ? (Publisher<T>) body : Mono.just(body);

		// The writer will call MultipartHttpOutputMessage#write which doesn't actually write
		// but only stores the body Flux and returns Mono.empty().

		Mono<Void> partContentReady = ((HttpMessageWriter<T>) writer.get())
				.write(bodyPublisher, resolvableType, contentType, message, DEFAULT_HINTS);

		// After partContentReady, we can access the part content from MultipartHttpOutputMessage
		// and use it for writing to the actual request body

		Flux<DataBuffer> partContent = partContentReady.thenMany(Flux.defer(message::getBody));

		return Flux.concat(
				generateBoundaryLine(boundary, factory),
				partContent,
				generateNewLine(factory));
	}


	private class MultipartHttpOutputMessage implements ReactiveHttpOutputMessage {

		private final DataBufferFactory bufferFactory;

		private final HttpHeaders headers = new HttpHeaders();

		private final AtomicBoolean committed = new AtomicBoolean();

		@Nullable
		private Flux<DataBuffer> body;

		public MultipartHttpOutputMessage(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.body != null ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public DataBufferFactory bufferFactory() {
			return this.bufferFactory;
		}

		@Override
		public void beforeCommit(Supplier<? extends Mono<Void>> action) {
			this.committed.set(true);
		}

		@Override
		public boolean isCommitted() {
			return this.committed.get();
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
			if (this.body != null) {
				return Mono.error(new IllegalStateException("Multiple calls to writeWith() not supported"));
			}
			this.body = generatePartHeaders(this.headers, this.bufferFactory).concatWith(body);

			// We don't actually want to write (just save the body Flux)
			return Mono.empty();
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
			return Mono.error(new UnsupportedOperationException());
		}

		public Flux<DataBuffer> getBody() {
			return (this.body != null ? this.body :
					Flux.error(new IllegalStateException("Body has not been written yet")));
		}

		@Override
		public Mono<Void> setComplete() {
			return Mono.error(new UnsupportedOperationException());
		}
	}

}
