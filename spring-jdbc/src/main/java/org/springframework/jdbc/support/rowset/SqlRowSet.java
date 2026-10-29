/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;

/**
 * {@link javax.sql.RowSet} 的镜像接口，表示 {@link java.sql.ResultSet} 数据的
 * 断开连接变体。
 *
 * <p>与标准 JDBC RowSet 的主要区别在于，这里从不抛出 {@link java.sql.SQLException}。
 * 这使得使用 SqlRowSet 时无需处理受检异常。SqlRowSet 会改为（在适当的时候）
 * 抛出 Spring 的 {@link InvalidResultSetAccessException}。
 *
 * <p>注意：此接口扩展了 {@code java.io.Serializable} 标记接口。
 * 实现类通常持有已断开连接的数据，鼓励尽可能地真正支持序列化。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.sql.RowSet
 * @see java.sql.ResultSet
 * @see org.springframework.jdbc.InvalidResultSetAccessException
 * @see org.springframework.jdbc.core.JdbcTemplate#queryForRowSet
 */
public interface SqlRowSet extends Serializable {

	/**
	 * 检索元数据，即此行集中各列的数量、类型和属性。
	 * @return a corresponding SqlRowSetMetaData instance
	 * @see java.sql.ResultSet#getMetaData()
	 */
	SqlRowSetMetaData getMetaData();

	/**
	 * 将给定的列标签映射到其列索引。
	 * @param columnLabel the name of the column
	 * @return the column index for the given column label
	 * @see java.sql.ResultSet#findColumn(String)
	 */
	int findColumn(String columnLabel) throws InvalidResultSetAccessException;


	// RowSet methods for extracting data values

