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

package org.springframework.messaging.simp.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link SubscriptionRegistry} 的实现，将订阅存储在内存中，并使用
 * {@link org.springframework.util.PathMatcher PathMatcher} 匹配目标。
 *
 * <p>从 4.2 开始，此类支持订阅消息上的
 * {@link #setSelectorHeaderName selector} 头，其中 Spring EL 表达式
 * 会根据头进行求值，以便在目标匹配之外进一步过滤消息。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry {

	/** 目标缓存的默认最大条目数：1024。 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/** 可重用的静态求值上下文。 */
	private static final EvaluationContext messageEvalContext =
			SimpleEvaluationContext.forPropertyAccessors(new SimpMessageHeaderPropertyAccessor()).build();


	private PathMatcher pathMatcher = new AntPathMatcher();

	private int cacheLimit = DEFAULT_CACHE_LIMIT;

	@Nullable
	private String selectorHeaderName = "selector";

	private volatile boolean selectorHeaderInUse;

	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final DestinationCache destinationCache = new DestinationCache();

	private final SessionRegistry sessionRegistry = new SessionRegistry();


	/**
	 * 指定要使用的 {@link PathMatcher}。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回已配置的 {@link PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 指定已解析目标缓存的最大条目数。默认值为 1024。
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
		this.destinationCache.ensureCacheLimit();
	}

	/**
	 * 返回已解析目标缓存的最大条目数。
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * 配置订阅消息可以带有的头名称，用于过滤与订阅匹配的消息。
	 * 该头值应为 Spring EL 布尔表达式，应用于与订阅匹配的消息头。
	 * <p>例如：
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认设置为 "selector"。可以将其设置为其他名称，或设置为 {@code null}
	 * 以关闭对选择器头的支持。
	 * @param selectorHeaderName 用于选择器消息头的名称
	 * @since 4.2
	 */
	public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
		this.selectorHeaderName = (StringUtils.hasText(selectorHeaderName) ? selectorHeaderName : null);
	}

	/**
	 * 返回选择器头的名称。
	 * @since 4.2
	 */
	@Nullable
	public String getSelectorHeaderName() {
		return this.selectorHeaderName;
	}

	@Override
	protected void addSubscriptionInternal(
			String sessionId, String subscriptionId, String destination, Message<?> message) {

		boolean isPattern = this.pathMatcher.isPattern(destination);
		Expression expression = getSelectorExpression(message.getHeaders());
		Subscription subscription = new Subscription(subscriptionId, destination, isPattern, expression);

		this.sessionRegistry.addSubscription(sessionId, subscription);
		this.destinationCache.updateAfterNewSubscription(sessionId, subscription);
	}

	@Nullable
	private Expression getSelectorExpression(MessageHeaders headers) {
		if (getSelectorHeaderName() == null) {
			return null;
		}
		String selector = SimpMessageHeaderAccessor.getFirstNativeHeader(getSelectorHeaderName(), headers);
		if (selector == null) {
			return null;
		}
		Expression expression = null;
		try {
			expression = this.expressionParser.parseExpression(selector);
			this.selectorHeaderInUse = true;
			if (logger.isTraceEnabled()) {
				logger.trace("Subscription selector: [" + selector + "]");
			}
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to parse selector: " + selector, ex);
			}
		}
		return expression;
	}

	@Override
	protected void removeSubscriptionInternal(String sessionId, String subscriptionId, Message<?> message) {
		SessionInfo info = this.sessionRegistry.getSession(sessionId);
		if (info != null) {
			Subscription subscription = info.removeSubscription(subscriptionId);
			if (subscription != null) {
				this.destinationCache.updateAfterRemovedSubscription(sessionId, subscription);
			}
		}
	}

	@Override
	public void unregisterAllSubscriptions(String sessionId) {
		SessionInfo info = this.sessionRegistry.removeSubscriptions(sessionId);
		if (info != null) {
			this.destinationCache.updateAfterRemovedSession(sessionId, info);
		}
	}

	@Override
	protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
		MultiValueMap<String, String> allMatches = this.destinationCache.getSubscriptions(destination);
		if (!this.selectorHeaderInUse) {
			return allMatches;
		}
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(allMatches.size());
		allMatches.forEach((sessionId, subscriptionIds) -> {
			SessionInfo info = this.sessionRegistry.getSession(sessionId);
			if (info != null) {
				for (String subscriptionId : subscriptionIds) {
					Subscription subscription = info.getSubscription(subscriptionId);
					if (subscription != null && evaluateExpression(subscription.getSelector(), message)) {
						result.add(sessionId, subscription.getId());
					}
				}
			}
		});
		return result;
	}

	private boolean evaluateExpression(@Nullable Expression expression, Message<?> message) {
		if (expression == null) {
			return true;
		}
		try {
			Boolean result = expression.getValue(messageEvalContext, message, Boolean.class);
			if (Boolean.TRUE.equals(result)) {
				return true;
			}
		}
		catch (SpelEvaluationException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate selector: " + ex.getMessage());
			}
		}
		catch (Throwable ex) {
			logger.debug("Failed to evaluate selector", ex);
		}
		return false;
	}


	/**
	 * 缓存之前通过
	 * {@link DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}
	 * 解析出的目标。
	 */
	private final class DestinationCache {

		// destination -> [sessionId -> subscriptionId's]
		private final Map<String, LinkedMultiValueMap<String, String>> destinationCache =
				new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

		private final AtomicInteger cacheSize = new AtomicInteger();

		private final Queue<String> cacheEvictionPolicy = new ConcurrentLinkedQueue<>();

		public LinkedMultiValueMap<String, String> getSubscriptions(String destination) {
			LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = this.destinationCache.get(destination);
			if (sessionIdToSubscriptionIds == null) {
				sessionIdToSubscriptionIds = this.destinationCache.computeIfAbsent(destination, _destination -> {
					LinkedMultiValueMap<String, String> matches = computeMatchingSubscriptions(destination);
					// Update queue first, so that cacheSize <= queue.size(
					this.cacheEvictionPolicy.add(destination);
					this.cacheSize.incrementAndGet();
					return matches;
				});
				ensureCacheLimit();
			}
			return sessionIdToSubscriptionIds;
		}

		private LinkedMultiValueMap<String, String> computeMatchingSubscriptions(String destination) {
			LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = new LinkedMultiValueMap<>();
			DefaultSubscriptionRegistry.this.sessionRegistry.forEachSubscription((sessionId, subscription) -> {
				if (subscription.isPattern()) {
					if (pathMatcher.match(subscription.getDestination(), destination)) {
						addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscription.getId());
					}
				}
				else if (destination.equals(subscription.getDestination())) {
					addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscription.getId());
				}
			});
			return sessionIdToSubscriptionIds;
		}

		private void addMatchedSubscriptionId(
				LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds,
				String sessionId, String subscriptionId) {

			sessionIdToSubscriptionIds.compute(sessionId, (_sessionId, subscriptionIds) -> {
				if (subscriptionIds == null) {
					return Collections.singletonList(subscriptionId);
				}
				else {
					List<String> result = new ArrayList<>(subscriptionIds.size() + 1);
					result.addAll(subscriptionIds);
					result.add(subscriptionId);
					return result;
				}
			});
		}

		private void ensureCacheLimit() {
			int size = this.cacheSize.get();
			if (size > cacheLimit) {
				do {
					if (this.cacheSize.compareAndSet(size, size - 1)) {
						// Remove (vs poll): we expect an element
						String head = this.cacheEvictionPolicy.remove();
						this.destinationCache.remove(head);
					}
				} while ((size = this.cacheSize.get()) > cacheLimit);
			}
		}

		public void updateAfterNewSubscription(String sessionId, Subscription subscription) {
			if (subscription.isPattern()) {
				for (String cachedDestination : this.destinationCache.keySet()) {
					if (pathMatcher.match(subscription.getDestination(), cachedDestination)) {
						addToDestination(cachedDestination, sessionId, subscription.getId());
					}
				}
			}
			else {
				addToDestination(subscription.getDestination(), sessionId, subscription.getId());
			}
		}

		private void addToDestination(String destination, String sessionId, String subscriptionId) {
			this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
				sessionIdToSubscriptionIds = sessionIdToSubscriptionIds.clone();
				addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscriptionId);
				return sessionIdToSubscriptionIds;
			});
		}

		public void updateAfterRemovedSubscription(String sessionId, Subscription subscription) {
			if (subscription.isPattern()) {
				String subscriptionId = subscription.getId();
				this.destinationCache.forEach((destination, sessionIdToSubscriptionIds) -> {
					List<String> subscriptionIds = sessionIdToSubscriptionIds.get(sessionId);
					if (subscriptionIds != null && subscriptionIds.contains(subscriptionId)) {
						removeInternal(destination, sessionId, subscriptionId);
					}
				});
			}
			else {
				removeInternal(subscription.getDestination(), sessionId, subscription.getId());
			}
		}

		private void removeInternal(String destination, String sessionId, String subscriptionId) {
			this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
				sessionIdToSubscriptionIds = sessionIdToSubscriptionIds.clone();
				sessionIdToSubscriptionIds.computeIfPresent(sessionId, (_sessionId, subscriptionIds) -> {
					/* Most likely case: single subscription per destination per session. */
					if (subscriptionIds.size() == 1 && subscriptionId.equals(subscriptionIds.get(0))) {
						return null;
					}
					subscriptionIds = new ArrayList<>(subscriptionIds);
					subscriptionIds.remove(subscriptionId);
					return (subscriptionIds.isEmpty() ? null : subscriptionIds);
				});
				return sessionIdToSubscriptionIds;
			});
		}

		public void updateAfterRemovedSession(String sessionId, SessionInfo info) {
			for (Subscription subscription : info.getSubscriptions()) {
				updateAfterRemovedSubscription(sessionId, subscription);
			}
		}
	}

	/**
	 * 所有会话及其订阅的注册表。
	 */
	private static final class SessionRegistry {

		private final ConcurrentMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

		@Nullable
		public SessionInfo getSession(String sessionId) {
			return this.sessions.get(sessionId);
		}

		public void forEachSubscription(BiConsumer<String, Subscription> consumer) {
			this.sessions.forEach((sessionId, info) ->
				info.getSubscriptions().forEach(subscription -> consumer.accept(sessionId, subscription)));
		}

		public void addSubscription(String sessionId, Subscription subscription) {
			SessionInfo info = this.sessions.computeIfAbsent(sessionId, _sessionId -> new SessionInfo());
			info.addSubscription(subscription);
		}

		@Nullable
		public SessionInfo removeSubscriptions(String sessionId) {
			return this.sessions.remove(sessionId);
		}
	}

	/**
	 * 会话订阅的容器。
	 */
	private static final class SessionInfo {

		// subscriptionId -> Subscription
		private final Map<String, Subscription> subscriptionMap = new ConcurrentHashMap<>();

		public Collection<Subscription> getSubscriptions() {
			return this.subscriptionMap.values();
		}

		@Nullable
		public Subscription getSubscription(String subscriptionId) {
			return this.subscriptionMap.get(subscriptionId);
		}

		public void addSubscription(Subscription subscription) {
			this.subscriptionMap.putIfAbsent(subscription.getId(), subscription);
		}

		@Nullable
		public Subscription removeSubscription(String subscriptionId) {
			return this.subscriptionMap.remove(subscriptionId);
		}
	}

	/**
	 * 表示一个订阅。
	 */
	private static final class Subscription {

		private final String id;

		private final String destination;

		private final boolean isPattern;

		@Nullable
		private final Expression selector;

		public Subscription(String id, String destination, boolean isPattern, @Nullable Expression selector) {
			Assert.notNull(id, "Subscription id must not be null");
			Assert.notNull(destination, "Subscription destination must not be null");
			this.id = id;
			this.selector = selector;
			this.destination = destination;
			this.isPattern = isPattern;
		}

		public String getId() {
			return this.id;
		}

		public String getDestination() {
			return this.destination;
		}

		public boolean isPattern() {
			return this.isPattern;
		}

		@Nullable
		public Expression getSelector() {
			return this.selector;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other ||
					(other instanceof Subscription && this.id.equals(((Subscription) other).id)));
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public String toString() {
			return "subscription(id=" + this.id + ")";
		}
	}


	private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Message.class, MessageHeaders.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
			return true;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {
			Object value;
			if (target instanceof Message) {
				value = name.equals("headers") ? ((Message) target).getHeaders() : null;
			}
			else if (target instanceof MessageHeaders) {
				MessageHeaders headers = (MessageHeaders) target;
				SimpMessageHeaderAccessor accessor =
						MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
				Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
				if ("destination".equalsIgnoreCase(name)) {
					value = accessor.getDestination();
				}
				else {
					value = accessor.getFirstNativeHeader(name);
					if (value == null) {
						value = headers.get(name);
					}
				}
			}
			else {
				// Should never happen...
				throw new IllegalStateException("Expected Message or MessageHeaders.");
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object value) {
		}
	}

}
