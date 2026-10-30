/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.web.util.NestedServletException;

/**
 * 致命的绑定异常，当我们希望将绑定异常视为不可恢复时抛出。
 *
 * <p>继承 ServletException 以便在任何 Servlet 资源（如 Filter）中方便地抛出，
 * 并继承 NestedServletException 以正确处理根因
 * （因为普通的 ServletException 根本不会暴露其根因）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ServletRequestBindingException extends NestedServletException {

	/**
	 * ServletRequestBindingException 的构造函数。
	 * @param msg 详细消息
	 */
	public ServletRequestBindingException(String msg) {
		super(msg);
	}

	/**
	 * ServletRequestBindingException 的构造函数。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public ServletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
