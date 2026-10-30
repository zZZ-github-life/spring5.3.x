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

package org.springframework.messaging;

import reactor.core.publisher.Mono;

/**
 * 用于处理 {@link Message} 的响应式契约。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see MessageHandler
 */
@FunctionalInterface
public interface ReactiveMessageHandler {

	/**
	 * 处理给定消息。
	 * @param message 要处理的消息
	 * @return 用于表示消息处理结果的完成 {@link Mono}
	 */
	Mono<Void> handleMessage(Message<?> message);

}
