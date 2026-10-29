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

package org.springframework.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.lang.Nullable;

/**
 * 供需要在 JNDI 上下文中执行操作（例如查找）的类实现的回调接口。这种回调方式有助于简化错误处理，错
 * 误处理由 JndiTemplate 类执行。这与 JdbcTemplate 的方式类似。
 *
 * <p>请注意，几乎不需要实现此回调接口，因为 JndiTemplate 通过便捷方法提供了所有常见的 JNDI 操作。
 *
 * @author Rod Johnson
 * @param <T> the resulting object type
 * @see JndiTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
@FunctionalInterface
public interface JndiCallback<T> {

	/**
	 * 对给定的 JNDI 上下文执行某些操作。
	 * <p>实现无需担心错误处理或清理，JndiTemplate 类会处理这些。
	 * @param ctx the current JNDI context
	 * @return a result object, or {@code null}
	 * @throws NamingException if thrown by JNDI methods
	 */
	@Nullable
	T doInContext(Context ctx) throws NamingException;

}

