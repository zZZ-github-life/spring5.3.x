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

package org.springframework.r2dbc.core;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * {@link BiFunction 映射函数}实现，为每一行创建一个 {@code java.util.Map}，
 * 将所有列表示为键值对：每列一个条目，以列名作为键。
 *
 * <p>要使用的 Map 实现以及列 Map 中每一列使用的键，
 * 可以分别通过覆写 {@link #createColumnMap} 和 {@link #getColumnKey} 进行自定义。
 *
 * <p><b>注意：</b>默认情况下，ColumnMapRowMapper 会尝试构建一个具有
 * 不区分大小写键的链式 Map，以便既保留列顺序，又允许列名使用任意大小写。
 * 这需要在类路径上存在 Commons Collections（将自动检测）。
 * 否则，回退为标准链式 HashMap，它仍会保留列顺序，
 * 但要求应用程序以驱动程序所提供的大小写指定列名。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class ColumnMapRowMapper implements BiFunction<Row, RowMetadata, Map<String, Object>> {

	/** 默认的 {@code ColumnMapRowMapper} 实例。 */
	public final static ColumnMapRowMapper INSTANCE = new ColumnMapRowMapper();


	@Override
	public Map<String, Object> apply(Row row, RowMetadata rowMetadata) {
		Collection<String> columns = rowMetadata.getColumnNames();
		int columnCount = columns.size();
		Map<String, Object> mapOfColValues = createColumnMap(columnCount);
		int index = 0;
		for (String column : columns) {
			String key = getColumnKey(column);
			Object obj = getColumnValue(row, index++);
			mapOfColValues.put(key, obj);
		}
		return mapOfColValues;
	}

	/**
	 * 创建一个用作列映射的 {@link Map} 实例。
	 * <p>默认情况下，将创建一个链式的不区分大小写 Map。
	 * @param columnCount 列数，用作 Map 的初始容量
	 * @return 新的 {@link Map} 实例
	 * @see LinkedCaseInsensitiveMap
	 */
	protected Map<String, Object> createColumnMap(int columnCount) {
		return new LinkedCaseInsensitiveMap<>(columnCount);
	}

	/**
	 * 确定列 {@link Map} 中给定列要使用的键。
	 * @param columnName {@link Row} 返回的列名
	 * @return 要使用的列键
	 * @see ColumnMetadata#getName()
	 */
	protected String getColumnKey(String columnName) {
		return columnName;
	}

	/**
	 * 获取指定列的 R2DBC 对象值。
	 * <p>默认实现使用 {@link Row#get(int)} 方法。
	 * @param row 是持有数据的 {@link Row}
	 * @param index 是列索引
	 * @return 返回的 Object
	 */
	@Nullable
	protected Object getColumnValue(Row row, int index) {
		return row.get(index);
	}

}
