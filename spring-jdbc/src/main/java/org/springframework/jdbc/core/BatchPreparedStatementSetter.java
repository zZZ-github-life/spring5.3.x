/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * {@link JdbcTemplate}类使用的批量更新回调接口。
 *
 * <p>此接口在JdbcTemplate类提供的{@link java.sql.PreparedStatement}上设置值，
 * 使用相同的SQL对批量中的多个更新分别设置。实现负责设置所有必要的参数。
 * 带占位符的SQL已经提供。
 *
 * <p>实现<i>不需要</i>关注其尝试的操作可能抛出的SQLException。
 * JdbcTemplate类会适当地捕获并处理SQLException。
 *
 * @author Rod Johnson
 * @since March 2, 2003
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see InterruptibleBatchPreparedStatementSetter
 */
public interface BatchPreparedStatementSetter {

	/**
	 * 在给定的PreparedStatement上设置参数值。
	 * @param ps the PreparedStatement to invoke setter methods on
	 * @param i index of the statement we're issuing in the batch, starting from 0
	 * @throws SQLException if an SQLException is encountered
	 * (i.e. there is no need to catch SQLException)
	 */
	void setValues(PreparedStatement ps, int i) throws SQLException;

	/**
	 * 返回批量的大小。
	 * @return the number of statements in the batch
	 */
	int getBatchSize();

}