	/**
	 * 以 BigDecimal 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return an BigDecimal object representing the column value
	 * @see java.sql.ResultSet#getBigDecimal(int)
	 */
	@Nullable
	BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 BigDecimal 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return an BigDecimal object representing the column value
	 * @see java.sql.ResultSet#getBigDecimal(String)
	 */
	@Nullable
	BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 boolean 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a boolean representing the column value
	 * @see java.sql.ResultSet#getBoolean(int)
	 */
	boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 boolean 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a boolean representing the column value
	 * @see java.sql.ResultSet#getBoolean(String)
	 */
	boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 byte 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a byte representing the column value
	 * @see java.sql.ResultSet#getByte(int)
	 */
	byte getByte(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 byte 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a byte representing the column value
	 * @see java.sql.ResultSet#getByte(String)
	 */
	byte getByte(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Date 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a Date object representing the column value
	 * @see java.sql.ResultSet#getDate(int)
	 */
	@Nullable
	Date getDate(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 Date 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a Date object representing the column value
	 * @see java.sql.ResultSet#getDate(String)
	 */
	@Nullable
	Date getDate(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Date 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Date object representing the column value
	 * @see java.sql.ResultSet#getDate(int, Calendar)
	 */
	@Nullable
	Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 以 Date 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Date object representing the column value
	 * @see java.sql.ResultSet#getDate(String, Calendar)
	 */
	@Nullable
	Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 以 Double 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a Double object representing the column value
	 * @see java.sql.ResultSet#getDouble(int)
	 */
	double getDouble(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 Double 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a Double object representing the column value
	 * @see java.sql.ResultSet#getDouble(String)
	 */
	double getDouble(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 float 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a float representing the column value
	 * @see java.sql.ResultSet#getFloat(int)
	 */
	float getFloat(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 float 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a float representing the column value
	 * @see java.sql.ResultSet#getFloat(String)
	 */
	float getFloat(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 int 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return an int representing the column value
	 * @see java.sql.ResultSet#getInt(int)
	 */
	int getInt(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 int 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return an int representing the column value
	 * @see java.sql.ResultSet#getInt(String)
	 */
	int getInt(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 long 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a long representing the column value
	 * @see java.sql.ResultSet#getLong(int)
	 */
	long getLong(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 long 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a long representing the column value
	 * @see java.sql.ResultSet#getLong(String)
	 */
	long getLong(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 String 形式检索当前行中指定列的值
	 * （适用于 NCHAR、NVARCHAR、LONGNVARCHAR 列）。
	 * @param columnIndex the column index
	 * @return a String representing the column value
	 * @since 4.1.3
	 * @see java.sql.ResultSet#getNString(int)
	 */
	@Nullable
	String getNString(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 String 形式检索当前行中指定列的值
	 * （适用于 NCHAR、NVARCHAR、LONGNVARCHAR 列）。
	 * @param columnLabel the column label
	 * @return a String representing the column value
	 * @since 4.1.3
	 * @see java.sql.ResultSet#getNString(String)
	 */
	@Nullable
	String getNString(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return an Object representing the column value
	 * @see java.sql.ResultSet#getObject(int)
	 */
	@Nullable
	Object getObject(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return an Object representing the column value
	 * @see java.sql.ResultSet#getObject(String)
	 */
	@Nullable
	Object getObject(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @param map a Map object containing the mapping from SQL types to Java types
	 * @return an Object representing the column value
	 * @see java.sql.ResultSet#getObject(int, Map)
	 */
	@Nullable
	Object getObject(int columnIndex,  Map<String, Class<?>> map) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @param map a Map object containing the mapping from SQL types to Java types
	 * @return an Object representing the column value
	 * @see java.sql.ResultSet#getObject(String, Map)
	 */
	@Nullable
	Object getObject(String columnLabel,  Map<String, Class<?>> map) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @param type the Java type to convert the designated column to
	 * @return an Object representing the column value
	 * @since 4.1.3
	 * @see java.sql.ResultSet#getObject(int, Class)
	 */
	@Nullable
	<T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException;

	/**
	 * 以 Object 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @param type the Java type to convert the designated column to
	 * @return an Object representing the column value
	 * @since 4.1.3
	 * @see java.sql.ResultSet#getObject(String, Class)
	 */
	@Nullable
	<T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException;

	/**
	 * 以 short 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a short representing the column value
	 * @see java.sql.ResultSet#getShort(int)
	 */
	short getShort(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 short 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a short representing the column value
	 * @see java.sql.ResultSet#getShort(String)
	 */
	short getShort(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 String 形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a String representing the column value
	 * @see java.sql.ResultSet#getString(int)
	 */
	@Nullable
	String getString(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 String 形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a String representing the column value
	 * @see java.sql.ResultSet#getString(String)
	 */
	@Nullable
	String getString(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Time 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a Time object representing the column value
	 * @see java.sql.ResultSet#getTime(int)
	 */
	@Nullable
	Time getTime(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 Time 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a Time object representing the column value
	 * @see java.sql.ResultSet#getTime(String)
	 */
	@Nullable
	Time getTime(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Time 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Time object representing the column value
	 * @see java.sql.ResultSet#getTime(int, Calendar)
	 */
	@Nullable
	Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 以 Time 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Time object representing the column value
	 * @see java.sql.ResultSet#getTime(String, Calendar)
	 */
	@Nullable
	Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 以 Timestamp 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @return a Timestamp object representing the column value
	 * @see java.sql.ResultSet#getTimestamp(int)
	 */
	@Nullable
	Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 以 Timestamp 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @return a Timestamp object representing the column value
	 * @see java.sql.ResultSet#getTimestamp(String)
	 */
	@Nullable
	Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 以 Timestamp 对象形式检索当前行中指定列的值。
	 * @param columnIndex the column index
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Timestamp object representing the column value
	 * @see java.sql.ResultSet#getTimestamp(int, Calendar)
	 */
	@Nullable
	Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 以 Timestamp 对象形式检索当前行中指定列的值。
	 * @param columnLabel the column label
	 * @param cal the Calendar to use in constructing the Date
	 * @return a Timestamp object representing the column value
	 * @see java.sql.ResultSet#getTimestamp(String, Calendar)
	 */
	@Nullable
	Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;


	// RowSet navigation methods

	/**
	 * 将游标移动到行集中给定的行号，即最后一行的后面。
	 * @param row the number of the row where the cursor should move
	 * @return {@code true} if the cursor is on the row set, {@code false} otherwise
	 * @see java.sql.ResultSet#absolute(int)
	 */
	boolean absolute(int row) throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的末尾。
	 * @see java.sql.ResultSet#afterLast()
	 */
	void afterLast() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的开头，即第一行的前面。
	 * @see java.sql.ResultSet#beforeFirst()
	 */
	void beforeFirst() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的第一行。
	 * @return {@code true} if the cursor is on a valid row, {@code false} otherwise
	 * @see java.sql.ResultSet#first()
	 */
	boolean first() throws InvalidResultSetAccessException;

	/**
	 * 检索当前行号。
	 * @return the current row number
	 * @see java.sql.ResultSet#getRow()
	 */
	int getRow() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否位于此行集最后一行之后。
	 * @return {@code true} if the cursor is after the last row, {@code false} otherwise
	 * @see java.sql.ResultSet#isAfterLast()
	 */
	boolean isAfterLast() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否位于此行集第一行之前。
	 * @return {@code true} if the cursor is before the first row, {@code false} otherwise
	 * @see java.sql.ResultSet#isBeforeFirst()
	 */
	boolean isBeforeFirst() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否位于此行集的第一行。
	 * @return {@code true} if the cursor is after the first row, {@code false} otherwise
	 * @see java.sql.ResultSet#isFirst()
	 */
	boolean isFirst() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否位于此行集的最后一行。
	 * @return {@code true} if the cursor is after the last row, {@code false} otherwise
	 * @see java.sql.ResultSet#isLast()
	 */
	boolean isLast() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的最后一行。
	 * @return {@code true} if the cursor is on a valid row, {@code false} otherwise
	 * @see java.sql.ResultSet#last()
	 */
	boolean last() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到下一行。
	 * @return {@code true} if the new row is valid, {@code false} if there are no more rows
	 * @see java.sql.ResultSet#next()
	 */
	boolean next() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到上一行。
	 * @return {@code true} if the new row is valid, {@code false} if it is off the row set
	 * @see java.sql.ResultSet#previous()
	 */
	boolean previous() throws InvalidResultSetAccessException;

	/**
	 * 将游标相对移动若干行，可以是正数或负数。
	 * @return {@code true} if the cursor is on a row, {@code false} otherwise
	 * @see java.sql.ResultSet#relative(int)
	 */
	boolean relative(int rows) throws InvalidResultSetAccessException;

	/**
	 * 报告最后读取的列是否为 SQL {@code NULL} 值。
	 * <p>注意，必须先调用某个 getter 方法，然后再调用
	 * {@code wasNull()} 方法。
	 * @return {@code true} if the most recent column retrieved was
	 * SQL {@code NULL}, {@code false} otherwise
	 * @see java.sql.ResultSet#wasNull()
	 */
	boolean wasNull() throws InvalidResultSetAccessException;

}
