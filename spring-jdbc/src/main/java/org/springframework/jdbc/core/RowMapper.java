/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * {@link JdbcTemplate}使用的接口，用于逐行映射{@link java.sql.ResultSet}的行数据。
 * 此接口的实现负责将每行映射为结果对象的实际工作，
 * 但不需要担心异常处理。{@link java.sql.SQLException SQLException}
 * 将被调用的{@code JdbcTemplate}捕获并处理。
 *
 * <p>通常用于{@code JdbcTemplate}的查询方法或存储过程的{@code out}参数。
 * {@code RowMapper}对象通常是无状态的，因此可以重复使用；
 * 它们是在单一位置实现行映射逻辑的理想选择。
 *
 * <p>或者，可以考虑从{@code jdbc.object}包中继承
 * {@link org.springframework.jdbc.object.MappingSqlQuery}：
 * 无需使用单独的{@code JdbcTemplate}和{@code RowMapper}对象，
 * 你可以在该风格中构建可执行的查询对象（包含行映射逻辑）。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @param <T> the result type
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see ResultSetExtractor
 * @see org.springframework.jdbc.object.MappingSqlQuery
 */
@FunctionalInterface
public interface RowMapper<T> {

	/**
	 * 实现必须实现此方法来映射{@code ResultSet}中的每一行数据。
	 * 此方法不应调用{@code ResultSet}上的{@code next()}；
	 * 它只应该映射当前行的值。
	 * @param rs the {@code ResultSet} to map (pre-initialized for the current row)
	 * @param rowNum the number of the current row
	 * @return the result object for the current row (may be {@code null})
	 * @throws SQLException if an SQLException is encountered while getting
	 * column values (that is, there's no need to catch SQLException)
	 */
	@Nullable
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
