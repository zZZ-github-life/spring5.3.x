/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * {@link Runnable} 接口的扩展，提供获取待处理 {@link MessageHandler} 和
 * {@link Message} 的方法。
 *
 * @author Rossen Stoyanchev
 * @since 4.1.1
 */
public interface MessageHandlingRunnable extends Runnable {

	/**
	 * 返回将要处理的 Message。
	 */
	Message<?> getMessage();

	/**
	 * 返回将用于处理消息的 MessageHandler。
	 */
	MessageHandler getMessageHandler();

}
