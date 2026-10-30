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
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的两个核心回调接口之一。
 * 此接口根据JdbcTemplate类提供的连接创建PreparedStatement。
 * 实现负责提供SQL和任何必要的参数。
 *
 * <p>实现<i>不需要</i>关注其尝试的操作可能抛出的SQLException。
 * JdbcTemplate类会适当地捕获并处理SQLException。
 *
 * <p>如果PreparedStatementCreator能够提供用于创建PreparedStatement的SQL，
 * 则它还应该实现SqlProvider接口。这允许在出现异常时提供更好的上下文信息。
 *
 * @author Rod Johnson
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 * @see JdbcTemplate#query(PreparedStatementCreator, RowCallbackHandler)
 * @see JdbcTemplate#update(PreparedStatementCreator)
 * @see SqlProvider
 */
@FunctionalInterface
public interface PreparedStatementCreator {

	/**
	 * 在此连接中创建语句。允许实现使用PreparedStatement。
	 * JdbcTemplate将关闭创建的语句。
	 * @param con 用于创建语句的连接
	 * @return 预编译语句
	 * @throws SQLException 无需捕获本方法实现中可能抛出的 SQLException。
	 * JdbcTemplate 类会处理它们。
	 */
	PreparedStatement createPreparedStatement(Connection con) throws SQLException;

}
