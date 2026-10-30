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

import org.springframework.util.concurrent.ListenableFuture;

/**
 * 用于建立 TCP 连接的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> 入站和出站消息的负载类型
 */
public interface TcpOperations<P> {

	/**
	 * 打开一个新连接。
	 * @param connectionHandler 用于管理连接的处理程序
	 * @return 一个 ListenableFuture，可用于确定连接何时以及是否成功建立
	 */
	ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler);

	/**
	 * 打开一个新连接，并提供连接失败时的重连策略。
	 * @param connectionHandler 用于管理连接的处理器
	 * @param reconnectStrategy 用于重连的策略
	 * @return 一个 ListenableFuture，可用于确定初始连接何时以及是否成功建立
	 */
	ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler, ReconnectStrategy reconnectStrategy);

	/**
	 * 关闭并停止任何已打开的连接。
	 * @return 一个 ListenableFuture，可用于确定连接何时以及是否成功关闭
	 */
	ListenableFuture<Void> shutdown();

}
