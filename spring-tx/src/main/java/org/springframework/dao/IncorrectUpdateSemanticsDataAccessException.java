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

package org.springframework.dao;

/**
 * 当更新操作发生了非预期的情况，但事务尚未回滚时抛出的数据访问异常。
 * 例如，当在 RDBMS 中期望更新 1 行但实际更新了 3 行时抛出。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * IncorrectUpdateSemanticsDataAccessException 的构造方法。
	 * @param msg 详细信息
	 */
	public IncorrectUpdateSemanticsDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * IncorrectUpdateSemanticsDataAccessException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自底层 API（例如 JDBC）的根本原因
	 */
	public IncorrectUpdateSemanticsDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 返回数据是否已更新。
	 * 如果此方法返回 false，则无需回滚。
	 * <p>默认实现始终返回 true。
	 * 子类可以覆盖此方法。
	 */
	public boolean wasDataUpdated() {
		return true;
	}

}
