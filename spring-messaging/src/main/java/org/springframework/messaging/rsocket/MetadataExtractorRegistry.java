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

package org.springframework.messaging.rsocket;

import java.util.Map;
import java.util.function.BiConsumer;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * 存储元数据条目提取器的注册信息。
 * 每个元数据条目根据其 {@code MimeType} 进行解码，并为解码后的值分配一个名称。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.2
 * @see MetadataExtractor
 */
public interface MetadataExtractorRegistry {

	/**
	 * 将给定 {@link MimeType} 的元数据条目解码为指定的目标类，并将解码后的值
	 * 以给定名称存储在输出映射中。
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 * @param name assign a name for the decoded value; if not provided, then
	 * the mime type is used as the key
	 */
	default void metadataToExtract(MimeType mimeType, Class<?> targetType, @Nullable String name) {
		String key = name != null ? name : mimeType.toString();
		metadataToExtract(mimeType, targetType, (value, map) -> map.put(key, value));
	}

	/**
	 * {@link #metadataToExtract(MimeType, Class, String)} 的变体，接受
	 * {@link ParameterizedTypeReference} 而不是 {@link Class}，
	 * 用于指定带泛型参数的目标类型。
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 */
	default void metadataToExtract(
			MimeType mimeType, ParameterizedTypeReference<?> targetType, @Nullable String name) {

		String key = name != null ? name : mimeType.toString();
		metadataToExtract(mimeType, targetType, (value, map) -> map.put(key, value));
	}

	/**
	 * {@link #metadataToExtract(MimeType, Class, String)} 的变体，允许使用
	 * 自定义逻辑将解码后的值映射为输出映射中的任意数量的值。
	 * @param mimeType the mime type of metadata entries to extract
	 * @param targetType the target value type to decode to
	 * @param mapper custom logic to add the decoded value to the output map
	 * @param <T> the target value type
	 */
	<T> void metadataToExtract(
			MimeType mimeType, Class<T> targetType, BiConsumer<T, Map<String, Object>> mapper);

	/**
	 * {@link #metadataToExtract(MimeType, Class, BiConsumer)} 的变体，接受
	 * {@link ParameterizedTypeReference} 而不是 {@link Class}，
	 * 用于指定带泛型参数的目标类型。
	 * @param mimeType the mime type of metadata entries to extract
	 * @param type the target value type to decode to
	 * @param mapper custom logic to add the decoded value to the output map
	 * @param <T> the target value type
	 */
	<T> void metadataToExtract(
			MimeType mimeType, ParameterizedTypeReference<T> type, BiConsumer<T, Map<String, Object>> mapper);

}
