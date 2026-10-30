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

package org.springframework.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code ExceptionCollector} 是一个测试实用工具，用于执行代码块、收集异常，并生成单个 {@link AssertionError}，其中包含遇到的所有异常，这些异常作为{@linkplain Throwable#getSuppressed() 抑制异常}。
 *
 * <p>此实用工具旨在支持<em>软断言</em>用例，类似于 AssertJ 中的 {@code SoftAssertions} 支持和 JUnit Jupiter 中的 {@code assertAll()} 支持。
 *
 * @author Sam Brannen
 * @since 5.3.10
 */
public class ExceptionCollector {

	private final List<Throwable> exceptions = new ArrayList<>();


	/**
	 * 执行所提供的 {@link Executable} 并跟踪抛出的任何异常。
	 * @param executable 要执行的 {@code Executable}
	 * @see #getExceptions()
	 * @see #assertEmpty()
	 */
	public void execute(Executable executable) {
		try {
			executable.execute();
		}
		catch (Throwable ex) {
			this.exceptions.add(ex);
		}
	}

	/**
	 * 获取在 {@link #execute(Executable)} 中遇到的异常列表。
	 * @return 异常列表的不可修改副本，可能为空
	 * @see #assertEmpty()
	 */
	public List<Throwable> getExceptions() {
		return Collections.unmodifiableList(this.exceptions);
	}

	/**
	 * 断言此 {@code ExceptionCollector} 不包含任何{@linkplain #getExceptions() 异常}。
	 * <p>如果此收集器为空，则此方法实际上是无操作。
	 * <p>如果此收集器包含单个 {@link Error} 或 {@link Exception}，则此方法会重新抛出该错误或异常。
	 * <p>如果此收集器包含单个 {@link Throwable}，则此方法会抛出一个 {@link AssertionError}，其错误消息为该 {@code Throwable} 的错误消息，且该 {@code Throwable} 作为{@linkplain Throwable#getCause() 原因}。
	 * <p>如果此收集器包含多个异常，则此方法会抛出一个 {@code AssertionError}，其消息为 &quot;Multiple Exceptions (#):&quot;，后跟一个换行符以及由换行符分隔的每个异常的错误消息，其中 {@code #} 替换为存在的异常数量。此外，每个异常都将作为{@link Throwable#addSuppressed(Throwable) 抑制异常}添加到该 {@code AssertionError} 中。
	 * @see #execute(Executable)
	 * @see #getExceptions()
	 */
	public void assertEmpty() throws Exception {
		if (this.exceptions.isEmpty()) {
			return;
		}

		if (this.exceptions.size() == 1) {
			Throwable exception = this.exceptions.get(0);
			if (exception instanceof Error) {
				throw (Error) exception;
			}
			if (exception instanceof Exception) {
				throw (Exception) exception;
			}
			AssertionError assertionError = new AssertionError(exception.getMessage());
			assertionError.initCause(exception);
			throw assertionError;
		}

		StringBuilder message = new StringBuilder();
		message.append("Multiple Exceptions (").append(this.exceptions.size()).append("):");
		for (Throwable exception : this.exceptions) {
			message.append('\n');
			message.append(exception.getMessage());
		}
		AssertionError assertionError = new AssertionError(message);
		this.exceptions.forEach(assertionError::addSuppressed);
		throw assertionError;
	}


	/**
	 * {@code Executable} 是一个函数式接口，可用于实现任何可能抛出 {@link Throwable} 的通用代码块。
	 *
	 * <p>{@code Executable} 接口类似于 {@link java.lang.Runnable}，不同之处在于 {@code Executable} 可以抛出任何类型的异常。
	 */
	@FunctionalInterface
	public interface Executable {

		void execute() throws Throwable;

	}

}
