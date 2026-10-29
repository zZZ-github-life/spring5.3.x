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

package org.springframework.messaging.simp.stomp;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 使用 {@link ReactorNettyTcpClient} 的基于 TCP 的 STOMP 客户端。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyTcpStompClient extends StompClientSupport {

	private final TcpOperations<byte[]> tcpClient;


	/**
	 * 创建主机为 "127.0.0.1"、端口为 61613 的实例。
	 */
	public ReactorNettyTcpStompClient() {
		this("127.0.0.1", 61613);
	}

	/**
	 * 使用给定主机和端口创建实例。
	 * @param host the host
	 * @param port the port
	 */
	public ReactorNettyTcpStompClient(String host, int port) {
		this.tcpClient = initTcpClient(host, port);
	}

	/**
	 * 使用预配置的 TCP 客户端创建实例。
	 * @param tcpClient the client to use
	 */
	public ReactorNettyTcpStompClient(TcpOperations<byte[]> tcpClient) {
		Assert.notNull(tcpClient, "'tcpClient' is required");
		this.tcpClient = tcpClient;
	}

	private static ReactorNettyTcpClient<byte[]> initTcpClient(String host, int port) {
		ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(host, port, new StompReactorNettyCodec());
		client.setLogger(SimpLogging.forLog(client.getLogger()));
		return client;
	}


	/**
	 * 连接并在 STOMP 层面上连接成功后通知给定的 {@link StompSessionHandler}。
	 * @param handler the handler for the STOMP session
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(StompSessionHandler handler) {
		return connect(null, handler);
	}

	/**
	 * {@link #connect(StompSessionHandler)} 的重载版本，
	 * 接受用于 STOMP CONNECT 帧的头。
	 * @param connectHeaders headers to add to the CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return a ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connect(session);
		return session.getSessionFuture();
	}

	/**
	 * 关闭客户端并释放资源。
	 */
	public void shutdown() {
		this.tcpClient.shutdown();
	}

	@Override
	public String toString() {
		return "ReactorNettyTcpStompClient[" + this.tcpClient + "]";
	}
}
