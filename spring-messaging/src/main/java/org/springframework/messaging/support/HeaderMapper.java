/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import org.springframework.messaging.MessageHeaders;

/**
 * 用于在 {@link MessageHeaders} 与其他类型对象之间进行映射的通用策略接口。
 * 通常由适配器使用，其中“其他类型”具有头部或属性的概念（HTTP、JMS、AMQP 等）。
 *
 * @author Mark Fisher
 * @since 4.1
 * @param <T> type of the instance to and from which headers will be mapped
 */
public interface HeaderMapper<T> {

	/**
	 * 将给定的 {@link MessageHeaders} 映射到指定的目标消息。
	 * @param headers the abstracted MessageHeaders
	 * @param target the native target message
	 */
	void fromHeaders(MessageHeaders headers, T target);

	/**
	 * 将给定的目标消息映射为抽象的 {@link MessageHeaders}。
	 * @param source the native target message
	 * @return the abstracted MessageHeaders
	 */
	MessageHeaders toHeaders(T source);

}
