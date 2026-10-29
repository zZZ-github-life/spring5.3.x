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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * 用于编码类型为 {@code <T>} 的对象流，并将编码后的字节流
 * 写入 {@link ReactiveHttpOutputMessage} 的策略。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 * @param <T> the type of objects in the input stream
 */
public interface HttpMessageWriter<T> {

	/**
	 * 返回此写入器支持的媒体类型列表。该列表未必适用于所有可能的
	 * 目标元素类型，对此方法的调用通常应通过
	 * {@link #canWrite(ResolvableType, MediaType) canWrite(elementType, null)}
	 * 进行保护。该列表还可能不包含仅针对特定元素类型支持的媒体类型。
	 * 或者，使用 {@link #getWritableMediaTypes(ResolvableType)} 获取更精确的列表。
	 * @return the general list of supported media types
	 */
	List<MediaType> getWritableMediaTypes();

	/**
	 * 返回此写入器针对给定元素类型所支持的媒体类型列表。如果写入器
	 * 不支持该元素类型，或仅支持其中的一部分媒体类型，此列表可能与
	 * {@link #getWritableMediaTypes()} 不同。
	 * @param elementType the type of element to encode
	 * @return the list of media types supported for the given class
	 * @since 5.3.4
	 */
	default List<MediaType> getWritableMediaTypes(ResolvableType elementType) {
		return (canWrite(elementType, null) ? getWritableMediaTypes() : Collections.emptyList());
	}

	/**
	 * 此写入器是否支持给定的对象类型。
	 * @param elementType the type of object to check
	 * @param mediaType the media type for the write (possibly {@code null})
	 * @return {@code true} if writable, {@code false} otherwise
	 */
	boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType);

	/**
	 * 将给定的对象流写入输出消息。
	 * @param inputStream the objects to write
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canWrite(ResolvableType, MediaType)}
	 * @param mediaType the content type for the write (possibly {@code null} to
	 * indicate that the default content type of the writer must be used)
	 * @param message the message to write to
	 * @param hints additional information about how to encode and write
	 * @return indicates completion or error
	 */
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints);

	/**
	 * 仅限服务器端的替代方法，用于
	 * {@link #write(Publisher, ResolvableType, MediaType, ReactiveHttpOutputMessage, Map)}，
	 * 可提供额外的上下文。
	 * @param actualType the actual return type of the method that returned the
	 * value; for annotated controllers, the {@link MethodParameter} can be
	 * accessed via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the input stream
	 * @param mediaType the content type to use (possibly {@code null} indicating
	 * the default content type of the writer should be used)
	 * @param request the current request
	 * @param response the current response
	 * @return a {@link Mono} that indicates completion of writing or error
	 */
	default Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return write(inputStream, elementType, mediaType, response, hints);
	}

}
