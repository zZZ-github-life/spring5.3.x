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
import java.sql.SQLException;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 目标 JDBC {@link javax.sql.DataSource} 的适配器，会将指定的用户凭据
 * 应用到每次标准 {@code getConnection()} 调用，并隐式地在目标上
 * 调用 {@code getConnection(username, password)}。所有其他方法都
 * 简单地委托给目标 DataSource 的相应方法。
 *
 * <p>可用于代理未配置用户凭据的目标 JNDI DataSource。
 * 客户端代码可以像往常一样通过标准的 {@code getConnection()} 调用
 * 使用此 DataSource。
 *
 * <p>在下面的示例中，客户端代码可以简单地透明使用预先配置的
 * "myDataSource"，并通过指定的用户凭据隐式访问 "myTargetDataSource"。
 *
 * <pre class="code">
 * &lt;bean id="myTargetDataSource" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jdbc/myds"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.UserCredentialsDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource" ref="myTargetDataSource"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果 "username" 为空，此代理将直接委托给目标 DataSource 的
 * 标准 {@code getConnection()} 方法。这可以用于保留
 * UserCredentialsDataSourceAdapter Bean 定义，仅作为在特定目标
 * DataSource 需要时<i>可选地</i>隐式传入用户凭据的手段。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #getConnection
 */
public class UserCredentialsDataSourceAdapter extends DelegatingDataSource {

	@Nullable
	private String username;

	@Nullable
	private String password;

	@Nullable
	private String catalog;

	@Nullable
	private String schema;

	private final ThreadLocal<JdbcUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<>("Current JDBC user credentials");


	/**
	 * 设置此适配器在获取 Connection 时应使用的默认用户名。
	 * <p>默认不指定特定用户。请注意，显式指定的用户名
	 * 将始终覆盖在 DataSource 级别指定的任何用户名/密码。
	 * @see #setPassword
	 * @see #setCredentialsForCurrentThread(String, String)
	 * @see #getConnection(String, String)
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 设置此适配器在获取 Connection 时应使用的默认用户密码。
	 * <p>默认不指定特定密码。请注意，显式指定的用户名
	 * 将始终覆盖在 DataSource 级别指定的任何用户名/密码。
	 * @see #setUsername
	 * @see #setCredentialsForCurrentThread(String, String)
	 * @see #getConnection(String, String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 指定要应用到每个获取到的 Connection 的数据库 catalog。
	 * @since 4.3.2
	 * @see Connection#setCatalog
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**
	 * 指定要应用到每个获取到的 Connection 的数据库 schema。
	 * @since 4.3.2
	 * @see Connection#setSchema
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}


	/**
	 * 为此代理和当前线程设置用户凭据。
	 * 给定的用户名和密码将应用到此后在此 DataSource 代理上的
	 * 所有 {@code getConnection()} 调用。
	 * <p>这将覆盖任何静态指定的用户凭据，
	 * 即 "username" 和 "password" Bean 属性的值。
	 * @param username 要应用的用户名
	 * @param password 要应用的密码
	 * @see #removeCredentialsFromCurrentThread
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JdbcUserCredentials(username, password));
	}

	/**
	 * 从当前线程移除此代理的任何用户凭据。
	 * 此后将重新应用静态指定的用户凭据。
	 * @see #setCredentialsForCurrentThread
	 */
	public void removeCredentialsFromCurrentThread() {
		this.threadBoundCredentials.remove();
	}


	/**
	 * 确定当前是否存在线程绑定的凭据：如果存在则使用它们，
	 * 否则回退到静态指定的用户名和密码（即 Bean 属性的值）。
	 * <p>以确定的凭据为参数委托给 {@link #doGetConnection(String, String)}。
	 * @see #doGetConnection
	 */
	@Override
	public Connection getConnection() throws SQLException {
		JdbcUserCredentials threadCredentials = this.threadBoundCredentials.get();
		Connection con = (threadCredentials != null ?
				doGetConnection(threadCredentials.username, threadCredentials.password) :
				doGetConnection(this.username, this.password));

		if (this.catalog != null) {
			con.setCatalog(this.catalog);
		}
		if (this.schema != null) {
			con.setSchema(this.schema);
		}
		return con;
	}

	/**
	 * 直接委托给 {@link #doGetConnection(String, String)}，
	 * 原样保留给定的用户凭据。
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return doGetConnection(username, password);
	}

	/**
	 * 此实现委托给目标 DataSource 的 {@code getConnection(username, password)}
	 * 方法，并传入指定的用户凭据。如果指定的用户名为空，
	 * 则直接委托给目标 DataSource 的标准 {@code getConnection()} 方法。
	 * @param username 要使用的用户名
	 * @param password 要使用的密码
	 * @return 该 Connection
	 * @see javax.sql.DataSource#getConnection(String, String)
	 * @see javax.sql.DataSource#getConnection()
	 */
	protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
		Assert.state(getTargetDataSource() != null, "'targetDataSource' is required");
		if (StringUtils.hasLength(username)) {
			return getTargetDataSource().getConnection(username, password);
		}
		else {
			return getTargetDataSource().getConnection();
		}
	}


	/**
	 * 用作 ThreadLocal 值的内部类。
	 */
	private static final class JdbcUserCredentials {

		public final String username;

		public final String password;

		public JdbcUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JdbcUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}

}
