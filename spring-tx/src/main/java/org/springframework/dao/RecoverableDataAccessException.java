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
 * 当先前失败的操作在执行一些恢复步骤并重试整个事务（或在分布式事务的情况下，
 * 重试事务分支）后可能成功时抛出的数据访问异常。
 * 恢复操作至少需要关闭当前连接并获取新的连接。
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLRecoverableException
 */
@SuppressWarnings("serial")
public class RecoverableDataAccessException extends DataAccessException {

	/**
	 * RecoverableDataAccessException 的构造方法。
	 * @param msg 详细信息
	 */
	public RecoverableDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * RecoverableDataAccessException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 根本原因（通常来自使用底层数据访问 API，例如 JDBC）
	 */
	public RecoverableDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
