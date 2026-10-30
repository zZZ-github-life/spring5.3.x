/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * {@link HttpMessageConverter} 的特化，可以将 HTTP 请求转换为
 * 指定泛型类型的目标对象，并将指定泛型类型的源对象转换为 HTTP 响应。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 3.2
 * @param <T> 所转换的对象类型
 * @see org.springframework.core.ParameterizedTypeReference
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类型。
	 * 此方法应执行与
	 * {@link HttpMessageConverter#canRead(Class, MediaType)} 相同的检查，
	 * 并额外执行与泛型类型相关的检查。
	 * @param type 要测试可读性的（可能是泛型的）类型
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中时
	 * 所在的类（可以为 {@code null}）
	 * @param mediaType 要读取的媒体类型，如果未指定则可以为 {@code null}。
	 * 通常是 {@code Content-Type} 头的值。
	 * @return 如果可读则返回 {@code true}；否则返回 {@code false}
	 */
	boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);

	/**
	 * 从给定的输入消息中读取给定类型的对象，并返回它。
	 * @param type 要返回的对象的（可能为泛型的）类型。该类型必须事先已传递给
	 * 该接口的 {@link #canRead canRead} 方法，并且该方法必须返回过
	 * {@code true}。
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中时
	 * 所在的类（可以为 {@code null}）
	 * @param inputMessage 要读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws HttpMessageNotReadableException 发生转换错误时抛出
	 */
	T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 指示此转换器是否可以写入给定类。
	 * <p>此方法应执行与
	 * {@link HttpMessageConverter#canWrite(Class, MediaType)} 相同的检查，
	 * 并额外执行与泛型类型相关的检查。
	 * @param type 要测试其可写性的（可能为泛型的）类型
	 * （如果未指定则可以为 {@code null}）
	 * @param clazz 要测试其可写性的源对象类
	 * @param mediaType 要写入的媒体类型（如果未指定则可以为 {@code null}）；
	 * 通常是 {@code Accept} 头的值。
	 * @return 如果可写则返回 {@code true}；否则返回 {@code false}
	 * @since 4.2
	 */
	boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 将给定对象写入给定的输出消息。
	 * @param t 要写入输出消息的对象。该对象的类型必须事先已传递给
	 * 该接口的 {@link #canWrite canWrite} 方法，并且该方法必须返回过
	 * {@code true}。
	 * @param type 要写入的对象的（可能为泛型的）类型。该类型必须事先已传递给
	 * 该接口的 {@link #canWrite canWrite} 方法，并且该方法必须返回过
	 * {@code true}。如果未指定则可以为 {@code null}。
	 * @param contentType 写入时要使用的内容类型。可以为 {@code null}，
	 * 表示必须使用转换器的默认内容类型。如果非 {@code null}，
	 * 该媒体类型必须事先已传递给该接口的 {@link #canWrite canWrite} 方法，
	 * 并且该方法必须返回过 {@code true}。
	 * @param outputMessage 要写入的消息
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws HttpMessageNotWritableException 发生转换错误时抛出
	 * @since 4.2
	 */
	void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
