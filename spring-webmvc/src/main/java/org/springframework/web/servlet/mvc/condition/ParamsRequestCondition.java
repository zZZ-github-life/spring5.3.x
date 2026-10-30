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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

/**
 * 逻辑合取（{@code ' && '}）请求条件，用于将请求与
 * 一组参数表达式进行匹配，语法在 {@link RequestMapping#params()} 中定义。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;


	/**
	 * 根据给定的参数表达式创建一个新实例。
	 * @param params 使用 {@link RequestMapping#params()} 中定义语法的表达式；
	 * 如果为 0，则该条件将匹配每个请求。
	 */
	public ParamsRequestCondition(String... params) {
		this.expressions = parseExpressions(params);
	}

	private static Set<ParamExpression> parseExpressions(String... params) {
		if (ObjectUtils.isEmpty(params)) {
			return Collections.emptySet();
		}
		Set<ParamExpression> expressions = new LinkedHashSet<>(params.length);
		for (String param : params) {
			expressions.add(new ParamExpression(param));
		}
		return expressions;
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
	 * 返回一个新实例，其中包含 "this" 和 "other" 实例的
	 * 参数表达式并集。
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
	 * 如果请求匹配所有参数表达式，则返回 "this" 实例；
	 * 否则返回 {@code null}。
	 */
	@Override
	@Nullable
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : this.expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * 基于参数表达式与另一个条件进行比较。如果某个条件具有以下特征，
	 * 则认为它是更具体的匹配：
	 * <ol>
	 * <li>表达式数量更多。
	 * <li>具有具体值的非否定表达式数量更多。
	 * </ol>
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，且每个实例
	 * 仅包含匹配的参数表达式，或者为空。
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
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
	 * 解析单个参数表达式并将其与请求进行匹配。
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		private final Set<String> namesToMatch = new HashSet<>(WebUtils.SUBMIT_IMAGE_SUFFIXES.length + 1);


		ParamExpression(String expression) {
			super(expression);
			this.namesToMatch.add(getName());
			for (String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
				this.namesToMatch.add(getName() + suffix);
			}
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
		protected boolean matchName(HttpServletRequest request) {
			for (String current : this.namesToMatch) {
				if (request.getParameterMap().get(current) != null) {
					return true;
				}
			}
			return request.getParameterMap().containsKey(this.name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
		}
	}

}
