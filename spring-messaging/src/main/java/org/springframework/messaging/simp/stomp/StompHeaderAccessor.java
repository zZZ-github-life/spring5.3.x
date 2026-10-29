/*
 * Copyright 2002-2021 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * 用于从已解码的 STOMP 帧创建 {@code Message}，或将 {@code Message}
 * 编码为 STOMP 帧时使用的 {@code MessageHeaderAccessor}。
 *
 * <p>从 STOMP 帧内容创建时，实际的 STOMP 头存储在由父类
 * {@link org.springframework.messaging.support.NativeMessageHeaderAccessor}
 * 管理的原生头子映射中，而父类
 * {@link SimpMessageHeaderAccessor} 管理通用处理头，
 * 其中一些基于 STOMP 头（例如目标、content-type 等）。
 *
 * <p>也可以通过包装现有 {@code Message} 来创建此类的实例。
 * 该消息可能是使用更通用的
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor} 创建的，
 * 此时 STOMP 头会根据通用处理头生成。在这种情况下，如果发送消息，
 * 还需要根据消息是发送给客户端还是消息代理，
 * 调用 {@link #updateStompCommandAsClientMessage()} 或
 * {@link #updateStompCommandAsServerMessage()}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderAccessor extends SimpMessageHeaderAccessor {

	private static final AtomicLong messageIdCounter = new AtomicLong();

	private static final long[] DEFAULT_HEARTBEAT = new long[] {0, 0};


	// STOMP header names

	public static final String STOMP_ID_HEADER = "id";

	public static final String STOMP_HOST_HEADER = "host";

	public static final String STOMP_ACCEPT_VERSION_HEADER = "accept-version";

	public static final String STOMP_MESSAGE_ID_HEADER = "message-id";

	public static final String STOMP_RECEIPT_HEADER = "receipt"; // any client frame except CONNECT

	public static final String STOMP_RECEIPT_ID_HEADER = "receipt-id"; // RECEIPT frame

	public static final String STOMP_SUBSCRIPTION_HEADER = "subscription";

	public static final String STOMP_VERSION_HEADER = "version";

	public static final String STOMP_MESSAGE_HEADER = "message";

	public static final String STOMP_ACK_HEADER = "ack";

	public static final String STOMP_NACK_HEADER = "nack";

	public static final String STOMP_LOGIN_HEADER = "login";

	public static final String STOMP_PASSCODE_HEADER = "passcode";

	public static final String STOMP_DESTINATION_HEADER = "destination";

	public static final String STOMP_CONTENT_TYPE_HEADER = "content-type";

	public static final String STOMP_CONTENT_LENGTH_HEADER = "content-length";

	public static final String STOMP_HEARTBEAT_HEADER = "heart-beat";

	// Other header names

	private static final String COMMAND_HEADER = "stompCommand";

	private static final String CREDENTIALS_HEADER = "stompCredentials";


	/**
	 * 用于从已解析的 STOMP 帧创建消息头的构造函数。
	 */
	StompHeaderAccessor(StompCommand command, @Nullable Map<String, List<String>> externalSourceHeaders) {
		super(command.getMessageType(), externalSourceHeaders);
		setHeader(COMMAND_HEADER, command);
		updateSimpMessageHeadersFromStompHeaders();
	}

	/**
	 * 用于访问和修改现有消息头的构造函数。
	 * 注意，消息头可能不是从 STOMP 帧创建的，
	 * 而是源自使用更通用的
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor}。
	 */
	StompHeaderAccessor(Message<?> message) {
		super(message);
		updateStompHeadersFromSimpMessageHeaders();
	}

	StompHeaderAccessor() {
		super(SimpMessageType.HEARTBEAT, null);
	}


	void updateSimpMessageHeadersFromStompHeaders() {
		if (getNativeHeaders() == null) {
			return;
		}
		String value = getFirstNativeHeader(STOMP_DESTINATION_HEADER);
		if (value != null) {
			super.setDestination(value);
		}
		value = getFirstNativeHeader(STOMP_CONTENT_TYPE_HEADER);
		if (value != null) {
			super.setContentType(MimeTypeUtils.parseMimeType(value));
		}
		StompCommand command = getCommand();
		if (StompCommand.MESSAGE.equals(command)) {
			value = getFirstNativeHeader(STOMP_SUBSCRIPTION_HEADER);
			if (value != null) {
				super.setSubscriptionId(value);
			}
		}
		else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.UNSUBSCRIBE.equals(command)) {
			value = getFirstNativeHeader(STOMP_ID_HEADER);
			if (value != null) {
				super.setSubscriptionId(value);
			}
		}
		else if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
			protectPasscode();
		}
	}

	void updateStompHeadersFromSimpMessageHeaders() {
		String destination = getDestination();
		if (destination != null) {
			setNativeHeader(STOMP_DESTINATION_HEADER, destination);
		}
		MimeType contentType = getContentType();
		if (contentType != null) {
			setNativeHeader(STOMP_CONTENT_TYPE_HEADER, contentType.toString());
		}
		trySetStompHeaderForSubscriptionId();
	}


	@Override
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return wrap(message);
	}

	// Redeclared for visibility within simp.stomp
	@Override
	@Nullable
	protected Map<String, List<String>> getNativeHeaders() {
		return super.getNativeHeaders();
	}

	public StompCommand updateStompCommandAsClientMessage() {
		SimpMessageType messageType = getMessageType();
		if (messageType != SimpMessageType.MESSAGE) {
			throw new IllegalStateException("Unexpected message type " + messageType);
		}
		StompCommand command = getCommand();
		if (command == null) {
			command = StompCommand.SEND;
			setHeader(COMMAND_HEADER, command);
		}
		else if (!command.equals(StompCommand.SEND)) {
			throw new IllegalStateException("Unexpected STOMP command " + command);
		}
		return command;
	}

	public void updateStompCommandAsServerMessage() {
		SimpMessageType messageType = getMessageType();
		if (messageType != SimpMessageType.MESSAGE) {
			throw new IllegalStateException("Unexpected message type " + messageType);
		}
		StompCommand command = getCommand();
		if ((command == null) || StompCommand.SEND.equals(command)) {
			setHeader(COMMAND_HEADER, StompCommand.MESSAGE);
		}
		else if (!StompCommand.MESSAGE.equals(command)) {
			throw new IllegalStateException("Unexpected STOMP command " + command);
		}
		trySetStompHeaderForSubscriptionId();
		if (getMessageId() == null) {
			String messageId = getSessionId() + '-' + messageIdCounter.getAndIncrement();
			setNativeHeader(STOMP_MESSAGE_ID_HEADER, messageId);
		}
	}

	/**
	 * 返回 STOMP 命令，如果尚未设置则返回 {@code null}。
	 */
	@Nullable
	public StompCommand getCommand() {
		return (StompCommand) getHeader(COMMAND_HEADER);
	}

	public boolean isHeartbeat() {
		return (SimpMessageType.HEARTBEAT == getMessageType());
	}

	public long[] getHeartbeat() {
		String rawValue = getFirstNativeHeader(STOMP_HEARTBEAT_HEADER);
		String[] rawValues = StringUtils.split(rawValue, ",");
		if (rawValues == null) {
			return Arrays.copyOf(DEFAULT_HEARTBEAT, 2);
		}
		return new long[] {Long.parseLong(rawValues[0]), Long.parseLong(rawValues[1])};
	}

	public void setAcceptVersion(String acceptVersion) {
		setNativeHeader(STOMP_ACCEPT_VERSION_HEADER, acceptVersion);
	}

	public Set<String> getAcceptVersion() {
		String rawValue = getFirstNativeHeader(STOMP_ACCEPT_VERSION_HEADER);
		return (rawValue != null ? StringUtils.commaDelimitedListToSet(rawValue) : Collections.emptySet());
	}

	public void setHost(@Nullable String host) {
		setNativeHeader(STOMP_HOST_HEADER, host);
	}

	@Nullable
	public String getHost() {
		return getFirstNativeHeader(STOMP_HOST_HEADER);
	}

	@Override
	public void setDestination(@Nullable String destination) {
		super.setDestination(destination);
		setNativeHeader(STOMP_DESTINATION_HEADER, destination);
	}

	@Override
	public void setContentType(MimeType contentType) {
		super.setContentType(contentType);
		setNativeHeader(STOMP_CONTENT_TYPE_HEADER, contentType.toString());
	}

	@Override
	public void setSubscriptionId(@Nullable String subscriptionId) {
		super.setSubscriptionId(subscriptionId);
		trySetStompHeaderForSubscriptionId();
	}

	private void trySetStompHeaderForSubscriptionId() {
		String subscriptionId = getSubscriptionId();
		if (subscriptionId != null) {
			StompCommand command = getCommand();
			if (command != null && StompCommand.MESSAGE.equals(command)) {
				setNativeHeader(STOMP_SUBSCRIPTION_HEADER, subscriptionId);
			}
			else {
				SimpMessageType messageType = getMessageType();
				if (SimpMessageType.SUBSCRIBE.equals(messageType) || SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
					setNativeHeader(STOMP_ID_HEADER, subscriptionId);
				}
			}
		}
	}

	@Nullable
	public Integer getContentLength() {
		String header = getFirstNativeHeader(STOMP_CONTENT_LENGTH_HEADER);
		return (header != null ? Integer.valueOf(header) : null);
	}

	public void setContentLength(int contentLength) {
		setNativeHeader(STOMP_CONTENT_LENGTH_HEADER, String.valueOf(contentLength));
	}

	public void setHeartbeat(long cx, long cy) {
		setNativeHeader(STOMP_HEARTBEAT_HEADER, cx + "," + cy);
	}

	public void setAck(@Nullable String ack) {
		setNativeHeader(STOMP_ACK_HEADER, ack);
	}

	@Nullable
	public String getAck() {
		return getFirstNativeHeader(STOMP_ACK_HEADER);
	}

	public void setNack(@Nullable String nack) {
		setNativeHeader(STOMP_NACK_HEADER, nack);
	}

	@Nullable
	public String getNack() {
		return getFirstNativeHeader(STOMP_NACK_HEADER);
	}

	public void setLogin(@Nullable String login) {
		setNativeHeader(STOMP_LOGIN_HEADER, login);
	}

	@Nullable
	public String getLogin() {
		return getFirstNativeHeader(STOMP_LOGIN_HEADER);
	}

	public void setPasscode(@Nullable String passcode) {
		setNativeHeader(STOMP_PASSCODE_HEADER, passcode);
		protectPasscode();
	}

	private void protectPasscode() {
		String value = getFirstNativeHeader(STOMP_PASSCODE_HEADER);
		if (value != null && !"PROTECTED".equals(value)) {
			setHeader(CREDENTIALS_HEADER, new StompPasscode(value));
			setNativeHeader(STOMP_PASSCODE_HEADER, "PROTECTED");
		}
	}

	/**
	 * 返回密码头值，如果未设置则返回 {@code null}。
	 */
	@Nullable
	public String getPasscode() {
		StompPasscode credentials = (StompPasscode) getHeader(CREDENTIALS_HEADER);
		return (credentials != null ? credentials.passcode : null);
	}

	public void setReceiptId(@Nullable String receiptId) {
		setNativeHeader(STOMP_RECEIPT_ID_HEADER, receiptId);
	}

	@Nullable
	public String getReceiptId() {
		return getFirstNativeHeader(STOMP_RECEIPT_ID_HEADER);
	}

	public void setReceipt(@Nullable String receiptId) {
		setNativeHeader(STOMP_RECEIPT_HEADER, receiptId);
	}

	@Nullable
	public String getReceipt() {
		return getFirstNativeHeader(STOMP_RECEIPT_HEADER);
	}

	@Nullable
	public String getMessage() {
		return getFirstNativeHeader(STOMP_MESSAGE_HEADER);
	}

	public void setMessage(@Nullable String content) {
		setNativeHeader(STOMP_MESSAGE_HEADER, content);
	}

	@Nullable
	public String getMessageId() {
		return getFirstNativeHeader(STOMP_MESSAGE_ID_HEADER);
	}

	public void setMessageId(@Nullable String id) {
		setNativeHeader(STOMP_MESSAGE_ID_HEADER, id);
	}

	@Nullable
	public String getVersion() {
		return getFirstNativeHeader(STOMP_VERSION_HEADER);
	}

	public void setVersion(@Nullable String version) {
		setNativeHeader(STOMP_VERSION_HEADER, version);
	}


	// Logging related

	@Override
	public String getShortLogMessage(Object payload) {
		StompCommand command = getCommand();
		if (StompCommand.SUBSCRIBE.equals(command)) {
			return "SUBSCRIBE " + getDestination() + " id=" + getSubscriptionId() + appendSession();
		}
		else if (StompCommand.UNSUBSCRIBE.equals(command)) {
			return "UNSUBSCRIBE id=" + getSubscriptionId() + appendSession();
		}
		else if (StompCommand.SEND.equals(command)) {
			return "SEND " + getDestination() + appendSession() + appendPayload(payload);
		}
		else if (StompCommand.CONNECT.equals(command)) {
			Principal user = getUser();
			return "CONNECT" + (user != null ? " user=" + user.getName() : "") + appendSession();
		}
		else if (StompCommand.STOMP.equals(command)) {
			Principal user = getUser();
			return "STOMP" + (user != null ? " user=" + user.getName() : "") + appendSession();
		}
		else if (StompCommand.CONNECTED.equals(command)) {
			return "CONNECTED heart-beat=" + Arrays.toString(getHeartbeat()) + appendSession();
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			String receipt = getReceipt();
			return "DISCONNECT" + (receipt != null ? " receipt=" + receipt : "") + appendSession();
		}
		else {
			return getDetailedLogMessage(payload);
		}
	}

	@Override
	public String getDetailedLogMessage(@Nullable Object payload) {
		if (isHeartbeat()) {
			String sessionId = getSessionId();
			return "heart-beat" + (sessionId != null ? " in session " + sessionId : "");
		}
		StompCommand command = getCommand();
		if (command == null) {
			return super.getDetailedLogMessage(payload);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(command.name()).append(' ');
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders != null) {
			sb.append(nativeHeaders);
		}
		sb.append(appendSession());
		if (getUser() != null) {
			sb.append(", user=").append(getUser().getName());
		}
		if (payload != null && command.isBodyAllowed()) {
			sb.append(appendPayload(payload));
		}
		return sb.toString();
	}

	private String appendSession() {
		return " session=" + getSessionId();
	}

	private String appendPayload(Object payload) {
		if (payload.getClass() != byte[].class) {
			throw new IllegalStateException(
					"Expected byte array payload but got: " + ClassUtils.getQualifiedName(payload.getClass()));
		}
		byte[] bytes = (byte[]) payload;
		MimeType mimeType = getContentType();
		String contentType = (mimeType != null ? " " + mimeType.toString() : "");
		if (bytes.length == 0 || mimeType == null || !isReadableContentType()) {
			return contentType;
		}
		Charset charset = mimeType.getCharset();
		charset = (charset != null ? charset : StandardCharsets.UTF_8);
		return (bytes.length < 80) ?
				contentType + " payload=" + new String(bytes, charset) :
				contentType + " payload=" + new String(Arrays.copyOf(bytes, 80), charset) + "...(truncated)";
	}


	// Static factory methods and accessors

	/**
	 * 为给定 STOMP 命令创建实例。
	 */
	public static StompHeaderAccessor create(StompCommand command) {
		return new StompHeaderAccessor(command, null);
	}

	/**
	 * 为给定 STOMP 命令和头创建实例。
	 */
	public static StompHeaderAccessor create(StompCommand command, Map<String, List<String>> headers) {
		return new StompHeaderAccessor(command, headers);
	}

	/**
	 * 创建心跳头。虽然 STOMP 心跳帧没有头，
	 * 但出于处理目的，至少需要一个会话 ID。
	 */
	public static StompHeaderAccessor createForHeartbeat() {
		return new StompHeaderAccessor();
	}

	/**
	 * 从给定 Message 的载荷和头创建实例。
	 */
	public static StompHeaderAccessor wrap(Message<?> message) {
		return new StompHeaderAccessor(message);
	}

	/**
	 * 从给定头中返回 STOMP 命令，如果未设置则返回 {@code null}。
	 */
	@Nullable
	public static StompCommand getCommand(Map<String, Object> headers) {
		return (StompCommand) headers.get(COMMAND_HEADER);
	}

	/**
	 * 返回密码头值，如果未设置则返回 {@code null}。
	 */
	@Nullable
	public static String getPasscode(Map<String, Object> headers) {
		StompPasscode credentials = (StompPasscode) headers.get(CREDENTIALS_HEADER);
		return (credentials != null ? credentials.passcode : null);
	}

	@Nullable
	public static Integer getContentLength(Map<String, List<String>> nativeHeaders) {
		List<String> values = nativeHeaders.get(STOMP_CONTENT_LENGTH_HEADER);
		return (!CollectionUtils.isEmpty(values) ? Integer.valueOf(values.get(0)) : null);
	}


	private static class StompPasscode {

		private final String passcode;

		public StompPasscode(String passcode) {
			this.passcode = passcode;
		}

		@Override
		public String toString() {
			return "[PROTECTED]";
		}
	}

}
