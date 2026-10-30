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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StreamUtils;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现，
 * 可使用 <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>
 * 读写 JSON。
 *
 * <p>此转换器可用于绑定 {@code @Serializable} Kotlin 类，
 * 但不支持
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">开放式多态序列化</a>。
 * 它支持使用各种字符集的 {@code application/json} 和 {@code application/*+json}，
 * 默认使用 {@code UTF-8}。
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.3
 */
public class KotlinSerializationJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;


	/**
	 * 使用默认配置构造一个新的 {@code KotlinSerializationJsonHttpMessageConverter}。
	 */
	public KotlinSerializationJsonHttpMessageConverter() {
		this(Json.Default);
	}

	/**
	 * 使用自定义配置构造一个新的 {@code KotlinSerializationJsonHttpMessageConverter}。
	 */
	public KotlinSerializationJsonHttpMessageConverter(Json json) {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		this.json = json;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		try {
			serializer(clazz);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		try {
			serializer(GenericTypeResolver.resolveType(type, contextClass));
			return canRead(mediaType);
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		try {
			serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz);
			return canWrite(mediaType);
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(GenericTypeResolver.resolveType(type, contextClass)), inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return decode(serializer(clazz), inputMessage);
	}

	private Object decode(KSerializer<Object> serializer, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		String jsonText = StreamUtils.copyToString(inputMessage.getBody(), getCharsetToUse(contentType));
		try {
			// TODO Use stream based API when available
			return this.json.decodeFromString(serializer, jsonText);
		}
		catch (SerializationException ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		encode(object, serializer(type != null ? type : object.getClass()), outputMessage);
	}

	private void encode(Object object, KSerializer<Object> serializer, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			String json = this.json.encodeToString(serializer, object);
			MediaType contentType = outputMessage.getHeaders().getContentType();
			outputMessage.getBody().write(json.getBytes(getCharsetToUse(contentType)));
			outputMessage.getBody().flush();
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	private Charset getCharsetToUse(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		return DEFAULT_CHARSET;
	}

	/**
	 * 尝试查找可使用 kotlinx.serialization 对给定类型实例进行编组或
	 * 反编组的序列化器。如果找不到序列化器，则抛出异常。
	 * <p>已解析的序列化器会被缓存，后续调用会返回缓存的结果。
	 * TODO 在 https://github.com/Kotlin/kotlinx.serialization/pull/1164 修复后，避免依赖抛出异常
	 * @param type 要为其查找序列化器的类型
	 * @return 给定类型解析后的序列化器
	 * @throws RuntimeException 如果找不到支持给定类型的序列化器则抛出
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
