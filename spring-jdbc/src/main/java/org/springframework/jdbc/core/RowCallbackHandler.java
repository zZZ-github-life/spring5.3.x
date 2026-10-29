/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * {@link JdbcTemplate}使用的接口，用于逐行处理{@link java.sql.ResultSet}。
 * 此接口的实现负责处理每行的实际工作，
 * 但不需要担心异常处理。{@link java.sql.SQLException SQLException}
 * 将被调用的JdbcTemplate捕获并处理。
 *
 * <p>与{@link ResultSetExtractor}不同，RowCallbackHandler对象通常是有状态的：
 * 它在对象内部保持结果状态，以便后续查看。使用示例请参见{@link RowCountCallbackHandler}。
 *
 * <p>如果需要为每行精确映射一个结果对象并组装成List，请考虑使用{@link RowMapper}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JdbcTemplate
 * @see RowMapper
 * @see ResultSetExtractor
 * @see RowCountCallbackHandler
 */
@FunctionalInterface
public interface RowCallbackHandler {

	/**
	 * 实现必须实现此方法来处理ResultSet中的每一行数据。
	 * 此方法不应调用ResultSet上的{@code next()}；它只应该提取当前行的值。
	 * <p>实现选择做什么完全由它决定：
	 * 一个简单的实现可能只是计算行数，而另一个实现可能构建一个XML文档。
	 * @param rs the ResultSet to process (pre-initialized for the current row)
	 * @throws SQLException if an SQLException is encountered getting
	 * column values (that is, there's no need to catch SQLException)
	 */
	void processRow(ResultSet rs) throws SQLException;

}
