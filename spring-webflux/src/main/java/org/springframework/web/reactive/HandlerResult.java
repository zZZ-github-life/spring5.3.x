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

package org.springframework.web.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;

/**
 * 表示处理器或处理器方法调用结果的对象。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HandlerResult {

	private final Object handler;

	@Nullable
	private final Object returnValue;

	private final ResolvableType returnType;

	private final BindingContext bindingContext;

	@Nullable
	private Function<Throwable, Mono<HandlerResult>> exceptionHandler;


	/**
	 * 创建一个新的 {@code HandlerResult}。
	 * @param handler the handler that handled the request
	 * @param returnValue the return value from the handler possibly {@code null}
	 * @param returnType the return value type
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType) {
		this(handler, returnValue, returnType, null);
	}

	/**
	 * 创建一个新的 {@code HandlerResult}。
	 * @param handler the handler that handled the request
	 * @param returnValue the return value from the handler possibly {@code null}
	 * @param returnType the return value type
	 * @param context the binding context used for request handling
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType,
			@Nullable BindingContext context) {

		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(returnType, "'returnType' is required");
		this.handler = handler;
		this.returnValue = returnValue;
		this.returnType = ResolvableType.forMethodParameter(returnType);
		this.bindingContext = (context != null ? context : new BindingContext());
	}


	/**
	 * 返回处理该请求的处理器。
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * 返回处理器返回的值（如果有）。
	 */
	@Nullable
	public Object getReturnValue() {
		return this.returnValue;
	}

	/**
	 * 返回处理器返回值类型，例如控制器方法签名中声明的返回类型。另请参见 {@link #getReturnTypeSource()} 以获取该返回类型对应的底层 {@link MethodParameter}。
	 */
	public ResolvableType getReturnType() {
		return this.returnType;
	}

	/**
	 * 返回创建 {@link #getReturnType() returnType} 时所使用的 {@link MethodParameter}。
	 */
	public MethodParameter getReturnTypeSource() {
		return (MethodParameter) this.returnType.getSource();
	}

	/**
	 * 返回用于请求处理的 BindingContext。
	 */
	public BindingContext getBindingContext() {
		return this.bindingContext;
	}

	/**
	 * 返回用于请求处理的模型。这是 {@code getBindingContext().getModel()} 的快捷方式。
	 */
	public Model getModel() {
		return this.bindingContext.getModel();
	}

	/**
	 * 配置异常处理器，用于在结果处理失败时生成替代结果。特别是对于异步返回值，错误可能在处理器调用之后才发生。
	 * @param function the error handler
	 * @return the current instance
	 */
	public HandlerResult setExceptionHandler(Function<Throwable, Mono<HandlerResult>> function) {
		this.exceptionHandler = function;
		return this;
	}

	/**
	 * 是否存在异常处理器。
	 */
	public boolean hasExceptionHandler() {
		return (this.exceptionHandler != null);
	}

	/**
	 * 应用异常处理器并返回替代结果。
	 * @param failure the exception
	 * @return the new result or the same error if there is no exception handler
	 */
	public Mono<HandlerResult> applyExceptionHandler(Throwable failure) {
		return (this.exceptionHandler != null ? this.exceptionHandler.apply(failure) : Mono.error(failure));
	}

}
