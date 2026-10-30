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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * ResultSetExtractor接口的适配器实现，委托给一个RowMapper，
 * 该RowMapper负责为每行创建一个对象。
 * 每个对象都被添加到此ResultSetExtractor的结果List中。
 *
 * <p>适用于数据库表中每行一个对象的典型情况。
 * 结果列表中的条目数将与行数匹配。
 *
 * <p>请注意，RowMapper对象通常是无状态的，因此可以重复使用；
 * 只有RowMapperResultSetExtractor适配器是有状态的。
 *
 * <p>与JdbcTemplate一起使用的示例：
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowMapper rowMapper = new UserRowMapper();  // reusable object
 *
 * List allUsers = (List) jdbcTemplate.query(
 *     "select * from user",
 *     new RowMapperResultSetExtractor(rowMapper, 10));
 *
 * User user = (User) jdbcTemplate.queryForObject(
 *     "select * from user where id=?", new Object[] {id},
 *     new RowMapperResultSetExtractor(rowMapper, 1));</pre>
 *
 * <p>或者，可以考虑从{@code jdbc.object}包中继承MappingSqlQuery：
 * 无需使用单独的JdbcTemplate和RowMapper对象，
 * 你可以在那里拥有可执行的查询对象（包含行映射逻辑）。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @param <T> 结果元素类型
 * @see RowMapper
 * @see JdbcTemplate
 * @see org.springframework.jdbc.object.MappingSqlQuery
 */
public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

	private final RowMapper<T> rowMapper;

	private final int rowsExpected;


	/**
	 * 创建新的RowMapperResultSetExtractor。
	 * @param rowMapper 为每行创建对象的 RowMapper
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
		this(rowMapper, 0);
	}

	/**
	 * 创建新的RowMapperResultSetExtractor。
	 * @param rowMapper 为每行创建对象的 RowMapper
	 * @param rowsExpected 预期的行数（仅用于优化的集合处理）
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
		Assert.notNull(rowMapper, "RowMapper is required");
		this.rowMapper = rowMapper;
		this.rowsExpected = rowsExpected;
	}


	@Override
	public List<T> extractData(ResultSet rs) throws SQLException {
		List<T> results = (this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>());
		int rowNum = 0;
		while (rs.next()) {
			results.add(this.rowMapper.mapRow(rs, rowNum++));
		}
		return results;
	}

}
