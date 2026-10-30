/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;

import org.springframework.lang.Nullable;

/**
 * 处理 STOMP 帧的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface StompFrameHandler {

	/**
	 * 在 {@link #handleFrame(StompHeaders, Object)} 之前调用，
	 * 以确定载荷应转换为的对象类型。
	 * @param headers 消息的消息头
	 */
	Type getPayloadType(StompHeaders headers);

	/**
	 * 处理 STOMP 帧，其载荷已转换为
	 * {@link #getPayloadType(StompHeaders)} 返回的目标类型。
	 * @param headers 帧的消息头
	 * @param payload 负载，如果没有负载则为 {@code null}
	 */
	void handleFrame(StompHeaders headers, @Nullable Object payload);

}
