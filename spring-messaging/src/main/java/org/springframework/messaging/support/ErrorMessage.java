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

package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * 带有 {@link Throwable} 负载的 {@link GenericMessage}。
 *
 * <p>负载通常是 {@link org.springframework.messaging.MessagingException}，
 * 其 {@code failedMessage} 属性中包含故障发生点处的消息。
 * 还可以提供可选的 {@code originalMessage}，它表示在创建错误消息的堆栈位置
 * 处存在的消息。
 *
 * <p>考虑这样一些代码：它从一条消息开始，调用某个对该消息执行转换的处理过程，
 * 然后由于某种原因失败并抛出异常。该异常被捕获，并生成一条错误消息，
 * 其中同时包含原始消息和失败时转换后的消息。
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 4.0
 * @see MessageBuilder
 */
public class ErrorMessage extends GenericMessage<Throwable> {

	private static final long serialVersionUID = -5470210965279837728L;

	@Nullable
	private final Message<?> originalMessage;


	/**
	 * 使用给定负载创建新的消息。
	 * @param payload 消息负载（永不为 {@code null}）
	 */
	public ErrorMessage(Throwable payload) {
		super(payload);
		this.originalMessage = null;
	}

	/**
	 * 使用给定负载和头部创建新的消息。将复制给定头部映射的内容。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 用于初始化的消息头
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers) {
		super(payload, headers);
		this.originalMessage = null;
	}

	/**
	 * 使用给定的 {@link MessageHeaders} 实例构造。
	 * <p><strong>注意：</strong>给定的 {@code MessageHeaders} 实例会直接用于
	 * 新消息中，即不会被复制。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 消息头
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers) {
		super(payload, headers);
		this.originalMessage = null;
	}

	/**
	 * 使用给定负载和原始消息创建新的消息。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param originalMessage 创建 ErrorMessage 时堆栈中存在的原始消息（如果有）
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, Message<?> originalMessage) {
		super(payload);
		this.originalMessage = originalMessage;
	}

	/**
	 * 使用给定负载、头部和原始消息创建新的消息。将复制给定头部映射的内容。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 用于初始化的消息头
	 * @param originalMessage 创建 ErrorMessage 时堆栈中存在的原始消息（如果有）
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}

	/**
	 * 使用负载、{@link MessageHeaders} 和原始消息创建新的消息。
	 * <p><strong>注意：</strong>给定的 {@code MessageHeaders} 实例会直接用于
	 * 新消息中，即不会被复制。
	 * @param payload 消息负载（永不为 {@code null}）
	 * @param headers 消息头
	 * @param originalMessage 创建 ErrorMessage 时堆栈中存在的原始消息（如果有）
	 * @since 5.0
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers, Message<?> originalMessage) {
		super(payload, headers);
		this.originalMessage = originalMessage;
	}


	/**
	 * 返回在创建 ErrorMessage 的堆栈位置处（如果可用）的原始消息。
	 * @since 5.0
	 */
	@Nullable
	public Message<?> getOriginalMessage() {
		return this.originalMessage;
	}

	@Override
	public String toString() {
		if (this.originalMessage == null) {
			return super.toString();
		}
		return super.toString() + " for original " + this.originalMessage;
	}

}
