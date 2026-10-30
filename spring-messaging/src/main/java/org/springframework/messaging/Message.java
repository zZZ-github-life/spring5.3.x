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

package org.springframework.messaging;

/**
 * 带有头和正文的通用消息表示。
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @since 4.0
 * @param <T> 载荷类型
 * @see org.springframework.messaging.support.MessageBuilder
 */
public interface Message<T> {

	/**
	 * 返回消息载荷。
	 */
	T getPayload();

	/**
	 * 返回消息的消息头（永不为 {@code null}，但可能为空）。
	 */
	MessageHeaders getHeaders();

}
