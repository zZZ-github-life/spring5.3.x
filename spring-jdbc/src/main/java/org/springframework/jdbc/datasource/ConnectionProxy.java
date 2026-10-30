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

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * {@link java.sql.Connection} 的子接口，由 Connection 代理实现。
 * 允许访问底层的目标 Connection。
 *
 * <p>当需要强制转换为原生 JDBC Connection（如 Oracle 的
 * OracleConnection）时，可以检查此接口。另外，所有这些连接也都
 * 支持 JDBC 4.0 的 {@link Connection#unwrap}。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see TransactionAwareDataSourceProxy
 * @see LazyConnectionDataSourceProxy
 * @see DataSourceUtils#getTargetConnection(java.sql.Connection)
 */
public interface ConnectionProxy extends Connection {

	/**
	 * 返回此代理的目标 Connection。
	 * <p>这通常是原生驱动程序的 Connection，
	 * 或连接池中的包装器。
	 * @return 底层 Connection（永不为 {@code null}）
	 */
	Connection getTargetConnection();

}
