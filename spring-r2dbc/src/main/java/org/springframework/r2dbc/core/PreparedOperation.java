/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.core;

import java.util.function.Supplier;

import org.springframework.r2dbc.core.binding.BindTarget;

/**
 * {@link QueryOperation} 的扩展，用于带绑定参数的预编译 SQL 查询 {@link Supplier}。
 * 包含可{@link #bindTo 绑定}到 {@link BindTarget} 的参数绑定。
 *
 * <p>可通过 {@link org.springframework.r2dbc.core.DatabaseClient} 执行。
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> underlying operation source.
 * @see org.springframework.r2dbc.core.DatabaseClient#sql(Supplier)
 */
public interface PreparedOperation<T> extends QueryOperation {

	/**
	 * 返回底层的查询源。
	 * @return the query source, such as a statement/criteria object
	 */
	T getSource();

	/**
	 * 将绑定应用到 {@link BindTarget}。
	 * @param target the target to apply bindings to
	 */
	void bindTo(BindTarget target);

}
