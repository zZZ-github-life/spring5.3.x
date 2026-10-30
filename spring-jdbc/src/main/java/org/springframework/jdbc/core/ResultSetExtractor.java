/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * {@link JdbcTemplate}的查询方法使用的回调接口。
 * 此接口的实现负责从{@link java.sql.ResultSet}中提取结果的实际工作，
 * 但不需要担心异常处理。{@link java.sql.SQLException SQLException}
 * 将被调用的JdbcTemplate捕获并处理。
 *
 * <p>此接口主要在JDBC框架内部使用。
 * 对于ResultSet处理，{@link RowMapper}通常是更简单的选择，
 * 它为每行映射一个结果对象，而不是为整个ResultSet映射一个结果对象。
 *
 * <p>注意：与{@link RowCallbackHandler}不同，ResultSetExtractor对象通常是无状态的，
 * 因此可以重复使用，只要它不访问有状态的资源（例如在流式传输LOB内容时的输出流）
 * 或在对象内部保持结果状态。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since April 24, 2003
 * @param <T> 结果类型
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.springframework.jdbc.core.support.AbstractLobStreamingResultSetExtractor
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {

	/**
	 * 实现必须实现此方法来处理整个ResultSet。
	 * @param rs 要从中提取数据的 ResultSet。实现不应关闭它：
	 * 它将由调用方 JdbcTemplate 关闭。
	 * @return 任意结果对象，如果没有则为 {@code null}
	 * （在后一种情况下，提取器通常是有状态的）。
	 * @throws SQLException 如果在获取列值或导航时遇到 SQLException
	 * （也就是说，无需捕获 SQLException）
	 * @throws DataAccessException 在出现自定义异常的情况下
	 */
	@Nullable
	T extractData(ResultSet rs) throws SQLException, DataAccessException;

}
