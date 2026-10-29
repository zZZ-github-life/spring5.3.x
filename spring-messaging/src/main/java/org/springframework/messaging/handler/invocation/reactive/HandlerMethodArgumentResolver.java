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

package org.springframework.messaging.handler.invocation.reactive;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * 在给定 {@link Message} 的上下文中将方法参数解析为参数值的策略接口。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 给定{@linkplain MethodParameter 方法参数}是否受此解析器支持。
	 * @param parameter the method parameter to check
	 * @return {@code true} if this resolver supports the supplied parameter;
	 * {@code false} otherwise
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 从给定消息中将方法参数解析为参数值。
	 * @param parameter the method parameter to resolve.
	 * This parameter must have previously been passed to
	 * {@link #supportsParameter(org.springframework.core.MethodParameter)}
	 * which must have returned {@code true}.
	 * @param message the currently processed message
	 * @return {@code Mono} for the argument value, possibly empty
	 */
	Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message);

}
