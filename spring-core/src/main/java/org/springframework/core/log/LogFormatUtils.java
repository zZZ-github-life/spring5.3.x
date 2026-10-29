/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.log;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于格式化和记录消息的工具方法。
 *
 * <p>主要供框架内部与 Apache Commons Logging 配合使用，
 * 通常以 {@code spring-jcl} 桥接的形式出现，但也兼容
 * 其他 Commons Logging 桥接。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 */
public abstract class LogFormatUtils {

	private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\n\r]");

	private static final Pattern CONTROL_CHARACTER_PATTERN = Pattern.compile("\\p{Cc}");


	/**
	 * {@link #formatValue(Object, int, boolean)} 的便捷变体，
	 * 它将日志消息的长度限制为 100 个字符，并且在
	 * {@code limitLength} 设置为 "true" 时替换换行符和控制字符。
	 * @param value the value to format
	 * @param limitLength whether to truncate the value at a length of 100
	 * @return the formatted value
	 */
	public static String formatValue(@Nullable Object value, boolean limitLength) {
		return formatValue(value, (limitLength ? 100 : -1), limitLength);
	}

	/**
	 * 通过 {@code toString()} 格式化给定值，如果它是
	 * {@link CharSequence} 则为其加引号，截断到指定的 {@code maxLength}，
	 * 并在设置了 {@code replaceNewLines} 时将其压缩为单行。
	 * @param value the value to be formatted
	 * @param maxLength the max length, after which to truncate, or -1 for unlimited
	 * @param replaceNewlinesAndControlCharacters whether to replace newline and
	 * control characters with placeholders
	 * @return the formatted value
	 */
	public static String formatValue(
			@Nullable Object value, int maxLength, boolean replaceNewlinesAndControlCharacters) {

		if (value == null) {
			return "";
		}
		String result;
		try {
			result = ObjectUtils.nullSafeToString(value);
		}
		catch (Throwable ex) {
			result = ObjectUtils.nullSafeToString(ex);
		}
		if (maxLength != -1) {
			result = StringUtils.truncate(result, maxLength);
		}
		if (replaceNewlinesAndControlCharacters) {
			result = NEWLINE_PATTERN.matcher(result).replaceAll("<EOL>");
			result = CONTROL_CHARACTER_PATTERN.matcher(result).replaceAll("?");
		}
		if (value instanceof CharSequence) {
			result = "\"" + result + "\"";
		}
		return result;
	}

	/**
	 * 用于在 TRACE 与 DEBUG 日志级别下记录不同详细程度（或不同
	 * 消息）的日志。实际上，它是以下内容的替代：
	 * <pre class="code">
	 * if (logger.isDebugEnabled()) {
	 *   String str = logger.isTraceEnabled() ? "..." : "...";
	 *   if (logger.isTraceEnabled()) {
	 *     logger.trace(str);
	 *   }
	 *   else {
	 *     logger.debug(str);
	 *   }
	 * }
	 * </pre>
	 * @param logger the logger to use to log the message
	 * @param messageFactory function that accepts a boolean set to the value
	 * of {@link Log#isTraceEnabled()}
	 */
	public static void traceDebug(Log logger, Function<Boolean, String> messageFactory) {
		if (logger.isDebugEnabled()) {
			boolean traceEnabled = logger.isTraceEnabled();
			String logMessage = messageFactory.apply(traceEnabled);
			if (traceEnabled) {
				logger.trace(logMessage);
			}
			else {
				logger.debug(logMessage);
			}
		}
	}

}
