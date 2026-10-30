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

package org.springframework.transaction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 当遇到一般事务系统错误时抛出此异常，例如在提交或回滚时。
 *
 * @author Juergen Hoeller
 * @since 24.03.2003
 */
@SuppressWarnings("serial")
public class TransactionSystemException extends TransactionException {

	@Nullable
	private Throwable applicationException;


	/**
	 * TransactionSystemException 的构造函数。
	 * @param msg 详细信息
	 */
	public TransactionSystemException(String msg) {
		super(msg);
	}

	/**
	 * TransactionSystemException 的构造函数。
	 * @param msg 详细信息
	 * @param cause 来自所用事务 API 的根本原因
	 */
	public TransactionSystemException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 设置在此事务异常之前抛出的应用程序异常，
	 * 尽管有覆盖性的 TransactionSystemException，仍保留原始异常。
	 * @param ex 应用程序异常
	 * @throws IllegalStateException 如果此 TransactionSystemException
	 * 已经持有应用程序异常
	 */
	public void initApplicationException(Throwable ex) {
		Assert.notNull(ex, "Application exception must not be null");
		if (this.applicationException != null) {
			throw new IllegalStateException("Already holding an application exception: " + this.applicationException);
		}
		this.applicationException = ex;
	}

	/**
	 * 返回在此事务异常之前抛出的应用程序异常（如果有）。
	 * @return 应用程序异常；如果未设置则为 {@code null}
	 */
	@Nullable
	public final Throwable getApplicationException() {
		return this.applicationException;
	}

	/**
	 * 返回在失败的事务中第一个被抛出的异常：
	 * 即应用程序异常（如果有），或 TransactionSystemException 本身的根原因。
	 * @return 原始异常；如果不存在则为 {@code null}
	 */
	@Nullable
	public Throwable getOriginalException() {
		return (this.applicationException != null ? this.applicationException : getCause());
	}

	@Override
	public boolean contains(@Nullable Class<?> exType) {
		return super.contains(exType) || (exType != null && exType.isInstance(this.applicationException));
	}

}
