/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 当资源完全失败时抛出的数据访问异常，
 * 例如，无法通过 JDBC 连接到数据库。
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class DataAccessResourceFailureException extends NonTransientDataAccessResourceException {

	/**
	 * DataAccessResourceFailureException 的构造方法。
	 * @param msg 详细信息
	 */
	public DataAccessResourceFailureException(String msg) {
		super(msg);
	}

	/**
	 * DataAccessResourceFailureException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public DataAccessResourceFailureException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
