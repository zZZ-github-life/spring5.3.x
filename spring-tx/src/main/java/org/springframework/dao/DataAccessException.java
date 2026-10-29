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

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 数据访问异常层次结构的根类，在
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * 中有详细讨论。请参阅该书第 9 章以了解本包的设计动机。
 *
 * <p>此异常层次结构旨在让用户代码能够查找和处理所遇到的错误，
 * 而无需了解所使用的特定数据访问 API 的细节（例如 JDBC）。
 * 因此，可以在不知道使用 JDBC 的情况下对乐观锁失败做出响应。
 *
 * <p>由于此类是运行时异常，如果任何错误都被视为致命错误（通常情况），
 * 则用户代码无需捕获它或其子类。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public abstract class DataAccessException extends NestedRuntimeException {

	/**
	 * DataAccessException 的构造方法。
	 * @param msg the detail message
	 */
	public DataAccessException(String msg) {
		super(msg);
	}

	/**
	 * DataAccessException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * data access API such as JDBC)
	 */
	public DataAccessException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
