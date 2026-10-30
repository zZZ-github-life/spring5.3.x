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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;

/**
 * RowCallbackHandler的实现。回调处理器的便利超类。
 * 一个实例只能使用一次。
 *
 * <p>我们可以单独使用它（例如，在测试用例中，确保我们的结果集具有有效的维度），
 * 也可以将其作为实际执行某些操作的回调处理器的超类使用，
 * 并将从其提供的维度信息中受益。
 *
 * <p>与JdbcTemplate一起使用的示例：
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 *
 * RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
 * jdbcTemplate.query("select * from user", countCallback);
 * int rowCount = countCallback.getRowCount();</pre>
 *
 * @author Rod Johnson
 * @since May 3, 2001
 */
public class RowCountCallbackHandler implements RowCallbackHandler {

	/** 到目前为止我们看到的行数。 */
	private int rowCount;

	/** 到目前为止我们看到的列数。 */
	private int columnCount;

	/**
	 * 从0开始索引。由ResultSetMetaData对象返回的列类型（如java.sql.Types）。
	 */
	@Nullable
	private int[] columnTypes;

	/**
	 * 从0开始索引。由ResultSetMetaData对象返回的列名。
	 */
	@Nullable
	private String[] columnNames;


	/**
	 * ResultSetCallbackHandler的实现。
	 * 如果是第一行则计算列数，否则只计算行数。
	 * <p>子类可以通过重写{@code processRow(ResultSet, int)}方法
	 * 执行自定义的提取或处理。
	 * @see #processRow(java.sql.ResultSet, int)
	 */
	@Override
	public final void processRow(ResultSet rs) throws SQLException {
		if (this.rowCount == 0) {
			ResultSetMetaData rsmd = rs.getMetaData();
			this.columnCount = rsmd.getColumnCount();
			this.columnTypes = new int[this.columnCount];
			this.columnNames = new String[this.columnCount];
			for (int i = 0; i < this.columnCount; i++) {
				this.columnTypes[i] = rsmd.getColumnType(i + 1);
				this.columnNames[i] = JdbcUtils.lookupColumnName(rsmd, i + 1);
			}
			// could also get column names
		}
		processRow(rs, this.rowCount++);
	}

	/**
	 * 子类可以重写此方法以执行自定义的提取或处理。
	 * 此类的实现什么都不做。
	 * @param rs 要从中提取数据的 ResultSet。
	 * 该方法会针对每一行被调用
	 * @param rowNum 当前行的编号（从 0 开始）
	 */
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {
	}


	/**
	 * 返回列的类型，以java.sql.Types常量表示。
	 * 在第一次调用processRow后有效。
	 * @return 各列的类型，以 java.sql.Types 常量表示。
	 * <b>索引从 0 到 n-1。</b>
	 */
	@Nullable
	public final int[] getColumnTypes() {
		return this.columnTypes;
	}

	/**
	 * 返回列的名称。
	 * 在第一次调用processRow后有效。
	 * @return 各列的名称。
	 * <b>索引从 0 到 n-1。</b>
	 */
	@Nullable
	public final String[] getColumnNames() {
		return this.columnNames;
	}

	/**
	 * 返回此ResultSet的行数。
	 * 仅在处理完成后有效。
	 * @return 此 ResultSet 中的行数
	 */
	public final int getRowCount() {
		return this.rowCount;
	}

	/**
	 * 返回此结果集中的列数。
	 * 在看到第一行后有效，因此子类可以在处理期间使用。
	 * @return 此结果集中的列数
	 */
	public final int getColumnCount() {
		return this.columnCount;
	}

}
