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
	 * @param parameter 要检查的方法参数
	 * @return 如果该解析器支持所提供的参数则返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 从给定消息中将方法参数解析为参数值。
	 * @param parameter 要解析的方法参数。
	 * 该参数必须事先传递给 {@link #supportsParameter(org.springframework.core.MethodParameter)}
	 * 且该方法必须返回 {@code true}。
	 * @param message 当前正在处理的消息
	 * @return 表示参数值的 {@code Mono}，可能为空
	 */
	Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message);

}
