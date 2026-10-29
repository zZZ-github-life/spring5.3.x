/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.IdGenerator;

/**
 * {@link Message} 的消息头。
 *
 * <p><b>重要</b>：此类是不可变的。任何修改操作（例如
 * {@code put(..)}、{@code putAll(..)} 等）都会抛出
 * {@link UnsupportedOperationException}。
 * <p>不过，子类可以通过 {@link #getRawHeaders()} 访问原始头。
 *
 * <p>创建消息头的一种方式是使用
 * {@link org.springframework.messaging.support.MessageBuilder MessageBuilder}：
 * <pre class="code">
 * MessageBuilder.withPayload("foo").setHeader("key1", "value1").setHeader("key2", "value2");
 * </pre>
 *
 * 第二种选择是创建 {@link org.springframework.messaging.support.GenericMessage}，
 * 将载荷作为 {@link Object} 传入，将头作为 {@link Map java.util.Map} 传入：
 * <pre class="code">
 * Map headers = new HashMap();
 * headers.put("key1", "value1");
 * headers.put("key2", "value2");
 * new GenericMessage("foo", headers);
 * </pre>
 *
 * 第三种选择是使用
 * {@link org.springframework.messaging.support.MessageHeaderAccessor}
 * 或其子类创建特定类别的头。
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Gary Russell
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.messaging.support.MessageBuilder
 * @see org.springframework.messaging.support.MessageHeaderAccessor
 */
public class MessageHeaders implements Map<String, Object>, Serializable {

	/**
	 * 表示“无”的 UUID。
	 */
	public static final UUID ID_VALUE_NONE = new UUID(0,0);

	/**
	 * 消息 ID 的键。这是一个自动生成的 UUID，
	 * <b>除</b>消息反序列化时恢复序列化消息所生成的 UUID 的情况外，
	 * 绝不应在头映射中显式设置。
	 */
	public static final String ID = "id";

	/**
	 * 消息时间戳的键。
	 */
	public static final String TIMESTAMP = "timestamp";

	/**
	 * 消息内容类型的键。
	 */
	public static final String CONTENT_TYPE = "contentType";

	/**
	 * 消息应答通道的键。
	 */
	public static final String REPLY_CHANNEL = "replyChannel";

	/**
	 * 消息错误通道的键。
	 */
	public static final String ERROR_CHANNEL = "errorChannel";


	private static final long serialVersionUID = 7035068984263400920L;

	private static final Log logger = LogFactory.getLog(MessageHeaders.class);

	private static final IdGenerator defaultIdGenerator = new AlternativeJdkIdGenerator();

	@Nullable
	private static volatile IdGenerator idGenerator;


	private final Map<String, Object> headers;


	/**
	 * 使用给定头构造 {@link MessageHeaders}。还会添加 {@link #ID} 和
	 * {@link #TIMESTAMP} 头，并覆盖任何现有值。
	 * @param headers a map with headers to add
	 */
	public MessageHeaders(@Nullable Map<String, Object> headers) {
		this(headers, null, null);
	}

	/**
	 * 提供对 ID 和 TIMESTAMP 头值控制的构造函数。
	 * @param headers a map with headers to add
	 * @param id the {@link #ID} header value
	 * @param timestamp the {@link #TIMESTAMP} header value
	 */
	protected MessageHeaders(@Nullable Map<String, Object> headers, @Nullable UUID id, @Nullable Long timestamp) {
		this.headers = (headers != null ? new HashMap<>(headers) : new HashMap<>());

		if (id == null) {
			this.headers.put(ID, getIdGenerator().generateId());
		}
		else if (id == ID_VALUE_NONE) {
			this.headers.remove(ID);
		}
		else {
			this.headers.put(ID, id);
		}

		if (timestamp == null) {
			this.headers.put(TIMESTAMP, System.currentTimeMillis());
		}
		else if (timestamp < 0) {
			this.headers.remove(TIMESTAMP);
		}
		else {
			this.headers.put(TIMESTAMP, timestamp);
		}
	}

	/**
	 * 允许忽略某些条目的拷贝构造函数。
	 * 用于不包含不可序列化条目的序列化。
	 * @param original the MessageHeaders to copy
	 * @param keysToIgnore the keys of the entries to ignore
	 */
	private MessageHeaders(MessageHeaders original, Set<String> keysToIgnore) {
		this.headers = CollectionUtils.newHashMap(original.headers.size());
		original.headers.forEach((key, value) -> {
			if (!keysToIgnore.contains(key)) {
				this.headers.put(key, value);
			}
		});
	}


	protected Map<String, Object> getRawHeaders() {
		return this.headers;
	}

	protected static IdGenerator getIdGenerator() {
		IdGenerator generator = idGenerator;
		return (generator != null ? generator : defaultIdGenerator);
	}

	@Nullable
	public UUID getId() {
		return get(ID, UUID.class);
	}

	@Nullable
	public Long getTimestamp() {
		return get(TIMESTAMP, Long.class);
	}

	@Nullable
	public Object getReplyChannel() {
		return get(REPLY_CHANNEL);
	}

	@Nullable
	public Object getErrorChannel() {
		return get(ERROR_CHANNEL);
	}


	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(Object key, Class<T> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" +
					key + "'. Expected [" + type + "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}


	// Delegating Map implementation

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableMap(this.headers).entrySet();
	}

	@Override
	@Nullable
	public Object get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}


	// Unsupported Map operations

	/**
	 * 由于 MessageHeaders 是不可变的，调用此方法将导致
	 * {@link UnsupportedOperationException}。
	 */
	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于 MessageHeaders 是不可变的，调用此方法将导致
	 * {@link UnsupportedOperationException}。
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于 MessageHeaders 是不可变的，调用此方法将导致
	 * {@link UnsupportedOperationException}。
	 */
	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	/**
	 * 由于 MessageHeaders 是不可变的，调用此方法将导致
	 * {@link UnsupportedOperationException}。
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}


	// Serialization methods

	private void writeObject(ObjectOutputStream out) throws IOException {
		Set<String> keysToIgnore = new HashSet<>();
		this.headers.forEach((key, value) -> {
			if (!(value instanceof Serializable)) {
				keysToIgnore.add(key);
			}
		});

		if (keysToIgnore.isEmpty()) {
			// All entries are serializable -> serialize the regular MessageHeaders instance
			out.defaultWriteObject();
		}
		else {
			// Some non-serializable entries -> serialize a temporary MessageHeaders copy
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring non-serializable message headers: " + keysToIgnore);
			}
			out.writeObject(new MessageHeaders(this, keysToIgnore));
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}


	// equals, hashCode, toString

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other ||
				(other instanceof MessageHeaders && this.headers.equals(((MessageHeaders) other).headers)));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}

}
