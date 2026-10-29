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

package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.InterruptibleBatchPreparedStatementSetter;

/**
 * {@link InterruptibleBatchPreparedStatementSetter} 接口的抽象实现，
 * 将对可用值的检查和对这些值的设置合并到单个回调方法
 * {@link #setValuesIfAvailable} 中。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setValuesIfAvailable
 */
public abstract class AbstractInterruptibleBatchPreparedStatementSetter
		implements InterruptibleBatchPreparedStatementSetter {

	private boolean exhausted;


	/**
	 * 此实现调用 {@link #setValuesIfAvailable}
	 * 并相应地设置此实例的耗尽标志。
	 */
	@Override
	public final void setValues(PreparedStatement ps, int i) throws SQLException {
		this.exhausted = !setValuesIfAvailable(ps, i);
	}

	/**
	 * 此实现返回此实例当前的耗尽标志。
	 */
	@Override
	public final boolean isBatchExhausted(int i) {
		return this.exhausted;
	}

	/**
	 * 此实现返回 {@code Integer.MAX_VALUE}。
	 * 可以在子类中覆盖以降低最大批量大小。
	 */
	@Override
	public int getBatchSize() {
		return Integer.MAX_VALUE;
	}


	/**
	 * 检查可用值并将其设置到给定的 PreparedStatement 上。
	 * 如果不再有可用值，则返回 {@code false}。
	 * @param ps the PreparedStatement we'll invoke setter methods on
	 * @param i index of the statement we're issuing in the batch, starting from 0
	 * @return whether there were values to apply (that is, whether the applied
	 * parameters should be added to the batch and this method should be called
	 * for a further iteration)
	 * @throws SQLException if an SQLException is encountered
	 * (i.e. there is no need to catch SQLException)
	 */
	protected abstract boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException;

}
