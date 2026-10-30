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

package org.springframework.transaction;

/**
 * 当尝试执行依赖现有事务的操作（如设置回滚状态）但不存在现有事务时抛出此异常。
 * 这代表对事务 API 的非法使用。
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class NoTransactionException extends TransactionUsageException {

	/**
	 * NoTransactionException 的构造函数。
	 * @param msg 详细信息
	 */
	public NoTransactionException(String msg) {
		super(msg);
	}

	/**
	 * NoTransactionException 的构造函数。
	 * @param msg 详细信息
	 * @param cause 来自所用事务 API 的根本原因
	 */
	public NoTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
