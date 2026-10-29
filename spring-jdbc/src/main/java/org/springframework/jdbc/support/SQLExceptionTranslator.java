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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 策略接口，用于在 {@link SQLException SQLExceptions} 与 Spring 的、与数据访问策略
 * 无关的 {@link DataAccessException} 层次结构之间进行转换。
 *
 * <p>实现可以是通用的（例如使用 JDBC 的
 * {@link java.sql.SQLException#getSQLState() SQLState} 代码），也可以是完全专有的
 * （例如使用 Oracle 错误码）以获得更高的精确度。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.dao.DataAccessException
 */
@FunctionalInterface
public interface SQLExceptionTranslator {

	/**
	 * 将给定的 {@link SQLException} 转换为通用的 {@link DataAccessException}。
	 * <p>返回的 DataAccessException 应当以原始的 {@code SQLException} 作为根本原因。
	 * 不过，客户端代码通常不应依赖这一点，因为 DataAccessException 也可能由其他
	 * 资源 API 引起。尽管如此，在预期发生基于 JDBC 的访问时，通过
	 * {@code getRootCause() instanceof SQLException} 检查（以及随后的强制类型转换）
	 * 被认为是可靠的。
	 * @param task readable text describing the task being attempted
	 * @param sql the SQL query or update that caused the problem (if known)
	 * @param ex the offending {@code SQLException}
	 * @return the DataAccessException wrapping the {@code SQLException},
	 * or {@code null} if no specific translation could be applied
	 * @see org.springframework.dao.DataAccessException#getRootCause()
	 */
	@Nullable
	DataAccessException translate(String task, @Nullable String sql, SQLException ex);

}
