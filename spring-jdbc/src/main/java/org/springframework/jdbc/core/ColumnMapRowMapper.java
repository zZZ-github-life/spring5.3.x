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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * {@link RowMapper}实现，为每行创建一个{@code java.util.Map}，
 * 将所有列表示为键值对：每个列一个条目，列名作为键。
 *
 * <p>要使用的Map实现和列Map中每列使用的键可以分别通过重写
 * {@link #createColumnMap}和{@link #getColumnKey}来定制。
 *
 * <p><b>注意：</b>默认情况下，{@code ColumnMapRowMapper}将尝试构建一个
 * 大小写不敏感的Linked Map，以保留列顺序并允许使用任何大小写的列名。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see JdbcTemplate#queryForList(String)
 * @see JdbcTemplate#queryForMap(String)
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {

	@Override
	public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			String column = JdbcUtils.lookupColumnName(rsmd, i);
			mapOfColumnValues.putIfAbsent(getColumnKey(column), getColumnValue(rs, i));
		}
		return mapOfColumnValues;
	}

	/**
	 * 创建用作列映射的Map实例。
	 * <p>默认情况下，将创建一个大小写不敏感的Linked Map。
	 * @param columnCount the column count, to be used as initial
	 * capacity for the Map
	 * @return the new Map instance
	 * @see org.springframework.util.LinkedCaseInsensitiveMap
	 */
	protected Map<String, Object> createColumnMap(int columnCount) {
		return new LinkedCaseInsensitiveMap<>(columnCount);
	}

	/**
	 * 确定在列Map中给定列使用的键。
	 * <p>默认情况下，返回提供的列名不做修改。
	 * @param columnName the column name as returned by the ResultSet
	 * @return the column key to use
	 * @see java.sql.ResultSetMetaData#getColumnName
	 */
	protected String getColumnKey(String columnName) {
		return columnName;
	}

	/**
	 * 检索指定列的JDBC对象值。
	 * <p>默认实现使用{@code getObject}方法。
	 * 此外，此实现包含一个"技巧"来绕过Oracle为其TIMESTAMP数据类型返回非标准对象的问题。
	 * @param rs the ResultSet holding the data
	 * @param index the column index
	 * @return the Object returned
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}

}
