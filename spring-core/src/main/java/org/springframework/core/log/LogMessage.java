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

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 Commons Logging 的简单日志消息类型，允许在
 * 其 {@link #toString()} 中便捷地延迟解析给定的 {@link Supplier} 实例
 * （通常绑定到 lambda 表达式）或 printf 风格的格式字符串（{@link String#format}）。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.2
 * @see #of(Supplier)
 * @see #format(String, Object)
 * @see #format(String, Object...)
 * @see org.apache.commons.logging.Log#fatal(Object)
 * @see org.apache.commons.logging.Log#error(Object)
 * @see org.apache.commons.logging.Log#warn(Object)
 * @see org.apache.commons.logging.Log#info(Object)
 * @see org.apache.commons.logging.Log#debug(Object)
 * @see org.apache.commons.logging.Log#trace(Object)
 */
public abstract class LogMessage implements CharSequence {

	@Nullable
	private String result;


	@Override
	public int length() {
		return toString().length();
	}

	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	/**
	 * 这将由日志记录提供程序调用，可能对每个
	 * 日志目标调用一次（因此在此处本地缓存结果）。
	 */
	@Override
	public String toString() {
		if (this.result == null) {
			this.result = buildString();
		}
		return this.result;
	}

	abstract String buildString();


	/**
	 * 根据给定的 supplier 构建一条延迟解析的消息。
	 * @param supplier the supplier (typically bound to a lambda expression)
	 * @see #toString()
	 */
	public static LogMessage of(Supplier<? extends CharSequence> supplier) {
		return new SupplierMessage(supplier);
	}

	/**
	 * 根据给定的格式字符串和参数构建一条延迟格式化的消息。
	 * @param format the format string (following {@link String#format} rules)
	 * @param arg1 the argument (can be {@code null})
	 * @see String#format(String, Object...)
	 */
	public static LogMessage format(String format, @Nullable Object arg1) {
		return new FormatMessage1(format, arg1);
	}

	/**
	 * 根据给定的格式字符串和参数构建一条延迟格式化的消息。
	 * @param format the format string (following {@link String#format} rules)
	 * @param arg1 the first argument (can be {@code null})
	 * @param arg2 the second argument (can be {@code null})
	 * @see String#format(String, Object...)
	 */
	public static LogMessage format(String format, @Nullable Object arg1, @Nullable Object arg2) {
		return new FormatMessage2(format, arg1, arg2);
	}

	/**
	 * 根据给定的格式字符串和参数构建一条延迟格式化的消息。
	 * @param format the format string (following {@link String#format} rules)
	 * @param arg1 the first argument (can be {@code null})
	 * @param arg2 the second argument (can be {@code null})
	 * @param arg3 the third argument (can be {@code null})
	 * @see String#format(String, Object...)
	 */
	public static LogMessage format(String format, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3) {
		return new FormatMessage3(format, arg1, arg2, arg3);
	}

	/**
	 * 根据给定的格式字符串和参数构建一条延迟格式化的消息。
	 * @param format the format string (following {@link String#format} rules)
	 * @param arg1 the first argument (can be {@code null})
	 * @param arg2 the second argument (can be {@code null})
	 * @param arg3 the third argument (can be {@code null})
	 * @param arg4 the fourth argument (can be {@code null})
	 * @see String#format(String, Object...)
	 */
	public static LogMessage format(String format, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3,
			@Nullable Object arg4) {
		return new FormatMessage4(format, arg1, arg2, arg3, arg4);
	}

	/**
	 * 根据给定的格式字符串和可变参数构建一条延迟格式化的消息。
	 * <p>这个可变参数形式的 {@code format()} 可能开销较大。因此，
	 * 应尽可能使用单独的参数变体：
	 * {@link #format(String, Object)}、{@link #format(String, Object, Object)} 等。
	 * @param format the format string (following {@link String#format} rules)
	 * @param args the varargs array (can be {@code null} and can contain {@code null}
	 * elements)
	 * @see String#format(String, Object...)
	 */
	public static LogMessage format(String format, @Nullable Object... args) {
		return new FormatMessageX(format, args);
	}


	private static final class SupplierMessage extends LogMessage {

		private final Supplier<? extends CharSequence> supplier;

		SupplierMessage(Supplier<? extends CharSequence> supplier) {
			Assert.notNull(supplier, "Supplier must not be null");
			this.supplier = supplier;
		}

		@Override
		String buildString() {
			return this.supplier.get().toString();
		}
	}


	private static abstract class FormatMessage extends LogMessage {

		protected final String format;

		FormatMessage(String format) {
			Assert.notNull(format, "Format must not be null");
			this.format = format;
		}
	}


	private static final class FormatMessage1 extends FormatMessage {

		@Nullable
		private final Object arg1;

		FormatMessage1(String format, @Nullable Object arg1) {
			super(format);
			this.arg1 = arg1;
		}

		@Override
		protected String buildString() {
			return String.format(this.format, this.arg1);
		}
	}


	private static final class FormatMessage2 extends FormatMessage {

		@Nullable
		private final Object arg1;

		@Nullable
		private final Object arg2;

		FormatMessage2(String format, @Nullable Object arg1, @Nullable Object arg2) {
			super(format);
			this.arg1 = arg1;
			this.arg2 = arg2;
		}

		@Override
		String buildString() {
			return String.format(this.format, this.arg1, this.arg2);
		}
	}


	private static final class FormatMessage3 extends FormatMessage {

		@Nullable
		private final Object arg1;

		@Nullable
		private final Object arg2;

		@Nullable
		private final Object arg3;

		FormatMessage3(String format, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3) {
			super(format);
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.arg3 = arg3;
		}

		@Override
		String buildString() {
			return String.format(this.format, this.arg1, this.arg2, this.arg3);
		}
	}


	private static final class FormatMessage4 extends FormatMessage {

		@Nullable
		private final Object arg1;

		@Nullable
		private final Object arg2;

		@Nullable
		private final Object arg3;

		@Nullable
		private final Object arg4;

		FormatMessage4(String format, @Nullable Object arg1, @Nullable Object arg2, @Nullable Object arg3,
				@Nullable Object arg4) {
			super(format);
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.arg3 = arg3;
			this.arg4 = arg4;
		}

		@Override
		String buildString() {
			return String.format(this.format, this.arg1, this.arg2, this.arg3, this.arg4);
		}
	}


	private static final class FormatMessageX extends FormatMessage {

		@Nullable
		private final Object[] args;

		FormatMessageX(String format, @Nullable Object... args) {
			super(format);
			this.args = args;
		}

		@Override
		String buildString() {
			return String.format(this.format, this.args);
		}
	}

}
