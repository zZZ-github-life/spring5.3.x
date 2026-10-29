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
 * @param <T> the converted object type
 * @see org.springframework.core.ParameterizedTypeReference
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类型。
	 * 此方法应执行与
	 * {@link HttpMessageConverter#canRead(Class, MediaType)} 相同的检查，
	 * 并额外执行与泛型类型相关的检查。
	 * @param type the (potentially generic) type to test for readability
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @param mediaType the media type to read, can be {@code null} if not specified.
	 * Typically, the value of a {@code Content-Type} header.
	 * @return {@code true} if readable; {@code false} otherwise
	 */
	boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);

	/**
	 * 从给定的输入消息中读取给定类型的对象，并返回它。
	 * @param type the (potentially generic) type of object to return. This type must have
	 * previously been passed to the {@link #canRead canRead} method of this interface,
	 * which must have returned {@code true}.
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 指示此转换器是否可以写入给定类。
	 * <p>此方法应执行与
	 * {@link HttpMessageConverter#canWrite(Class, MediaType)} 相同的检查，
	 * 并额外执行与泛型类型相关的检查。
	 * @param type the (potentially generic) type to test for writability
	 * (can be {@code null} if not specified)
	 * @param clazz the source object class to test for writability
	 * @param mediaType the media type to write (can be {@code null} if not specified);
	 * typically the value of an {@code Accept} header.
	 * @return {@code true} if writable; {@code false} otherwise
	 * @since 4.2
	 */
	boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 将给定对象写入给定的输出消息。
	 * @param t the object to write to the output message. The type of this object must
	 * have previously been passed to the {@link #canWrite canWrite} method of this
	 * interface, which must have returned {@code true}.
	 * @param type the (potentially generic) type of object to write. This type must have
	 * previously been passed to the {@link #canWrite canWrite} method of this interface,
	 * which must have returned {@code true}. Can be {@code null} if not specified.
	 * @param contentType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used. If not
	 * {@code null}, this media type must have previously been passed to the
	 * {@link #canWrite canWrite} method of this interface, which must have returned
	 * {@code true}.
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 * @since 4.2
	 */
	void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
