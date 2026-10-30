/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 一个 {@link MessageChannel}，可以通过轮询主动从中接收消息。
 *
 * @author Mark Fisher
 * @since 4.0
 */
public interface PollableChannel extends MessageChannel {

	/**
	 * 从此通道接收消息，如有必要会无限期阻塞。
	 * @return 下一个可用的 {@link Message}；如果被中断则返回 {@code null}
	 */
	@Nullable
	Message<?> receive();

	/**
	 * 从此通道接收消息，阻塞直到有消息可用或指定的超时时间过去。
	 * @param timeout 以毫秒为单位的超时时间，或 {@link MessageChannel#INDEFINITE_TIMEOUT}
	 * @return 下一个可用的 {@link Message}；如果指定的超时时间已过
	 * 或消息接收被中断，则返回 {@code null}
	 */
	@Nullable
	Message<?> receive(long timeout);

}
