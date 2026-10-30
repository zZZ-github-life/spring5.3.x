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

package org.springframework.messaging.tcp;

import java.io.Closeable;

import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 用于发送消息和管理 TCP 连接的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> 出站 {@link Message Messages} 的负载类型
 */
public interface TcpConnection<P> extends Closeable {

	/**
	 * 发送给定的消息。
	 * @param message 消息
	 * @return 可用于确定消息何时发送成功以及是否发送成功的 ListenableFuture
	 */
	ListenableFuture<Void> send(Message<P> message);

	/**
	 * 注册一个在读取空闲一段时间后调用的任务。
	 * @param runnable 要调用的任务
	 * @param duration 非活动时间量，单位为毫秒
	 */
	void onReadInactivity(Runnable runnable, long duration);

	/**
	 * 注册一个在写入空闲一段时间后调用的任务。
	 * @param runnable 要调用的任务
	 * @param duration 非活动时间量，单位为毫秒
	 */
	void onWriteInactivity(Runnable runnable, long duration);

	/**
	 * 关闭连接。
	 */
	@Override
	void close();

}
