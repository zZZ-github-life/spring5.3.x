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

package org.springframework.messaging.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * {@link ChannelInterceptor} 的扩展，提供回调以拦截通过
 * {@link java.util.concurrent.Executor} 向特定订阅者异步发送
 * {@link org.springframework.messaging.Message} 的过程。
 * 支持在可配置 {@code Executor} 的
 * {@link org.springframework.messaging.MessageChannel} 实现上使用。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see Message
 * @see MessageChannel
 * @see MessageHandler
 */
public interface ExecutorChannelInterceptor extends ChannelInterceptor {

	/**
	 * 在提交给 Executor 的 {@link Runnable} 内部、调用目标 MessageHandler 处理
	 * 消息之前调用。如有必要可以修改 Message；如果返回 {@code null}，
	 * 则不会调用 MessageHandler。
	 * @param message 要处理的消息
	 * @param channel 消息被发送到的通道
	 * @param handler 处理该消息的目标处理器
	 * @return 输入消息，或新实例，或 {@code null}
	 */
	@Nullable
	default Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
		return message;
	}

	/**
	 * 在提交给 Executor 的 {@link Runnable} 内部、调用目标 MessageHandler 之后调用，
	 * 无论结果如何（即是否抛出异常），从而允许进行适当的资源清理。
	 * <p>请注意，仅当 beforeHandle 成功完成并返回 Message（即未返回
	 * {@code null}）时才会调用此方法。
	 * @param message 已处理的消息
	 * @param channel 消息被发送到的通道
	 * @param handler 处理该消息的目标处理器
	 * @param ex 处理器可能抛出的任何异常
	 */
	default void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler,
			@Nullable Exception ex) {
	}

}
