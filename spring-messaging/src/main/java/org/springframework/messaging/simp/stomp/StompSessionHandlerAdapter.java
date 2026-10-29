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
 * {@link StompSessionHandler} 的抽象适配器类，其实现方法大多为空，
 * 但 {@link #getPayloadType} 除外，它返回 String，
 * 作为 STOMP ERROR 帧载荷预期的默认 Object 类型。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public abstract class StompSessionHandlerAdapter implements StompSessionHandler {

	/**
	 * 此实现返回 String，作为 STOMP ERROR 帧的预期载荷类型。
	 */
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void handleFrame(StompHeaders headers, @Nullable Object payload) {
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void handleException(StompSession session, @Nullable StompCommand command,
			StompHeaders headers, byte[] payload, Throwable exception) {
	}

	/**
	 * 此实现为空。
	 */
	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
	}

}
