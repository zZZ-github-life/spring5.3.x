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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 策略接口，用于处理调用处理器方法后返回的值。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see HandlerMethodArgumentResolver
 */
public interface HandlerMethodReturnValueHandler {

	/**
	 * 此处理器是否支持给定的{@linkplain MethodParameter 方法返回类型}。
	 * @param returnType 要检查的方法返回类型
	 * @return 如果此处理器支持提供的返回类型则返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * 通过向模型中添加属性并设置视图，或将
	 * {@link ModelAndViewContainer#setRequestHandled} 标志设为 {@code true}
	 * 来表示响应已被直接处理，从而处理给定的返回值。
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型。该类型之前必须已传递给
	 * {@link #supportsReturnType}，且其必须返回 {@code true}。
	 * @param mavContainer 当前请求的 ModelAndViewContainer
	 * @param webRequest 当前请求
	 * @throws Exception 如果返回值处理出现错误
	 */
	void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;

}
