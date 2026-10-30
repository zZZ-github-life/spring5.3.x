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

package org.springframework.http.codec;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageReader}，包装并委托给 {@link Decoder}。
 *
 * <p>同时也是一个 {@code HttpMessageReader}，可依据服务器端提供的
 * 额外信息（如请求或控制器方法参数上的注解）预先解析解码提示。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 解码输出流中的对象类型
 */
public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {

	private final Decoder<T> decoder;

	private final List<MediaType> mediaTypes;


	/**
	 * 创建包装给定 {@link Decoder} 的实例。
	 */
	public DecoderHttpMessageReader(Decoder<T> decoder) {
		Assert.notNull(decoder, "Decoder is required");
		initLogger(decoder);
		this.decoder = decoder;
		this.mediaTypes = MediaType.asMediaTypes(decoder.getDecodableMimeTypes());
	}

	private static void initLogger(Decoder<?> decoder) {
		if (decoder instanceof AbstractDecoder &&
				decoder.getClass().getName().startsWith("org.springframework.core.codec")) {
			Log logger = HttpLogging.forLog(((AbstractDecoder<?>) decoder).getLogger());
			((AbstractDecoder<?>) decoder).setLogger(logger);
		}
	}


	/**
	 * 返回此读取器的 {@link Decoder}。
	 */
	public Decoder<T> getDecoder() {
		return this.decoder;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
		return MediaType.asMediaTypes(this.decoder.getDecodableMimeTypes(elementType));
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.decoder.canDecode(elementType, mediaType);
	}

	@Override
	public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		MediaType contentType = getContentType(message);
		Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
		return this.decoder.decode(message.getBody(), elementType, contentType, allHints);
	}

	@Override
	public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		MediaType contentType = getContentType(message);
		Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
		return this.decoder.decodeToMono(message.getBody(), elementType, contentType, allHints);
	}

	/**
	 * 根据 "Content-Type" 头确定 HTTP 消息的 Content-Type，
	 * 否则默认为 {@link MediaType#APPLICATION_OCTET_STREAM}。
	 * @param inputMessage HTTP 消息
	 * @return MediaType，可能为 {@code null}。
	 */
	@Nullable
	protected MediaType getContentType(HttpMessage inputMessage) {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
	}

	/**
	 * 根据输入 HTTP 消息获取用于解码的额外提示。
	 * @since 5.3
	 */
	protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
		return Hints.none();
	}


	// Server-side only...

	@Override
	public Flux<T> read(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = Hints.merge(hints,
				getReadHints(actualType, elementType, request, response));

		return read(elementType, request, allHints);
	}

	@Override
	public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = Hints.merge(hints,
				getReadHints(actualType, elementType, request, response));

		return readMono(elementType, request, allHints);
	}

	/**
	 * 获取用于解码的额外提示，例如基于服务器请求
	 * 或控制器方法参数上的注解。默认情况下，如果解码器是
	 * {@link HttpMessageDecoder} 的实例，则委托给该解码器。
	 */
	protected Map<String, Object> getReadHints(ResolvableType actualType,
			ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.decoder instanceof HttpMessageDecoder) {
			HttpMessageDecoder<?> decoder = (HttpMessageDecoder<?>) this.decoder;
			return decoder.getDecodeHints(actualType, elementType, request, response);
		}
		return Hints.none();
	}

}
