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

package org.springframework.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 * 执行批量更新操作的 SqlUpdate 子类。封装了将待更新的记录排入队列的逻辑，
 * 并在调用 {@code flush} 或达到指定的批大小后，将它们作为单个批次添加。
 *
 * <p>请注意，本类是<b>非线程安全的对象</b>，这与本包中所有其他 JDBC
 * 操作对象不同。每次使用时都需要创建新实例，或在同一线程内重用之前
 * 调用 {@code reset}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.1
 * @see #flush
 * @see #reset
 */
public class BatchSqlUpdate extends SqlUpdate {

	/**
	 * 提交批次之前累积的默认插入条数（5000）。
	 */
	public static final int DEFAULT_BATCH_SIZE = 5000;


	private int batchSize = DEFAULT_BATCH_SIZE;

	private boolean trackRowsAffected = true;

	private final Deque<Object[]> parameterQueue = new ArrayDeque<>();

	private final List<Integer> rowsAffected = new ArrayList<>();


	/**
	 * 允许作为 JavaBean 使用的构造函数。在编译和使用之前，
	 * 必须提供 DataSource 和 SQL。
	 * @see #setDataSource
	 * @see #setSql
	 */
	public BatchSqlUpdate() {
		super();
	}

	/**
	 * 使用给定的 DataSource 和 SQL 构造更新对象。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 */
	public BatchSqlUpdate(DataSource ds, String sql) {
		super(ds, sql);
	}

	/**
	 * 使用给定的 DataSource、SQL 和匿名参数构造更新对象。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types) {
		super(ds, sql, types);
	}

	/**
	 * 使用给定的 DataSource、SQL、匿名参数构造更新对象，
	 * 并指定最多可影响的行数。
	 * @param ds 用于获取连接的 DataSource
	 * @param sql 要执行的 SQL 语句
	 * @param types 参数的 SQL 类型，定义于
	 * {@code java.sql.Types} 类中
	 * @param batchSize 触发自动中间刷新的语句数量
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types, int batchSize) {
		super(ds, sql, types);
		setBatchSize(batchSize);
	}


	/**
	 * 设置触发自动中间刷新的语句数。{@code update} 调用或给定的语句参数
	 * 会被排入队列，直到达到批大小，此时将清空队列并执行该批次。
	 * <p>也可以显式调用 {@code flush} 来刷新已排队的语句。请注意，需要在
	 * 将所有参数排队之后执行此操作，以确保所有语句都已被刷新。
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * 设置是否跟踪由此操作对象执行的批量更新所影响的行数。
	 * <p>默认为 "true"。关闭该选项可以节省行计数列表所需的内存。
	 * @see #getRowsAffected()
	 */
	public void setTrackRowsAffected(boolean trackRowsAffected) {
		this.trackRowsAffected = trackRowsAffected;
	}

	/**
	 * BatchSqlUpdate 不支持 BLOB 或 CLOB 参数。
	 */
	@Override
	protected boolean supportsLobParameters() {
		return false;
	}


	/**
	 * 重写的 {@code update} 版本，将给定的语句参数添加到队列中，而不是
	 * 立即执行它们。SqlUpdate 基类的所有其他 {@code update} 方法都会经过
	 * 此方法，因此行为类似。
	 * <p>需要调用 {@code flush} 才能真正执行批次。如果达到指定的批大小，
	 * 会发生隐式刷新；但最终仍需要调用 {@code flush} 来刷新所有语句。
	 * @param params 参数对象数组
	 * @return 受影响的行数（始终为 -1，表示“不适用”，
	 * 因为该语句实际上不会由此方法执行）
	 * @see #flush
	 */
	@Override
	public int update(Object... params) throws DataAccessException {
		validateParameters(params);
		this.parameterQueue.add(params.clone());

		if (this.parameterQueue.size() == this.batchSize) {
			if (logger.isDebugEnabled()) {
				logger.debug("Triggering auto-flush because queue reached batch size of " + this.batchSize);
			}
			flush();
		}

		return -1;
	}

	/**
	 * 触发将任何已排队的更新操作作为最终批次添加。
	 * @return 各语句受影响行数组成的数组
	 */
	public int[] flush() {
		if (this.parameterQueue.isEmpty()) {
			return new int[0];
		}

		int[] rowsAffected = getJdbcTemplate().batchUpdate(
				resolveSql(),
				new BatchPreparedStatementSetter() {
					@Override
					public int getBatchSize() {
						return parameterQueue.size();
					}
					@Override
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						Object[] params = parameterQueue.removeFirst();
						newPreparedStatementSetter(params).setValues(ps);
					}
				});

		for (int rowCount : rowsAffected) {
			checkRowsAffected(rowCount);
			if (this.trackRowsAffected) {
				this.rowsAffected.add(rowCount);
			}
		}

		return rowsAffected;
	}

	/**
	 * 返回队列中当前的语句或语句参数数量。
	 */
	public int getQueueCount() {
		return this.parameterQueue.size();
	}

	/**
	 * 返回已执行语句的数量。
	 */
	public int getExecutionCount() {
		return this.rowsAffected.size();
	}

	/**
	 * 返回所有已执行语句的影响行数。在调用 {@code reset} 之前，
	 * 会累积所有 {@code flush} 的返回值。
	 * @return 各语句受影响行数组成的数组
	 * @see #reset
	 */
	public int[] getRowsAffected() {
		int[] result = new int[this.rowsAffected.size()];
		for (int i = 0; i < this.rowsAffected.size(); i++) {
			result[i] = this.rowsAffected.get(i);
		}
		return result;
	}

	/**
	 * 重置语句参数队列、影响行数缓存和执行计数。
	 */
	public void reset() {
		this.parameterQueue.clear();
		this.rowsAffected.clear();
	}

}
