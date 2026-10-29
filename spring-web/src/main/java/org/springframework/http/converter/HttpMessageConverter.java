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
 * @param <T> the converted object type
 */
public interface HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类。
	 * @param clazz the class to test for readability
	 * @param mediaType the media type to read (can be {@code null} if not specified);
	 * typically the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 指示此转换器是否可以写入给定类。
	 * @param clazz the class to test for writability
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 返回此转换器支持的媒体类型列表。该列表未必适用于所有可能的
	 * 目标元素类型，对此方法的调用通常应通过
	 * {@link #canWrite(Class, MediaType) canWrite(clazz, null} 进行保护。
	 * 该列表还可能不包含仅针对特定类支持的 MIME 类型。
	 * 或者，使用 {@link #getSupportedMediaTypes(Class)} 获取更精确的列表。
	 * @return the list of supported media types
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * 返回此转换器针对给定类所支持的媒体类型列表。如果转换器
	 * 不支持给定类，或仅支持其中的一部分媒体类型，此列表可能与
	 * {@link #getSupportedMediaTypes()} 不同。
	 * @param clazz the type of class to check
	 * @return the list of media types supported for the given class
	 * @since 5.3.4
	 */
	default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		return (canRead(clazz, null) || canWrite(clazz, null) ?
				getSupportedMediaTypes() : Collections.emptyList());
	}

	/**
	 * 从给定的输入消息中读取给定类型的对象，并返回它。
	 * @param clazz the type of object to return. This type must have previously been passed to the
	 * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 将给定对象写入给定的输出消息。
	 * @param t the object to write to the output message. The type of this object must have previously been
	 * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
	 * @param contentType the content type to use when writing. May be {@code null} to indicate that the
	 * default content type of the converter must be used. If not {@code null}, this media type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
	 * returned {@code true}.
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
