/*
 * Copyright 2002-2020 the original author or authors.
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

import java.security.Principal;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 表示已连接的用户。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpUser {

	/**
	 * 唯一用户名。
	 */
	String getName();

	/**
	 * 返回与会话关联的用户（如果可用）。通常，除非在多服务器用户注册表场景中
	 * 用户连接到其他服务器，否则用户信息是可用的。
	 * @since 5.3
	 */
	@Nullable
	Principal getPrincipal();

	/**
	 * 用户是否有任何会话。
	 */
	boolean hasSessions();

	/**
	 * 查找给定 ID 的会话。
	 * @param sessionId the session id
	 * @return the matching session, or {@code null} if none found
	 */
	@Nullable
	SimpSession getSession(String sessionId);

	/**
	 * 返回用户的会话。
	 * 返回的集合是副本，永远不会被修改。
	 * @return a set of session ids, or an empty set if none
	 */
	Set<SimpSession> getSessions();

}
