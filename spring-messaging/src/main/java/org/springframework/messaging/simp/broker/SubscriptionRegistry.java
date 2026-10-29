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

package org.springframework.messaging.simp.broker;

import org.springframework.messaging.Message;
import org.springframework.util.MultiValueMap;

/**
 * 按会话组织的订阅注册表，允许查找订阅。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SubscriptionRegistry {

	/**
	 * 注册由给定消息表示的订阅。
	 * @param subscribeMessage the subscription request
	 */
	void registerSubscription(Message<?> subscribeMessage);

	/**
	 * 取消注册订阅。
	 * @param unsubscribeMessage the request to unsubscribe
	 */
	void unregisterSubscription(Message<?> unsubscribeMessage);

	/**
	 * 移除与给定 sessionId 关联的所有订阅。
	 */
	void unregisterAllSubscriptions(String sessionId);

	/**
	 * 查找应接收给定消息的所有订阅。
	 * 返回的映射可以安全地迭代，并且永远不会被修改。
	 * @param message the message
	 * @return a {@code MultiValueMap} with sessionId-subscriptionId pairs
	 * (possibly empty)
	 */
	MultiValueMap<String, String> findSubscriptions(Message<?> message);

}
