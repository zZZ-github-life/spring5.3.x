/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging;

/**
 * 一个 {@link MessageChannel}，它维护订阅者注册表，并调用订阅者
 * 来处理通过此通道发送的消息。
 *
 * @author Mark Fisher
 * @since 4.0
 */
public interface SubscribableChannel extends MessageChannel {

	/**
	 * 注册消息处理器。
	 * @return 如果处理器已订阅则返回 {@code true}，如果此前已订阅则返回 {@code false}。
	 */
	boolean subscribe(MessageHandler handler);

	/**
	 * 取消注册消息处理器。
	 * @return 如果处理器已注销则返回 {@code true}，如果此前未注册则返回 {@code false}。
	 */
	boolean unsubscribe(MessageHandler handler);

}
