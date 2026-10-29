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

package org.springframework.jdbc.support.rowset;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Spring {@link SqlRowSet} 接口的默认实现，它包装一个 {@link java.sql.ResultSet}，
 * 捕获所有 {@link SQLException SQLExceptions}，
 * 并将其转换为相应的 Spring {@link InvalidResultSetAccessException}。
 *
 * <p>如果希望 SqlRowSet 能以断开连接的方式使用，传入的 ResultSet 应已断开连接。
 * 这意味着通常你会传入一个实现了 ResultSet 接口的
 * {@code javax.sql.rowset.CachedRowSet}。
 *
 * <p>注意：自 JDBC 4.0 起已明确，任何使用 String 标识列的方法都应使用列标签。
 * 列标签通过 SQL 查询字符串中的 ALIAS 关键字指定。
 * 当查询未使用 ALIAS 时，默认标签为列名。
 * 大多数 JDBC ResultSet 实现遵循这一新模式，但也存在例外，
 * 例如 {@code com.sun.rowset.CachedRowSetImpl} 类只使用列名，忽略任何列标签。
 * 从 Spring 3.0.5 起，ResultSetWrappingSqlRowSet 会将列标签转换为正确的列索引，
 * 以便为 {@code com.sun.rowset.CachedRowSetImpl} 提供更好的支持，
 * 后者是 {@link org.springframework.jdbc.core.JdbcTemplate}
 * 在处理 RowSet 时使用的默认实现。
 *
 * <p>注意：此类通过 SqlRowSet 接口实现了 {@code java.io.Serializable} 标记接口，
 * 但仅当其中包含的已断开连接的 ResultSet/RowSet 可序列化时，它才真正可序列化。
 * 大多数 CachedRowSet 实现实际上是可序列化的，因此通常不会出现问题。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.2
 * @see java.sql.ResultSet
 * @see javax.sql.rowset.CachedRowSet
 * @see org.springframework.jdbc.core.JdbcTemplate#queryForRowSet
 */
public class ResultSetWrappingSqlRowSet implements SqlRowSet {

	/** 使用 Spring 1.2 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = -4688694393146734764L;


	private final ResultSet resultSet;

	private final SqlRowSetMetaData rowSetMetaData;

	private final Map<String, Integer> columnLabelMap;


	/**
	 * 为给定 ResultSet 创建一个新的 ResultSetWrappingSqlRowSet。
	 * @param resultSet a disconnected ResultSet to wrap
	 * (usually a {@code javax.sql.rowset.CachedRowSet})
	 * @throws InvalidResultSetAccessException if extracting
	 * the ResultSetMetaData failed
	 * @see javax.sql.rowset.CachedRowSet
	 * @see java.sql.ResultSet#getMetaData
	 * @see ResultSetWrappingSqlRowSetMetaData
	 */
	public ResultSetWrappingSqlRowSet(ResultSet resultSet) throws InvalidResultSetAccessException {
		this.resultSet = resultSet;
		try {
			this.rowSetMetaData = new ResultSetWrappingSqlRowSetMetaData(resultSet.getMetaData());
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
		try {
			ResultSetMetaData rsmd = resultSet.getMetaData();
			if (rsmd != null) {
				int columnCount = rsmd.getColumnCount();
				this.columnLabelMap = CollectionUtils.newHashMap(columnCount);
				for (int i = 1; i <= columnCount; i++) {
					String key = rsmd.getColumnLabel(i);
					// Make sure to preserve first matching column for any given name,
					// as defined in ResultSet's type-level javadoc (lines 81 to 83).
					if (!this.columnLabelMap.containsKey(key)) {
						this.columnLabelMap.put(key, i);
					}
				}
			}
			else {
				this.columnLabelMap = Collections.emptyMap();
			}
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}

	}


	/**
	 * 返回底层的 ResultSet
	 * （通常是 {@code javax.sql.rowset.CachedRowSet}）。
	 * @see javax.sql.rowset.CachedRowSet
	 */
	public final ResultSet getResultSet() {
		return this.resultSet;
	}

	/**
	 * @see java.sql.ResultSetMetaData#getCatalogName(int)
	 */
	@Override
	public final SqlRowSetMetaData getMetaData() {
		return this.rowSetMetaData;
	}

	/**
	 * @see java.sql.ResultSet#findColumn(String)
	 */
	@Override
	public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
		Integer columnIndex = this.columnLabelMap.get(columnLabel);
		if (columnIndex != null) {
			return columnIndex;
		}
		else {
			try {
				return this.resultSet.findColumn(columnLabel);
			}
			catch (SQLException se) {
				throw new InvalidResultSetAccessException(se);
			}
		}
	}


	// RowSet methods for extracting data values

