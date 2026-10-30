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

package org.springframework.http.converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * 用于在 HTTP 请求和响应之间进行转换的策略接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <T> 转换后的对象类型
 */
public interface HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类。
	 * @param clazz 要测试其可读性的类
	 * @param mediaType 要读取的媒体类型（如果未指定则可以为 {@code null}）；
	 * 通常是 {@code Content-Type} 头的值。
	 * @return 如果可读则返回 {@code true}；否则返回 {@code false}
	 */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 指示此转换器是否可以写入给定类。
	 * @param clazz 要测试其可写性的类
	 * @param mediaType 要写入的媒体类型（如果未指定则可以为 {@code null}）；
	 * 通常是 {@code Accept} 头的值。
	 * @return 如果可写则返回 {@code true}；否则返回 {@code false}
	 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 返回此转换器支持的媒体类型列表。该列表未必适用于所有可能的
	 * 目标元素类型，对此方法的调用通常应通过
	 * {@link #canWrite(Class, MediaType) canWrite(clazz, null} 进行保护。
	 * 该列表还可能不包含仅针对特定类支持的 MIME 类型。
	 * 或者，使用 {@link #getSupportedMediaTypes(Class)} 获取更精确的列表。
	 * @return 受支持的媒体类型列表
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * 返回此转换器针对给定类所支持的媒体类型列表。如果转换器
	 * 不支持给定类，或仅支持其中的一部分媒体类型，此列表可能与
	 * {@link #getSupportedMediaTypes()} 不同。
	 * @param clazz 要检查的类类型
	 * @return 给定类所支持的媒体类型列表
	 * @since 5.3.4
	 */
	default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		return (canRead(clazz, null) || canWrite(clazz, null) ?
				getSupportedMediaTypes() : Collections.emptyList());
	}

	/**
	 * 从给定的输入消息中读取给定类型的对象，并返回它。
	 * @param clazz 要返回的对象的类型。该类型必须事先已传递给
	 * 该接口的 {@link #canRead canRead} 方法，并且该方法必须返回过 {@code true}。
	 * @param inputMessage 要读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws HttpMessageNotReadableException 发生转换错误时抛出
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 将给定对象写入给定的输出消息。
	 * @param t 要写入输出消息的对象。该对象的类型必须事先已传递给
	 * 该接口的 {@link #canWrite canWrite} 方法，并且该方法必须返回过 {@code true}。
	 * @param contentType 写入时要使用的内容类型。可以为 {@code null}，表示必须使用
	 * 转换器的默认内容类型。如果非 {@code null}，该媒体类型必须事先
	 * 已传递给该接口的 {@link #canWrite canWrite} 方法，并且该方法
	 * 必须返回过 {@code true}。
	 * @param outputMessage 要写入的消息
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws HttpMessageNotWritableException 发生转换错误时抛出
	 */
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
