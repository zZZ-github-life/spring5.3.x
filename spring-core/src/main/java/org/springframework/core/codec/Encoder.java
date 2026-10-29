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

package org.springframework.core.codec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * 将 {@code <T>} 类型的对象流编码为字节输出流的策略。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the input stream
 */
public interface Encoder<T> {

	/**
	 * 此编码器是否支持给定的源元素类型以及输出流的 MIME 类型。
	 * @param elementType the type of elements in the source stream
	 * @param mimeType the MIME type for the output stream
	 * (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
	 */
	boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * 将 {@code T} 类型的对象流编码为 {@link DataBuffer} 输出流。
	 * @param inputStream the input stream of Objects to encode. If the input should be
	 * encoded as a single value rather than as a stream of elements, an instance of
	 * {@link Mono} should be used.
	 * @param bufferFactory for creating output stream {@code DataBuffer}'s
	 * @param elementType the expected type of elements in the input stream;
	 * this type must have been previously passed to the {@link #canEncode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type for the output content (optional)
	 * @param hints additional information about how to encode
	 * @return the output stream
	 */
	Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * 将类型 T 的对象编码为数据缓冲区。这适用于需要将独立的消息（或事件）以完全聚合的形式逐个编码并处
	 * 理的场景。
	 * <p>默认情况下，此方法会抛出 {@link UnsupportedOperationException}，因为某些编码器无法生成单个缓
	 * 冲区，或者无法同步地完成（例如编码 {@code Resource}）。
	 * @param value the value to be encoded
	 * @param bufferFactory for creating the output {@code DataBuffer}
	 * @param valueType the type for the value being encoded
	 * @param mimeType the MIME type for the output content (optional)
	 * @param hints additional information about how to encode
	 * @return the encoded content
	 * @since 5.2
	 */
	default DataBuffer encodeValue(T value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// It may not be possible to produce a single DataBuffer synchronously
		throw new UnsupportedOperationException();
	}

	/**
	 * 返回此 Encoder 支持的 MIME 类型列表。该列表可能并不适用于所有可能的目标元素类型，调用此方法时通
	 * 常应通过 {@link #canEncode(ResolvableType, MimeType) canEncode(elementType, null)} 加以防护。该
	 * 列表也可能不包含仅对特定元素类型提供支持的 MIME 类型。或者，可使用
	 * {@link #getEncodableMimeTypes(ResolvableType)} 获取更精确的列表。
	 * @return the list of supported MIME types
	 */
	List<MimeType> getEncodableMimeTypes();

	/**
	 * 返回此 Encoder 针对给定元素类型支持的 MIME 类型列表。如果 Encoder 不支持该元素类型，或者仅对部分
	 * MIME 类型提供支持，则该列表可能与 {@link #getEncodableMimeTypes()} 不同。
	 * @param elementType the type of element to check for encoding
	 * @return the list of MIME types supported for the given element type
	 * @since 5.3.4
	 */
	default List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return (canEncode(elementType, null) ? getEncodableMimeTypes() : Collections.emptyList());
	}

}
