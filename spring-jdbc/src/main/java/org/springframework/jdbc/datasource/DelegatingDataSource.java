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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JDBC {@link javax.sql.DataSource} 实现，将所有调用
 * 委托给指定的目标 {@link javax.sql.DataSource}。
 *
 * <p>此类旨在被继承，子类只需重写那些不应简单委托给
 * 目标 DataSource 的方法（例如 {@link #getConnection()}）。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #getConnection
 */
public class DelegatingDataSource implements DataSource, InitializingBean {

	@Nullable
	private DataSource targetDataSource;


	/**
	 * 创建一个新的 DelegatingDataSource。
	 * @see #setTargetDataSource
	 */
	public DelegatingDataSource() {
	}

	/**
	 * 创建一个新的 DelegatingDataSource。
	 * @param targetDataSource the target DataSource
	 */
	public DelegatingDataSource(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
	}


	/**
	 * 设置此 DataSource 应委托到的目标 DataSource。
	 */
	public void setTargetDataSource(@Nullable DataSource targetDataSource) {
		this.targetDataSource = targetDataSource;
	}

	/**
	 * 返回此 DataSource 应委托到的目标 DataSource。
	 */
	@Nullable
	public DataSource getTargetDataSource() {
		return this.targetDataSource;
	}

	/**
	 * 获取实际使用的目标 {@code DataSource}（永不为 {@code null}）。
	 * @since 5.0
	 */
	protected DataSource obtainTargetDataSource() {
		DataSource dataSource = getTargetDataSource();
		Assert.state(dataSource != null, "No 'targetDataSource' set");
		return dataSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetDataSource() == null) {
			throw new IllegalArgumentException("Property 'targetDataSource' is required");
		}
	}


	@Override
	public Connection getConnection() throws SQLException {
		return obtainTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return obtainTargetDataSource().getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return obtainTargetDataSource().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		obtainTargetDataSource().setLogWriter(out);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return obtainTargetDataSource().getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		obtainTargetDataSource().setLoginTimeout(seconds);
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.0's Wrapper interface
	//---------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return obtainTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || obtainTargetDataSource().isWrapperFor(iface));
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.1's getParentLogger method
	//---------------------------------------------------------------------

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
