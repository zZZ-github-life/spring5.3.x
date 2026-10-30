/*
 * Copyright 2002-2020 the original author or authors.
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
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 标准 JDBC {@link javax.sql.DataSource} 接口的简单实现，
 * 通过 Bean 属性配置朴素的 JDBC {@link java.sql.Driver}，
 * 并在每次 {@code getConnection} 调用时返回一个新的 {@link java.sql.Connection}。
 *
 * <p><b>注意：此类不是真正的连接池；它实际上并不池化 Connection。</b>
 * 它只是作为完整连接池的简单替代，实现相同的标准接口，
 * 但每次调用时都会创建新的 Connection。
 *
 * <p>在 Java EE 容器中，建议使用容器提供的 JNDI DataSource。
 * 此类 DataSource 可以通过 {@link org.springframework.jndi.JndiObjectFactoryBean}
 * 在 Spring ApplicationContext 中暴露为 DataSource Bean，
 * 从而可以无缝地与此类这样的本地 DataSource Bean 相互切换。
 *
 * <p>这个 {@code SimpleDriverDataSource} 类最初是与
 * <a href="https://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * 和 <a href="https://sourceforge.net/projects/c3p0">C3P0</a> 一起设计的，
 * 后两者提供了 Bean 风格的 {@code BasicDataSource}/{@code ComboPooledDataSource}
 * 类，并带有用于本地资源设置的配置属性。对于现代的 JDBC 连接池，
 * 可以考虑改用 <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a>，
 * 向应用程序暴露相应的 {@code HikariDataSource} 实例。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see DriverManagerDataSource
 */
public class SimpleDriverDataSource extends AbstractDriverBasedDataSource {

	@Nullable
	private Driver driver;


	/**
	 * 用于 Bean 风格配置的构造函数。
	 */
	public SimpleDriverDataSource() {
	}

	/**
	 * 使用给定的标准 Driver 参数创建一个新的 DriverManagerDataSource。
	 * @param driver JDBC Driver 对象
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url) {
		setDriver(driver);
		setUrl(url);
	}

	/**
	 * 使用给定的标准 Driver 参数创建一个新的 DriverManagerDataSource。
	 * @param driver JDBC Driver 对象
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @param username 用于访问 DriverManager 的 JDBC 用户名
	 * @param password 用于访问 DriverManager 的 JDBC 密码
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url, String username, String password) {
		setDriver(driver);
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	/**
	 * 使用给定的标准 Driver 参数创建一个新的 DriverManagerDataSource。
	 * @param driver JDBC Driver 对象
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @param conProps JDBC 连接属性
	 * @see java.sql.Driver#connect(String, java.util.Properties)
	 */
	public SimpleDriverDataSource(Driver driver, String url, Properties conProps) {
		setDriver(driver);
		setUrl(url);
		setConnectionProperties(conProps);
	}


	/**
	 * 指定要使用的 JDBC Driver 实现类。
	 * <p>将创建此 Driver 类的一个实例并保留在
	 * SimpleDriverDataSource 中。
	 * @see #setDriver
	 */
	public void setDriverClass(Class<? extends Driver> driverClass) {
		this.driver = BeanUtils.instantiateClass(driverClass);
	}

	/**
	 * 指定要使用的 JDBC Driver 实例。
	 * <p>这样可以传入一个共享的、可能已预先配置的
	 * Driver 实例。
	 * @see #setDriverClass
	 */
	public void setDriver(@Nullable Driver driver) {
		this.driver = driver;
	}

	/**
	 * 返回要使用的 JDBC Driver 实例。
	 */
	@Nullable
	public Driver getDriver() {
		return this.driver;
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		Driver driver = getDriver();
		String url = getUrl();
		Assert.notNull(driver, "Driver must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC Driver Connection to [" + url + "]");
		}
		return driver.connect(url, props);
	}

}
