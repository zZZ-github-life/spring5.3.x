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

package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 通过委托给多个 {@code RequestCondition} 类型并使用逻辑合取
 * （{@code ' && '}）来确保所有条件都匹配给定请求，
 * 从而实现 {@link RequestCondition} 约定。
 *
 * <p>当组合或比较 {@code CompositeRequestCondition} 实例时，
 * 期望它们 (a) 包含相同数量的条件，且 (b) 相应索引处的
 * 条件类型相同。允许向构造函数提供 {@code null} 条件
 * 或完全不提供条件。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	private final RequestConditionHolder[] requestConditions;


	/**
	 * 创建一个包含 0 个或多个 {@code RequestCondition} 类型的实例。
	 * 重要的是，创建的 {@code CompositeRequestCondition} 实例应具有
	 * 相同数量的条件，这样才能进行比较和组合。
	 * 允许提供 {@code null} 条件。
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
	 * 返回底层条件（可能为空，但绝不为 {@code null}）。
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
	 * 如果两个实例都有条件，则在确保它们的类型和数量相同后，
	 * 合并各个条件。
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
	 * 委托给包含的<em>所有</em>条件来匹配请求，并返回
	 * 所得的“匹配”条件实例。
	 * <p>空的 {@code CompositeRequestCondition} 匹配所有请求。
	 */
	@Override
	@Nullable
	public CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(request);
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果其中一个实例为空，则另一个“胜出”。如果两个实例都有
	 * 条件，则按提供它们的顺序进行比较。
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
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
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
