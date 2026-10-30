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

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;

/**
 * 逻辑合取（{@code ' && '}）请求条件，用于将请求与一组参数表达式匹配，语法定义参见 {@link RequestMapping#params()}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;


	/**
	 * 根据给定的参数表达式创建新实例。
	 * @param params 表达式，语法由 {@link RequestMapping#params()} 定义；
	 * 如果为 0 个，则该条件将匹配所有请求。
	 */
	public ParamsRequestCondition(String... params) {
		this.expressions = parseExpressions(params);
	}

	private static Set<ParamExpression> parseExpressions(String... params) {
		if (ObjectUtils.isEmpty(params)) {
			return Collections.emptySet();
		}
		Set<ParamExpression> result = new LinkedHashSet<>(params.length);
		for (String param : params) {
			result.add(new ParamExpression(param));
		}
		return result;
	}

	private ParamsRequestCondition(Set<ParamExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * 返回包含的请求参数表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的参数表达式并集。
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有参数表达式，则返回 "this" 实例；否则返回 {@code null}。
	 */
	@Override
	public ParamsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		for (ParamExpression expression : this.expressions) {
			if (!expression.match(exchange)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 基于参数表达式与另一个条件进行比较。如果条件具有以下特征，则被视为更具体的匹配：
	 * <ol>
	 * <li>表达式数量更多。
	 * <li>具有具体值的非否定表达式数量更多。
	 * </ol>
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，并且每个实例仅包含匹配的参数表达式，或者为空。
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, ServerWebExchange exchange) {
		int result = other.expressions.size() - this.expressions.size();
		if (result != 0) {
			return result;
		}
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}

	private long getValueMatchCount(Set<ParamExpression> expressions) {
		long count = 0;
		for (ParamExpression e : expressions) {
			if (e.getValue() != null && !e.isNegated()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * 解析单个参数表达式并将其与请求匹配。
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		ParamExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(ServerWebExchange exchange) {
			return exchange.getRequest().getQueryParams().containsKey(this.name);
		}

		@Override
		protected boolean matchValue(ServerWebExchange exchange) {
			return (this.value != null &&
					this.value.equals(exchange.getRequest().getQueryParams().getFirst(this.name)));
		}
	}

}
