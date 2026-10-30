/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * {@code UserDestinationResolver} 的默认实现，依赖 {@link SimpUserRegistry}
 * 查找用户的活跃会话。
 *
 * <p>当用户尝试订阅（例如订阅 "/user/queue/position-updates"）时，
 * 会移除 "/user" 前缀，并根据会话 ID 添加唯一后缀，
 * 例如 "/queue/position-updates-useri9oqdfzo"，
 * 以确保不同用户可以订阅相同的逻辑目标而不会冲突。
 *
 * <p>当向用户发送消息（例如 "/user/{username}/queue/position-updates"）时，
 * 会移除 "/user/{username}" 前缀，并添加基于活跃会话 ID 的后缀，
 * 例如 "/queue/position-updates-useri9oqdfzo"。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class DefaultUserDestinationResolver implements UserDestinationResolver {

	private static final Log logger = SimpLogging.forLogName(DefaultUserDestinationResolver.class);


	private final SimpUserRegistry userRegistry;

	private String prefix = "/user/";

	private boolean removeLeadingSlash = false;


	/**
	 * 创建将通过提供的注册表访问用户会话 ID 信息的实例。
	 * @param userRegistry 注册表，永不为 {@code null}
	 */
	public DefaultUserDestinationResolver(SimpUserRegistry userRegistry) {
		Assert.notNull(userRegistry, "SimpUserRegistry must not be null");
		this.userRegistry = userRegistry;
	}


	/**
	 * 返回已配置的 {@link SimpUserRegistry}。
	 */
	public SimpUserRegistry getSimpUserRegistry() {
		return this.userRegistry;
	}

	/**
	 * 用于识别用户目标的前缀。不以该前缀开头的目标不会被解析。
	 * <p>默认前缀为 "/user/"。
	 * @param prefix 要使用的前缀
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "Prefix must not be empty");
		this.prefix = (prefix.endsWith("/") ? prefix : prefix + "/");
	}

	/**
	 * 返回已配置的用户目标前缀。
	 */
	public String getDestinationPrefix() {
		return this.prefix;
	}

	/**
	 * 使用此属性指示是否应移除转换后的用户目标中的前导斜杠。
	 * 这取决于消息代理配置的目标前缀。
	 * <p>默认设置为 {@code false}，即“不更改目标目标”，
	 * 不过如果配置的目标没有前导斜杠，
	 * {@link org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration
	 * AbstractMessageBrokerConfiguration} 可能会将其改为 {@code true}。
	 * @param remove 是否移除前导斜杠
	 * @since 4.3.14
	 */
	public void setRemoveLeadingSlash(boolean remove) {
		this.removeLeadingSlash = remove;
	}

	/**
	 * 是否移除目标目标中的前导斜杠。
	 * @since 4.3.14
	 */
	public boolean isRemoveLeadingSlash() {
		return this.removeLeadingSlash;
	}

	/**
	 * 提供用于处理目标的 {@code PathMatcher}，这有助于确定在移除
	 * {@link #setUserDestinationPrefix userDestinationPrefix} 后，
	 * 实际目标中是否应保留前导斜杠。
	 * <p>默认情况下，实际目标带有前导斜杠，例如 {@code /queue/position-updates}，
	 * 这适用于支持以斜杠作为分隔符的目标的代理。如果提供的 {@code PathMatcher}
	 * 支持其他分隔符，则生成的目标将没有前导斜杠，例如
	 * {@code jms.queue.position-updates}。
	 * @param pathMatcher 用于处理目标地址的 PathMatcher
	 * @since 4.3
	 * @deprecated as of 4.3.14 this property is no longer used and is replaced
	 * by {@link #setRemoveLeadingSlash(boolean)} that indicates more explicitly
	 * whether to keep the leading slash which may or may not be the case
	 * regardless of how the {@code PathMatcher} is configured.
	 */
	@Deprecated
	public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
		// Do nothing
	}


	@Override
	@Nullable
	public UserDestinationResult resolveDestination(Message<?> message) {
		ParseResult parseResult = parse(message);
		if (parseResult == null) {
			return null;
		}
		String user = parseResult.getUser();
		String sourceDestination = parseResult.getSourceDestination();
		Set<String> targetSet = new HashSet<>();
		for (String sessionId : parseResult.getSessionIds()) {
			String actualDestination = parseResult.getActualDestination();
			String targetDestination = getTargetDestination(
					sourceDestination, actualDestination, sessionId, user);
			if (targetDestination != null) {
				targetSet.add(targetDestination);
			}
		}
		String subscribeDestination = parseResult.getSubscribeDestination();
		return new UserDestinationResult(sourceDestination, targetSet, subscribeDestination, user);
	}

	@Nullable
	private ParseResult parse(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String sourceDestination = SimpMessageHeaderAccessor.getDestination(headers);
		if (sourceDestination == null || !checkDestination(sourceDestination, this.prefix)) {
			return null;
		}
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (messageType != null) {
			switch (messageType) {
				case SUBSCRIBE:
				case UNSUBSCRIBE:
					return parseSubscriptionMessage(message, sourceDestination);
				case MESSAGE:
					return parseMessage(headers, sourceDestination);
			}
		}
		return null;
	}

	@Nullable
	private ParseResult parseSubscriptionMessage(Message<?> message, String sourceDestination) {
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			logger.error("No session id. Ignoring " + message);
			return null;
		}
		int prefixEnd = this.prefix.length() - 1;
		String actualDestination = sourceDestination.substring(prefixEnd);
		if (isRemoveLeadingSlash()) {
			actualDestination = actualDestination.substring(1);
		}
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		String user = (principal != null ? principal.getName() : null);
		Assert.isTrue(user == null || !user.contains("%2F"), () -> "Invalid sequence \"%2F\" in user name: " + user);
		Set<String> sessionIds = Collections.singleton(sessionId);
		return new ParseResult(sourceDestination, actualDestination, sourceDestination, sessionIds, user);
	}

	private ParseResult parseMessage(MessageHeaders headers, String sourceDest) {
		int prefixEnd = this.prefix.length();
		int userEnd = sourceDest.indexOf('/', prefixEnd);
		Assert.isTrue(userEnd > 0, "Expected destination pattern \"/user/{userId}/**\"");
		String actualDest = sourceDest.substring(userEnd);
		String subscribeDest = this.prefix.substring(0, prefixEnd - 1) + actualDest;
		String userName = sourceDest.substring(prefixEnd, userEnd);
		userName = StringUtils.replace(userName, "%2F", "/");

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Set<String> sessionIds;
		if (userName.equals(sessionId)) {
			userName = null;
			sessionIds = Collections.singleton(sessionId);
		}
		else {
			sessionIds = getSessionIdsByUser(userName, sessionId);
		}

		if (isRemoveLeadingSlash()) {
			actualDest = actualDest.substring(1);
		}
		return new ParseResult(sourceDest, actualDest, subscribeDest, sessionIds, userName);
	}

	private Set<String> getSessionIdsByUser(String userName, @Nullable String sessionId) {
		Set<String> sessionIds;
		SimpUser user = this.userRegistry.getUser(userName);
		if (user != null) {
			if (sessionId != null && user.getSession(sessionId) != null) {
				sessionIds = Collections.singleton(sessionId);
			}
			else {
				Set<SimpSession> sessions = user.getSessions();
				sessionIds = new HashSet<>(sessions.size());
				for (SimpSession session : sessions) {
					sessionIds.add(session.getId());
				}
			}
		}
		else {
			sessionIds = Collections.emptySet();
		}
		return sessionIds;
	}

	protected boolean checkDestination(String destination, String requiredPrefix) {
		return destination.startsWith(requiredPrefix);
	}

	/**
	 * 此方法决定如何将源“用户”目标转换为给定活跃用户会话的实际目标目标。
	 * @param sourceDestination 输入消息中的源目标地址。
	 * @param actualDestination 不包含任何用户前缀的目标地址子集。
	 * @param sessionId 活动用户会话的 id，永不为 {@code null}。
	 * @param user 目标用户，可能为 {@code null}，例如未认证时。
	 * @return 目标地址，如果没有则为 {@code null}
	 */
	@SuppressWarnings("unused")
	@Nullable
	protected String getTargetDestination(String sourceDestination, String actualDestination,
			String sessionId, @Nullable String user) {

		return actualDestination + "-user" + sessionId;
	}

	@Override
	public String toString() {
		return "DefaultUserDestinationResolver[prefix=" + this.prefix + "]";
	}


	/**
	 * 已解析源“用户”目标的临时占位符。
	 */
	private static class ParseResult {

		private final String sourceDestination;

		private final String actualDestination;

		private final String subscribeDestination;

		private final Set<String> sessionIds;

		@Nullable
		private final String user;

		public ParseResult(String sourceDest, String actualDest, String subscribeDest,
				Set<String> sessionIds, @Nullable String user) {

			this.sourceDestination = sourceDest;
			this.actualDestination = actualDest;
			this.subscribeDestination = subscribeDest;
			this.sessionIds = sessionIds;
			this.user = user;
		}

		public String getSourceDestination() {
			return this.sourceDestination;
		}

		public String getActualDestination() {
			return this.actualDestination;
		}

		public String getSubscribeDestination() {
			return this.subscribeDestination;
		}

		public Set<String> getSessionIds() {
			return this.sessionIds;
		}

		@Nullable
		public String getUser() {
			return this.user;
		}
	}

}
