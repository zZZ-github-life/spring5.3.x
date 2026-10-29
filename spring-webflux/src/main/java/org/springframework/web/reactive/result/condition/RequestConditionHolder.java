/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于持有 {@link RequestCondition} 的容器，适用于事先不知道请求条件类型的情况，例如自定义条件。由于此类也是 {@code RequestCondition} 的实现，因此它实际上装饰了所持有的请求条件，并允许以类型安全和空安全的方式与其他请求条件组合和比较。
 *
 * <p>当组合或比较两个 {@code RequestConditionHolder} 实例时，期望它们持有的条件类型相同。如果不同，则抛出 {@link ClassCastException}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	@Nullable
	private final RequestCondition<Object> condition;


	/**
	 * 创建新容器以包装给定的请求条件。
	 * @param requestCondition the condition to hold (may be {@code null})
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回持有的请求条件；如果未持有，则返回 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	@Override
	protected Collection<?> getContent() {
		return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * 在确保条件类型相同后，合并两个 RequestConditionHolder 实例持有的请求条件。如果其中一个容器为空，则返回另一个容器。
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		if (this.condition == null && other.condition == null) {
			return this;
		}
		else if (this.condition == null) {
			return other;
		}
		else if (other.condition == null) {
			return this;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * 获取所持有请求条件的匹配条件，并将其包装到新的 RequestConditionHolder 实例中。如果这是空容器，则返回同一容器实例。
	 */
	@Override
	public RequestConditionHolder getMatchingCondition(ServerWebExchange exchange) {
		if (this.condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(exchange);
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * 在确保条件类型相同后，比较两个 RequestConditionHolder 实例持有的请求条件。如果其中一个容器为空，则优先使用另一个容器。
	 */
	@Override
	public int compareTo(RequestConditionHolder other, ServerWebExchange exchange) {
		if (this.condition == null && other.condition == null) {
			return 0;
		}
		else if (this.condition == null) {
			return 1;
		}
		else if (other.condition == null) {
			return -1;
		}
		else {
			assertEqualConditionTypes(this.condition, other.condition);
			return this.condition.compareTo(other.condition, exchange);
		}
	}

	/**
	 * 确保所持有的请求条件类型相同。
	 */
	private void assertEqualConditionTypes(RequestCondition<?> cond1, RequestCondition<?> cond2) {
		Class<?> clazz = cond1.getClass();
		Class<?> otherClazz = cond2.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " vs " + otherClazz);
		}
	}

}
