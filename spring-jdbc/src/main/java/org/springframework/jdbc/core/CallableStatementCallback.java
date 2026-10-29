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

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 对操作CallableStatement的代码的通用回调接口。
 * 允许在单个CallableStatement上执行任意数量的操作，
 * 例如单次execute调用或具有不同参数的重复execute调用。
 *
 * <p>由JdbcTemplate内部使用，但也对应用程序代码有用。
 * 请注意，传入的CallableStatement可以由框架创建，
 * 也可以由自定义的CallableStatementCreator创建。然而，后者几乎不需要，
 * 因为大多数自定义回调操作都执行更新操作，在这种情况下标准的CallableStatement就足够了。
 * 自定义操作总是自己设置参数值，因此也不需要CallableStatementCreator功能。
 *
 * @author Juergen Hoeller
 * @since 16.03.2004
 * @param <T> the result type
 * @see JdbcTemplate#execute(String, CallableStatementCallback)
 * @see JdbcTemplate#execute(CallableStatementCreator, CallableStatementCallback)
 */
@FunctionalInterface
public interface CallableStatementCallback<T> {

	/**
	 * 由{@code JdbcTemplate.execute}使用活跃的JDBC CallableStatement调用。
	 * 不需要关心关闭Statement或Connection，也不需要处理事务：
	 * 这些都由Spring的JdbcTemplate处理。
	 *
	 * <p><b>注意：</b>在回调实现的finally块中应关闭打开的任何ResultSet。
	 * Spring将在回调返回后关闭Statement对象，但这并不意味着ResultSet资源也会被关闭：
	 * Statement对象可能被连接池池化，{@code close}调用只是将对象返回到池中，
	 * 而不会物理关闭资源。
	 *
	 * <p>如果在没有线程绑定的JDBC事务（由DataSourceTransactionManager发起）的情况下调用，
	 * 代码将在JDBC连接上以其事务语义直接执行。如果JdbcTemplate配置为使用支持JTA的DataSource，
	 * 则如果有活跃的JTA事务，JDBC连接和回调代码将具有事务性。
	 *
	 * <p>允许返回在回调内创建的结果对象，即领域对象或领域对象集合。
	 * 抛出的RuntimeException被视为应用程序异常：它会传播到模板的调用者。
	 * @param cs active JDBC CallableStatement
	 * @return a result object, or {@code null} if none
	 * @throws SQLException if thrown by a JDBC method, to be auto-converted
	 * into a DataAccessException by an SQLExceptionTranslator
	 * @throws DataAccessException in case of custom exceptions
	 */
	@Nullable
	T doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException;

}
