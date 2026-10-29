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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的三个核心回调接口之一。
 * 此接口根据JdbcTemplate类提供的连接创建CallableStatement。
 * 实现负责提供SQL和任何必要的参数。
 *
 * <p>实现<i>不需要</i>关注其尝试的操作可能抛出的SQLException。
 * JdbcTemplate类会适当地捕获并处理SQLException。
 *
 * <p>如果PreparedStatementCreator能够提供用于创建PreparedStatement的SQL，
 * 则它还应该实现SqlProvider接口。这允许在出现异常时提供更好的上下文信息。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 * @see JdbcTemplate#call
 * @see SqlProvider
 */
@FunctionalInterface
public interface CallableStatementCreator {

	/**
	 * 在此连接中创建可调用语句。允许实现使用CallableStatement。
	 * @param con the Connection to use to create statement
	 * @return a callable statement
	 * @throws SQLException there is no need to catch SQLExceptions
	 * that may be thrown in the implementation of this method.
	 * The JdbcTemplate class will handle them.
	 */
	CallableStatement createCallableStatement(Connection con) throws SQLException;

}
