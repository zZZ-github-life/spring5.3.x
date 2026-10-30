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

package org.springframework.messaging.support;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MessageHeaders} 的包装器，提供额外功能，例如针对特定头部的强类型访问器、
 * 使 {@link Message} 中的头部保持可变的能力，以及禁止自动生成
 * {@link MessageHeaders#ID id} 和 {@link MessageHeaders#TIMESTAMP
 * timestamp} 头部的选项。{@link NativeMessageHeaderAccessor} 等子类
 * 提供了对处理头部与外部来源头部以及特定协议头部的管理支持。
 *
 * <p>下面是先通过 {@code MessageHeaderAccessor} 或其子类初始化头部，
 * 然后创建 {@link Message}，随后（可能从不同组件中）重新获取该访问器的工作流程：
 * <pre class="code">
 * // Create a message with headers
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * MessageHeaders headers = accessor.getMessageHeaders();
 * Message message = MessageBuilder.createMessage("payload", headers);
 *
 * // Later on
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * Assert.notNull(accessor, "No MessageHeaderAccessor");
 * </pre>
 *
 * <p>为使上述流程正常工作，所有参与的组件都必须使用 {@code MessageHeaders} 来创建、
 * 访问或修改头部，否则
 * {@link MessageHeaderAccessor#getAccessor(Message, Class)} 将返回 null。
 * 下面的工作流程展示了如何创建并保持头部可变，然后（可能由不同组件）进行修改，
 * 最后（可能在不同线程上被访问之前）将其变为不可变：
 * <pre class="code">
 * // Create a message with mutable headers
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * accessor.setLeaveMutable(true);
 * MessageHeaders headers = accessor.getMessageHeaders();
 * Message message = MessageBuilder.createMessage("payload", headers);
 *
 * // Later on
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * if (accessor.isMutable()) {
 *     // It's mutable, just change the headers
 *     accessor.setHeader("bar", "baz");
 * }
 * else {
 *     // It's not, so get a mutable copy, change and re-create
 *     accessor = MessageHeaderAccessor.getMutableAccessor(message);
 *     accessor.setHeader("bar", "baz");
 *     accessor.setLeaveMutable(true); // leave mutable again or not?
 *     message = MessageBuilder.createMessage(message.getPayload(), accessor);
 * }
 *
 * // Make the accessor immutable
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * accessor.setImmutable();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MessageHeaderAccessor {

	/**
	 * 头部使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final MimeType[] READABLE_MIME_TYPES = new MimeType[] {
			MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.APPLICATION_XML,
			new MimeType("text", "*"), new MimeType("application", "*+json"), new MimeType("application", "*+xml")
	};


	private final MutableMessageHeaders headers;

	private boolean leaveMutable = false;

	private boolean modified = false;

	private boolean enableTimestamp = false;

	@Nullable
	private IdGenerator idGenerator;


	/**
	 * 创建新头部的构造函数。
	 */
	public MessageHeaderAccessor() {
		this(null);
	}

	/**
	 * 接受现有消息头部进行复制的构造函数。
	 * @param message 要从中复制消息头的消息，如果没有则为 {@code null}
	 */
	public MessageHeaderAccessor(@Nullable Message<?> message) {
		this.headers = new MutableMessageHeaders(message != null ? message.getHeaders() : null);
	}


	/**
	 * 为给定消息构建“嵌套”访问器。
	 * @param message 要为其构建新访问器的消息
	 * @return 嵌套的访问器（通常是特定的子类）
	 */
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return new MessageHeaderAccessor(message);
	}


	// Configuration properties

	/**
	 * 默认情况下，调用 {@link #getMessageHeaders()} 后，{@code "this"}
	 * {@code MessageHeaderAccessor} 实例不能再用于修改底层消息头部，
	 * 并且返回的 {@code MessageHeaders} 是不可变的。
	 * <p>不过，当此项设置为 {@code true} 时，返回的（底层）
	 * {@code MessageHeaders} 实例保持可变。如需进一步修改，请继续使用同一个
	 * 访问器实例，或通过以下方式重新获取它：<br>
	 * {@link MessageHeaderAccessor#getAccessor(Message, Class)
	 * MessageHeaderAccessor.getAccessor(Message, Class)}
	 * <p>修改完成后，使用 {@link #setImmutable()} 防止进一步更改。
	 * 此机制的预期使用场景是在单个线程内初始化 Message。
	 * <p>默认情况下此项设置为 {@code false}。
	 * @since 4.1
	 */
	public void setLeaveMutable(boolean leaveMutable) {
		Assert.state(this.headers.isMutable(), "Already immutable");
		this.leaveMutable = leaveMutable;
	}

	/**
	 * 默认情况下，调用 {@link #getMessageHeaders()} 后，{@code "this"}
	 * {@code MessageHeaderAccessor} 实例不能再用于修改底层消息头部。
	 * 不过，如果使用了 {@link #setLeaveMutable(boolean)}，则必须通过此方法
	 * 显式指示 {@code MessageHeaders} 实例何时不应再被修改。
	 * @since 4.1
	 */
	public void setImmutable() {
		this.headers.setImmutable();
	}

	/**
	 * 底层头部是否仍可修改。
	 * @since 4.1
	 */
	public boolean isMutable() {
		return this.headers.isMutable();
	}

	/**
	 * 将底层消息头部标记为已修改。
	 * @param modified 通常为 {@code true}，或 {@code false} 以重置该标志
	 * @since 4.1
	 */
	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * 检查底层消息头部是否已被标记为已修改。
	 * @return 如果已设置该标志则为 {@code true}，否则为 {@code false}
	 */
	public boolean isModified() {
		return this.modified;
	}

	/**
	 * 一种包级私有机制，用于启用
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} 头部的自动添加。
	 * <p>默认情况下，此属性设置为 {@code false}。
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * 一种包级私有机制，用于配置要使用的 IdGenerator 策略。
	 * <p>默认情况下不设置此属性，此时使用
	 * {@link org.springframework.messaging.MessageHeaders} 中的默认 IdGenerator。
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}


	// Accessors for the resulting MessageHeaders

	/**
	 * 返回底层的 {@code MessageHeaders} 实例。
	 * <p>除非将 {@link #setLeaveMutable(boolean)} 设置为 {@code true}，
	 * 否则在此调用之后，头部将变为不可变，此访问器不能再修改它们。
	 * <p>如果多次调用，此方法始终返回相同的 {@code MessageHeaders} 实例。
	 * 若要获取底层头部的副本，请改用 {@link #toMessageHeaders()} 或
	 * {@link #toMap()}。
	 * @since 4.1
	 */
	public MessageHeaders getMessageHeaders() {
		if (!this.leaveMutable) {
			setImmutable();
		}
		return this.headers;
	}

	/**
	 * 以 {@link MessageHeaders} 对象形式返回底层头部值的副本。
	 * <p>此方法可以多次调用，其间可进行修改，每次调用都会返回当前头部值的
	 * 新副本。
	 * @since 4.1
	 */
	public MessageHeaders toMessageHeaders() {
		return new MessageHeaders(this.headers);
	}

	/**
	 * 以普通 {@link Map} 对象形式返回底层头部值的副本。
	 * <p>此方法可以多次调用，其间可进行修改，每次调用都会返回当前头部值的
	 * 新副本。
	 */
	public Map<String, Object> toMap() {
		return new HashMap<>(this.headers);
	}


	// Generic header accessors

	/**
	 * 检索具有给定名称的头部值。
	 * @param headerName 消息头名称
	 * @return 关联的值，如果未找到则为 {@code null}
	 */
	@Nullable
	public Object getHeader(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * 设置给定头部名称的值。
	 * <p>如果提供的值为 {@code null}，则将移除该头部。
	 */
	public void setHeader(String name, @Nullable Object value) {
		if (isReadOnly(name)) {
			throw new IllegalArgumentException("'" + name + "' header is read-only");
		}
		verifyType(name, value);
		if (value != null) {
			// Modify header if necessary
			if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
				this.modified = true;
				this.headers.getRawHeaders().put(name, value);
			}
		}
		else {
			// Remove header if available
			if (this.headers.containsKey(name)) {
				this.modified = true;
				this.headers.getRawHeaders().remove(name);
			}
		}
	}

	protected void verifyType(@Nullable String headerName, @Nullable Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ERROR_CHANNEL.equals(headerName) ||
					MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				if (!(headerValue instanceof MessageChannel || headerValue instanceof String)) {
					throw new IllegalArgumentException(
							"'" + headerName + "' header value must be a MessageChannel or String");
				}
			}
		}
	}

	/**
	 * 仅当给定头部名称尚未关联值时，才设置该头部名称的值。
	 */
	public void setHeaderIfAbsent(String name, Object value) {
		if (getHeader(name) == null) {
			setHeader(name, value);
		}
	}

	/**
	 * 移除给定头部名称的值。
	 */
	public void removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !isReadOnly(headerName)) {
			setHeader(headerName, null);
		}
	}

	/**
	 * 移除通过 'headerPatterns' 数组提供的所有头部。
	 * <p>顾名思义，该数组可以包含头部名称的简单匹配模式。支持的样式包括：
	 * "xxx*"、"*xxx"、"*xxx*" 和 "xxx*yyy"。
	 */
	public void removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
					headersToRemove.addAll(getMatchingHeaderNames(pattern, this.headers));
				}
				else {
					headersToRemove.add(pattern);
				}
			}
		}
		for (String headerToRemove : headersToRemove) {
			removeHeader(headerToRemove);
		}
	}

	private List<String> getMatchingHeaderNames(String pattern, @Nullable Map<String, Object> headers) {
		if (headers == null) {
			return Collections.emptyList();
		}
		List<String> matchingHeaderNames = new ArrayList<>();
		for (String key : headers.keySet()) {
			if (PatternMatchUtils.simpleMatch(pattern, key)) {
				matchingHeaderNames.add(key);
			}
		}
		return matchingHeaderNames;
	}

	/**
	 * 从提供的 Map 复制名称-值对。
	 * <p>此操作将覆盖任何现有值。可使用
	 * {@link #copyHeadersIfAbsent(Map)} 避免覆盖值。
	 */
	public void copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null || this.headers == headersToCopy) {
			return;
		}
		headersToCopy.forEach((key, value) -> {
			if (!isReadOnly(key)) {
				setHeader(key, value);
			}
		});
	}

	/**
	 * 从提供的 Map 复制名称-值对。
	 * <p>此操作<em>不会</em>覆盖任何现有值。
	 */
	public void copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null || this.headers == headersToCopy) {
			return;
		}
		headersToCopy.forEach((key, value) -> {
			if (!isReadOnly(key)) {
				setHeaderIfAbsent(key, value);
			}
		});
	}

	protected boolean isReadOnly(String headerName) {
		return (MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName));
	}


	// Specific header accessors

	@Nullable
	public UUID getId() {
		Object value = getHeader(MessageHeaders.ID);
		if (value == null) {
			return null;
		}
		return (value instanceof UUID ? (UUID) value : UUID.fromString(value.toString()));
	}

	@Nullable
	public Long getTimestamp() {
		Object value = getHeader(MessageHeaders.TIMESTAMP);
		if (value == null) {
			return null;
		}
		return (value instanceof Long ? (Long) value : Long.parseLong(value.toString()));
	}

	public void setContentType(MimeType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	@Nullable
	public MimeType getContentType() {
		Object value = getHeader(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		return (value instanceof MimeType ? (MimeType) value : MimeType.valueOf(value.toString()));
	}

	private Charset getCharset() {
		MimeType contentType = getContentType();
		Charset charset = (contentType != null ? contentType.getCharset() : null);
		return (charset != null ? charset : DEFAULT_CHARSET);
	}

	public void setReplyChannelName(String replyChannelName) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	@Nullable
	public Object getReplyChannel() {
		return getHeader(MessageHeaders.REPLY_CHANNEL);
	}

	public void setErrorChannelName(String errorChannelName) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

	@Nullable
	public Object getErrorChannel() {
		return getHeader(MessageHeaders.ERROR_CHANNEL);
	}


	// Log message stuff

	/**
	 * 返回用于日志记录目的的简洁消息。
	 * @param payload 与消息头对应的负载。
	 * @return 消息
	 */
	public String getShortLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getShortPayloadLogMessage(payload);
	}

	/**
	 * 返回用于日志记录目的的更详细消息。
	 * @param payload 与消息头对应的负载。
	 * @return 消息
	 */
	public String getDetailedLogMessage(@Nullable Object payload) {
		return "headers=" + this.headers.toString() + getDetailedPayloadLogMessage(payload);
	}

	protected String getShortPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			String payloadText = (String) payload;
			return (payloadText.length() < 80) ?
				" payload=" + payloadText :
				" payload=" + payloadText.substring(0, 80) + "...(truncated)";
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				return (bytes.length < 80) ?
						" payload=" + new String(bytes, getCharset()) :
						" payload=" + new String(Arrays.copyOf(bytes, 80), getCharset()) + "...(truncated)";
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			String payloadText = payload.toString();
			return (payloadText.length() < 80) ?
					" payload=" + payloadText :
					" payload=" + ObjectUtils.identityToString(payload);
		}
	}

	protected String getDetailedPayloadLogMessage(@Nullable Object payload) {
		if (payload instanceof String) {
			return " payload=" + payload;
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				return " payload=" + new String(bytes, getCharset());
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			return " payload=" + payload;
		}
	}

	protected boolean isReadableContentType() {
		MimeType contentType = getContentType();
		for (MimeType mimeType : READABLE_MIME_TYPES) {
			if (mimeType.includes(contentType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [headers=" + this.headers + "]";
	}


	// Static factory methods

	/**
	 * 返回用于创建给定 {@code Message} 头部的原始 {@code MessageHeaderAccessor}，
	 * 如果不可用或其类型与所需类型不匹配，则返回 {@code null}。
	 * <p>这适用于强烈预期访问器存在（后续会进行断言）或否则将创建访问器的情况。
	 * @param message 要获取访问器的消息
	 * @return 指定类型的访问器实例，如果没有则为 {@code null}
	 * @since 5.1.19
	 */
	@Nullable
	public static MessageHeaderAccessor getAccessor(Message<?> message) {
		return getAccessor(message.getHeaders(), null);
	}

	/**
	 * 返回用于创建给定 {@code Message} 头部的原始 {@code MessageHeaderAccessor}，
	 * 如果不可用或其类型与所需类型不匹配，则返回 {@code null}。
	 * <p>这适用于强烈预期访问器存在（后续会进行断言）或否则将创建访问器的情况。
	 * @param message 要获取访问器的消息
	 * @param requiredType 所需的访问器类型（或 {@code null} 表示任意类型）
	 * @return 指定类型的访问器实例，如果没有则为 {@code null}
	 * @since 4.1
	 */
	@Nullable
	public static <T extends MessageHeaderAccessor> T getAccessor(Message<?> message, @Nullable Class<T> requiredType) {
		return getAccessor(message.getHeaders(), requiredType);
	}

	/**
	 * {@link #getAccessor(org.springframework.messaging.Message, Class)} 的变体，
	 * 使用 {@code MessageHeaders} 实例而不是 {@code Message}。
	 * <p>这适用于完整消息可能尚未创建的情况。
	 * @param messageHeaders 要获取访问器的消息头
	 * @param requiredType 所需的访问器类型（或 {@code null} 表示任意类型）
	 * @return 指定类型的访问器实例，如果没有则为 {@code null}
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends MessageHeaderAccessor> T getAccessor(
			MessageHeaders messageHeaders, @Nullable Class<T> requiredType) {

		if (messageHeaders instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) messageHeaders;
			MessageHeaderAccessor headerAccessor = mutableHeaders.getAccessor();
			if (requiredType == null || requiredType.isInstance(headerAccessor))  {
				return (T) headerAccessor;
			}
		}
		return null;
	}

	/**
	 * 为给定消息返回可变的 {@code MessageHeaderAccessor}，尝试匹配用于创建该消息
	 * 头部的访问器类型，否则使用 {@code MessageHeaderAccessor} 实例包装该消息。
	 * <p>这适用于通用代码中需要更新头部、同时为下游处理保留访问器类型的情况。
	 * @return 所需类型的访问器（永不为 {@code null}）
	 * @since 4.1
	 */
	public static MessageHeaderAccessor getMutableAccessor(Message<?> message) {
		if (message.getHeaders() instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) message.getHeaders();
			MessageHeaderAccessor accessor = mutableHeaders.getAccessor();
			return (accessor.isMutable() ? accessor : accessor.createAccessor(message));
		}
		return new MessageHeaderAccessor(message);
	}


	/**
	 * {@link MessageHeaders} 的扩展，有助于保留对创建它的外部
	 * {@link MessageHeaderAccessor} 实例的链接，并跟踪头部是否仍可变。
	 */
	@SuppressWarnings("serial")
	private class MutableMessageHeaders extends MessageHeaders {

		private boolean mutable = true;

		public MutableMessageHeaders(@Nullable Map<String, Object> headers) {
			super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			Assert.state(this.mutable, "Already immutable");
			return super.getRawHeaders();
		}

		public void setImmutable() {
			if (!this.mutable) {
				return;
			}

			if (getId() == null) {
				IdGenerator idGenerator = (MessageHeaderAccessor.this.idGenerator != null ?
						MessageHeaderAccessor.this.idGenerator : MessageHeaders.getIdGenerator());
				UUID id = idGenerator.generateId();
				if (id != MessageHeaders.ID_VALUE_NONE) {
					getRawHeaders().put(ID, id);
				}
			}

			if (getTimestamp() == null) {
				if (MessageHeaderAccessor.this.enableTimestamp) {
					getRawHeaders().put(TIMESTAMP, System.currentTimeMillis());
				}
			}

			this.mutable = false;
		}

		public boolean isMutable() {
			return this.mutable;
		}

		public MessageHeaderAccessor getAccessor() {
			return MessageHeaderAccessor.this;
		}

		protected Object writeReplace() {
			// Serialize as regular MessageHeaders (without MessageHeaderAccessor reference)
			return new MessageHeaders(this);
		}
	}

}
