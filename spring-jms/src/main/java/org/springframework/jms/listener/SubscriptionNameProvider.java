/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.listener;

/**
 * 由消息监听器对象实现的接口，用于为其可能注册的持久订阅
 * 建议一个特定名称。否则，监听器类名将用作默认订阅名称。
 *
 * <p>适用于 {@link javax.jms.MessageListener} 对象，
 * 也适用于 {@link SessionAwareMessageListener} 对象和普通监听器方法
 * （如 {@link org.springframework.jms.listener.adapter.MessageListenerAdapter} 所支持）。
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public interface SubscriptionNameProvider {

	/**
	 * 确定此消息监听器对象的订阅名称。
	 */
	String getSubscriptionName();

}
