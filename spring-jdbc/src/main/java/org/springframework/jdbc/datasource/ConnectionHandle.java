/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * 由 JDBC Connection 句柄实现的简单接口。
 * 例如由 JpaDialect 使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 */
@FunctionalInterface
public interface ConnectionHandle {

	/**
	 * 获取此句柄所引用的 JDBC Connection。
	 */
	Connection getConnection();

	/**
	 * 释放此句柄所引用的 JDBC Connection。
	 * <p>默认实现为空，假定连接的生命周期由外部管理。
	 * @param con 要释放的 JDBC Connection
	 */
	default void releaseConnection(Connection con) {
	}

}
