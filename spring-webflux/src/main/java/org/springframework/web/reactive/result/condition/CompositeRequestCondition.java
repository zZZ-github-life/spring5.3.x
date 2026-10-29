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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 通过委托给多个 {@code RequestCondition} 类型并使用逻辑合取（{@code ' && '}）来确保所有条件都匹配给定请求，从而实现 {@link RequestCondition} 契约。
 *
 * <p>当组合或比较 {@code CompositeRequestCondition} 实例时，要求 (a) 它们包含相同数量的条件，且 (b) 相同索引处的条件类型相同。构造函数可以接受 {@code null} 条件或完全不提供条件。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	private final RequestConditionHolder[] requestConditions;


	/**
	 * 创建包含 0 个或多个 {@code RequestCondition} 类型的实例。请务必使用相同数量的条件创建 {@code CompositeRequestCondition} 实例，以便它们可以比较和组合。可以提供 {@code null} 条件。
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		this.requestConditions = wrap(requestConditions);
	}

	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		for (int i = 0; i < rawConditions.length; i++) {
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		return wrappedConditions;
	}

	/**
	 * 此实例是否包含 0 个条件。
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * 返回底层条件，可能为空，但绝不会是 {@code null}。
	 */
	public List<RequestCondition<?>> getConditions() {
		return unwrap();
	}

	private List<RequestCondition<?>> unwrap() {
		List<RequestCondition<?>> result = new ArrayList<>();
		for (RequestConditionHolder holder : this.requestConditions) {
			result.add(holder.getCondition());
		}
		return result;
	}

	@Override
	protected Collection<?> getContent() {
		return (!isEmpty() ? getConditions() : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	private int getLength() {
		return this.requestConditions.length;
	}

	/**
	 * 如果其中一个实例为空，则返回另一个。
	 * <p>如果两个实例都包含条件，则在确保类型和数量相同后合并各个条件。
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		if (isEmpty() && other.isEmpty()) {
			return this;
		}
		else if (other.isEmpty()) {
			return this;
		}
		else if (isEmpty()) {
			return other;
		}
		else {
			assertNumberOfConditions(other);
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				() -> "Cannot combine CompositeRequestConditions with a different number of conditions. " +
				ObjectUtils.nullSafeToString(this.requestConditions) + " and " +
				ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * 委托给<em>所有</em>包含的条件来匹配请求，并返回得到的“匹配”条件实例。
	 * <p>空的 {@code CompositeRequestCondition} 匹配所有请求。
	 */
	@Override
	public CompositeRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(exchange);
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果其中一个实例为空，则另一个“获胜”。如果两个实例都包含条件，则按提供顺序进行比较。
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, ServerWebExchange exchange) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		}
		else if (isEmpty()) {
			return 1;
		}
		else if (other.isEmpty()) {
			return -1;
		}
		else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], exchange);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
