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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * null Bean 实例的内部表示，例如从 {@link FactoryBean#getObject()} 或工厂方法返回的 {@code null}
 * 值。
 *
 * <p>每个这样的 null Bean 都由一个专用的 {@code NullBean} 实例表示，这些实例彼此不相等，从而唯一地
 * 区分从 {@link org.springframework.beans.factory.BeanFactory#getBean} 的各种变体返回的每个 Bean。
 * 然而，每个这样的实例对 {@code #equals(null)} 返回 {@code true}，并且 {@code #toString()} 返回
 * "null"，这正是从外部测试它们的方式（因为此类本身不是 public）。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
