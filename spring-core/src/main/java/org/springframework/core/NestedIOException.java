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

import java.io.IOException;

import org.springframework.lang.Nullable;

/**
 * {@link IOException} 的子类，能够正确处理根因，像 NestedChecked/RuntimeException 那样暴露根因。
 *
 * <p>Java 6 为标准 {@code IOException} 添加了正确的根因处理，这也是 Spring 最初引入
 * {@code NestedIOException} 以兼容 Java 6 之前版本的原因。
 *
 * <p>此类与 NestedChecked/RuntimeException 类之间的相似性不可避免，因为此类需要派生自 IOException
 * 。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getMessage
 * @see #printStackTrace
 * @see org.springframework.core.NestedCheckedException
 * @see org.springframework.core.NestedRuntimeException
 * @deprecated as of 5.3.23, in favor of using {@link IOException} directly
 */
@Deprecated
@SuppressWarnings("serial")
public class NestedIOException extends IOException {

	static {
		// Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
		// issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造一个 {@code NestedIOException}。
	 * @param msg the detail message
	 */
	public NestedIOException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造一个 {@code NestedIOException}。
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public NestedIOException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息，其中包括来自嵌套异常的消息（如果有）。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
