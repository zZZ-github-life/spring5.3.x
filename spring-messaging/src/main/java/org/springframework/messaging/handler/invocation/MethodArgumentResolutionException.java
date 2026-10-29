/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 调用
 * {@link org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver}
 * 时产生的通用异常。
 *
 * @author Juergen Hoeller
 * @since 4.3.6
 * @see HandlerMethodArgumentResolver
 */
@SuppressWarnings("serial")
public class MethodArgumentResolutionException extends MessagingException {

	private final MethodParameter parameter;


	/**
	 * 创建一个提供无效 {@code MethodParameter} 的新实例。
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter) {
		super(message, getMethodParameterMessage(parameter));
		this.parameter = parameter;
	}

	/**
	 * 创建一个提供无效 {@code MethodParameter} 和预准备描述的新实例。
	 */
	public MethodArgumentResolutionException(Message<?> message, MethodParameter parameter, String description) {
		super(message, getMethodParameterMessage(parameter) + ": " + description);
		this.parameter = parameter;
	}

	/**
	 * 创建一个提供无效 {@code MethodParameter}、预准备描述和原因的新实例。
	 */
	public MethodArgumentResolutionException(
			Message<?> message, MethodParameter parameter, String description, @Nullable Throwable cause) {

		super(message, getMethodParameterMessage(parameter) + ": " + description, cause);
		this.parameter = parameter;
	}


	/**
	 * 返回被拒绝的 MethodParameter。
	 */
	public final MethodParameter getMethodParameter() {
		return this.parameter;
	}


	private static String getMethodParameterMessage(MethodParameter parameter) {
		return "Could not resolve method parameter at index " + parameter.getParameterIndex() +
				" in " + parameter.getExecutable().toGenericString();
	}

}
