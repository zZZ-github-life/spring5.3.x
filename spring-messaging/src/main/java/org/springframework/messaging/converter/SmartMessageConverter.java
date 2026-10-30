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
 * 带转换提示支持的扩展 {@link MessageConverter} SPI。
 *
 * <p>如果提供了转换提示，并且转换器实现了此接口，框架将调用这些扩展方法，
 * 而不是调用常规的 {@code fromMessage} / {@code toMessage} 变体。
 *
 * @author Juergen Hoeller
 * @since 4.2.1
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * {@link #fromMessage(Message, Class)} 的一个变体，接受额外的转换上下文作为参数，
	 * 从而可以考虑例如载荷参数上的注解。
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * @param conversionHint 传递给 {@link MessageConverter} 的额外对象，
	 * 例如关联的 {@code MethodParameter}（可以为 {@code null}）
	 * @return 转换结果；如果转换器无法执行转换则返回 {@code null}
	 * @see #fromMessage(Message, Class)
	 */
	@Nullable
	Object fromMessage(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint);

	/**
	 * {@link #toMessage(Object, MessageHeaders)} 的一个变体，接受额外的转换上下文作为参数，
	 * 从而可以考虑例如返回类型上的注解。
	 * @param payload 要转换的对象
	 * @param headers 消息的可选消息头（可以为 {@code null}）
	 * @param conversionHint 传递给 {@link MessageConverter} 的额外对象，
	 * 例如关联的 {@code MethodParameter}（可以为 {@code null}）
	 * @return 新的消息；如果转换器不支持该对象类型或目标媒体类型，
	 * 则返回 {@code null}
	 * @see #toMessage(Object, MessageHeaders)
	 */
	@Nullable
	Message<?> toMessage(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint);

}
