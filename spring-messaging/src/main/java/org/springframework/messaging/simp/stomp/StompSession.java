/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.util.function.Consumer;

import org.springframework.lang.Nullable;

/**
 * 表示 STOMP 会话，提供发送消息、创建订阅以及在这些订阅上接收消息的操作。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface StompSession {

	/**
	 * 返回会话的 ID。
	 */
	String getSessionId();

	/**
	 * 会话是否已连接。
	 */
	boolean isConnected();

	/**
	 * 启用后，会自动向此会话上后续的 {@code send} 和 {@code subscribe} 操作添加回执头，
	 * 这会使服务器返回 RECEIPT。然后应用程序可以使用操作返回的
	 * {@link StompSession.Receiptable Receiptable} 跟踪回执。
	 * <p>也可以通过接受 {@code StompHeaders} 的重载方法手动添加回执头。
	 */
	void setAutoReceipt(boolean enabled);

	/**
	 * 向指定目标发送消息，借助
	 * {@link org.springframework.messaging.converter.MessageConverter MessageConverter}
	 * 将载荷转换为 {@code byte[]}。
	 * @param destination 发送消息的目标地址
	 * @param payload 消息负载
	 * @return 用于跟踪回执的 Receiptable
	 */
	Receiptable send(String destination, Object payload);

	/**
	 * {@link #send(String, Object)} 的重载版本，使用完整的 {@link StompHeaders}
	 * 而不仅仅是目标。头中必须包含目标，还可以包含其他头，
	 * 例如 "content-type" 或供代理传播给订阅者的自定义头，
	 * 或代理特定的非标准头。
	 * @param headers 消息头
	 * @param payload 消息负载
	 * @return 用于跟踪回执的 Receiptable
	 */
	Receiptable send(StompHeaders headers, Object payload);

	/**
	 * 通过发送 SUBSCRIBE 帧订阅给定目标，并使用指定的
	 * {@link StompFrameHandler} 处理接收到的消息。
	 * @param destination 要订阅的目标地址
	 * @param handler 接收消息的处理器
	 * @return 用于取消订阅和/或跟踪回执的句柄
	 */
	Subscription subscribe(String destination, StompFrameHandler handler);

	/**
	 * {@link #subscribe(String, StompFrameHandler)} 的重载版本，
	 * 使用完整的 {@link StompHeaders} 而不仅仅是目标。
	 * @param headers 订阅消息帧的消息头
	 * @param handler 接收消息的处理器
	 * @return 用于取消订阅和/或跟踪回执的句柄
	 */
	Subscription subscribe(StompHeaders headers, StompFrameHandler handler);

	/**
	 * 发送确认，表示消息是否已消费，分别产生 ACK 或 NACK 帧。
	 * <p><strong>注意：</strong>订阅时若要使用此功能，必须将
	 * {@link StompHeaders#setAck(String) ack} 头设置为 "client" 或
	 * "client-individual"。
	 * @param messageId 消息的 id
	 * @param consumed 消息是否已被消费
	 * @return 用于跟踪回执的 Receiptable
	 * @since 4.3
	 */
	Receiptable acknowledge(String messageId, boolean consumed);

	/**
	 * {@link #acknowledge(String, boolean)} 的重载版本，
	 * 使用完整的 {@link StompHeaders} 而不仅仅是 {@code messageId}。
	 * @param headers ACK 或 NACK 消息帧的消息头
	 * @param consumed 消息是否已被消费
	 * @return 用于跟踪回执的 Receiptable
	 * @since 5.0.5
	 */
	Receiptable acknowledge(StompHeaders headers, boolean consumed);

	/**
	 * 通过发送 DISCONNECT 帧断开会话。
	 */
	void disconnect();

	/**
	 * 带头的 {@link #disconnect()} 变体。
	 * @param headers 断开连接消息帧的消息头
	 * @since 5.2.2
	 */
	void disconnect(StompHeaders headers);


	/**
	 * 用于跟踪回执的句柄。
	 * @see #setAutoReceipt(boolean)
	 */
	interface Receiptable {

		/**
		 * 返回回执 ID；如果返回该句柄的 STOMP 帧没有 "receipt" 头，则返回 {@code null}。
		 */
		@Nullable
		String getReceiptId();

		/**
		 * 收到回执时要调用的任务。
		 * @param task 要调用的任务
		 * @throws java.lang.IllegalArgumentException 如果 receiptId 为 {@code null}
		 */
		void addReceiptTask(Runnable task);

		/**
		 * {@link #addReceiptTask(Runnable)} 的变体，使用来自 {@code RECEIPT} 帧头的
		 * {@link Consumer}。
		 * @param task 要调用的消费者
		 * @throws java.lang.IllegalArgumentException 如果 receiptId 为 {@code null}
		 * @since 5.3.23
		 */
		void addReceiptTask(Consumer<StompHeaders> task);

		/**
		 * 在配置的时间内未收到回执时要调用的任务。
		 * @param task 要调用的任务
		 * @throws java.lang.IllegalArgumentException 如果 receiptId 为 {@code null}
		 * @see org.springframework.messaging.simp.stomp.StompClientSupport#setReceiptTimeLimit(long)
		 */
		void addReceiptLostTask(Runnable task);
	}


	/**
	 * 用于取消订阅或跟踪回执的句柄。
	 */
	interface Subscription extends Receiptable {

		/**
		 * 返回订阅的 ID。
		 */
		@Nullable
		String getSubscriptionId();

		/**
		 * 返回 SUBSCRIBE 帧上使用的头。
		 * @since 5.0
		 */
		StompHeaders getSubscriptionHeaders();

		/**
		 * 通过发送 UNSUBSCRIBE 帧移除订阅。
		 */
		void unsubscribe();

		/**
	 * {@link #unsubscribe()} 的替代方法，可以附加要发送到服务器的自定义头。
	 * <p><strong>注意：</strong>无需设置订阅 ID。
	 * @param headers 自定义消息头（如果有）
	 * @since 5.0
	 */
		void unsubscribe(@Nullable StompHeaders headers);
	}

}
