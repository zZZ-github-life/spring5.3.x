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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 逻辑析取（' || '）请求条件，用于将请求与一组 {@link RequestMethod RequestMethods} 进行匹配。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	/** 按 HTTP 方法的缓存，用于从 getMatchingCondition 返回现成实例。 */
	private static final Map<HttpMethod, RequestMethodsRequestCondition> requestMethodConditionCache;

	static {
		requestMethodConditionCache = CollectionUtils.newHashMap(RequestMethod.values().length);
		for (RequestMethod method : RequestMethod.values()) {
			requestMethodConditionCache.put(
					HttpMethod.valueOf(method.name()), new RequestMethodsRequestCondition(method));
		}
	}


	private final Set<RequestMethod> methods;


	/**
	 * 使用给定的请求方法创建新实例。
	 * @param requestMethods 0 or more HTTP request methods;
	 * if, 0 the condition will match to every request
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this.methods = (ObjectUtils.isEmpty(requestMethods) ?
				Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
	}

	/**
	 * 内部私有构造函数，用于组合条件。
	 */
	private RequestMethodsRequestCondition(Set<RequestMethod> requestMethods) {
		this.methods = requestMethods;
	}


	/**
	 * 返回此条件中包含的所有 {@link RequestMethod RequestMethods}。
	 */
	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	@Override
	protected Collection<RequestMethod> getContent() {
		return this.methods;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的 HTTP 请求方法并集。
	 */
	@Override
	public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
		set.addAll(other.methods);
		return new RequestMethodsRequestCondition(set);
	}

	/**
	 * 检查是否有任何 HTTP 请求方法匹配给定请求，并返回仅包含匹配 HTTP 请求方法的实例。
	 * @param exchange the current exchange
	 * @return the same instance if the condition is empty (unless the request
	 * method is HTTP OPTIONS), a new condition with the matched request method,
	 * or {@code null} if there is no match or the condition is empty and the
	 * request method is OPTIONS.
	 */
	@Override
	@Nullable
	public RequestMethodsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return matchPreFlight(exchange.getRequest());
		}
		if (getMethods().isEmpty()) {
			if (RequestMethod.OPTIONS.name().equals(exchange.getRequest().getMethodValue())) {
				return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
			}
			return this;
		}
		return matchRequestMethod(exchange.getRequest().getMethod());
	}

	/**
	 * 在预检请求中，匹配将要执行的实际请求。因此空条件即为匹配，否则尝试匹配 "Access-Control-Request-Method" 头中的 HTTP 方法。
	 */
	@Nullable
	private RequestMethodsRequestCondition matchPreFlight(ServerHttpRequest request) {
		if (getMethods().isEmpty()) {
			return this;
		}
		HttpMethod expectedMethod = request.getHeaders().getAccessControlRequestMethod();
		return expectedMethod != null ? matchRequestMethod(expectedMethod) : null;
	}

	@Nullable
	private RequestMethodsRequestCondition matchRequestMethod(@Nullable HttpMethod httpMethod) {
		if (httpMethod == null) {
			return null;
		}
		RequestMethod requestMethod = RequestMethod.valueOf(httpMethod.name());
		if (getMethods().contains(requestMethod)) {
			return requestMethodConditionCache.get(httpMethod);
		}
		if (requestMethod.equals(RequestMethod.HEAD) && getMethods().contains(RequestMethod.GET)) {
			return requestMethodConditionCache.get(HttpMethod.GET);
		}
		return null;
	}

	/**
	 * 返回值：
	 * <ul>
	 * <li>如果两个条件包含相同数量的 HTTP 请求方法，则返回 0
	 * <li>如果 "this" 实例包含 HTTP 请求方法而 "other" 不包含，则小于 0
	 * <li>如果 "other" 包含 HTTP 请求方法而 "this" 不包含，则大于 0
	 * </ul>
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，因此每个实例仅包含匹配的 HTTP 请求方法，或者为空。
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, ServerWebExchange exchange) {
		if (other.methods.size() != this.methods.size()) {
			return other.methods.size() - this.methods.size();
		}
		else if (this.methods.size() == 1) {
			if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
				return -1;
			}
			else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
				return 1;
			}
		}
		return 0;
	}

}
