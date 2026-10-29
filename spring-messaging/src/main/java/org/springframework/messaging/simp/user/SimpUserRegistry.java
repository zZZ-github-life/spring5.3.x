/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 当前已连接用户的注册表。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpUserRegistry {

	/**
	 * 获取给定名称的用户。
	 * @param userName the name of the user to look up
	 * @return the user, or {@code null} if not connected
	 */
	@Nullable
	SimpUser getUser(String userName);

	/**
	 * 返回所有已连接用户的快照。
	 * <p>返回的集合是副本，不会反映后续更改。
	 * @return the connected users, or an empty set if none
	 */
	Set<SimpUser> getUsers();

	/**
	 * 返回所有已连接用户的数量。
	 * @return the number of connected users
	 * @since 4.3.5
	 */
	int getUserCount();

	/**
	 * 使用给定匹配器查找订阅。
	 * @param matcher the matcher to use
	 * @return a set of matching subscriptions, or an empty set if none
	 */
	Set<SimpSubscription> findSubscriptions(SimpSubscriptionMatcher matcher);

}
