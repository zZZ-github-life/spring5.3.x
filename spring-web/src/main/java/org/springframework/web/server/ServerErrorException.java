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

package org.springframework.web.server;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * 针对 {@link HttpStatus#INTERNAL_SERVER_ERROR} 的异常，
 * 会额外暴露关于失败的控制器方法或无法解析的控制器方法参数的信息。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class ServerErrorException extends ResponseStatusException {

	@Nullable
	private final Method handlerMethod;

	@Nullable
	private final MethodParameter parameter;


	/**
	 * 带有原因和可选起因的 500 错误构造函数。
	 * @since 5.0.5
	 */
	public ServerErrorException(String reason, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = null;
		this.parameter = null;
	}

	/**
	 * 带有处理器 {@link Method} 和可选起因的 500 错误构造函数。
	 * @since 5.0.5
	 */
	public ServerErrorException(String reason, Method handlerMethod, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = handlerMethod;
		this.parameter = null;
	}

	/**
	 * 带有 {@link MethodParameter} 和可选起因的 500 错误构造函数。
	 */
	public ServerErrorException(String reason, MethodParameter parameter, @Nullable Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
		this.handlerMethod = parameter.getMethod();
		this.parameter = parameter;
	}

	/**
	 * 与特定 {@code MethodParameter} 关联的 500 错误构造函数。
	 * @deprecated in favor of {@link #ServerErrorException(String, MethodParameter, Throwable)}
	 */
	@Deprecated
	public ServerErrorException(String reason, MethodParameter parameter) {
		this(reason, parameter, null);
	}

	/**
	 * 仅带有原因的 500 错误构造函数。
	 * @deprecated in favor of {@link #ServerErrorException(String, Throwable)}
	 */
	@Deprecated
	public ServerErrorException(String reason) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, null);
		this.handlerMethod = null;
		this.parameter = null;
	}


	/**
	 * 返回与此错误关联的处理器方法（如果有）。
	 * @since 5.0.5
	 */
	@Nullable
	public Method getHandlerMethod() {
		return this.handlerMethod;
	}

	/**
	 * 返回与此错误关联的特定方法参数（如果有）。
	 */
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.parameter;
	}

}
