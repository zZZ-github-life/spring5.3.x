/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

/**
 * {@code ConnectionProperties} 是一个简单的数据容器，允许以一致的方式
 * 配置基本的 JDBC 连接属性，且与实际的 {@link javax.sql.DataSource DataSource}
 * 实现无关。
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 * @see DataSourceFactory
 */
public interface ConnectionProperties {

	/**
	 * 设置用于连接数据库的 JDBC 驱动程序类。
	 * @param driverClass jdbc 驱动类
	 */
	void setDriverClass(Class<? extends Driver> driverClass);

	/**
	 * 设置数据库的 JDBC 连接 URL。
	 * @param url 连接 url
	 */
	void setUrl(String url);

	/**
	 * 设置用于连接数据库的用户名。
	 * @param username 用户名
	 */
	void setUsername(String username);

	/**
	 * 设置用于连接数据库的密码。
	 * @param password 密码
	 */
	void setPassword(String password);

}
