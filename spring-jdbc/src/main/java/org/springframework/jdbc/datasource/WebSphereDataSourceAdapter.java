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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DataSource} 实现，将所有调用委托给 WebSphere 目标
 * {@link DataSource}（通常从 JNDI 获取），并将当前的隔离级别
 * 和/或当前用户凭据应用到从其中获取的每个 Connection。
 *
 * <p>使用 IBM 特定的 API 从 WebSphere DataSource 获取具有特定隔离级别
 * （和只读标志）的 JDBC Connection
 * （<a href="https://publib.boulder.ibm.com/infocenter/wasinfo/v5r1//topic/com.ibm.websphere.base.doc/info/aes/ae/rdat_extiapi.html">IBM 代码示例</a>）。
 * 支持由 {@link org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()}
 * 暴露的事务特定隔离级别。也可以指定默认隔离级别，在当前 Spring 管理的事务
 * 未定义特定隔离级别时应用。
 *
 * <p>用法示例，将目标 DataSource 定义为内部 Bean 的 JNDI 查找
 * （当然，你也可以通过 Bean 引用链接到任何 WebSphere DataSource）：
 *
 * <pre class="code">
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.WebSphereDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource"&gt;
 *     &lt;bean class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *       &lt;property name="jndiName" value="jdbc/myds"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 感谢 Ricardo Olivieri 提交此方法的原始实现！
 *
 * @author Juergen Hoeller
 * @author <a href="mailto:lari.hotari@sagire.fi">Lari Hotari</a>
 * @author <a href="mailto:roliv@us.ibm.com">Ricardo N. Olivieri</a>
 * @since 2.0.3
 * @see com.ibm.websphere.rsadapter.JDBCConnectionSpec
 * @see com.ibm.websphere.rsadapter.WSDataSource#getConnection(com.ibm.websphere.rsadapter.JDBCConnectionSpec)
 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
 */
public class WebSphereDataSourceAdapter extends IsolationLevelDataSourceAdapter {

	protected final Log logger = LogFactory.getLog(getClass());

	private Class<?> wsDataSourceClass;

	private Method newJdbcConnSpecMethod;

	private Method wsDataSourceGetConnectionMethod;

	private Method setTransactionIsolationMethod;

	private Method setReadOnlyMethod;

	private Method setUserNameMethod;

	private Method setPasswordMethod;


	/**
	 * 此构造函数获取 WebSphere JDBC 连接规格 API，
	 * 以便我们可以通过反射获取特定的 WebSphere Connection。
	 */
	public WebSphereDataSourceAdapter() {
		try {
			this.wsDataSourceClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSDataSource");
			Class<?> jdbcConnSpecClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.JDBCConnectionSpec");
			Class<?> wsrraFactoryClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSRRAFactory");
			this.newJdbcConnSpecMethod = wsrraFactoryClass.getMethod("createJDBCConnectionSpec");
			this.wsDataSourceGetConnectionMethod =
					this.wsDataSourceClass.getMethod("getConnection", jdbcConnSpecClass);
			this.setTransactionIsolationMethod =
					jdbcConnSpecClass.getMethod("setTransactionIsolation", int.class);
			this.setReadOnlyMethod = jdbcConnSpecClass.getMethod("setReadOnly", Boolean.class);
			this.setUserNameMethod = jdbcConnSpecClass.getMethod("setUserName", String.class);
			this.setPasswordMethod = jdbcConnSpecClass.getMethod("setPassword", String.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebSphereDataSourceAdapter because WebSphere API classes are not available: " + ex);
		}
	}

	/**
	 * 检查指定的 'targetDataSource' 实际上是否是
	 * WebSphere WSDataSource。
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		if (!this.wsDataSourceClass.isInstance(getTargetDataSource())) {
			throw new IllegalStateException(
					"Specified 'targetDataSource' is not a WebSphere WSDataSource: " + getTargetDataSource());
		}
	}


	/**
	 * 为当前设置构建一个 WebSphere JDBCConnectionSpec 对象，
	 * 并调用 {@code WSDataSource.getConnection(JDBCConnectionSpec)}。
	 * @see #createConnectionSpec
	 * @see com.ibm.websphere.rsadapter.WSDataSource#getConnection(com.ibm.websphere.rsadapter.JDBCConnectionSpec)
	 */
	@Override
	protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
		// Create JDBCConnectionSpec using current isolation level value and read-only flag.
		Object connSpec = createConnectionSpec(
				getCurrentIsolationLevel(), getCurrentReadOnlyFlag(), username, password);
		if (logger.isDebugEnabled()) {
			logger.debug("Obtaining JDBC Connection from WebSphere DataSource [" +
					getTargetDataSource() + "], using ConnectionSpec [" + connSpec + "]");
		}
		// Create Connection through invoking WSDataSource.getConnection(JDBCConnectionSpec)
		Connection con = (Connection) invokeJdbcMethod(
				this.wsDataSourceGetConnectionMethod, obtainTargetDataSource(), connSpec);
		Assert.state(con != null, "No Connection");
		return con;
	}

	/**
	 * 为给定的特征创建一个 WebSphere {@code JDBCConnectionSpec} 对象。
	 * <p>默认实现使用反射来应用给定的设置。
	 * 可以在子类中重写以自定义 JDBCConnectionSpec 对象
	 * （<a href="https://publib.boulder.ibm.com/infocenter/wasinfo/v6r0/topic/com.ibm.websphere.javadoc.doc/public_html/api/com/ibm/websphere/rsadapter/JDBCConnectionSpec.html">JDBCConnectionSpec javadoc</a>;
	 * <a href="https://www.ibm.com/developerworks/websphere/library/techarticles/0404_tang/0404_tang.html">IBM developerWorks 文章</a>）。
	 * @param isolationLevel 要应用的隔离级别（如果没有则为 {@code null}）
	 * @param readOnlyFlag 要应用的只读标志（如果没有则为 {@code null}）
	 * @param username 要应用的用户名（{@code null} 或空表示默认）
	 * @param password 要应用的密码（可以为 {@code null} 或空）
	 * @throws SQLException 如果由 JDBCConnectionSpec API 方法抛出
	 * @see com.ibm.websphere.rsadapter.JDBCConnectionSpec
	 */
	protected Object createConnectionSpec(@Nullable Integer isolationLevel, @Nullable Boolean readOnlyFlag,
			@Nullable String username, @Nullable String password) throws SQLException {

		Object connSpec = invokeJdbcMethod(this.newJdbcConnSpecMethod, null);
		Assert.state(connSpec != null, "No JDBCConnectionSpec");
		if (isolationLevel != null) {
			invokeJdbcMethod(this.setTransactionIsolationMethod, connSpec, isolationLevel);
		}
		if (readOnlyFlag != null) {
			invokeJdbcMethod(this.setReadOnlyMethod, connSpec, readOnlyFlag);
		}
		// If the username is empty, we'll simply let the target DataSource
		// use its default credentials.
		if (StringUtils.hasLength(username)) {
			invokeJdbcMethod(this.setUserNameMethod, connSpec, username);
			invokeJdbcMethod(this.setPasswordMethod, connSpec, password);
		}
		return connSpec;
	}


	@Nullable
	private static Object invokeJdbcMethod(Method method, @Nullable Object target, @Nullable Object... args)
			throws SQLException {
		try {
			return method.invoke(target, args);
		}
		catch (IllegalAccessException ex) {
			ReflectionUtils.handleReflectionException(ex);
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof SQLException) {
				throw (SQLException) ex.getTargetException();
			}
			ReflectionUtils.handleInvocationTargetException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

}
