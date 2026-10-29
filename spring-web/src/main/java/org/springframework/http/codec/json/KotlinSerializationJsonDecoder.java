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

package org.springframework.http.codec.json;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * 将字节流解码为 JSON，并使用
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a> 转换为 Object。
 *
 * <p>此解码器可用于绑定 {@code @Serializable} Kotlin 类，
 * 但不支持
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放式多态序列化</a>。
 * 它支持使用各种字符集的 {@code application/json} 和 {@code application/*+json}，
 * 默认使用 {@code UTF-8}。
 *
 * <p>尚不支持流式解码，请参阅
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * 相关问题。
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonDecoder extends AbstractDecoder<Object> {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;

	// String decoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final StringDecoder stringDecoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);


	public KotlinSerializationJsonDecoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonDecoder(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}

	/**
	 * 配置当输入流需要聚合时可缓冲的字节数限制。这可能源于
	 * 解码为单个 {@code DataBuffer}、
	 * {@link java.nio.ByteBuffer ByteBuffer}、{@code byte[]}、
	 * {@link org.springframework.core.io.Resource Resource}、{@code String} 等。
	 * 在拆分输入流时（例如分隔文本）也可能发生，此时该限制
	 * 适用于分隔符之间缓冲的数据。
	 * <p>默认设置为 256K。
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.stringDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * 返回{@link #setMaxInMemorySize 已配置}的字节数限制。
	 */
	public int getMaxInMemorySize() {
		return this.stringDecoder.getMaxInMemorySize();
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		try {
			serializer(elementType.getType());
			return (super.canDecode(elementType, mimeType) && !CharSequence.class.isAssignableFrom(elementType.toClass()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.error(new UnsupportedOperationException());
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return this.stringDecoder
				.decodeToMono(inputStream, elementType, mimeType, hints)
				.map(jsonText -> this.json.decodeFromString(serializer(elementType.getType()), jsonText));
	}

	/**
	 * 尝试查找可使用 kotlinx.serialization 对给定类型实例进行编组或
	 * 反编组的序列化器。如果找不到序列化器，则抛出异常。
	 * <p>已解析的序列化器会被缓存，后续调用会返回缓存的结果。
	 * TODO 在 https://github.com/Kotlin/kotlinx.serialization/pull/1164 修复后，避免依赖抛出异常
	 * @param type the type to find a serializer for
	 * @return a resolved serializer for the given type
	 * @throws RuntimeException if no serializer supporting the given type can be found
	 */
	private KSerializer<Object> serializer(Type type) {
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			serializer = SerializersKt.serializer(type);
			if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
				throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
			}
			serializerCache.put(type, serializer);
		}
		return serializer;
	}

	private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
		alreadyProcessed.add(descriptor.getSerialName());
		if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
			return true;
		}
		for (int i = 0 ; i < descriptor.getElementsCount() ; i++) {
			SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);
			if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
				return true;
			}
		}
		return false;
	}

}
