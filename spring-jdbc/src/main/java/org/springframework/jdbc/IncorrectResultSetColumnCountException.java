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

package org.springframework.jdbc;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * 当结果集的列数不正确时抛出的数据访问异常，
 * 例如期望单列但得到0列或多于1列。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
 */
@SuppressWarnings("serial")
public class IncorrectResultSetColumnCountException extends DataRetrievalFailureException {

	private final int expectedCount;

	private final int actualCount;


	/**
	 * 创建IncorrectResultSetColumnCountException实例。
	 * @param expectedCount 预期的列数
	 * @param actualCount 实际的列数
	 */
	public IncorrectResultSetColumnCountException(int expectedCount, int actualCount) {
		super("Incorrect column count: expected " + expectedCount + ", actual " + actualCount);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}

	/**
	 * 创建IncorrectResultCountDataAccessException实例。
	 * @param msg 详细消息
	 * @param expectedCount 预期的列数
	 * @param actualCount 实际的列数
	 */
	public IncorrectResultSetColumnCountException(String msg, int expectedCount, int actualCount) {
		super(msg);
		this.expectedCount = expectedCount;
		this.actualCount = actualCount;
	}


	/**
	 * 返回期望的列数。
	 */
	public int getExpectedCount() {
		return this.expectedCount;
	}

	/**
	 * 返回实际的列数。
	 */
	public int getActualCount() {
		return this.actualCount;
	}

}
