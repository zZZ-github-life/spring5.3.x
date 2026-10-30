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

package org.springframework.messaging.simp;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.IdTimestampMessageHeaderInitializer;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 用于处理简单消息协议中消息头的基类，这些协议支持基本的消息传递模式。
 * 提供对协议间通用特定值的统一访问，例如目标、消息类型（发布、订阅等）、
 * 会话 ID 等。
 *
 * <p>使用此类中的某个静态工厂方法，然后调用 getter 和 setter，
 * 最后如有必要调用 {@link #toMap()} 获取更新后的头。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageHeaderAccessor extends NativeMessageHeaderAccessor {

	private static final IdTimestampMessageHeaderInitializer headerInitializer;

	static {
		headerInitializer = new IdTimestampMessageHeaderInitializer();
		headerInitializer.setDisableIdGeneration();
		headerInitializer.setEnableTimestamp(false);
	}

	// SiMP header names

	public static final String DESTINATION_HEADER = "simpDestination";

	public static final String MESSAGE_TYPE_HEADER = "simpMessageType";

	public static final String SESSION_ID_HEADER = "simpSessionId";

	public static final String SESSION_ATTRIBUTES = "simpSessionAttributes";

	public static final String SUBSCRIPTION_ID_HEADER = "simpSubscriptionId";

	public static final String USER_HEADER = "simpUser";

	public static final String CONNECT_MESSAGE_HEADER = "simpConnectMessage";

	public static final String DISCONNECT_MESSAGE_HEADER = "simpDisconnectMessage";

	public static final String HEART_BEAT_HEADER = "simpHeartbeat";


	/**
	 * 供内部使用的头，用于“用户”目标，在向客户端发送消息之前需要恢复该目标。
	 */
	public static final String ORIGINAL_DESTINATION = "simpOrigDestination";

	/**
	 * 向代理指示发送者将忽略错误的头。只需检查该头是否存在。
	 */
	public static final String IGNORE_ERROR = "simpIgnoreError";


	@Nullable
	private Consumer<Principal> userCallback;


	/**
	 * 用于创建新消息头的构造函数。
	 * <p>此构造函数为 protected。请参阅此类及子类中的工厂方法。
	 */
	protected SimpMessageHeaderAccessor(SimpMessageType messageType,
			@Nullable Map<String, List<String>> externalSourceHeaders) {

		super(externalSourceHeaders);
		Assert.notNull(messageType, "MessageType must not be null");
		setHeader(MESSAGE_TYPE_HEADER, messageType);
		headerInitializer.initHeaders(this);
	}

	/**
	 * 用于访问和修改现有消息头的构造函数。
	 * <p>此构造函数为 protected。请参阅此类及子类中的工厂方法。
	 */
	protected SimpMessageHeaderAccessor(Message<?> message) {
		super(message);
		headerInitializer.initHeaders(this);
	}


	@Override
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return wrap(message);
	}

	public void setMessageTypeIfNotSet(SimpMessageType messageType) {
		if (getMessageType() == null) {
			setHeader(MESSAGE_TYPE_HEADER, messageType);
		}
	}

	@Nullable
	public SimpMessageType getMessageType() {
		return (SimpMessageType) getHeader(MESSAGE_TYPE_HEADER);
	}

	public void setDestination(@Nullable String destination) {
		setHeader(DESTINATION_HEADER, destination);
	}

	@Nullable
	public String getDestination() {
		return (String) getHeader(DESTINATION_HEADER);
	}

	public void setSubscriptionId(@Nullable String subscriptionId) {
		setHeader(SUBSCRIPTION_ID_HEADER, subscriptionId);
	}

	@Nullable
	public String getSubscriptionId() {
		return (String) getHeader(SUBSCRIPTION_ID_HEADER);
	}

	public void setSessionId(@Nullable String sessionId) {
		setHeader(SESSION_ID_HEADER, sessionId);
	}

	/**
	 * 返回当前会话的 ID。
	 */
	@Nullable
	public String getSessionId() {
		return (String) getHeader(SESSION_ID_HEADER);
	}

	/**
	 * 用于访问会话属性头的静态替代方法。
	 */
	public void setSessionAttributes(@Nullable Map<String, Object> attributes) {
		setHeader(SESSION_ATTRIBUTES, attributes);
	}

	/**
	 * 返回与当前会话关联的属性。
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public Map<String, Object> getSessionAttributes() {
		return (Map<String, Object>) getHeader(SESSION_ATTRIBUTES);
	}

	public void setUser(@Nullable Principal principal) {
		setHeader(USER_HEADER, principal);
		if (this.userCallback != null) {
			this.userCallback.accept(principal);
		}
	}

	/**
	 * 返回与当前会话关联的用户。
	 */
	@Nullable
	public Principal getUser() {
		return (Principal) getHeader(USER_HEADER);
	}

	/**
	 * 提供在调用 {@link #setUser(Principal)} 时（如果发生）要调用的回调。
	 * 这在入站通道内部用于通过拦截器检测基于令牌的认证。
	 * @param callback 要调用的回调
	 * @since 5.1.9
	 */
	public void setUserChangeCallback(Consumer<Principal> callback) {
		Assert.notNull(callback, "'callback' is required");
		this.userCallback = this.userCallback != null ? this.userCallback.andThen(callback) : callback;
	}

	@Override
	public String getShortLogMessage(Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes[").append(getSessionAttributes().size()).append(']');
		}
		sb.append(getShortPayloadLogMessage(payload));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getDetailedLogMessage(@Nullable Object payload) {
		if (getMessageType() == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = getBaseLogMessage();
		if (!CollectionUtils.isEmpty(getSessionAttributes())) {
			sb.append(" attributes=").append(getSessionAttributes());
		}
		if (!CollectionUtils.isEmpty((Map<String, List<String>>) getHeader(NATIVE_HEADERS))) {
			sb.append(" nativeHeaders=").append(getHeader(NATIVE_HEADERS));
		}
		sb.append(getDetailedPayloadLogMessage(payload));
		return sb.toString();
	}

	private StringBuilder getBaseLogMessage() {
		StringBuilder sb = new StringBuilder();
		SimpMessageType messageType = getMessageType();
		sb.append(messageType != null ? messageType.name() : SimpMessageType.OTHER);
		String destination = getDestination();
		if (destination != null) {
			sb.append(" destination=").append(destination);
		}
		String subscriptionId = getSubscriptionId();
		if (subscriptionId != null) {
			sb.append(" subscriptionId=").append(subscriptionId);
		}
		sb.append(" session=").append(getSessionId());
		Principal user = getUser();
		if (user != null) {
			sb.append(" user=").append(user.getName());
		}
		return sb;
	}


	// Static factory methods and accessors

	/**
	 * 创建一个 {@link org.springframework.messaging.simp.SimpMessageType} 为
	 * {@code MESSAGE} 的实例。
	 */
	public static SimpMessageHeaderAccessor create() {
		return new SimpMessageHeaderAccessor(SimpMessageType.MESSAGE, null);
	}

	/**
	 * 创建具有给定 {@link org.springframework.messaging.simp.SimpMessageType} 的实例。
	 */
	public static SimpMessageHeaderAccessor create(SimpMessageType messageType) {
		return new SimpMessageHeaderAccessor(messageType, null);
	}

	/**
	 * 从给定 Message 的载荷和头创建实例。
	 */
	public static SimpMessageHeaderAccessor wrap(Message<?> message) {
		return new SimpMessageHeaderAccessor(message);
	}

	@Nullable
	public static SimpMessageType getMessageType(Map<String, Object> headers) {
		return (SimpMessageType) headers.get(MESSAGE_TYPE_HEADER);
	}

	@Nullable
	public static String getDestination(Map<String, Object> headers) {
		return (String) headers.get(DESTINATION_HEADER);
	}

	@Nullable
	public static String getSubscriptionId(Map<String, Object> headers) {
		return (String) headers.get(SUBSCRIPTION_ID_HEADER);
	}

	@Nullable
	public static String getSessionId(Map<String, Object> headers) {
		return (String) headers.get(SESSION_ID_HEADER);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static Map<String, Object> getSessionAttributes(Map<String, Object> headers) {
		return (Map<String, Object>) headers.get(SESSION_ATTRIBUTES);
	}

	@Nullable
	public static Principal getUser(Map<String, Object> headers) {
		return (Principal) headers.get(USER_HEADER);
	}

	@Nullable
	public static long[] getHeartbeat(Map<String, Object> headers) {
		return (long[]) headers.get(HEART_BEAT_HEADER);
	}

}
