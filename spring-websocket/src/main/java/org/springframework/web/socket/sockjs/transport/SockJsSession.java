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

package org.springframework.web.socket.sockjs.transport;

import org.springframework.web.socket.WebSocketSession;

/**
 * Spring 标准 {@link WebSocketSession} 的 SockJS 扩展。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsSession extends WebSocketSession {

	/**
	 * 返回自会话上次活动以来的时间（以毫秒为单位）；如果会话是新建的，
	 * 则返回自会话创建以来的时间。
	 */
	long getTimeSinceLastActive();

	/**
	 * 禁用 SockJS 心跳，可能是因为更高层的协议已为会话启用了心跳。
	 * 否则不建议禁用，因为它有助于代理了解连接
	 * 并未挂起。
	 */
	void disableHeartbeat();

}
