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

/**
 * {@link BatchPreparedStatementSetter}接口的扩展，
 * 添加了批量耗尽检查。
 *
 * <p>此接口允许你发出批量结束的信号，而不必预先确定确切的批量大小。
 * 批量大小仍然被遵守，但现在是批量的最大大小。
 *
 * <p>{@link #isBatchExhausted}方法在每次调用{@link #setValues}之后被调用，
 * 以确定是否添加了某些值，或者批量是否已确定完成且在最后一次调用{@code setValues}期间
 * 没有提供额外的值。
 *
 * <p>请考虑继承
 * {@link org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter}
 * 基类，而不是直接实现此接口，使用单一的{@code setValuesIfAvailable}回调方法来检查
 * 可用值并设置它们，返回是否实际提供了值。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter
 */
public interface InterruptibleBatchPreparedStatementSetter extends BatchPreparedStatementSetter {

	/**
	 * 返回批量是否已完成，即最后一次{@code setValues}调用期间是否没有添加额外的值。
	 * <p><b>注意：</b>如果此方法返回{@code true}，在最后一次{@code setValues}调用期间
	 * 可能已设置的任何参数都将被忽略！确保如果在{@code setValues}实现的<em>开始</em>处
	 * 检测到耗尽，设置相应的内部标志，让此方法基于该标志返回{@code true}。
	 * @param i index of the statement we're issuing in the batch, starting from 0
	 * @return whether the batch is already exhausted
	 * @see #setValues
	 * @see org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter#setValuesIfAvailable
	 */
	boolean isBatchExhausted(int i);

}
