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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;

/**
 * 为设置标准{@code setObject}方法不支持的
 * 更复杂的数据库特定类型的值而实现的接口。
 * 这实际上是{@link org.springframework.jdbc.support.SqlValue}的扩展变体。
 *
 * <p>实现负责设置实际值的实际工作。它们必须实现回调方法{@code setTypeValue}，
 * 该方法可以抛出SQLException，由调用代码捕获并转换。
 * 此回调方法可以通过给定的PreparedStatement对象访问底层的Connection，
 * 如果需要创建任何数据库特定对象的话。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.sql.Types
 * @see java.sql.PreparedStatement#setObject
 * @see JdbcOperations#update(String, Object[], int[])
 * @see org.springframework.jdbc.support.SqlValue
 */
public interface SqlTypeValue {

	/**
	 * 表示未知（或未指定）SQL类型的常量。
	 * 如果原始操作方法未指定SQL类型，则传入{@code setTypeValue}。
	 * @see java.sql.Types
	 * @see JdbcOperations#update(String, Object[])
	 */
	int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;


	/**
	 * 在给定的PreparedStatement上设置类型值。
	 * @param ps 要操作的 PreparedStatement
	 * @param paramIndex 需要为其设置值的参数索引
	 * @param sqlType 正在设置的参数的 SQL 类型
	 * @param typeName 参数的类型名称（可选）
	 * @throws SQLException 如果在设置参数值时遇到 SQLException
	 * @see java.sql.Types
	 * @see java.sql.PreparedStatement#setObject
	 */
	void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException;

}
