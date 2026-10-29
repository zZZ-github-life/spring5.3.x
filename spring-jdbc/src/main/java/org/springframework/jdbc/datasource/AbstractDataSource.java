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

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Spring 的 {@link javax.sql.DataSource} 实现类的抽象基类，
 * 负责处理填充（padding）功能。
 *
 * <p>在本类的上下文中，“填充”指的是为 {@code DataSource} 接口中的某些方法
 * 提供默认实现，例如 {@link #getLoginTimeout()}、{@link #setLoginTimeout(int)} 等。
 *
 * @author Juergen Hoeller
 * @since 07.05.2003
 * @see DriverManagerDataSource
 */
public abstract class AbstractDataSource implements DataSource {

	/** 供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 返回 0，表示将使用系统默认超时时间。
	 */
	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	/**
	 * 不支持设置登录超时时间。
	 */
	@Override
	public void setLoginTimeout(int timeout) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout");
	}

	/**
	 * 不支持 LogWriter 相关方法。
	 */
	@Override
	public PrintWriter getLogWriter() {
		throw new UnsupportedOperationException("getLogWriter");
	}

	/**
	 * 不支持 LogWriter 相关方法。
	 */
	@Override
	public void setLogWriter(PrintWriter pw) throws SQLException {
		throw new UnsupportedOperationException("setLogWriter");
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
		throw new SQLException("DataSource of type [" + getClass().getName() +
				"] cannot be unwrapped as [" + iface.getName() + "]");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this);
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.1's getParentLogger method
	//---------------------------------------------------------------------

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
