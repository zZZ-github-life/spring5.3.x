/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * 用于将 {@link Message} 的载荷从序列化形式转换为类型化对象（反之亦然）的转换器。
 * 可以使用 {@link MessageHeaders#CONTENT_TYPE} 消息头指定消息内容的媒体类型。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MessageConverter {

	/**
	 * 将 {@link Message} 的载荷从序列化形式转换为指定目标类的类型化对象。
	 * {@link MessageHeaders#CONTENT_TYPE} 头应指明要转换的 MIME 类型。
	 * <p>如果转换器不支持指定的媒体类型或无法执行转换，则应返回 {@code null}。
	 * @param message the input message
	 * @param targetClass the target class for the conversion
	 * @return the result of the conversion, or {@code null} if the converter cannot
	 * perform the conversion
	 */
	@Nullable
	Object fromMessage(Message<?> message, Class<?> targetClass);

	/**
	 * 创建一个 {@link Message}，其载荷是将给定载荷对象转换为序列化形式的结果。
	 * 可选的 {@link MessageHeaders} 参数可以包含 {@link MessageHeaders#CONTENT_TYPE} 头，
	 * 用于指定转换的目标媒体类型，也可以包含要添加到消息中的其他头。
	 * <p>如果转换器不支持指定的媒体类型或无法执行转换，则应返回 {@code null}。
	 * @param payload the Object to convert
	 * @param headers optional headers for the message (may be {@code null})
	 * @return the new message, or {@code null} if the converter does not support the
	 * Object type or the target media type
	 */
	@Nullable
	Message<?> toMessage(Object payload, @Nullable MessageHeaders headers);

}
