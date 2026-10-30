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

package org.springframework.jdbc.support;

import org.springframework.core.NestedCheckedException;

/**
 * 表示在 JDBC 元数据查找过程中出现问题的异常。
 *
 * <p>这是一个受检异常，因为我们希望它被捕获、记录并处理，而不是导致应用程序
 * 失败。读取 JDBC 元数据失败通常并不是致命问题。
 *
 * @author Thomas Risberg
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class MetaDataAccessException extends NestedCheckedException {

	/**
	 * MetaDataAccessException 的构造函数。
	 * @param msg 详细消息
	 */
	public MetaDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * MetaDataAccessException 的构造函数。
	 * @param msg 详细消息
	 * @param cause 所用数据访问 API 的根本原因
	 */
	public MetaDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
