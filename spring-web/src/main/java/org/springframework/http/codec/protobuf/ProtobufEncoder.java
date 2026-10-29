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

package org.springframework.http.codec.protobuf;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * 使用 <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * 写入 {@link com.google.protobuf.Message} 的 {@code Encoder}。
 *
 * <p>Flux 使用
 * <a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">带分隔符的 Protobuf 消息</a>
 * 进行序列化，每条消息的大小需在消息本身之前指定。单个值
 * 使用常规的 Protobuf 消息格式进行序列化（不在消息前附加大小）。
 *
 * <p>要生成 {@code Message} Java 类，需要安装 {@code protoc} 二进制文件。
 *
 * <p>此编码器要求 Protobuf 3 或更高版本，并支持
 * {@code "application/x-protobuf"} 和 {@code "application/octet-stream"}，
 * 需使用官方的 {@code "com.google.protobuf:protobuf-java"} 库。
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see ProtobufDecoder
 */
public class ProtobufEncoder extends ProtobufCodecSupport implements HttpMessageEncoder<Message> {

	private static final List<MediaType> streamingMediaTypes = MIME_TYPES
			.stream()
			.map(mimeType -> new MediaType(mimeType.getType(), mimeType.getSubtype(),
					Collections.singletonMap(DELIMITED_KEY, DELIMITED_VALUE)))
			.collect(Collectors.toList());


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends Message> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(message ->
				encodeValue(message, bufferFactory, !(inputStream instanceof Mono)));
	}

	@Override
	public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return encodeValue(message, bufferFactory, false);
	}

	private DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, boolean delimited) {

		DataBuffer buffer = bufferFactory.allocateBuffer();
		boolean release = true;
		try {
			if (delimited) {
				message.writeDelimitedTo(buffer.asOutputStream());
			}
			else {
				message.writeTo(buffer.asOutputStream());
			}
			release = false;
			return buffer;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
		}
		finally {
			if (release) {
				DataBufferUtils.release(buffer);
			}
		}
	}

	@Override
	public List<MediaType> getStreamingMediaTypes() {
		return streamingMediaTypes;
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return getMimeTypes();
	}

}
