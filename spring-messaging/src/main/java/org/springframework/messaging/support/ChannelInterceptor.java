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

/**
 * 拦截器接口，能够查看和/或修改发往和/或接收自
 * {@link MessageChannel} 的 {@link Message Messages}。
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see Message
 * @see MessageChannel
 */
public interface ChannelInterceptor {

	/**
	 * 在 Message 实际发送到通道之前调用。
	 * 如有必要，可以对 Message 进行修改。
	 * 如果此方法返回 {@code null}，则不会执行实际的发送调用。
	 */
	@Nullable
	default Message<?> preSend(Message<?> message, MessageChannel channel) {
		return message;
	}

	/**
	 * 在发送调用之后立即调用。布尔值参数表示该调用的返回值。
	 */
	default void postSend(Message<?> message, MessageChannel channel, boolean sent) {
	}

	/**
	 * 在发送完成后调用，无论是否抛出任何异常，从而允许进行适当的资源清理。
	 * <p>请注意，仅当 {@link #preSend} 成功完成并返回 Message（即未返回
	 * {@code null}）时才会调用此方法。
	 * @since 4.1
	 */
	default void afterSendCompletion(
			Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
	}

	/**
	 * 在调用 receive 后立即、实际检索 Message 之前调用。如果返回值为 'false'，
	 * 则不会检索 Message。这仅适用于 PollableChannels。
	 */
	default boolean preReceive(MessageChannel channel) {
		return true;
	}

	/**
	 * 在检索到 Message 之后、返回给调用方之前立即调用。如有必要，可以修改
	 * Message；返回 {@code null} 会中止后续拦截器的调用。这仅适用于
	 * PollableChannels。
	 */
	@Nullable
	default Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return message;
	}

	/**
	 * 在接收完成后调用，无论是否抛出任何异常，从而允许进行适当的资源清理。
	 * <p>请注意，仅当 {@link #preReceive} 成功完成并返回 {@code true} 时
	 * 才会调用此方法。
	 * @since 4.1
	 */
	default void afterReceiveCompletion(@Nullable Message<?> message, MessageChannel channel,
			@Nullable Exception ex) {
	}

}
