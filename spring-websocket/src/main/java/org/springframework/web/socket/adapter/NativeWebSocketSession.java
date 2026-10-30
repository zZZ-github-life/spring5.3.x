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

package org.springframework.web.socket.adapter;

import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketSession;

/**
 * 通过 getter 方法暴露底层原生 WebSocketSession 的 {@link WebSocketSession}。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface NativeWebSocketSession extends WebSocketSession {

	/**
	 * 返回底层原生 WebSocketSession。
	 */
	Object getNativeSession();

	/**
	 * 返回底层原生 WebSocketSession（如果可用）。
	 * @param requiredType 会话所需的类型
	 * @return 所需类型的原生会话，
	 * 如果不可用则为 {@code null}
	 */
	@Nullable
	<T> T getNativeSession(@Nullable Class<T> requiredType);

}
