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

package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * {@link MessageSendingOperations} 的特化，提供用于 Spring Framework 对简单消息传递协议
 * （如 STOMP）支持的方法。
 *
 * <p>有关用户目标的更多信息，请参阅
 * {@link org.springframework.messaging.simp.user.UserDestinationResolver
 * UserDestinationResolver}。
 *
 * <p>通常期望用户是已通过 WebSocket 会话认证的用户（或者扩展地说，是通过启动该会话的
 * 握手请求认证的用户）。不过，如果会话未认证，也可以传入会话 ID（如果已知）
 * 来代替用户名。但请记住，在这种情况下，必须使用接受头的重载方法之一，
 * 并确保已相应设置
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor#setSessionId
 * sessionId} 头。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SimpMessageSendingOperations extends MessageSendingOperations<String> {

	/**
	 * 向给定用户发送消息。
	 * @param user the user that should receive the message.
	 * @param destination the destination to send the message to.
	 * @param payload the payload to send
	 */
	void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException;

	/**
	 * 向给定用户发送消息。
	 * <p>默认情况下，头会被解释为原生头（例如 STOMP），并保存在生成的 Spring
	 * {@link org.springframework.messaging.Message Message} 中的特殊键下。
	 * 实际上，当消息离开应用程序时，提供的头会随消息一起包含，
	 * 并投递到目标（例如 STOMP 客户端或代理）。
	 * <p>如果映射中已经包含键
	 * {@link org.springframework.messaging.support.NativeMessageHeaderAccessor#NATIVE_HEADERS "nativeHeaders"}
	 * 或者已使用
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor SimpMessageHeaderAccessor}
	 * 准备，则直接使用这些头。常见的预期情况是提供内容类型（以影响消息转换）
	 * 和原生头。可以按如下方式完成：
	 * <pre class="code">
	 * SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
	 * accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);
	 * accessor.setNativeHeader("foo", "bar");
	 * accessor.setLeaveMutable(true);
	 * MessageHeaders headers = accessor.getMessageHeaders();
	 * messagingTemplate.convertAndSendToUser(user, destination, payload, headers);
	 * </pre>
	 * <p><strong>注意：</strong>如果如上例所示 {@code MessageHeaders} 是可变的，
	 * 此接口的实现应注意在同一实例中更新头（而不是复制或重新创建），
	 * 然后在发送最终消息之前将其设置为不可变。
	 * @param user the user that should receive the message (must not be {@code null})
	 * @param destination the destination to send the message to (must not be {@code null})
	 * @param payload the payload to send (may be {@code null})
	 * @param headers the message headers (may be {@code null})
	 */
	void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 向给定用户发送消息。
	 * @param user the user that should receive the message (must not be {@code null})
	 * @param destination the destination to send the message to (must not be {@code null})
	 * @param payload the payload to send (may be {@code null})
	 * @param postProcessor a postProcessor to post-process or modify the created message
	 */
	void convertAndSendToUser(String user, String destination, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 向给定用户发送消息。
	 * <p>有关输入头的重要说明，请参阅
	 * {@link #convertAndSend(Object, Object, java.util.Map)}。
	 * @param user the user that should receive the message
	 * @param destination the destination to send the message to
	 * @param payload the payload to send
	 * @param headers the message headers
	 * @param postProcessor a postProcessor to post-process or modify the created message
	 */
	void convertAndSendToUser(String user, String destination, Object payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException;

}
