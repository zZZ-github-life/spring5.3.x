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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 标准 JDBC {@link javax.sql.DataSource} 接口的简单实现，
 * 通过 Bean 属性配置朴素的 JDBC {@link java.sql.DriverManager}，
 * 并在每次 {@code getConnection} 调用时返回一个新的 {@link java.sql.Connection}。
 *
 * <p><b>注意：此类不是真正的连接池；它实际上并不池化 Connection。</b>
 * 它只是作为完整连接池的简单替代，实现相同的标准接口，
 * 但每次调用时都会创建新的 Connection。
 *
 * <p>适用于 Java EE 容器之外的测试或独立环境，
 * 既可以作为相应 ApplicationContext 中的 DataSource Bean，
 * 也可以与简单的 JNDI 环境结合使用。假定存在连接池的
 * {@code Connection.close()} 调用只会简单地关闭 Connection，
 * 因此任何能感知 DataSource 的持久化代码都应能正常工作。
 *
 * <p><b>注意：在 OSGi 等特殊的类加载环境中，由于 JDBC DriverManager
 * 存在一般的类加载问题，而这些问题可以通过直接使用 Driver 来解决
 * （这正是 SimpleDriverDataSource 所做的），因此此类实际上已被
 * {@link SimpleDriverDataSource} 所取代。</b>
 *
 * <p>在 Java EE 容器中，建议使用容器提供的 JNDI DataSource。
 * 此类 DataSource 可以通过 {@link org.springframework.jndi.JndiObjectFactoryBean}
 * 在 Spring ApplicationContext 中暴露为 DataSource Bean，
 * 从而可以无缝地与此类这样的本地 DataSource Bean 相互切换。
 * 对于测试，你可以通过 Spring 的
 * {@link org.springframework.mock.jndi.SimpleNamingContextBuilder}
 * 来设置模拟的 JNDI 环境，或者将 Bean 定义切换为本地 DataSource
 * （这种方式更简单，因此推荐使用）。
 *
 * <p>这个 {@code DriverManagerDataSource} 类最初是与
 * <a href="https://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * 和 <a href="https://sourceforge.net/projects/c3p0">C3P0</a> 一起设计的，
 * 后两者提供了 Bean 风格的 {@code BasicDataSource}/{@code ComboPooledDataSource}
 * 类，并带有用于本地资源设置的配置属性。对于现代的 JDBC 连接池，
 * 可以考虑改用 <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a>，
 * 向应用程序暴露相应的 {@code HikariDataSource} 实例。
 *
 * @author Juergen Hoeller
 * @since 14.03.2003
 * @see SimpleDriverDataSource
 */
public class DriverManagerDataSource extends AbstractDriverBasedDataSource {

	/**
	 * 用于 Bean 风格配置的构造函数。
	 */
	public DriverManagerDataSource() {
	}

	/**
	 * 使用给定的 JDBC URL 创建一个新的 DriverManagerDataSource，
	 * 不为 JDBC 访问指定用户名或密码。
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @see java.sql.DriverManager#getConnection(String)
	 */
	public DriverManagerDataSource(String url) {
		setUrl(url);
	}

	/**
	 * 使用给定的标准 DriverManager 参数创建一个新的 DriverManagerDataSource。
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @param username 用于访问 DriverManager 的 JDBC 用户名
	 * @param password 用于访问 DriverManager 的 JDBC 密码
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public DriverManagerDataSource(String url, String username, String password) {
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	/**
	 * 使用给定的 JDBC URL 创建一个新的 DriverManagerDataSource，
	 * 不为 JDBC 访问指定用户名或密码。
	 * @param url 用于访问 DriverManager 的 JDBC URL
	 * @param conProps JDBC 连接属性
	 * @see java.sql.DriverManager#getConnection(String)
	 */
	public DriverManagerDataSource(String url, Properties conProps) {
		setUrl(url);
		setConnectionProperties(conProps);
	}


	/**
	 * 设置 JDBC 驱动程序类名。该驱动程序将在启动时初始化，
	 * 并向 JDK 的 DriverManager 注册自身。
	 * <p><b>注意：DriverManagerDataSource 主要用于访问<i>预先注册</i>的
	 * JDBC 驱动程序。</b>如果你需要注册新的驱动程序，请考虑使用
	 * {@link SimpleDriverDataSource}。或者，考虑在实例化此 DataSource
	 * 之前自行初始化 JDBC 驱动程序。"driverClassName" 属性主要为了
	 * 向后兼容而保留，也用于在 Commons DBCP 与此 DataSource 之间迁移。
	 * @see java.sql.DriverManager#registerDriver(java.sql.Driver)
	 * @see SimpleDriverDataSource
	 */
	public void setDriverClassName(String driverClassName) {
		Assert.hasText(driverClassName, "Property 'driverClassName' must not be empty");
		String driverClassNameToUse = driverClassName.trim();
		try {
			Class.forName(driverClassNameToUse, true, ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Could not load JDBC driver class [" + driverClassNameToUse + "]", ex);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Loaded JDBC driver: " + driverClassNameToUse);
		}
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC DriverManager Connection to [" + url + "]");
		}
		return getConnectionFromDriverManager(url, props);
	}

	/**
	 * 通过 DriverManager 中那个糟糕的静态方法获取 Connection 的逻辑被提取到
	 * 一个受保护方法中，以便于进行单元测试。
	 * @see java.sql.DriverManager#getConnection(String, java.util.Properties)
	 */
	protected Connection getConnectionFromDriverManager(String url, Properties props) throws SQLException {
		return DriverManager.getConnection(url, props);
	}

}
