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

package org.springframework.messaging.tcp;

import org.springframework.messaging.Message;

/**
 * 用于管理 TCP 连接生命周期事件（包括处理传入消息）的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> 入站和出站消息的负载类型
 */
public interface TcpConnectionHandler<P> {

	/**
	 * 在连接成功建立后调用。
	 * @param connection 连接
	 */
	void afterConnected(TcpConnection<P> connection);

	/**
	 * 在连接失败时调用。
	 * @param ex 异常
	 */
	void afterConnectFailure(Throwable ex);

	/**
	 * 处理从远程主机接收到的消息。
	 * @param message 消息
	 */
	void handleMessage(Message<P> message);

	/**
	 * 处理连接上的故障。
	 * @param ex 异常
	 */
	void handleFailure(Throwable ex);

	/**
	 * 在连接关闭后调用。
	 */
	void afterConnectionClosed();

}
