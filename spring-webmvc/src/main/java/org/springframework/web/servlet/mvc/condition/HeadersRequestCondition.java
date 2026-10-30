/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;

/**
 * 逻辑合取（{@code ' && '}）请求条件，用于将请求与
 * 一组头表达式进行匹配，语法在 {@link RequestMapping#headers()} 中定义。
 *
 * <p>传递给构造函数的头名称为 'Accept' 或
 * 'Content-Type' 的表达式将被忽略。这些请参见 {@link ConsumesRequestCondition} 和
 * {@link ProducesRequestCondition}。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();


	private final Set<HeaderExpression> expressions;


	/**
	 * 根据给定的头表达式创建一个新实例。头名称为
	 * 'Accept' 或 'Content-Type' 的表达式将被忽略。这些请参见 {@link ConsumesRequestCondition}
	 * 和 {@link ProducesRequestCondition}。
	 * @param headers 使用 {@link RequestMapping#headers()} 中定义语法的媒体类型表达式；
	 * 如果为 0，则该条件将匹配每个请求
	 */
	public HeadersRequestCondition(String... headers) {
		this.expressions = parseExpressions(headers);
	}

	private static Set<HeaderExpression> parseExpressions(String... headers) {
		Set<HeaderExpression> result = null;
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
					continue;
				}
				result = (result != null ? result : new LinkedHashSet<>(headers.length));
				result.add(expr);
			}
		}
		return (result != null ? result : Collections.emptySet());
	}

	private HeadersRequestCondition(Set<HeaderExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * 返回包含的请求头表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<HeaderExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的
	 * 头表达式并集。
	 */
	@Override
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		Set<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		return new HeadersRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有表达式，则返回 "this" 实例；
	 * 否则返回 {@code null}。
	 */
	@Override
	@Nullable
	public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		for (HeaderExpression expression : this.expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 基于头表达式与另一个条件进行比较。如果某个条件具有以下特征，
	 * 则认为它是更具体的匹配：
	 * <ol>
	 * <li>表达式数量更多。
	 * <li>具有具体值的非否定表达式数量更多。
	 * </ol>
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，且每个实例
	 * 仅包含匹配的头表达式，或者为空。
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, HttpServletRequest request) {
		int result = other.expressions.size() - this.expressions.size();
		if (result != 0) {
			return result;
		}
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}

	private long getValueMatchCount(Set<HeaderExpression> expressions) {
		long count = 0;
		for (HeaderExpression e : expressions) {
			if (e.getValue() != null && !e.isNegated()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * 解析单个头表达式并将其与请求进行匹配。
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		HeaderExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return false;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return (request.getHeader(this.name) != null);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getHeader(this.name));
		}
	}

}
