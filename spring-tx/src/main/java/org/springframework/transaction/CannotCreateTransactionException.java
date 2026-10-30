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
 * 当无法使用底层事务 API（如 JTA）创建事务时抛出此异常。
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class CannotCreateTransactionException extends TransactionException {

	/**
	 * CannotCreateTransactionException 的构造函数。
	 * @param msg 详细信息
	 */
	public CannotCreateTransactionException(String msg) {
		super(msg);
	}

	/**
	 * CannotCreateTransactionException 的构造函数。
	 * @param msg 详细信息
	 * @param cause 来自所用事务 API 的根本原因
	 */
	public CannotCreateTransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
