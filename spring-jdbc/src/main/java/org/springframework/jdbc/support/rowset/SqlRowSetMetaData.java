/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.support.rowset;

import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * Spring {@link SqlRowSet} 的元数据接口，类似于 JDBC 的
 * {@link java.sql.ResultSetMetaData}。
 *
 * <p>它与标准 JDBC ResultSetMetaData 的主要区别在于这里从不抛出
 * {@link java.sql.SQLException}。这样使用 SqlRowSetMetaData 时
 * 就无需处理受检异常。SqlRowSetMetaData 会改为（在适当的时候）
 * 抛出 Spring 的 {@link InvalidResultSetAccessException}。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.2
 * @see SqlRowSet#getMetaData()
 * @see java.sql.ResultSetMetaData
 * @see org.springframework.jdbc.InvalidResultSetAccessException
 */
public interface SqlRowSetMetaData {

	/**
	 * 检索作为指定列来源的表的目录名称。
	 * @param columnIndex the index of the column
	 * @return the catalog name
	 * @see java.sql.ResultSetMetaData#getCatalogName(int)
	 */
	String getCatalogName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列将映射到的完全限定类。
	 * @param columnIndex the index of the column
	 * @return the class name as a String
	 * @see java.sql.ResultSetMetaData#getColumnClassName(int)
	 */
	String getColumnClassName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索 RowSet 中的列数。
	 * @return the number of columns
	 * @see java.sql.ResultSetMetaData#getColumnCount()
	 */
	int getColumnCount() throws InvalidResultSetAccessException;

	/**
	 * 返回结果集所代表的表的列名。
	 * @return the column names
	 */
	String[] getColumnNames() throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的最大宽度。
	 * @param columnIndex the index of the column
	 * @return the width of the column
	 * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
	 */
	int getColumnDisplaySize(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的推荐列标题。
	 * @param columnIndex the index of the column
	 * @return the column title
	 * @see java.sql.ResultSetMetaData#getColumnLabel(int)
	 */
	String getColumnLabel(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的列名。
	 * @param columnIndex the index of the column
	 * @return the column name
	 * @see java.sql.ResultSetMetaData#getColumnName(int)
	 */
	String getColumnName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的 SQL 类型代码。
	 * @param columnIndex the index of the column
	 * @return the SQL type code
	 * @see java.sql.ResultSetMetaData#getColumnType(int)
	 * @see java.sql.Types
	 */
	int getColumnType(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的 DBMS 特定类型名称。
	 * @param columnIndex the index of the column
	 * @return the type name
	 * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
	 */
	String getColumnTypeName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的精度。
	 * @param columnIndex the index of the column
	 * @return the precision
	 * @see java.sql.ResultSetMetaData#getPrecision(int)
	 */
	int getPrecision(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的小数位数。
	 * @param columnIndex the index of the column
	 * @return the scale
	 * @see java.sql.ResultSetMetaData#getScale(int)
	 */
	int getScale(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索作为指定列来源的表的模式名称。
	 * @param columnIndex the index of the column
	 * @return the schema name
	 * @see java.sql.ResultSetMetaData#getSchemaName(int)
	 */
	String getSchemaName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索作为指定列来源的表的名称。
	 * @param columnIndex the index of the column
	 * @return the name of the table
	 * @see java.sql.ResultSetMetaData#getTableName(int)
	 */
	String getTableName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指示指定列的大小写是否重要。
	 * @param columnIndex the index of the column
	 * @return true if the column is case-sensitive, false otherwise
	 * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
	 */
	boolean isCaseSensitive(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指示指定列是否包含货币值。
	 * @param columnIndex the index of the column
	 * @return true if the value is a currency value, false otherwise
	 * @see java.sql.ResultSetMetaData#isCurrency(int)
	 */
	boolean isCurrency(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指示指定列是否包含带符号的数值。
	 * @param columnIndex the index of the column
	 * @return true if the column contains a signed number, false otherwise
	 * @see java.sql.ResultSetMetaData#isSigned(int)
	 */
	boolean isSigned(int columnIndex) throws InvalidResultSetAccessException;

}
