/*
 * Copyright 2002-2016 the original author or authors.
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
 * 定义发送消息的方法。
 *
 * @author Mark Fisher
 * @since 4.0
 */
@FunctionalInterface
public interface MessageChannel {

	/**
	 * 用于在没有指定超时的情况下发送消息的常量。
	 */
	long INDEFINITE_TIMEOUT = -1;


	/**
	 * 向此通道发送 {@link Message}。如果消息发送成功，该方法返回 {@code true}。
	 * 如果由于非致命原因无法发送消息，该方法返回 {@code false}。
	 * 对于不可恢复的错误，该方法还可能抛出 RuntimeException。
	 * <p>取决于具体实现，此方法可能无限期阻塞。
	 * 要提供最大等待时间，请使用 {@link #send(Message, long)}。
	 * @param message 要发送的消息
	 * @return 消息是否已发送
	 */
	default boolean send(Message<?> message) {
		return send(message, INDEFINITE_TIMEOUT);
	}

	/**
	 * 发送消息，阻塞直到消息被接受或指定的超时时间过去。
	 * @param message 要发送的消息
	 * @param timeout 以毫秒为单位的超时时间，或 {@link #INDEFINITE_TIMEOUT}
	 * @return 如果消息已发送则返回 {@code true}，否则返回 {@code false}，
	 * 包括发送超时或被中断的情况
	 */
	boolean send(Message<?> message, long timeout);

}
