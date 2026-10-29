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
	 * @param msg the detail message
	 */
	public TransactionSystemException(String msg) {
		super(msg);
	}

	/**
	 * TransactionSystemException 的构造函数。
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionSystemException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 设置在此事务异常之前抛出的应用程序异常，
	 * 尽管有覆盖性的 TransactionSystemException，仍保留原始异常。
	 * @param ex the application exception
	 * @throws IllegalStateException if this TransactionSystemException already holds an
	 * application exception
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
	 * @return the application exception, or {@code null} if none set
	 */
	@Nullable
	public final Throwable getApplicationException() {
		return this.applicationException;
	}

	/**
	 * 返回在失败的事务中第一个被抛出的异常：
	 * 即应用程序异常（如果有），或 TransactionSystemException 本身的根原因。
	 * @return the original exception, or {@code null} if there was none
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
