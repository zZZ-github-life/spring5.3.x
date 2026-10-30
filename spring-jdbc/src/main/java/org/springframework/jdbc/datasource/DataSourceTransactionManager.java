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
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * 针对单个 JDBC {@link javax.sql.DataSource} 的
 * {@link org.springframework.transaction.PlatformTransactionManager} 实现。
 * 只要设置中使用 {@code javax.sql.DataSource} 作为其 {@code Connection} 工厂机制，
 * 此类就能在任何环境中与任何 JDBC 驱动程序配合工作。
 * 它会把来自指定 DataSource 的 JDBC Connection 绑定到当前线程，
 * 从而可能为每个 DataSource 提供一个线程绑定的 Connection。
 *
 * <p><b>注意：此事务管理器所操作的 DataSource 需要返回相互独立的 Connection。</b>
 * Connection 可以来自连接池（这是典型情况），但 DataSource 不得返回
 * 线程作用域/请求作用域的 Connection 或类似对象。此事务管理器会根据
 * 指定的传播行为，自己将 Connection 与线程绑定事务关联起来。它假定
 * 即使在事务进行期间，也能获得一个单独的、独立的 Connection。
 *
 * <p>应用程序代码需要通过 {@link DataSourceUtils#getConnection(DataSource)}
 * 而不是标准的 Java EE 风格 {@link DataSource#getConnection()} 调用来获取
 * JDBC Connection。诸如 {@link org.springframework.jdbc.core.JdbcTemplate}
 * 之类的 Spring 类会隐式使用这一策略。如果不与此事务管理器结合使用，
 * {@link DataSourceUtils} 查找策略的行为与原生 DataSource 查找完全一致，
 * 因此可以用可移植的方式使用。
 *
 * <p>另外，也可以允许应用程序代码使用标准的 Java EE 风格查找模式
 * {@link DataSource#getConnection()}，例如用于完全不了解 Spring 的遗留代码。
 * 在这种情况下，为目标 DataSource 定义一个 {@link TransactionAwareDataSourceProxy}，
 * 并将该代理 DataSource 传递给你的 DAO，这样它们在访问时就会自动参与到
 * Spring 管理的事务中。
 *
 * <p>支持自定义隔离级别，以及会按需应用为 JDBC 语句超时的超时设置。
 * 为了支持后者，应用程序代码必须使用 {@link org.springframework.jdbc.core.JdbcTemplate}、
 * 为每个创建的 JDBC Statement 调用 {@link DataSourceUtils#applyTransactionTimeout}，
 * 或者通过 {@link TransactionAwareDataSourceProxy} 进行操作，后者会自动创建
 * 具备超时感知能力的 JDBC Connection 和 Statement。
 *
 * <p>考虑为目标 DataSource 定义一个 {@link LazyConnectionDataSourceProxy}，
 * 并让此事务管理器和你的 DAO 都指向它。这样可以优化“空”事务的处理，
 * 即不执行任何 JDBC 语句的事务。LazyConnectionDataSourceProxy 在
 * Statement 被执行之前不会从目标 DataSource 获取实际的 JDBC Connection，
 * 而是延迟将指定的事务设置应用到目标 Connection 上。
 *
 * <p>此事务管理器通过 JDBC 3.0 的 {@link java.sql.Savepoint} 机制支持嵌套事务。
 * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} 标志默认为
 * "true"，因为在支持保存点的 JDBC 驱动程序（如 Oracle JDBC 驱动程序）上，
 * 嵌套事务可以不受限制地工作。
 *
 * <p>在单资源场景下，此事务管理器可以替代
 * {@link org.springframework.transaction.jta.JtaTransactionManager}，因为它无需
 * 支持 JTA 的容器，通常与本地定义的 JDBC DataSource（例如 Apache Commons DBCP
 * 连接池）结合使用。在此本地策略与 JTA 环境之间切换只是配置的问题！
 *
 * <p>从 4.3.4 开始，此事务管理器会在已注册的事务同步上触发 flush 回调
 * （如果同步通常处于活动状态），并假定资源操作的是底层 JDBC {@code Connection}。
 * 这使得其设置方式与 {@code JtaTransactionManager} 类似，尤其是在
 * 延迟注册的 ORM 资源（例如 Hibernate {@code Session}）方面。
 *
 * <p><b>注意：从 5.3 开始，可以使用 {@link org.springframework.jdbc.support.JdbcTransactionManager}
 * 作为扩展子类，它包含与 {@link org.springframework.jdbc.core.JdbcTemplate} 一致的
 * 提交/回滚异常转换功能。</b>
 *
 * @author Juergen Hoeller
 * @since 02.05.2003
 * @see #setNestedTransactionAllowed
 * @see java.sql.Savepoint
 * @see DataSourceUtils#getConnection(javax.sql.DataSource)
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#releaseConnection
 * @see TransactionAwareDataSourceProxy
 * @see LazyConnectionDataSourceProxy
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
@SuppressWarnings("serial")
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	@Nullable
	private DataSource dataSource;

	private boolean enforceReadOnly = false;


	/**
	 * 创建一个新的 DataSourceTransactionManager 实例。
	 * 必须设置 DataSource 才能使用它。
	 * @see #setDataSource
	 */
	public DataSourceTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * 创建一个新的 DataSourceTransactionManager 实例。
	 * @param dataSource 要为其管理事务的 JDBC DataSource
	 */
	public DataSourceTransactionManager(DataSource dataSource) {
		this();
		setDataSource(dataSource);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的 JDBC DataSource。
	 * <p>这通常是本地定义的 DataSource，例如 Apache Commons DBCP 连接池。
	 * 或者，也可以为从 JNDI 获取的非 XA J2EE DataSource 驱动事务。
	 * 对于 XA DataSource，请使用 JtaTransactionManager。
	 * <p>此处指定的 DataSource 应是用于管理事务的目标 DataSource，
	 * 而不是 TransactionAwareDataSourceProxy。只有数据访问代码可以
	 * 与 TransactionAwareDataSourceProxy 配合工作，而事务管理器
	 * 需要基于底层的目标 DataSource 工作。如果仍然传入了
	 * TransactionAwareDataSourceProxy，它将被解包以提取其目标 DataSource。
	 * <p><b>此处传入的 DataSource 需要返回相互独立的 Connection。</b>
	 * Connection 可以来自连接池（这是典型情况），但 DataSource 不得返回
	 * 线程作用域/请求作用域的 Connection 或类似对象。
	 * @see TransactionAwareDataSourceProxy
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	public void setDataSource(@Nullable DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
			// for its underlying target DataSource, else data access code won't see
			// properly exposed transactions (i.e. transactions for the target DataSource).
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * 返回此实例为其管理事务的 JDBC DataSource。
	 */
	@Nullable
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 获取实际使用的 DataSource。
	 * @return 该 DataSource（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 DataSource
	 * @since 5.0
	 */
	protected DataSource obtainDataSource() {
		DataSource dataSource = getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		return dataSource;
	}

	/**
	 * 指定是否通过在事务连接上显式执行语句
	 * "SET TRANSACTION READ ONLY"（Oracle、MySQL 和 Postgres 所理解的形式）
	 * 来强制事务的只读性质（由 {@link TransactionDefinition#isReadOnly()} 指示）。
	 * <p>确切的处理方式（包括在连接上执行的任何 SQL 语句）
	 * 可以通过 {@link #prepareTransactionalConnection} 进行自定义。
	 * <p>这种只读处理模式超出了 Spring 默认应用的 {@link Connection#setReadOnly}
	 * 提示。与标准的 JDBC 提示不同，"SET TRANSACTION READ ONLY" 会强制执行
	 * 类似隔离级别的连接模式，严格禁止数据操作语句。此外，在 Oracle 上，
	 * 此只读模式会为整个事务提供读一致性。
	 * <p>请注意，较早的 Oracle JDBC 驱动程序（9i、10g）即使对于
	 * {@code Connection.setReadOnly(true} 也会强制实施此只读模式。不过，在较新的
	 * 驱动程序中，这种强制的实施需要显式应用，例如通过此标志。
	 * @since 4.3.7
	 * @see #prepareTransactionalConnection
	 */
	public void setEnforceReadOnly(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * 返回是否通过在事务连接上显式执行语句
	 * 来强制事务的只读性质。
	 * @since 4.3.7
	 * @see #setEnforceReadOnly
	 */
	public boolean isEnforceReadOnly() {
		return this.enforceReadOnly;
	}

	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
	}


	@Override
	public Object getResourceFactory() {
		return obtainDataSource();
	}

	@Override
	protected Object doGetTransaction() {
		DataSourceTransactionObject txObject = new DataSourceTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());
		ConnectionHolder conHolder =
				(ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		Connection con = null;

		try {
			if (!txObject.hasConnectionHolder() ||
					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Connection newCon = obtainDataSource().getConnection();
				if (logger.isDebugEnabled()) {
					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
				}
				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
			}

			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
			con = txObject.getConnectionHolder().getConnection();

			Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
			txObject.setPreviousIsolationLevel(previousIsolationLevel);
			txObject.setReadOnly(definition.isReadOnly());

			// Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
			// so we don't want to do it unnecessarily (for example if we've explicitly
			// configured the connection pool to set it already).
			if (con.getAutoCommit()) {
				txObject.setMustRestoreAutoCommit(true);
				if (logger.isDebugEnabled()) {
					logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
				}
				con.setAutoCommit(false);
			}

			prepareTransactionalConnection(con, definition);
			txObject.getConnectionHolder().setTransactionActive(true);

			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
			}

			// Bind the connection holder to the thread.
			if (txObject.isNewConnectionHolder()) {
				TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
			}
		}

		catch (Throwable ex) {
			if (txObject.isNewConnectionHolder()) {
				DataSourceUtils.releaseConnection(con, obtainDataSource());
				txObject.setConnectionHolder(null, false);
			}
			throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainDataSource());
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing JDBC transaction on Connection [" + con + "]");
		}
		try {
			con.commit();
		}
		catch (SQLException ex) {
			throw translateException("JDBC commit", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
		}
		try {
			con.rollback();
		}
		catch (SQLException ex) {
			throw translateException("JDBC rollback", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() +
					"] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

		// Remove the connection holder from the thread, if exposed.
		if (txObject.isNewConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(obtainDataSource());
		}

		// Reset connection.
		Connection con = txObject.getConnectionHolder().getConnection();
		try {
			if (txObject.isMustRestoreAutoCommit()) {
				con.setAutoCommit(true);
			}
			DataSourceUtils.resetConnectionAfterTransaction(
					con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}

		if (txObject.isNewConnectionHolder()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
			}
			DataSourceUtils.releaseConnection(con, this.dataSource);
		}

		txObject.getConnectionHolder().clear();
	}


	/**
	 * 在事务开始后立即准备事务性 {@code Connection}。
	 * <p>默认实现会在 {@link #setEnforceReadOnly "enforceReadOnly"} 标志
	 * 设置为 {@code true} 且事务定义指示为只读事务时，执行
	 * "SET TRANSACTION READ ONLY" 语句。
	 * <p>"SET TRANSACTION READ ONLY" 能被 Oracle、MySQL 和 Postgres 理解，
	 * 也可能适用于其他数据库。如果你希望调整这一处理方式，
	 * 请相应地重写此方法。
	 * @param con 事务性 JDBC Connection
	 * @param definition 当前事务定义
	 * @throws SQLException 如果由 JDBC API 抛出
	 * @since 4.3.7
	 * @see #setEnforceReadOnly
	 */
	protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
			throws SQLException {

		if (isEnforceReadOnly() && definition.isReadOnly()) {
			try (Statement stmt = con.createStatement()) {
				stmt.executeUpdate("SET TRANSACTION READ ONLY");
			}
		}
	}

	/**
	 * 将给定的 JDBC 提交/回滚异常转换为通用的 Spring 异常，
	 * 以便从 {@link #commit}/{@link #rollback} 调用中传播。
	 * <p>默认实现抛出 {@link TransactionSystemException}。
	 * 子类可以专门识别并发失败等情况。
	 * @param task 任务描述（提交或回滚）
	 * @param ex 从 commit/rollback 抛出的 SQLException
	 * @return 要抛出的转换后异常，
	 * 可以是 {@link org.springframework.dao.DataAccessException} 或
	 * {@link org.springframework.transaction.TransactionException}
	 * @since 5.3
	 */
	protected RuntimeException translateException(String task, SQLException ex) {
		return new TransactionSystemException(task + " failed", ex);
	}


	/**
	 * DataSource 事务对象，表示一个 ConnectionHolder。
	 * 由 DataSourceTransactionManager 用作事务对象。
	 */
	private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {

		private boolean newConnectionHolder;

		private boolean mustRestoreAutoCommit;

		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
			super.setConnectionHolder(connectionHolder);
			this.newConnectionHolder = newConnectionHolder;
		}

		public boolean isNewConnectionHolder() {
			return this.newConnectionHolder;
		}

		public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
			this.mustRestoreAutoCommit = mustRestoreAutoCommit;
		}

		public boolean isMustRestoreAutoCommit() {
			return this.mustRestoreAutoCommit;
		}

		public void setRollbackOnly() {
			getConnectionHolder().setRollbackOnly();
		}

		@Override
		public boolean isRollbackOnly() {
			return getConnectionHolder().isRollbackOnly();
		}

		@Override
		public void flush() {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationUtils.triggerFlush();
			}
		}
	}

}
