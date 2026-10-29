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

package org.springframework.jca.cci.core;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 对操作 CCI Connection 的代码的通用回调接口。
 * 允许在单个 Connection 上使用任何类型和数量的 Interaction 执行任意数量的操作。
 *
 * <p>这对于委托给期望操作 Connection 并抛出 ResourceException 的现有数据访问代码特别有用。
 * 对于新编写的代码，强烈建议使用 CciTemplate 更具体的 {@code execute} 变体。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @param <T> the result type
 * @see CciTemplate#execute(ConnectionCallback)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, javax.resource.cci.Record)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@FunctionalInterface
public interface ConnectionCallback<T> {

	/**
	 * 由 {@code CciTemplate.execute} 使用活动的 CCI Connection 调用。
	 * 无需关心激活或关闭 Connection，也无需处理事务。
	 * <p>如果在没有线程绑定的 CCI 事务（由 CciLocalTransactionManager 发起）的情况下调用，
	 * 代码将简单地在 CCI Connection 上以其事务语义执行。如果 CciTemplate 配置为使用
	 * 支持 JTA 的 ConnectionFactory，则当 JTA 事务处于活动状态时，CCI Connection 和
	 * 回调代码将是事务性的。
	 * <p>允许返回在回调中创建的结果对象，即领域对象或领域对象的集合。
	 * 注意对单步操作有特殊支持：参见 {@code CciTemplate.execute} 的各种变体。
	 * 抛出的 RuntimeException 被视为应用程序异常：它会传播到模板的调用者。
	 * @param connection active CCI Connection
	 * @param connectionFactory the CCI ConnectionFactory that the Connection was
	 * created with (gives access to RecordFactory and ResourceAdapterMetaData)
	 * @return a result object, or {@code null} if none
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @throws SQLException if thrown by a ResultSet method, to be auto-converted
	 * to a DataAccessException
	 * @throws DataAccessException in case of custom exceptions
	 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
	 * @see javax.resource.cci.ConnectionFactory#getMetaData()
	 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
	 */
	@Nullable
	T doInConnection(Connection connection, ConnectionFactory connectionFactory)
			throws ResourceException, SQLException, DataAccessException;

}
