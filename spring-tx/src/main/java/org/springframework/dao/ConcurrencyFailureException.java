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

import org.springframework.lang.Nullable;

/**
 * 在 various 数据访问并发失败时抛出的异常。
 *
 * <p>此异常为特定类型的失败提供了子类，
 * 特别是乐观锁与悲观锁失败。
 *
 * @author Thomas Risberg
 * @since 1.1
 * @see OptimisticLockingFailureException
 * @see PessimisticLockingFailureException
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

	/**
	 * ConcurrencyFailureException 的构造方法。
	 * @param msg 详细信息
	 */
	public ConcurrencyFailureException(String msg) {
		super(msg);
	}

	/**
	 * ConcurrencyFailureException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public ConcurrencyFailureException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
