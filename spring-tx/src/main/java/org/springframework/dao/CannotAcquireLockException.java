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
 * 在更新过程中获取锁失败时抛出的异常，
 * 例如在执行 "select for update" 语句时。
 *
 * <p>建议考虑处理更通用的 {@link PessimisticLockingFailureException}，
 * 它在语义上涵盖了更广泛的锁相关故障。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class CannotAcquireLockException extends PessimisticLockingFailureException {

	/**
	 * CannotAcquireLockException 的构造方法。
	 * @param msg the detail message
	 */
	public CannotAcquireLockException(String msg) {
		super(msg);
	}

	/**
	 * CannotAcquireLockException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public CannotAcquireLockException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
