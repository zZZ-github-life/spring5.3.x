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
import java.util.List;
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
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * 使用
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>
 * 将 {@code Object} 流编码为 JSON 对象的字节流。
 *
 * <p>此编码器可用于绑定 {@code @Serializable} Kotlin 类，
 * 但不支持
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放式多态序列化</a>。
 * 它支持使用各种字符集的 {@code application/json} 和 {@code application/*+json}，
 * 默认使用 {@code UTF-8}。
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonEncoder extends AbstractEncoder<Object> {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;

	// CharSequence encoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
	private final CharSequenceEncoder charSequenceEncoder = CharSequenceEncoder.allMimeTypes();


	public KotlinSerializationJsonEncoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonEncoder(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		try {
			serializer(elementType.getType());
			return (super.canEncode(elementType, mimeType) && !String.class.isAssignableFrom(elementType.toClass()) &&
					!ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (inputStream instanceof Mono) {
			return Mono.from(inputStream)
					.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
					.flux();
		}
		else {
			ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
			return Flux.from(inputStream)
					.collectList()
					.map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
					.flux();
		}
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		String json = this.json.encodeToString(serializer(valueType.getType()), value);
		return this.charSequenceEncoder.encodeValue(json, bufferFactory, valueType, mimeType, null);
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
