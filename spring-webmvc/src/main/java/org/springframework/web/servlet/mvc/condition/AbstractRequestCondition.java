/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * {@link RequestCondition} 类型的基类，为
 * {@link #equals(Object)}、{@link #hashCode()} 和 {@link #toString()} 提供实现。
 * @author Rossen Stoyanchev
 * @since 3.1
 * @param <T> 该 RequestCondition 可与之组合和比较的对象类型
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

	/**
	 * 指示此条件是否为空，即是否
	 * 包含任何离散项。
	 * @return 为空时为 {@code true}；否则为 {@code false}
	 */
	public boolean isEmpty() {
		return getContent().isEmpty();
	}

	/**
	 * 返回组成请求条件的各个离散项。
	 * <p>例如 URL 模式、HTTP 请求方法、参数表达式等。
	 * @return 对象集合（永不为 {@code null}）
	 */
	protected abstract Collection<?> getContent();

	/**
	 * 打印内容离散项时使用的表示法。
	 * <p>例如 URL 模式使用 {@code " || "}，
	 * 参数表达式使用 {@code " && "}。
	 */
	protected abstract String getToStringInfix();


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return getContent().equals(((AbstractRequestCondition<?>) other).getContent());
	}

	@Override
	public int hashCode() {
		return getContent().hashCode();
	}

	@Override
	public String toString() {
		String infix = getToStringInfix();
		StringJoiner joiner = new StringJoiner(infix, "[", "]");
		for (Object expression : getContent()) {
			joiner.add(expression.toString());
		}
		return joiner.toString();
	}

}
