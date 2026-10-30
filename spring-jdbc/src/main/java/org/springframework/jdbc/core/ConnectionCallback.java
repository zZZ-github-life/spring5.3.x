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

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 对JDBC Connection进行操作的代码的通用回调接口。
 * 允许在单个Connection上执行任意数量的操作，
 * 使用任意类型和数量的Statement。
 *
 * <p>这对于委托给期望使用Connection并抛出SQLException的现有数据访问代码特别有用。
 * 对于新编写的代码，强烈建议使用JdbcTemplate更具体的操作，
 * 例如{@code query}或{@code update}变体。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @param <T> 结果类型
 * @see JdbcTemplate#execute(ConnectionCallback)
 * @see JdbcTemplate#query
 * @see JdbcTemplate#update
 */
@FunctionalInterface
public interface ConnectionCallback<T> {

	/**
	 * 由{@code JdbcTemplate.execute}使用活跃的JDBC Connection调用。
	 * 不需要关心激活或关闭Connection，也不需要处理事务。
	 * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager发起）的情况下调用，
	 * 代码将在JDBC连接上以其事务语义直接执行。如果JdbcTemplate配置为使用支持JTA的DataSource，
	 * 则如果有活跃的JTA事务，JDBC Connection和回调代码将具有事务性。
	 * <p>允许返回在回调内创建的结果对象，即领域对象或领域对象集合。
	 * 请注意，对单步操作有特殊支持：请参见{@code JdbcTemplate.queryForObject}等。
	 * 抛出的RuntimeException被视为应用程序异常：它会传播到模板的调用者。
	 * @param con 活动的 JDBC Connection
	 * @return 结果对象；如果没有则为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出，
	 * 将由 SQLExceptionTranslator 自动转换为 DataAccessException
	 * @throws DataAccessException 在自定义异常的情况下
	 * @see JdbcTemplate#queryForObject(String, Class)
	 * @see JdbcTemplate#queryForRowSet(String)
	 */
	@Nullable
	T doInConnection(Connection con) throws SQLException, DataAccessException;

}
