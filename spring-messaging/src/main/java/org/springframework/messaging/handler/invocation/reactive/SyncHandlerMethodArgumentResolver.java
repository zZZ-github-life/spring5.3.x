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

package org.springframework.messaging.handler.invocation.reactive;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * {@link HandlerMethodArgumentResolver} 的扩展，用于本身是同步且不会阻塞解析值的实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface SyncHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver {

	/**
	 * {@inheritDoc}
	 * <p>默认情况下，此方法仅委托给 {@link #resolveArgumentValue} 进行同步解析。
	 */
	@Override
	default Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		return Mono.justOrEmpty(resolveArgumentValue(parameter, message));
	}

	/**
	 * 同步解析方法参数的值。
	 * @param parameter the method parameter
	 * @param message the currently processed message
	 * @return the resolved value, if any
	 */
	@Nullable
	Object resolveArgumentValue(MethodParameter parameter, Message<?> message);

}
