/*
 * Copyright 2002-2022 the original author or authors.
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
 * 悲观锁违反时抛出的异常。
 * 当遇到相应的数据库错误时，由 Spring 的 SQLException 翻译机制抛出。
 *
 * <p>作为更具体异常的超类，例如 {@link CannotAcquireLockException}。
 * 然而，通常建议直接处理 {@code PessimisticLockingFailureException}，
 * 而不是依赖于特定的异常子类。
 *
 * @author Thomas Risberg
 * @since 1.2
 * @see OptimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class PessimisticLockingFailureException extends ConcurrencyFailureException {

	/**
	 * PessimisticLockingFailureException 的构造方法。
	 * @param msg 详细信息
	 */
	public PessimisticLockingFailureException(String msg) {
		super(msg);
	}

	/**
	 * PessimisticLockingFailureException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public PessimisticLockingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
