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
 * 当我们错误地使用数据访问资源时抛出的异常根类。
 * 例如，在使用 RDBMS 时指定了错误的 SQL。
 * 特定资源的子类由具体的数据访问包提供。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class InvalidDataAccessResourceUsageException extends NonTransientDataAccessException {

	/**
	 * InvalidDataAccessResourceUsageException 的构造方法。
	 * @param msg 详细信息
	 */
	public InvalidDataAccessResourceUsageException(String msg) {
		super(msg);
	}

	/**
	 * InvalidDataAccessResourceUsageException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public InvalidDataAccessResourceUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
