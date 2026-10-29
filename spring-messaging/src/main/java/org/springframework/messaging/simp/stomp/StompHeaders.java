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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 表示 STOMP 帧头。
 *
 * <p>除了 {@link Map} 定义的常规方法外，此类还提供以下便捷方法：
 * <ul>
 * <li>{@link #getFirst(String)} 返回头名称的第一个值</li>
 * <li>{@link #add(String, String)} 添加到头名称的值列表</li>
 * <li>{@link #set(String, String)} 将头名称设置为单个字符串值</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#Frames_and_Headers">
 * https://stomp.github.io/stomp-specification-1.2.html#Frames_and_Headers</a>
 */
public class StompHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = 7514642206528452544L;


	// Standard headers (as defined in the spec)

	public static final String CONTENT_TYPE = "content-type"; // SEND, MESSAGE, ERROR

	public static final String CONTENT_LENGTH = "content-length"; // SEND, MESSAGE, ERROR

	public static final String RECEIPT = "receipt"; // any client frame other than CONNECT

	// CONNECT

	public static final String HOST = "host";

	public static final String ACCEPT_VERSION = "accept-version";

	public static final String LOGIN = "login";

	public static final String PASSCODE = "passcode";

	public static final String HEARTBEAT = "heart-beat";

	// CONNECTED

	public static final String SESSION = "session";

	public static final String SERVER = "server";

	// SEND

	public static final String DESTINATION = "destination";

	// SUBSCRIBE, UNSUBSCRIBE

	public static final String ID = "id";

	public static final String ACK = "ack";

	// MESSAGE

	public static final String SUBSCRIPTION = "subscription";

	public static final String MESSAGE_ID = "message-id";

	// RECEIPT

	public static final String RECEIPT_ID = "receipt-id";


	private final Map<String, List<String>> headers;


	/**
	 * 创建一个新实例，用于填充新的头值。
	 */
	public StompHeaders() {
		this(new LinkedMultiValueMap<>(3), false);
	}

	private StompHeaders(Map<String, List<String>> headers, boolean readOnly) {
		Assert.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map = new LinkedMultiValueMap<>(headers.size());
			headers.forEach((key, value) -> map.put(key, Collections.unmodifiableList(value)));
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
	}


	/**
	 * 设置 content-type 头。
	 * 适用于 SEND、MESSAGE 和 ERROR 帧。
	 */
	public void setContentType(@Nullable MimeType mimeType) {
		if (mimeType != null) {
			Assert.isTrue(!mimeType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
			Assert.isTrue(!mimeType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
			set(CONTENT_TYPE, mimeType.toString());
		}
		else {
			set(CONTENT_TYPE, null);
		}
	}

	/**
	 * 返回 content-type 头值。
	 */
	@Nullable
	public MimeType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MimeTypeUtils.parseMimeType(value) : null);
	}

	/**
	 * 设置 content-length 头。
	 * 适用于 SEND、MESSAGE 和 ERROR 帧。
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * 返回 content-length 头，如果未知则返回 -1。
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置 receipt 头。
	 * 适用于除 CONNECT 之外的任何客户端帧。
	 */
	public void setReceipt(@Nullable String receipt) {
		set(RECEIPT, receipt);
	}

	/**
	 * 获取 receipt 头。
	 */
	@Nullable
	public String getReceipt() {
		return getFirst(RECEIPT);
	}

	/**
	 * 设置 host 头。
	 * 适用于 CONNECT 帧。
	 */
	public void setHost(@Nullable String host) {
		set(HOST, host);
	}

	/**
	 * 获取 host 头。
	 */
	@Nullable
	public String getHost() {
		return getFirst(HOST);
	}

	/**
	 * 设置 accept-version 头。必须为 "1.1"、"1.2" 或两者。
	 * 适用于 CONNECT 帧。
	 * @since 5.0.7
	 */
	public void setAcceptVersion(@Nullable String... acceptVersions) {
		if (ObjectUtils.isEmpty(acceptVersions)) {
			set(ACCEPT_VERSION, null);
			return;
		}
		Arrays.stream(acceptVersions).forEach(version ->
				Assert.isTrue(version != null && (version.equals("1.1") || version.equals("1.2")),
						() -> "Invalid version: " + version));
		set(ACCEPT_VERSION, StringUtils.arrayToCommaDelimitedString(acceptVersions));
	}

	/**
	 * 获取 accept-version 头。
	 * @since 5.0.7
	 */
	@Nullable
	public String[] getAcceptVersion() {
		String value = getFirst(ACCEPT_VERSION);
		return value != null ? StringUtils.commaDelimitedListToStringArray(value) : null;
	}

	/**
	 * 设置 login 头。
	 * 适用于 CONNECT 帧。
	 */
	public void setLogin(@Nullable String login) {
		set(LOGIN, login);
	}

	/**
	 * 获取 login 头。
	 */
	@Nullable
	public String getLogin() {
		return getFirst(LOGIN);
	}

	/**
	 * 设置 passcode 头。
	 * 适用于 CONNECT 帧。
	 */
	public void setPasscode(@Nullable String passcode) {
		set(PASSCODE, passcode);
	}

	/**
	 * 获取 passcode 头。
	 */
	@Nullable
	public String getPasscode() {
		return getFirst(PASSCODE);
	}

	/**
	 * 设置 heartbeat 头。
	 * 适用于 CONNECT 和 CONNECTED 帧。
	 */
	public void setHeartbeat(@Nullable long[] heartbeat) {
		if (heartbeat == null || heartbeat.length != 2) {
			throw new IllegalArgumentException("Heart-beat array must be of length 2, not " +
					(heartbeat != null ? heartbeat.length : "null"));
		}
		String value = heartbeat[0] + "," + heartbeat[1];
		if (heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Heart-beat values cannot be negative: " + value);
		}
		set(HEARTBEAT, value);
	}

	/**
	 * 获取 heartbeat 头。
	 */
	@Nullable
	public long[] getHeartbeat() {
		String rawValue = getFirst(HEARTBEAT);
		String[] rawValues = StringUtils.split(rawValue, ",");
		if (rawValues == null) {
			return null;
		}
		return new long[] {Long.parseLong(rawValues[0]), Long.parseLong(rawValues[1])};
	}

	/**
	 * 是否启用心跳。如果 {@link #setHeartbeat} 设置为 "0,0"，则返回
	 * {@code false}，否则返回 {@code true}。
	 */
	public boolean isHeartbeatEnabled() {
		long[] heartbeat = getHeartbeat();
		return (heartbeat != null && heartbeat[0] != 0 && heartbeat[1] != 0);
	}

	/**
	 * 设置 session 头。
	 * 适用于 CONNECTED 帧。
	 */
	public void setSession(@Nullable String session) {
		set(SESSION, session);
	}

	/**
	 * 获取 session 头。
	 */
	@Nullable
	public String getSession() {
		return getFirst(SESSION);
	}

	/**
	 * 设置 server 头。
	 * 适用于 CONNECTED 帧。
	 */
	public void setServer(@Nullable String server) {
		set(SERVER, server);
	}

	/**
	 * 获取 server 头。
	 * 适用于 CONNECTED 帧。
	 */
	@Nullable
	public String getServer() {
		return getFirst(SERVER);
	}

	/**
	 * 设置 destination 头。
	 */
	public void setDestination(@Nullable String destination) {
		set(DESTINATION, destination);
	}

	/**
	 * 获取 destination 头。
	 * 适用于 SEND、SUBSCRIBE 和 MESSAGE 帧。
	 */
	@Nullable
	public String getDestination() {
		return getFirst(DESTINATION);
	}

	/**
	 * 设置 id 头。
	 * 适用于 SUBSCR0BE、UNSUBSCRIBE 以及 ACK 或 NACK 帧。
	 */
	public void setId(@Nullable String id) {
		set(ID, id);
	}

	/**
	 * 获取 id 头。
	 */
	@Nullable
	public String getId() {
		return getFirst(ID);
	}

	/**
	 * 将 ack 头设置为 "auto"、"client" 或 "client-individual" 之一。
	 * 适用于 SUBSCRIBE 和 MESSAGE 帧。
	 */
	public void setAck(@Nullable String ack) {
		set(ACK, ack);
	}

	/**
	 * 获取 ack 头。
	 */
	@Nullable
	public String getAck() {
		return getFirst(ACK);
	}

	/**
	 * 设置 login 头。
	 * 适用于 MESSAGE 帧。
	 */
	public void setSubscription(@Nullable String subscription) {
		set(SUBSCRIPTION, subscription);
	}

	/**
	 * 获取 subscription 头。
	 */
	@Nullable
	public String getSubscription() {
		return getFirst(SUBSCRIPTION);
	}

	/**
	 * 设置 message-id 头。
	 * 适用于 MESSAGE 帧。
	 */
	public void setMessageId(@Nullable String messageId) {
		set(MESSAGE_ID, messageId);
	}

	/**
	 * 获取 message-id 头。
	 */
	@Nullable
	public String getMessageId() {
		return getFirst(MESSAGE_ID);
	}

	/**
	 * 设置 receipt-id 头。
	 * 适用于 RECEIPT 帧。
	 */
	public void setReceiptId(@Nullable String receiptId) {
		set(RECEIPT_ID, receiptId);
	}

	/**
	 * 获取 receipt 头。
	 */
	@Nullable
	public String getReceiptId() {
		return getFirst(RECEIPT_ID);
	}

	/**
	 * 返回给定头名称的第一个头值（如果有）。
	 * @param headerName the header name
	 * @return the first header value, or {@code null} if none
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		List<String> headerValues = this.headers.get(headerName);
		return headerValues != null ? headerValues.get(0) : null;
	}

	/**
	 * 在给定名称下添加给定的单个头值。
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		List<String> headerValues = this.headers.computeIfAbsent(headerName, k -> new ArrayList<>(1));
		headerValues.add(headerValue);
	}

	@Override
	public void addAll(String headerName, List<? extends String> headerValues) {
		List<String> currentValues = this.headers.computeIfAbsent(headerName, k -> new ArrayList<>(1));
		currentValues.addAll(headerValues);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	/**
	 * 在给定名称下设置给定的单个头值。
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		List<String> headerValues = new ArrayList<>(1);
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
		this.headers.forEach((key, value) -> singleValueMap.put(key, value.get(0)));
		return singleValueMap;
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof StompHeaders &&
				this.headers.equals(((StompHeaders) other).headers)));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}


	/**
	 * 返回只能读取、不能写入的 {@code StompHeaders} 对象。
	 */
	public static StompHeaders readOnlyStompHeaders(@Nullable Map<String, List<String>> headers) {
		return new StompHeaders((headers != null ? headers : Collections.emptyMap()), true);
	}

}
