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

package org.springframework.web.servlet.mvc.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

/**
 * 逻辑析取（' || '）请求条件，用于将请求
 * 与一组 {@link RequestMethod RequestMethods} 进行匹配。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

	/** 按 HTTP 方法缓存，用于从 getMatchingCondition 返回现成的实例。 */
	private static final Map<String, RequestMethodsRequestCondition> requestMethodConditionCache;

	static {
		requestMethodConditionCache = CollectionUtils.newHashMap(RequestMethod.values().length);
		for (RequestMethod method : RequestMethod.values()) {
			requestMethodConditionCache.put(method.name(), new RequestMethodsRequestCondition(method));
		}
	}


	private final Set<RequestMethod> methods;


	/**
	 * 使用给定的请求方法创建一个新实例。
	 * @param requestMethods 0 个或多个 HTTP 请求方法；如果为 0，则该条件将匹配每个请求
	 */
	public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
		this.methods = (ObjectUtils.isEmpty(requestMethods) ?
				Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
	}

	/**
	 * 用于组合条件时内部使用的私有构造函数。
	 */
	private RequestMethodsRequestCondition(Set<RequestMethod> methods) {
		this.methods = methods;
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
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的
	 * HTTP 请求方法的并集。
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
	 * 检查是否有任何 HTTP 请求方法匹配给定请求，
	 * 并返回一个仅包含匹配的 HTTP 请求方法的实例。
	 * @param request 当前请求
	 * @return 如果条件为空（除非请求方法为 HTTP OPTIONS），则为同一实例；
	 * 或包含匹配请求方法的新条件；
	 * 如果无匹配，或条件为空且请求方法为 OPTIONS，则为 {@code null}。
	 */
	@Override
	@Nullable
	public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return matchPreFlight(request);
		}

		if (getMethods().isEmpty()) {
			if (RequestMethod.OPTIONS.name().equals(request.getMethod()) &&
					!DispatcherType.ERROR.equals(request.getDispatcherType())) {

				return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
			}
			return this;
		}

		return matchRequestMethod(request.getMethod());
	}

	/**
	 * 对于预检请求，匹配实际将要发生的请求。
	 * 因此，空条件即为匹配；否则尝试匹配
	 * "Access-Control-Request-Method" 头中的 HTTP 方法。
	 */
	@Nullable
	private RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
		if (getMethods().isEmpty()) {
			return this;
		}
		String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
		return matchRequestMethod(expectedMethod);
	}

	@Nullable
	private RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
		RequestMethod requestMethod;
		try {
			requestMethod = RequestMethod.valueOf(httpMethodValue);
			if (getMethods().contains(requestMethod)) {
				return requestMethodConditionCache.get(httpMethodValue);
			}
			if (requestMethod.equals(RequestMethod.HEAD) && getMethods().contains(RequestMethod.GET)) {
				return requestMethodConditionCache.get(HttpMethod.GET.name());
			}
		}
		catch (IllegalArgumentException ex) {
			// Custom request method
		}
		return null;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件包含的 HTTP 请求方法数量相同，则为 0
	 * <li>如果 "this" 实例有 HTTP 请求方法而 "other" 没有，则小于 0
	 * <li>如果 "other" 有 HTTP 请求方法而 "this" 没有，则大于 0
	 * </ul>
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，因此每个实例
	 * 仅包含匹配的 HTTP 请求方法，或者为空。
	 */
	@Override
	public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
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
