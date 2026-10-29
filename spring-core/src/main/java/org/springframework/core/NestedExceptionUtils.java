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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 用于实现能够持有嵌套异常的异常类的辅助类。之所以必要，是因为我们无法在不同异常类型之间共享一个基
 * 类。
 *
 * <p>主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see NestedRuntimeException
 * @see NestedCheckedException
 * @see org.springframework.web.util.NestedServletException
 */
public abstract class NestedExceptionUtils {

	/**
	 * 为给定的基础消息和根因构建一条消息。
	 * @param message the base message
	 * @param cause the root cause
	 * @return the full exception message
	 */
	@Nullable
	public static String buildMessage(@Nullable String message, @Nullable Throwable cause) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("nested exception is ").append(cause);
		return sb.toString();
	}

	/**
	 * 获取给定异常的最内层原因（如果有）。
	 * @param original the original exception to introspect
	 * @return the innermost exception, or {@code null} if none
	 * @since 4.3.9
	 */
	@Nullable
	public static Throwable getRootCause(@Nullable Throwable original) {
		if (original == null) {
			return null;
		}
		Throwable rootCause = null;
		Throwable cause = original.getCause();
		while (cause != null && cause != rootCause) {
			rootCause = cause;
			cause = cause.getCause();
		}
		return rootCause;
	}

	/**
	 * 获取给定异常最具体的原因，即最内层原因（根因）或该异常本身。
	 *
	 * <p>与 {@link #getRootCause} 的不同之处在于：如果没有根因，则回退到原始异常。
	 * @param original the original exception to introspect
	 * @return the most specific cause (never {@code null})
	 * @since 4.3.9
	 */
	public static Throwable getMostSpecificCause(Throwable original) {
		Throwable rootCause = getRootCause(original);
		return (rootCause != null ? rootCause : original);
	}

}
