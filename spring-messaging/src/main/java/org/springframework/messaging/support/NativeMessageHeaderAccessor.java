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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link MessageHeaderAccessor} 的子类，支持存储和访问来自外部来源（例如消息代理）
 * 的头部。来自外部来源的头部与其他头部分开保存，位于
 * {@link #NATIVE_HEADERS} 键下的子映射中。这样可以将处理头部与需要发送到
 * 外部来源或从外部来源接收的头部区分开来。
 *
 * <p>此类通常通过特定协议的子类间接使用，这些子类还提供用于在消息头部与
 * 外部消息源之间相互转换的工厂方法。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class NativeMessageHeaderAccessor extends MessageHeaderAccessor {

	/** 用于存储原生头部的头部名称。 */
	public static final String NATIVE_HEADERS = "nativeHeaders";


	/**
	 * 用于创建新实例的受保护构造函数。
	 */
	protected NativeMessageHeaderAccessor() {
		this((Map<String, List<String>>) null);
	}

	/**
	 * 使用给定原生头部创建实例的受保护构造函数。
	 * @param nativeHeaders native headers to create the message with (may be {@code null})
	 */
	protected NativeMessageHeaderAccessor(@Nullable Map<String, List<String>> nativeHeaders) {
		if (!CollectionUtils.isEmpty(nativeHeaders)) {
			setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<>(nativeHeaders));
		}
	}

	/**
	 * 从另一条消息复制头部的受保护构造函数。
	 */
	protected NativeMessageHeaderAccessor(@Nullable Message<?> message) {
		super(message);
		if (message != null) {
			@SuppressWarnings("unchecked")
			Map<String, List<String>> map = (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
			if (map != null) {
				// setHeader checks for equality but we need copy of native headers
				setHeader(NATIVE_HEADERS, null);
				setHeader(NATIVE_HEADERS, new LinkedMultiValueMap<>(map));
			}
		}
	}


	/**
	 * 子类可以使用此方法访问“原生”头部子映射。
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected Map<String, List<String>> getNativeHeaders() {
		return (Map<String, List<String>>) getHeader(NATIVE_HEADERS);
	}

	/**
	 * 返回原生头部子映射的副本，或空映射。
	 */
	public Map<String, List<String>> toNativeHeaderMap() {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? new LinkedMultiValueMap<>(map) : Collections.emptyMap());
	}

	@Override
	public void setImmutable() {
		if (isMutable()) {
			Map<String, List<String>> map = getNativeHeaders();
			if (map != null) {
				// setHeader checks for equality but we need immutable wrapper
				setHeader(NATIVE_HEADERS, null);
				setHeader(NATIVE_HEADERS, Collections.unmodifiableMap(map));
			}
			super.setImmutable();
		}
	}

	@Override
	public void copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		Map<String, List<String>> map = (Map<String, List<String>>) headersToCopy.get(NATIVE_HEADERS);
		if (map != null && map != getNativeHeaders()) {
			map.forEach(this::setNativeHeaderValues);
		}

		// setHeader checks for equality, native headers should be equal by now
		super.copyHeaders(headersToCopy);
	}

	@Override
	public void copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		Map<String, List<String>> map = (Map<String, List<String>>) headersToCopy.get(NATIVE_HEADERS);
		if (map != null && getNativeHeaders() == null) {
			map.forEach(this::setNativeHeaderValues);
		}

		super.copyHeadersIfAbsent(headersToCopy);
	}

	/**
	 * 原生头部映射是否包含给定头部名称。
	 * @param headerName the name of the header
	 */
	public boolean containsNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null && map.containsKey(headerName));
	}

	/**
	 * 返回指定原生头部的所有值（如果存在）。
	 * @param headerName the name of the header
	 * @return the associated values, or {@code null} if none
	 */
	@Nullable
	public List<String> getNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		return (map != null ? map.get(headerName) : null);
	}

	/**
	 * 返回指定原生头部的第一个值（如果存在）。
	 * @param headerName the name of the header
	 * @return the associated value, or {@code null} if none
	 */
	@Nullable
	public String getFirstNativeHeader(String headerName) {
		Map<String, List<String>> map = getNativeHeaders();
		if (map != null) {
			List<String> values = map.get(headerName);
			if (!CollectionUtils.isEmpty(values)) {
				return values.get(0);
			}
		}
		return null;
	}

	/**
	 * 设置指定的原生头部值，替换现有值。
	 * <p>为此，访问器必须是 {@link #isMutable() mutable} 的。
	 * 详情请参阅 {@link MessageHeaderAccessor}。
	 */
	public void setNativeHeader(String name, @Nullable String value) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> map = getNativeHeaders();
		if (value == null) {
			if (map != null && map.get(name) != null) {
				setModified(true);
				map.remove(name);
			}
			return;
		}
		if (map == null) {
			map = new LinkedMultiValueMap<>(3);
			setHeader(NATIVE_HEADERS, map);
		}
		List<String> values = new ArrayList<>(1);
		values.add(value);
		if (!ObjectUtils.nullSafeEquals(values, getHeader(name))) {
			setModified(true);
			map.put(name, values);
		}
	}

	/**
	 * {@link #addNativeHeader(String, String)} 的变体，适用于所有值。
	 * @since 5.2.12
	 */
	public void setNativeHeaderValues(String name, @Nullable List<String> values) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> map = getNativeHeaders();
		if (values == null) {
			if (map != null && map.get(name) != null) {
				setModified(true);
				map.remove(name);
			}
			return;
		}
		if (map == null) {
			map = new LinkedMultiValueMap<>(3);
			setHeader(NATIVE_HEADERS, map);
		}
		if (!ObjectUtils.nullSafeEquals(values, getHeader(name))) {
			setModified(true);
			map.put(name, new ArrayList<>(values));
		}
	}

	/**
	 * 将指定的原生头部值添加到现有值中。
	 * <p>为此，访问器必须是 {@link #isMutable() mutable} 的。
	 * 详情请参阅 {@link MessageHeaderAccessor}。
	 * @param name the name of the header
	 * @param value the header value to set
	 */
	public void addNativeHeader(String name, @Nullable String value) {
		Assert.state(isMutable(), "Already immutable");
		if (value == null) {
			return;
		}
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (nativeHeaders == null) {
			nativeHeaders = new LinkedMultiValueMap<>(3);
			setHeader(NATIVE_HEADERS, nativeHeaders);
		}
		List<String> values = nativeHeaders.computeIfAbsent(name, k -> new ArrayList<>(1));
		values.add(value);
		setModified(true);
	}

	/**
	 * 将指定的原生头部添加到现有值中。
	 * @param headers the headers to set
	 */
	public void addNativeHeaders(@Nullable MultiValueMap<String, String> headers) {
		if (headers == null) {
			return;
		}
		headers.forEach((key, values) -> values.forEach(value -> addNativeHeader(key, value)));
	}

	/**
	 * 移除指定的原生头部值，替换现有值。
	 * <p>为此，访问器必须是 {@link #isMutable() mutable} 的。
	 * 详情请参阅 {@link MessageHeaderAccessor}。
	 * @param headerName the name of the header
	 * @return the associated values, or {@code null} if the header was not present
	 */
	@Nullable
	public List<String> removeNativeHeader(String headerName) {
		Assert.state(isMutable(), "Already immutable");
		Map<String, List<String>> nativeHeaders = getNativeHeaders();
		if (CollectionUtils.isEmpty(nativeHeaders)) {
			return null;
		}
		return nativeHeaders.remove(headerName);
	}


	/**
	 * 返回指定原生头部的第一个值，如果没有则返回 {@code null}。
	 * @param headerName the name of the header
	 * @param headers the headers map to introspect
	 * @return the associated value, or {@code null} if none
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static String getFirstNativeHeader(String headerName, Map<String, Object> headers) {
		Map<String, List<String>> map = (Map<String, List<String>>) headers.get(NATIVE_HEADERS);
		if (map != null) {
			List<String> values = map.get(headerName);
			if (!CollectionUtils.isEmpty(values)) {
				return values.get(0);
			}
		}
		return null;
	}

}