	/**
	 * @see java.sql.ResultSet#getBigDecimal(int)
	 */
	@Override
	@Nullable
	public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getBigDecimal(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getBigDecimal(String)
	 */
	@Override
	@Nullable
	public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
		return getBigDecimal(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getBoolean(int)
	 */
	@Override
	public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getBoolean(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getBoolean(String)
	 */
	@Override
	public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
		return getBoolean(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getByte(int)
	 */
	@Override
	public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getByte(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getByte(String)
	 */
	@Override
	public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
		return getByte(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getDate(int)
	 */
	@Override
	@Nullable
	public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDate(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getDate(String)
	 */
	@Override
	@Nullable
	public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
		return getDate(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getDate(int, Calendar)
	 */
	@Override
	@Nullable
	public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDate(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getDate(String, Calendar)
	 */
	@Override
	@Nullable
	public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getDate(findColumn(columnLabel), cal);
	}

	/**
	 * @see java.sql.ResultSet#getDouble(int)
	 */
	@Override
	public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDouble(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getDouble(String)
	 */
	@Override
	public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
		return getDouble(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getFloat(int)
	 */
	@Override
	public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getFloat(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getFloat(String)
	 */
	@Override
	public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
		return getFloat(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getInt(int)
	 */
	@Override
	public int getInt(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getInt(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getInt(String)
	 */
	@Override
	public int getInt(String columnLabel) throws InvalidResultSetAccessException {
		return getInt(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getLong(int)
	 */
	@Override
	public long getLong(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getLong(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getLong(String)
	 */
	@Override
	public long getLong(String columnLabel) throws InvalidResultSetAccessException {
		return getLong(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getNString(int)
	 */
	@Override
	@Nullable
	public String getNString(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getNString(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getNString(String)
	 */
	@Override
	@Nullable
	public String getNString(String columnLabel) throws InvalidResultSetAccessException {
		return getNString(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getObject(int)
	 */
	@Override
	@Nullable
	public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getObject(String)
	 */
	@Override
	@Nullable
	public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getObject(int, Map)
	 */
	@Override
	@Nullable
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex, map);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getObject(String, Map)
	 */
	@Override
	@Nullable
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel), map);
	}

	/**
	 * @see java.sql.ResultSet#getObject(int, Class)
	 */
	@Override
	@Nullable
	public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex, type);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getObject(String, Class)
	 */
	@Override
	@Nullable
	public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel), type);
	}

	/**
	 * @see java.sql.ResultSet#getShort(int)
	 */
	@Override
	public short getShort(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getShort(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getShort(String)
	 */
	@Override
	public short getShort(String columnLabel) throws InvalidResultSetAccessException {
		return getShort(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getString(int)
	 */
	@Override
	@Nullable
	public String getString(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getString(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getString(String)
	 */
	@Override
	@Nullable
	public String getString(String columnLabel) throws InvalidResultSetAccessException {
		return getString(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getTime(int)
	 */
	@Override
	@Nullable
	public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTime(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getTime(String)
	 */
	@Override
	@Nullable
	public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
		return getTime(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getTime(int, Calendar)
	 */
	@Override
	@Nullable
	public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTime(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getTime(String, Calendar)
	 */
	@Override
	@Nullable
	public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getTime(findColumn(columnLabel), cal);
	}

	/**
	 * @see java.sql.ResultSet#getTimestamp(int)
	 */
	@Override
	@Nullable
	public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTimestamp(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getTimestamp(String)
	 */
	@Override
	@Nullable
	public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
		return getTimestamp(findColumn(columnLabel));
	}

	/**
	 * @see java.sql.ResultSet#getTimestamp(int, Calendar)
	 */
	@Override
	@Nullable
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTimestamp(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getTimestamp(String, Calendar)
	 */
	@Override
	@Nullable
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getTimestamp(findColumn(columnLabel), cal);
	}


	// RowSet navigation methods

	/**
	 * @see java.sql.ResultSet#absolute(int)
	 */
	@Override
	public boolean absolute(int row) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.absolute(row);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#afterLast()
	 */
	@Override
	public void afterLast() throws InvalidResultSetAccessException {
		try {
			this.resultSet.afterLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#beforeFirst()
	 */
	@Override
	public void beforeFirst() throws InvalidResultSetAccessException {
		try {
			this.resultSet.beforeFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#first()
	 */
	@Override
	public boolean first() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.first();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#getRow()
	 */
	@Override
	public int getRow() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getRow();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#isAfterLast()
	 */
	@Override
	public boolean isAfterLast() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isAfterLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#isBeforeFirst()
	 */
	@Override
	public boolean isBeforeFirst() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isBeforeFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#isFirst()
	 */
	@Override
	public boolean isFirst() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#isLast()
	 */
	@Override
	public boolean isLast() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#last()
	 */
	@Override
	public boolean last() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.last();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#next()
	 */
	@Override
	public boolean next() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.next();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#previous()
	 */
	@Override
	public boolean previous() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.previous();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#relative(int)
	 */
	@Override
	public boolean relative(int rows) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.relative(rows);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	/**
	 * @see java.sql.ResultSet#wasNull()
	 */
	@Override
	public boolean wasNull() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.wasNull();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

}
