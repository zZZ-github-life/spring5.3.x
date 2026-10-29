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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 逻辑合取（{@code ' && '}）请求条件，用于将请求与一组头表达式匹配，语法定义参见 {@link RequestMapping#headers()}。
 *
 * <p>传递给构造函数且头名称为 'Accept' 或 'Content-Type' 的表达式将被忽略。相关处理请参见 {@link ConsumesRequestCondition} 和 {@link ProducesRequestCondition}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();


	private final Set<HeaderExpression> expressions;


	/**
	 * 根据给定的头表达式创建新实例。头名称为 'Accept' 或 'Content-Type' 的表达式将被忽略。相关处理请参见 {@link ConsumesRequestCondition} 和 {@link ProducesRequestCondition}。
	 * @param headers media type expressions with syntax defined in {@link RequestMapping#headers()};
	 * if 0, the condition will match to every request
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
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的头表达式并集。
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
	 * 如果请求匹配所有表达式，则返回 "this" 实例；否则返回 {@code null}。
	 */
	@Override
	@Nullable
	public HeadersRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return PRE_FLIGHT_MATCH;
		}
		for (HeaderExpression expression : this.expressions) {
			if (!expression.match(exchange)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 基于头表达式与另一个条件进行比较。如果条件具有以下特征，则被视为更具体的匹配：
	 * <ol>
	 * <li>表达式数量更多。
	 * <li>具有具体值的非否定表达式数量更多。
	 * </ol>
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，并且每个实例仅包含匹配的头表达式，或者为空。
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, ServerWebExchange exchange) {
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
	 * 解析单个头表达式并将其与请求匹配。
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		public HeaderExpression(String expression) {
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
		protected boolean matchName(ServerWebExchange exchange) {
			return (exchange.getRequest().getHeaders().get(this.name) != null);
		}

		@Override
		protected boolean matchValue(ServerWebExchange exchange) {
			return (this.value != null && this.value.equals(exchange.getRequest().getHeaders().getFirst(this.name)));
		}
	}

}
