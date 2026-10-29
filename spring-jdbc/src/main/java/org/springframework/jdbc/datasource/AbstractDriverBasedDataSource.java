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
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.lang.Nullable;

/**
 * JDBC {@link javax.sql.DataSource} 实现类的抽象基类，
 * 这些实现类基于 JDBC {@link java.sql.Driver} 进行操作。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see SimpleDriverDataSource
 * @see DriverManagerDataSource
 */
public abstract class AbstractDriverBasedDataSource extends AbstractDataSource {

	@Nullable
	private String url;

	@Nullable
	private String username;

	@Nullable
	private String password;

	@Nullable
	private String catalog;

	@Nullable
	private String schema;

	@Nullable
	private Properties connectionProperties;


	/**
	 * 设置通过 Driver 建立连接时使用的 JDBC URL。
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setUrl(@Nullable String url) {
		this.url = (url != null ? url.trim() : null);
	}

	/**
	 * 返回通过 Driver 建立连接时使用的 JDBC URL。
	 */
	@Nullable
	public String getUrl() {
		return this.url;
	}

	/**
	 * 设置通过 Driver 建立连接时使用的 JDBC 用户名。
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * 返回通过 Driver 建立连接时使用的 JDBC 用户名。
	 */
	@Nullable
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置通过 Driver 建立连接时使用的 JDBC 密码。
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * 返回通过 Driver 建立连接时使用的 JDBC 密码。
	 */
	@Nullable
	public String getPassword() {
		return this.password;
	}

	/**
	 * 指定要应用到每个 Connection 的数据库 catalog。
	 * @since 4.3.2
	 * @see Connection#setCatalog
	 */
	public void setCatalog(@Nullable String catalog) {
		this.catalog = catalog;
	}

	/**
	 * 返回要应用到每个 Connection 的数据库 catalog（如果有）。
	 * @since 4.3.2
	 */
	@Nullable
	public String getCatalog() {
		return this.catalog;
	}

	/**
	 * 指定要应用到每个 Connection 的数据库 schema。
	 * @since 4.3.2
	 * @see Connection#setSchema
	 */
	public void setSchema(@Nullable String schema) {
		this.schema = schema;
	}

	/**
	 * 返回要应用到每个 Connection 的数据库 schema（如果有）。
	 * @since 4.3.2
	 */
	@Nullable
	public String getSchema() {
		return this.schema;
	}

	/**
	 * 以键值对形式指定任意连接属性，这些属性将传递给 Driver。
	 * <p>还可以包含 "user" 和 "password" 属性。不过，在此
	 * DataSource 上指定的任何 "username" 和 "password" Bean 属性
	 * 都将覆盖对应的连接属性。
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public void setConnectionProperties(@Nullable Properties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	/**
	 * 返回要传递给 Driver 的连接属性（如果有）。
	 */
	@Nullable
	public Properties getConnectionProperties() {
		return this.connectionProperties;
	}


	/**
	 * 此实现委托给 {@code getConnectionFromDriver}，
	 * 并使用此 DataSource 的默认用户名和密码。
	 * @see #getConnectionFromDriver(String, String)
	 * @see #setUsername
	 * @see #setPassword
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return getConnectionFromDriver(getUsername(), getPassword());
	}

	/**
	 * 此实现委托给 {@code getConnectionFromDriver}，
	 * 并使用给定的用户名和密码。
	 * @see #getConnectionFromDriver(String, String)
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnectionFromDriver(username, password);
	}


	/**
	 * 为 Driver 构建属性（包括给定的用户名和密码，如果有），
	 * 并获取相应的 Connection。
	 * @param username the name of the user
	 * @param password the password to use
	 * @return the obtained Connection
	 * @throws SQLException in case of failure
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	protected Connection getConnectionFromDriver(@Nullable String username, @Nullable String password) throws SQLException {
		Properties mergedProps = new Properties();
		Properties connProps = getConnectionProperties();
		if (connProps != null) {
			mergedProps.putAll(connProps);
		}
		if (username != null) {
			mergedProps.setProperty("user", username);
		}
		if (password != null) {
			mergedProps.setProperty("password", password);
		}

		Connection con = getConnectionFromDriver(mergedProps);
		if (this.catalog != null) {
			con.setCatalog(this.catalog);
		}
		if (this.schema != null) {
			con.setSchema(this.schema);
		}
		return con;
	}

	/**
	 * 使用给定属性获取一个 Connection。
	 * <p>这是一个模板方法，由子类实现。
	 * @param props the merged connection properties
	 * @return the obtained Connection
	 * @throws SQLException in case of failure
	 */
	protected abstract Connection getConnectionFromDriver(Properties props) throws SQLException;

}
