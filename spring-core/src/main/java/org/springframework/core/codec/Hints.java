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

package org.springframework.core.codec;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 用于处理提示的常量和便捷方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see ResourceRegionEncoder#BOUNDARY_STRING_HINT
 */
public abstract class Hints {

	/**
	 * 提示的名称，用于暴露关联日志消息时所使用的前缀。
	 */
	public static final String LOG_PREFIX_HINT = Log.class.getName() + ".PREFIX";

	/**
	 * 布尔提示的名称，用于指示是否避免记录数据，原因可能是数据具有潜在敏感性，或者数据已由复合编码器记
	 * 录过，例如对于 multipart 请求。
	 */
	public static final String SUPPRESS_LOGGING_HINT = Log.class.getName() + ".SUPPRESS_LOGGING";


	/**
	 * 通过 {@link Collections#singletonMap} 创建仅包含单个提示的映射。
	 * @param hintName the hint name
	 * @param value the hint value
	 * @return the created map
	 */
	public static Map<String, Object> from(String hintName, Object value) {
		return Collections.singletonMap(hintName, value);
	}

	/**
	 * 通过 {@link Collections#emptyMap()} 返回空的提示映射。
	 * @return the empty map
	 */
	public static Map<String, Object> none() {
		return Collections.emptyMap();
	}

	/**
	 * 获取必需提示的值。
	 * @param hints the hints map
	 * @param hintName the required hint name
	 * @param <T> the hint type to cast to
	 * @return the hint value
	 * @throws IllegalArgumentException if the hint is not found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequiredHint(@Nullable Map<String, Object> hints, String hintName) {
		if (hints == null) {
			throw new IllegalArgumentException("No hints map for required hint '" + hintName + "'");
		}
		T hint = (T) hints.get(hintName);
		if (hint == null) {
			throw new IllegalArgumentException("Hints map must contain the hint '" + hintName + "'");
		}
		return hint;
	}

	/**
	 * 获取提示 {@link #LOG_PREFIX_HINT}（如果存在），否则返回空 String。
	 * @param hints the hints passed to the encode method
	 * @return the log prefix
	 */
	public static String getLogPrefix(@Nullable Map<String, Object> hints) {
		return (hints != null ? (String) hints.getOrDefault(LOG_PREFIX_HINT, "") : "");
	}

	/**
	 * 是否根据提示 {@link #SUPPRESS_LOGGING_HINT} 抑制日志记录。
	 * @param hints the hints map
	 * @return whether logging of data is allowed
	 */
	public static boolean isLoggingSuppressed(@Nullable Map<String, Object> hints) {
		return (hints != null && (boolean) hints.getOrDefault(SUPPRESS_LOGGING_HINT, false));
	}

	/**
	 * 合并两个提示映射：如果两者都有值，则创建新映射并将它们复制进去；如果只有一个非空，则返回该非空映
	 * 射；如果两者都为空，则返回空映射。
	 * @param hints1 1st map of hints
	 * @param hints2 2nd map of hints
	 * @return a single map with hints from both
	 */
	public static Map<String, Object> merge(Map<String, Object> hints1, Map<String, Object> hints2) {
		if (hints1.isEmpty() && hints2.isEmpty()) {
			return Collections.emptyMap();
		}
		else if (hints2.isEmpty()) {
			return hints1;
		}
		else if (hints1.isEmpty()) {
			return hints2;
		}
		else {
			Map<String, Object> result = CollectionUtils.newHashMap(hints1.size() + hints2.size());
			result.putAll(hints1);
			result.putAll(hints2);
			return result;
		}
	}

	/**
	 * 将单个提示合并到提示映射中：可能需要创建新映射并将所有提示复制进去，或者如果提示映射为空，则创建
	 * 一个仅含单个条目的新映射。
	 * @param hints a map of hints to be merged
	 * @param hintName the hint name to merge
	 * @param hintValue the hint value to merge
	 * @return a single map with all hints
	 */
	public static Map<String, Object> merge(Map<String, Object> hints, String hintName, Object hintValue) {
		if (hints.isEmpty()) {
			return Collections.singletonMap(hintName, hintValue);
		}
		else {
			Map<String, Object> result = CollectionUtils.newHashMap(hints.size() + 1);
			result.putAll(hints);
			result.put(hintName, hintValue);
			return result;
		}
	}

	/**
	 * 如果提示中包含 {@link #LOG_PREFIX_HINT}，并且给定的 logger 已启用 DEBUG 级别，则通过
	 * {@link DataBufferUtils#touch(DataBuffer, Object)} 将日志前缀作为提示应用到给定缓冲区。
	 * @param buffer the buffer to touch
	 * @param hints the hints map to check for a log prefix
	 * @param logger the logger whose level to check
	 * @since 5.3.2
	 */
	public static void touchDataBuffer(DataBuffer buffer, @Nullable Map<String, Object> hints, Log logger) {
		if (logger.isDebugEnabled() && hints != null) {
			Object logPrefix = hints.get(LOG_PREFIX_HINT);
			if (logPrefix != null) {
				DataBufferUtils.touch(buffer, logPrefix);
			}
		}
	}

}
