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

package org.springframework.jdbc.datasource;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * {@code javax.sql.DataSource} 接口的扩展，由以非包装方式
 * 返回 JDBC Connection 的特殊 DataSource 实现。
 *
 * <p>使用此接口的类可以查询操作后是否应关闭 Connection。
 * Spring 的 DataSourceUtils 和 JdbcTemplate 类会自动执行此类检查。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SingleConnectionDataSource#shouldClose
 * @see DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public interface SmartDataSource extends DataSource {

	/**
	 * 是否应关闭从此 DataSource 获取的这个 Connection？
	 * <p>使用 SmartDataSource 的 Connection 的代码应始终
	 * 在调用 {@code close()} 之前通过此方法进行检查。
	 * <p>请注意，'jdbc.core' 包中的 JdbcTemplate 类负责
	 * 释放 JDBC Connection，从而免除了应用程序代码的这一职责。
	 * @param con the Connection to check
	 * @return whether the given Connection should be closed
	 * @see java.sql.Connection#close()
	 */
	boolean shouldClose(Connection con);

}
