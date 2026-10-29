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

package org.springframework.messaging.tcp.reactor;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;

import org.springframework.messaging.Message;

/**
 * 用于配合 Reactor Netty 的解码 {@link Function} 和编码 {@link BiConsumer}
 * 的简单持有者。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <P> the message payload type
 */
public interface ReactorNettyCodec<P> {

	/**
	 * 将输入的 {@link ByteBuf} 解码为一条或多条 {@link Message Messages}。
	 * @param inputBuffer the input buffer to decode from
	 * @return 0 or more decoded messages
	 */
	Collection<Message<P>> decode(ByteBuf inputBuffer);

	/**
	 * 将给定的 {@link Message} 编码到输出 {@link ByteBuf} 中。
	 * @param message the message to encode
	 * @param outputBuffer the buffer to write to
	 */
	void encode(Message<P> message, ByteBuf outputBuffer);

}
