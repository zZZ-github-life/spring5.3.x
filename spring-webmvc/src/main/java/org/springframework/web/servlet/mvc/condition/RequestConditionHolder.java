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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * {@link RequestCondition} 的持有者，适用于事先不知道请求条件类型的情况，
 * 例如自定义条件。由于此类也是 {@code RequestCondition} 的实现，它实际上
 * 装饰了所持有的请求条件，使其能够以类型安全且可空安全的方式
 * 与其他请求条件进行组合和比较。
 *
 * <p>当两个 {@code RequestConditionHolder} 实例相互组合或比较时，
 * 期望它们所持有的条件类型相同。
 * 如果类型不同，将抛出 {@link ClassCastException}。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	@Nullable
	private final RequestCondition<Object> condition;


	/**
	 * 创建一个新的持有者，用于包装给定的请求条件。
	 * @param requestCondition 要持有的条件，可为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回所持有的请求条件；如果未持有任何条件，则返回 {@code null}。
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
	 * 在确保两个 RequestConditionHolder 实例所持有的请求条件
	 * 类型相同后，将它们组合起来。
	 * 如果其中一个持有者为空，则返回另一个持有者。
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
	 * 确保所持有的请求条件类型相同。
	 */
	private void assertEqualConditionTypes(RequestCondition<?> thisCondition, RequestCondition<?> otherCondition) {
		Class<?> clazz = thisCondition.getClass();
		Class<?> otherClazz = otherCondition.getClass();
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
		}
	}

	/**
	 * 获取所持有的请求条件的匹配条件，并将其包装在一个
	 * 新的 RequestConditionHolder 实例中。或者，如果这是一个空的
	 * 持有者，则返回同一个持有者实例。
	 */
	@Override
	@Nullable
	public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
		if (this.condition == null) {
			return this;
		}
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * 在确保两个 RequestConditionHolder 实例所持有的请求条件
	 * 类型相同后，对它们进行比较。
	 * 如果其中一个持有者为空，则优先使用另一个持有者。
	 */
	@Override
	public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
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
			return this.condition.compareTo(other.condition, request);
		}
	}

}
