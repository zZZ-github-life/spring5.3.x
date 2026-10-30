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

package org.springframework.messaging.handler.invocation.reactive;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 处理调用带注解的 {@link Message} 处理方法所返回的值。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface HandlerMethodReturnValueHandler {

	/** 包含用于返回值处理的 DataBufferFactory 的头。 */
	String DATA_BUFFER_FACTORY_HEADER = "dataBufferFactory";


	/**
	 * 给定{@linkplain MethodParameter 方法返回类型}是否受此处理器支持。
	 * @param returnType 要检查的方法返回类型
	 * @return 如果该处理器支持所提供的返回类型则返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * 处理给定返回值。
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型。该类型必须事先传递给 {@link #supportsReturnType(MethodParameter)}
	 * 且该方法必须返回 {@code true}。
	 * @return 用于指示处理何时完成的 {@code Mono<Void>}。
	 */
	Mono<Void> handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message);

}
