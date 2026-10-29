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

/**
 * 表示已连接用户的会话。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpSession {

	/**
	 * 返回会话 ID。
	 */
	String getId();

	/**
	 * 返回与会话关联的用户。
	 */
	SimpUser getUser();

	/**
	 * 返回此会话的订阅。
	 */
	Set<SimpSubscription> getSubscriptions();

}
