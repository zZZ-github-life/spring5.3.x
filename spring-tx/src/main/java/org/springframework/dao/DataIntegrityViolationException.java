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
 * 当插入或更新数据的尝试导致违反完整性约束时抛出的异常。
 * 请注意，这不仅仅是关系型数据库的概念；大多数数据库类型
 * 都需要唯一主键等完整性约束。
 *
 * <p>作为更具体异常的超类，例如 {@link DuplicateKeyException}。
 * 然而，通常建议直接处理 {@code DataIntegrityViolationException}，
 * 而不是依赖于特定的异常子类。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class DataIntegrityViolationException extends NonTransientDataAccessException {

	/**
	 * DataIntegrityViolationException 的构造方法。
	 * @param msg the detail message
	 */
	public DataIntegrityViolationException(String msg) {
		super(msg);
	}

	/**
	 * DataIntegrityViolationException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DataIntegrityViolationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
