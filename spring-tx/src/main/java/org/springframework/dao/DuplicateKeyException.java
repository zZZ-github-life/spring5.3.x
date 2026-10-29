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
 * 当插入或更新数据的尝试导致违反主键或唯一约束时抛出的异常。
 * 请注意，这不一定纯粹是关系型数据库的概念；
 * 大多数数据库类型都需要唯一主键。
 *
 * <p>建议考虑处理更通用的 {@link DataIntegrityViolationException}，
 * 它在语义上涵盖了更广泛的约束违反情况。
 *
 * @author Thomas Risberg
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends DataIntegrityViolationException {

	/**
	 * DuplicateKeyException 的构造方法。
	 * @param msg the detail message
	 */
	public DuplicateKeyException(String msg) {
		super(msg);
	}

	/**
	 * DuplicateKeyException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public DuplicateKeyException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
