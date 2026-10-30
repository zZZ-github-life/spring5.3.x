/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * 包含从源消息中解析“用户”目标并将其转换为目标目标
 * （每个活跃用户会话对应一个）的结果。
 *
 * @author Rossen Stoyanchev
 * @since 4.0.2
 * @see org.springframework.messaging.simp.user.UserDestinationResolver
 */
public class UserDestinationResult {

	private final String sourceDestination;

	private final Set<String> targetDestinations;

	private final String subscribeDestination;

	@Nullable
	private final String user;


	public UserDestinationResult(String sourceDestination, Set<String> targetDestinations,
			String subscribeDestination, @Nullable String user) {

		Assert.notNull(sourceDestination, "'sourceDestination' must not be null");
		Assert.notNull(targetDestinations, "'targetDestinations' must not be null");
		Assert.notNull(subscribeDestination, "'subscribeDestination' must not be null");

		this.sourceDestination = sourceDestination;
		this.targetDestinations = targetDestinations;
		this.subscribeDestination = subscribeDestination;
		this.user = user;
	}


	/**
	 * 源消息中的“用户”目标。订阅时可能类似于
	 * "/user/queue/position-updates"，发送消息时可能类似于
	 * "/user/{username}/queue/position-updates"。
	 * @return "用户"目标地址，永不为 {@code null}。
	 */
	public String getSourceDestination() {
		return this.sourceDestination;
	}

	/**
	 * 源目标转换后的目标目标，每个活跃用户会话对应一个，
	 * 例如 "/queue/position-updates-useri9oqdfzo"。
	 * @return 目标地址，永不为 {@code null}，
	 * 但如果该用户没有活动会话则可能为空集合。
	 */
	public Set<String> getTargetDestinations() {
		return this.targetDestinations;
	}

	/**
	 * 客户端订阅时预期形式的用户目标，例如 "/user/queue/position-updates"。
	 * @return "用户"目标地址的订阅形式，永不为 {@code null}。
	 */
	public String getSubscribeDestination() {
		return this.subscribeDestination;
	}

	/**
	 * 此用户目标对应的用户。
	 * @return 用户名；如果只有会话 id（例如用户未认证）则为 {@code null}；
	 * 在这种情况下可以用 sessionId 代替用户名，
	 * 从而无需通过 {@link SimpUserRegistry} 进行用户到会话的查找。
	 */
	@Nullable
	public String getUser() {
		return this.user;
	}


	@Override
	public String toString() {
		return "UserDestinationResult [source=" + this.sourceDestination + ", target=" + this.targetDestinations +
				", subscribeDestination=" + this.subscribeDestination + ", user=" + this.user + "]";
	}

}
