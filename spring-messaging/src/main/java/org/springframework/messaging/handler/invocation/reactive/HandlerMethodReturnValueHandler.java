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
	 * @param returnType the method return type to check
	 * @return {@code true} if this handler supports the supplied return type;
	 * {@code false} otherwise
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * 处理给定返回值。
	 * @param returnValue the value returned from the handler method
	 * @param returnType the type of the return value. This type must have previously
	 * been passed to {@link #supportsReturnType(MethodParameter)}
	 * and it must have returned {@code true}.
	 * @return {@code Mono<Void>} to indicate when handling is complete.
	 */
	Mono<Void> handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message);

}
