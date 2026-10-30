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
 * 乐观锁违反时抛出的异常。
 *
 * <p>此异常将由 O/R 映射工具或自定义 DAO 实现抛出。
 * 乐观锁失败通常<i>不是</i>由数据库本身检测到的。
 *
 * @author Rod Johnson
 * @see PessimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class OptimisticLockingFailureException extends ConcurrencyFailureException {

	/**
	 * OptimisticLockingFailureException 的构造方法。
	 * @param msg 详细信息
	 */
	public OptimisticLockingFailureException(String msg) {
		super(msg);
	}

	/**
	 * OptimisticLockingFailureException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public OptimisticLockingFailureException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
