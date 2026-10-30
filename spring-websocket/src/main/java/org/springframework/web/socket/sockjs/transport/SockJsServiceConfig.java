/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * 为传输处理代码提供对其所需 {@link SockJsService} 配置选项的访问。
 * 主要用于内部使用。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsServiceConfig {

	/**
	 * 用于调度心跳消息的调度器实例。
	 */
	TaskScheduler getTaskScheduler();

	/**
	 * 流式传输会在客户端保留响应，并且不会释放
	 * 已传递消息占用的内存。此类传输需要不时地回收
	 * 连接。此属性设置单个 HTTP 流式请求在被关闭之前
	 * 可以发送的最小字节数。之后客户端将打开
	 * 新的请求。将此值设置为 1 会实际禁用流式传输，
	 * 并使流式传输的行为类似于轮询传输。
	 * <p>默认值为 128K（即 128 * 1024）。
	 */
	int getStreamBytesLimit();

	/**
	 * 服务器未发送任何消息时的毫秒数，超过该时间后，
	 * 服务器应向客户端发送心跳帧，
	 * 以防止连接中断。
	 * <p>默认值为 25,000（25 秒）。
	 */
	long getHeartbeatTime();

	/**
	 * 会话在等待客户端下一次 HTTP 轮询请求时可以缓存的服务端到客户端消息数。
	 * 所有 HTTP 传输都使用此属性，因为即使是流式传输也会定期回收 HTTP 请求。
	 * <p>HTTP 请求之间的时间应相对较短，
	 * 并且不会超过允许的断开连接延迟（参见
	 * {@link org.springframework.web.socket.sockjs.support.AbstractSockJsService#setDisconnectDelay(long)}），
	 * 默认情况下为 5 秒。
	 * <p>默认大小为 100。
	 */
	int getHttpMessageCacheSize();

	/**
	 * 用于编码和解码 SockJS 消息的编解码器。
	 * @throws IllegalStateException 如果没有任何 {@link SockJsMessageCodec} 可用
	 */
	SockJsMessageCodec getMessageCodec();

}
