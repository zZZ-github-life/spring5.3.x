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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * 用于从 {@link ReactiveHttpInputMessage} 读取并将字节流解码为
 * 类型为 {@code <T>} 的对象的策略。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 * @param <T> the type of objects in the decoded output stream
 */
public interface HttpMessageReader<T> {

	/**
	 * 返回此读取器支持的媒体类型列表。该列表未必适用于所有可能的
	 * 目标元素类型，对此方法的调用通常应通过
	 * {@link #canRead(ResolvableType, MediaType) canWrite(elementType, null)}
	 * 进行保护。该列表还可能不包含仅针对特定元素类型支持的媒体类型。
	 * 或者，使用 {@link #getReadableMediaTypes(ResolvableType)} 获取更精确的列表。
	 * @return the general list of supported media types
	 */
	List<MediaType> getReadableMediaTypes();

	/**
	 * 返回此读取器针对给定元素类型所支持的媒体类型列表。如果读取器
	 * 不支持该元素类型，或仅支持其中的一部分媒体类型，此列表可能与
	 * {@link #getReadableMediaTypes()} 不同。
	 * @param elementType the type of element to read
	 * @return the list of media types supported for the given class
	 * @since 5.3.4
	 */
	default List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
		return (canRead(elementType, null) ? getReadableMediaTypes() : Collections.emptyList());
	}

	/**
	 * 此读取器是否支持给定的对象类型。
	 * @param elementType the type of object to check
	 * @param mediaType the media type for the read (possibly {@code null})
	 * @return {@code true} if readable, {@code false} otherwise
	 */
	boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType);

	/**
	 * 从输入消息读取并解码为对象流。
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canRead(ResolvableType, MediaType)}
	 * @param message the message to read from
	 * @param hints additional information about how to read and decode the input
	 * @return the decoded stream of elements
	 */
	Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * 从输入消息读取并解码为单个对象。
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canRead(ResolvableType, MediaType)}
	 * @param message the message to read from
	 * @param hints additional information about how to read and decode the input
	 * @return the decoded object
	 */
	Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * 仅限服务器端的替代方法，用于
	 * {@link #read(ResolvableType, ReactiveHttpInputMessage, Map)}，
	 * 可提供额外的上下文。
	 * @param actualType the actual type of the target method parameter;
	 * for annotated controllers, the {@link MethodParameter} can be accessed
	 * via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	default Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return read(elementType, request, hints);
	}

	/**
	 * 仅限服务器端的替代方法，用于
	 * {@link #readMono(ResolvableType, ReactiveHttpInputMessage, Map)}，
	 * 可提供额外的上下文。
	 * @param actualType the actual type of the target method parameter;
	 * for annotated controllers, the {@link MethodParameter} can be accessed
	 * via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	default Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return readMono(elementType, request, hints);
	}

}
