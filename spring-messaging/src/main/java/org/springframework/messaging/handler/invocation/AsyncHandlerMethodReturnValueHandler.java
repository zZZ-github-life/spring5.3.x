/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link HandlerMethodReturnValueHandler} 的扩展，用于处理支持成功和错误回调的
 * 异步、类似 Future 的返回值类型。基本上任何可以适配为 {@link ListenableFuture}
 * 的类型都可以。
 *
 * <p>实现应考虑继承便捷基类 {@link AbstractAsyncReturnValueHandler}。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see AbstractAsyncReturnValueHandler
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * 返回值是否表示具有成功和错误回调的异步、类似 Future 的类型。
	 * 如果此方法返回 {@code true}，则接下来会调用 {@link #toListenableFuture}。
	 * 如果返回 {@code false}，则调用 {@link #handleReturnValue}。
	 * <p><strong>注意：</strong>只有在调用
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * 并返回 {@code true} 后，才会调用此方法。
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型
	 * @return 如果返回值类型表示异步值则返回 {@code true}
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

	/**
	 * 将异步返回值适配为 {@link ListenableFuture}。
	 * 实现应考虑返回
	 * {@link org.springframework.util.concurrent.SettableListenableFuture
	 * SettableListenableFuture} 实例。随后，当 ListenableFuture 以成功或错误完成时，
	 * 返回值处理将继续进行。
	 * <p><strong>注意：</strong>只有在调用
	 * {@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * 并返回 {@code true} 后，才会调用此方法。
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型
	 * @return 生成的 ListenableFuture；如果为 {@code null}，则不执行进一步处理
	 */
	@Nullable
	ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType);

}
