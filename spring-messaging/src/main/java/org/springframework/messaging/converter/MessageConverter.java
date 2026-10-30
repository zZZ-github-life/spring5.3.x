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
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * @return 转换结果；如果转换器无法执行转换则返回 {@code null}
	 */
	@Nullable
	Object fromMessage(Message<?> message, Class<?> targetClass);

	/**
	 * 创建一个 {@link Message}，其载荷是将给定载荷对象转换为序列化形式的结果。
	 * 可选的 {@link MessageHeaders} 参数可以包含 {@link MessageHeaders#CONTENT_TYPE} 头，
	 * 用于指定转换的目标媒体类型，也可以包含要添加到消息中的其他头。
	 * <p>如果转换器不支持指定的媒体类型或无法执行转换，则应返回 {@code null}。
	 * @param payload 要转换的对象
	 * @param headers 消息的可选消息头（可以为 {@code null}）
	 * @return 新的消息；如果转换器不支持该对象类型或目标媒体类型，
	 * 则返回 {@code null}
	 */
	@Nullable
	Message<?> toMessage(Object payload, @Nullable MessageHeaders headers);

}
