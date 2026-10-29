/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.ServletException;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;

/**
 * {@link ServletException} 的子类，能像 NestedChecked/RuntimeException 那样
 * 正确处理根因的消息和堆栈跟踪。
 *
 * <p>请注意，普通 ServletException 完全不公开其根因，无论是在异常消息中
 * 还是在打印的堆栈跟踪中！虽然在后续的 Servlet API 变体中可能会修复这一点
 * （同一 API 版本在不同供应商之间甚至也有差异），但在 Servlet 2.4
 * （Spring 3.x 要求的最低版本）上无法可靠获得该信息，因此我们需要自行处理。
 *
 * <p>此类与 NestedChecked/RuntimeException 类之间的相似性不可避免，
 * 因为此类必须派生自 ServletException。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #getMessage
 * @see #printStackTrace
 * @see org.springframework.core.NestedCheckedException
 * @see org.springframework.core.NestedRuntimeException
 */
public class NestedServletException extends ServletException {

	/** 使用 Spring 1.2 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = -5292377985529381145L;

	static {
		// Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
		// issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造一个 {@code NestedServletException}。
	 * @param msg the detail message
	 */
	public NestedServletException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造一个 {@code NestedServletException}。
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public NestedServletException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息；如果存在嵌套异常，则包含其消息。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
