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

package org.springframework.messaging.converter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.json.Json;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.FileCopyUtils;

/**
 * 使用 <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>
 * 读写 JSON 的 {@link MessageConverter} 实现。
 *
 * <p>此转换器可用于绑定 {@code @Serializable} Kotlin 类。
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class KotlinSerializationJsonMessageConverter extends AbstractJsonMessageConverter {

	private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

	private final Json json;


	/**
	 * 使用默认配置构造新的 {@code KotlinSerializationJsonMessageConverter}。
	 */
	public KotlinSerializationJsonMessageConverter() {
		this(Json.Default);
	}

	/**
	 * 使用给定的委托构造新的 {@code KotlinSerializationJsonMessageConverter}。
	 * @param json 要使用的 Json 实例
	 */
	public KotlinSerializationJsonMessageConverter(Json json) {
		this.json = json;
	}

	@Override
	protected Object fromJson(Reader reader, Type resolvedType) {
		try {
			return fromJson(FileCopyUtils.copyToString(reader), resolvedType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return this.json.decodeFromString(serializer(resolvedType), payload);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		try {
			writer.write(toJson(payload, resolvedType).toCharArray());
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		return this.json.encodeToString(serializer(resolvedType), payload);
	}

	/**
	 * 尝试查找能够使用 kotlinx.serialization 编组或解组给定类型实例的序列化器。
	 * 如果找不到序列化器，则抛出异常。
	 * <p>已解析的序列化器会被缓存，后续调用将返回缓存结果。
	 * TODO 避免在 https://github.com/Kotlin/kotlinx.serialization/pull/1164 修复之前依赖抛出异常
	 * @param type 要为其查找序列化器的类型
	 * @return 为给定类型解析得到的序列化器
	 * @throws RuntimeException 如果找不到支持给定类型的序列化器
	 */
	private KSerializer<Object> serializer(Type type) {
		KSerializer<Object> serializer = serializerCache.get(type);
		if (serializer == null) {
			serializer = SerializersKt.serializer(type);
			serializerCache.put(type, serializer);
		}
		return serializer;
	}
}
