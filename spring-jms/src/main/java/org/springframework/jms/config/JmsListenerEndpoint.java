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

package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * JMS 监听器端点的模型。可与
 * {@link org.springframework.jms.annotation.JmsListenerConfigurer
 * JmsListenerConfigurer} 配合使用，以编程方式注册端点。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface JmsListenerEndpoint {

	/**
	 * 返回此端点的 id。
	 */
	String getId();

	/**
	 * 使用此端点定义的模型设置指定的消息监听器容器。
	 * <p>此端点必须提供指定容器所缺少的必需选项，使其可用。
	 * 通常，这涉及设置要使用的 {@code destination} 和 {@code messageListener}，
	 * 但实现也可以覆盖任何已设置的默认设置。
	 * @param listenerContainer the listener container to configure
	 */
	void setupListenerContainer(MessageListenerContainer listenerContainer);

}
