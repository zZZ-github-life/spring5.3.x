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

package org.springframework.orm.hibernate5;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.function.Consumer;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 用于单个 Hibernate {@link SessionFactory} 的
 * {@link org.springframework.transaction.PlatformTransactionManager} 实现。
 * 将从指定工厂获取的 Hibernate Session 绑定到线程，
 * 从而可能允许每个工厂对应一个线程绑定的 Session。
 * 需要支持此事务处理机制的 Hibernate 访问代码必须使用
 * {@code SessionFactory.getCurrentSession()}，
 * 并将 SessionFactory 配置为使用 {@link SpringSessionContext}。
 *
 * <p>支持自定义隔离级别，以及会作为
 * Hibernate 事务超时应用的超时设置。
 *
 * <p>此事务管理器适用于使用单个 Hibernate SessionFactory
 * 进行事务性数据访问的应用程序，但它也支持在事务内
 * 直接访问 DataSource（即使用同一 DataSource 的普通 JDBC 代码）。
 * 这允许混合使用访问 Hibernate 的服务和使用普通 JDBC
 * 的服务（后者无需感知 Hibernate）！
 * 应用程序代码需要遵循与
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * 相同的简单 Connection 查找模式
 * （即 {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * 或通过
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}）。
 *
 * <p>注意：为了能够为普通 JDBC 代码注册 DataSource 的 Connection，
 * 此实例需要知道 DataSource（{@link #setDataSource}）。
 * 给定的 DataSource 显然应与给定 SessionFactory 所使用的 DataSource 一致。
 *
 * <p>若要在同一事务中访问多个事务性资源，需要使用 JTA
 * （通常通过 {@link org.springframework.transaction.jta.JtaTransactionManager}）。
 * 在这种场景下，Hibernate 使用的 DataSource 需要启用 JTA
 * （参见容器配置）。
 *
 * <p>此事务管理器通过 JDBC 3.0 Savepoints 支持嵌套事务。
 * 不过，{@link #setNestedTransactionAllowed} "nestedTransactionAllowed"} 标志
 * 默认为 "false"，因为嵌套事务只会应用于 JDBC Connection，
 * 而不会应用于 Hibernate Session 及其缓存的实体对象和相关上下文。
 * 如果你希望对参与 Hibernate 事务的 JDBC 访问代码使用嵌套事务
 * （前提是你的 JDBC 驱动支持 Savepoints），可以手动将该标志设置为 "true"。
 * <i>请注意，Hibernate 本身并不支持嵌套事务！因此，不要指望 Hibernate 访问代码
 * 在语义上参与嵌套事务。</i>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setSessionFactory
 * @see #setDataSource
 * @see SessionFactory#getCurrentSession()
 * @see DataSourceUtils#getConnection
 * @see DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
@SuppressWarnings("serial")
public class HibernateTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	@Nullable
	private SessionFactory sessionFactory;

	@Nullable
	private DataSource dataSource;

	private boolean autodetectDataSource = true;

	private boolean prepareConnection = true;

	private boolean allowResultAccessAfterCompletion = false;

	private boolean hibernateManagedSession = false;

	@Nullable
	private Consumer<Session> sessionInitializer;

	@Nullable
	private Object entityInterceptor;

	/**
	 * 仅用于 entityInterceptorBeanName。
	 * @see #setEntityInterceptorBeanName
	 */
	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 创建一个新的 HibernateTransactionManager 实例。
	 * 必须设置 SessionFactory 才能使用它。
	 * @see #setSessionFactory
	 */
	public HibernateTransactionManager() {
	}

	/**
	 * 创建一个新的 HibernateTransactionManager 实例。
	 * @param sessionFactory 要为其管理事务的 SessionFactory
	 */
	public HibernateTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的 SessionFactory。
	 */
	public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回此实例应为其管理事务的 SessionFactory。
	 */
	@Nullable
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * 获取实际使用的 SessionFactory。
	 * @return SessionFactory（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 SessionFactory
	 * @since 5.0
	 */
	protected final SessionFactory obtainSessionFactory() {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory set");
		return sessionFactory;
	}

	/**
	 * 设置此实例应为其管理事务的 JDBC DataSource。
	 * DataSource 应与 Hibernate SessionFactory 所使用的 DataSource 一致：
	 * 例如，可以为两者指定相同的 JNDI DataSource。
	 * <p>如果 SessionFactory 使用 LocalDataSourceConnectionProvider 配置，
	 * 即通过 Spring 的 LocalSessionFactoryBean 并指定 "dataSource"，
	 * 则会自动检测 DataSource：你仍然可以显式指定
	 * DataSource，但在这种情况下无需这样做。
	 * <p>通过 DataSourceUtils 或 JdbcTemplate 直接访问此 DataSource 的
	 * 应用程序代码将获得此 DataSource 的事务性 JDBC Connection。
	 * 该 Connection 将从 Hibernate Session 中获取。
	 * <p>此处指定的 DataSource 应是用于管理事务的目标 DataSource，
	 * 而不是 TransactionAwareDataSourceProxy。只有数据访问
	 * 代码可以使用 TransactionAwareDataSourceProxy，而事务
	 * 管理器需要基于底层的目标 DataSource 工作。如果
	 * 仍然传入了 TransactionAwareDataSourceProxy，它将被
	 * 解包以提取其目标 DataSource。
	 * <p><b>注意：对于包含大量仅从 Hibernate 缓存读取数据
	 * （而不实际访问数据库）的事务的场景，考虑为实际的目标 DataSource
	 * 使用 {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}。
	 * 或者，考虑将 {@link #setPrepareConnection "prepareConnection"} 切换为 {@code false}。</b>
	 * 在这两种情况下，此事务管理器都将不再为每个 Hibernate Session
	 * 急切地获取 JDBC Connection（自 Spring 5.1 起）。
	 * @see #setAutodetectDataSource
	 * @see TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
	 * @see org.springframework.jdbc.core.JdbcTemplate
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
	 * 设置是否自动检测 Hibernate SessionFactory 所使用的 JDBC DataSource，
	 * 前提是通过 LocalSessionFactoryBean 的 {@code setDataSource} 进行了设置。默认值为 "true"。
	 * <p>可以关闭此选项以有意忽略可用的 DataSource，从而
	 * 不为该 DataSource 将 Hibernate 事务暴露为 JDBC 事务。
	 * @see #setDataSource
	 */
	public void setAutodetectDataSource(boolean autodetectDataSource) {
		this.autodetectDataSource = autodetectDataSource;
	}

	/**
	 * 设置是否准备事务性 Hibernate Session 底层的 JDBC Connection，
	 * 即是否将事务特定的隔离级别和/或事务的只读标志
	 * 应用到该底层 JDBC Connection。
	 * <p>默认值为 "true"。如果关闭此标志，事务管理器
	 * 将不再支持每事务隔离级别。它也将不再为只读事务
	 * 调用 {@code Connection.setReadOnly(true)}。
	 * 如果关闭此标志，事务结束后无需清理 JDBC Connection，
	 * 因为不会修改任何 Connection 设置。
	 * @see Connection#setTransactionIsolation
	 * @see Connection#setReadOnly
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.prepareConnection = prepareConnection;
	}

	/**
	 * 设置是否允许在事务完成后访问结果，通常通过 Hibernate 的
	 * ScrollableResults 机制。
	 * <p>默认值为 "false"。开启此标志会在底层 JDBC Connection 上强制使用
	 * 超过提交后可保持性（over-commit holdability）（如果 {@link #prepareConnection "prepareConnection"} 开启），
	 * 并跳过完成时断开连接的步骤。
	 * @see Connection#setHoldability
	 * @see ResultSet#HOLD_CURSORS_OVER_COMMIT
	 * @see #disconnectOnCompletion(Session)
	 */
	public void setAllowResultAccessAfterCompletion(boolean allowResultAccessAfterCompletion) {
		this.allowResultAccessAfterCompletion = allowResultAccessAfterCompletion;
	}

	/**
	 * 设置是操作 Hibernate 管理的 Session 而不是
	 * Spring 管理的 Session，即是否通过
	 * Hibernate 的 {@link SessionFactory#getCurrentSession()} 获取 Session，
	 * 而不是通过 {@link SessionFactory#openSession()}（后者前面会有
	 * Spring {@link TransactionSynchronizationManager} 检查）。
	 * <p>默认值为 "false"，即使用 Spring 管理的 Session：如果存在
	 * 当前线程绑定的 Session（例如在 Open-Session-in-View 场景中）则使用它，
	 * 否则为当前事务创建一个新 Session。
	 * <p>将此标志切换为 "true" 可强制使用 Hibernate 管理的 Session。
	 * 注意这要求 {@link SessionFactory#getCurrentSession()}
	 * 在针对 Spring 管理的事务调用时始终返回合适的 Session；
	 * 如果 {@code getCurrentSession()} 调用失败，事务开启将失败。
	 * <p>此模式通常与自定义 Hibernate
	 * {@link org.hibernate.context.spi.CurrentSessionContext} 实现结合使用，
	 * 后者将 Session 存储在 Spring 的 TransactionSynchronizationManager 以外的位置。
	 * 它也可以与 Spring 的 Open-Session-in-View 支持结合使用
	 * （使用 Spring 默认的 {@link SpringSessionContext}），在这种情况下它与
	 * Spring 管理的 Session 模式有细微差别：在这种场景下，预先绑定的 Session
	 * <i>不会</i>收到 {@code clear()} 调用（回滚时）或 {@code disconnect()}
	 * 调用（事务完成时）；这些操作将留给自定义的 CurrentSessionContext 实现
	 * （如果需要）。
	 */
	public void setHibernateManagedSession(boolean hibernateManagedSession) {
		this.hibernateManagedSession = hibernateManagedSession;
	}

	/**
	 * 指定一个回调，用于自定义为此 {@code HibernateTransactionManager}
	 * 管理的每个新事务创建的 Hibernate {@code Session} 资源。
	 * <p>这为应用程序目的提供了便捷的自定义方式，例如
	 * 设置 Hibernate 过滤器。
	 * @since 5.3
	 * @see Session#enableFilter
	 */
	public void setSessionInitializer(Consumer<Session> sessionInitializer) {
		this.sessionInitializer = sessionInitializer;
	}

	/**
	 * 设置 Hibernate 实体拦截器的 bean 名称，该拦截器允许在写入和读取数据库
	 * 之前检查和更改属性值。
	 * 将应用于此事务管理器创建的任何新 Session。
	 * <p>需要已知 bean 工厂，以便在创建 Session 时能够将 bean
	 * 名称解析为拦截器实例。通常用于原型拦截器，
	 * 即每个 Session 一个拦截器实例。
	 * <p>也可用于共享拦截器实例，但在这种场景下
	 * 建议直接设置拦截器引用。
	 * @param entityInterceptorBeanName Bean 工厂中实体拦截器的名称
	 * @see #setBeanFactory
	 * @see #setEntityInterceptor
	 */
	public void setEntityInterceptorBeanName(String entityInterceptorBeanName) {
		this.entityInterceptor = entityInterceptorBeanName;
	}

	/**
	 * 设置 Hibernate 实体拦截器，该拦截器允许在写入和读取数据库
	 * 之前检查和更改属性值。
	 * 将应用于此事务管理器创建的任何新 Session。
	 * <p>此类拦截器既可以在 SessionFactory 级别设置，
	 * 即在 LocalSessionFactoryBean 上设置，也可以在 Session 级别设置，
	 * 即在 HibernateTransactionManager 上设置。
	 * @see LocalSessionFactoryBean#setEntityInterceptor
	 */
	public void setEntityInterceptor(@Nullable Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * 返回当前的 Hibernate 实体拦截器，如果没有则返回 {@code null}。
	 * 如有必要，通过 bean 工厂解析实体拦截器的 bean 名称。
	 * @throws IllegalStateException 如果指定了 Bean 名称但未设置 Bean 工厂
	 * @throws BeansException 如果通过 Bean 工厂解析 Bean 名称失败
	 * @see #setEntityInterceptor
	 * @see #setEntityInterceptorBeanName
	 * @see #setBeanFactory
	 */
	@Nullable
	public Interceptor getEntityInterceptor() throws IllegalStateException, BeansException {
		if (this.entityInterceptor instanceof Interceptor) {
			return (Interceptor) this.entityInterceptor;
		}
		else if (this.entityInterceptor instanceof String) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("Cannot get entity interceptor via bean name if no bean factory set");
			}
			String beanName = (String) this.entityInterceptor;
			return this.beanFactory.getBean(beanName, Interceptor.class);
		}
		else {
			return null;
		}
	}

	/**
	 * bean 工厂仅需用于解析实体拦截器的 bean 名称。
	 * 对于任何其他操作模式，都无需设置它。
	 * @see #setEntityInterceptorBeanName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
		if (this.entityInterceptor instanceof String && this.beanFactory == null) {
			throw new IllegalArgumentException("Property 'beanFactory' is required for 'entityInterceptorBeanName'");
		}

		// Check for SessionFactory's DataSource.
		if (this.autodetectDataSource && getDataSource() == null) {
			DataSource sfds = SessionFactoryUtils.getDataSource(getSessionFactory());
			if (sfds != null) {
				// Use the SessionFactory's DataSource for exposing transactions to JDBC code.
				if (logger.isDebugEnabled()) {
					logger.debug("Using DataSource [" + sfds +
							"] of Hibernate SessionFactory for HibernateTransactionManager");
				}
				setDataSource(sfds);
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return obtainSessionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		HibernateTransactionObject txObject = new HibernateTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		SessionFactory sessionFactory = obtainSessionFactory();
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() + "] for Hibernate transaction");
			}
			txObject.setSessionHolder(sessionHolder);
		}
		else if (this.hibernateManagedSession) {
			try {
				Session session = sessionFactory.getCurrentSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Found Hibernate-managed Session [" + session + "] for Spring-managed transaction");
				}
				txObject.setExistingSession(session);
			}
			catch (HibernateException ex) {
				throw new DataAccessResourceFailureException(
						"Could not obtain Hibernate-managed Session for Spring-managed transaction", ex);
			}
		}

		if (getDataSource() != null) {
			ConnectionHolder conHolder = (ConnectionHolder)
					TransactionSynchronizationManager.getResource(getDataSource());
			txObject.setConnectionHolder(conHolder);
		}

		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		return (txObject.hasSpringManagedTransaction() ||
				(this.hibernateManagedSession && txObject.hasHibernateManagedTransaction()));
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! HibernateTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single HibernateTransactionManager for all transactions " +
					"on a single DataSource, no matter whether Hibernate or JDBC access.");
		}

		SessionImplementor session = null;

		try {
			if (!txObject.hasSessionHolder() || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Interceptor entityInterceptor = getEntityInterceptor();
				Session newSession = (entityInterceptor != null ?
						obtainSessionFactory().withOptions().interceptor(entityInterceptor).openSession() :
						obtainSessionFactory().openSession());
				if (this.sessionInitializer != null) {
					this.sessionInitializer.accept(newSession);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + newSession + "] for Hibernate transaction");
				}
				txObject.setSession(newSession);
			}

			session = txObject.getSessionHolder().getSession().unwrap(SessionImplementor.class);

			boolean holdabilityNeeded = this.allowResultAccessAfterCompletion && !txObject.isNewSession();
			boolean isolationLevelNeeded = (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT);
			if (holdabilityNeeded || isolationLevelNeeded || definition.isReadOnly()) {
				if (this.prepareConnection && ConnectionReleaseMode.ON_CLOSE.equals(
						session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode())) {
					// We're allowed to change the transaction settings of the JDBC Connection.
					if (logger.isDebugEnabled()) {
						logger.debug("Preparing JDBC Connection of Hibernate Session [" + session + "]");
					}
					Connection con = session.connection();
					Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
					txObject.setPreviousIsolationLevel(previousIsolationLevel);
					txObject.setReadOnly(definition.isReadOnly());
					if (this.allowResultAccessAfterCompletion && !txObject.isNewSession()) {
						int currentHoldability = con.getHoldability();
						if (currentHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
							txObject.setPreviousHoldability(currentHoldability);
							con.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
						}
					}
					txObject.connectionPrepared();
				}
				else {
					// Not allowed to change the transaction settings of the JDBC Connection.
					if (isolationLevelNeeded) {
						// We should set a specific isolation level but are not allowed to...
						throw new InvalidIsolationLevelException(
								"HibernateTransactionManager is not allowed to support custom isolation levels: " +
								"make sure that its 'prepareConnection' flag is on (the default) and that the " +
								"Hibernate connection release mode is set to ON_CLOSE.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Not preparing JDBC Connection of Hibernate Session [" + session + "]");
					}
				}
			}

			if (definition.isReadOnly() && txObject.isNewSession()) {
				// Just set to MANUAL in case of a new Session for this transaction.
				session.setHibernateFlushMode(FlushMode.MANUAL);
				// As of 5.1, we're also setting Hibernate's read-only entity mode by default.
				session.setDefaultReadOnly(true);
			}

			if (!definition.isReadOnly() && !txObject.isNewSession()) {
				// We need AUTO or COMMIT for a non-read-only transaction.
				FlushMode flushMode = session.getHibernateFlushMode();
				if (FlushMode.MANUAL.equals(flushMode)) {
					session.setHibernateFlushMode(FlushMode.AUTO);
					txObject.getSessionHolder().setPreviousFlushMode(flushMode);
				}
			}

			Transaction hibTx;

			// Register transaction timeout.
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				// Use Hibernate's own transaction timeout mechanism on Hibernate 3.1+
				// Applies to all statements, also to inserts, updates and deletes!
				hibTx = session.getTransaction();
				hibTx.setTimeout(timeout);
				hibTx.begin();
			}
			else {
				// Open a plain Hibernate transaction without specified timeout.
				hibTx = session.beginTransaction();
			}

			// Add the Hibernate transaction to the session holder.
			txObject.getSessionHolder().setTransaction(hibTx);

			// Register the Hibernate Session's JDBC Connection for the DataSource, if set.
			if (getDataSource() != null) {
				ConnectionHolder conHolder = new ConnectionHolder(session::connection);
				if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
					conHolder.setTimeoutInSeconds(timeout);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Exposing Hibernate transaction as JDBC [" + conHolder.getConnectionHandle() + "]");
				}
				TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
				txObject.setConnectionHolder(conHolder);
			}

			// Bind the session holder to the thread.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(obtainSessionFactory(), txObject.getSessionHolder());
			}
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);
		}

		catch (Throwable ex) {
			if (txObject.isNewSession()) {
				try {
					if (session != null && session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
						session.getTransaction().rollback();
					}
				}
				catch (Throwable ex2) {
					logger.debug("Could not rollback Session after failed transaction begin", ex);
				}
				finally {
					SessionFactoryUtils.closeSession(session);
					txObject.setSessionHolder(null);
				}
			}
			throw new CannotCreateTransactionException("Could not open Hibernate Session for transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		txObject.setSessionHolder(null);
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(sessionHolder, connectionHolder);
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		SessionFactory sessionFactory = obtainSessionFactory();

		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// From non-transactional code running in active transaction synchronization
			// -> can be safely removed, will be closed on transaction completion.
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		TransactionSynchronizationManager.bindResource(sessionFactory, resourcesHolder.getSessionHolder());
		if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		Transaction hibTx = txObject.getSessionHolder().getTransaction();
		Assert.state(hibTx != null, "No Hibernate transaction");
		if (status.isDebug()) {
			logger.debug("Committing Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try {
			hibTx.commit();
		}
		catch (org.hibernate.TransactionException ex) {
			// assumably from commit call to the underlying JDBC connection
			throw new TransactionSystemException("Could not commit Hibernate transaction", ex);
		}
		catch (HibernateException ex) {
			// assumably failed to flush changes to database
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		Transaction hibTx = txObject.getSessionHolder().getTransaction();
		Assert.state(hibTx != null, "No Hibernate transaction");
		if (status.isDebug()) {
			logger.debug("Rolling back Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try {
			hibTx.rollback();
		}
		catch (org.hibernate.TransactionException ex) {
			throw new TransactionSystemException("Could not roll back Hibernate transaction", ex);
		}
		catch (HibernateException ex) {
			// Shouldn't really happen, as a rollback doesn't cause a flush.
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
		finally {
			if (!txObject.isNewSession() && !this.hibernateManagedSession) {
				// Clear all pending inserts/updates/deletes in the Session.
				// Necessary for pre-bound Sessions, to avoid inconsistent state.
				txObject.getSessionHolder().getSession().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		// Remove the session holder from the thread.
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		}

		// Remove the JDBC connection holder from the thread, if exposed.
		if (getDataSource() != null) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
		}

		SessionImplementor session = txObject.getSessionHolder().getSession().unwrap(SessionImplementor.class);
		if (txObject.needsConnectionReset() &&
				session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected()) {
			// We're running with connection release mode ON_CLOSE: We're able to reset
			// the isolation level and/or read-only flag of the JDBC Connection here.
			// Else, we need to rely on the connection pool to perform proper cleanup.
			try {
				Connection con = session.connection();
				Integer previousHoldability = txObject.getPreviousHoldability();
				if (previousHoldability != null) {
					con.setHoldability(previousHoldability);
				}
				DataSourceUtils.resetConnectionAfterTransaction(
						con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
			}
			catch (HibernateException ex) {
				logger.debug("Could not access JDBC Connection of Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Could not reset JDBC Connection after transaction", ex);
			}
		}

		if (txObject.isNewSession()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Hibernate Session [" + session + "] after transaction");
			}
			SessionFactoryUtils.closeSession(session);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not closing pre-bound Hibernate Session [" + session + "] after transaction");
			}
			if (txObject.getSessionHolder().getPreviousFlushMode() != null) {
				session.setHibernateFlushMode(txObject.getSessionHolder().getPreviousFlushMode());
			}
			if (!this.allowResultAccessAfterCompletion && !this.hibernateManagedSession) {
				disconnectOnCompletion(session);
			}
		}
		txObject.getSessionHolder().clear();
	}

	/**
	 * 在事务完成时断开预先存在的 Hibernate Session，
	 * 返回其数据库连接但保留其实体状态。
	 * <p>默认实现仅调用 {@link Session#disconnect()}。
	 * 子类可以重写此方法以执行空操作或精细调整的断开连接逻辑。
	 * @param session 要断开的 Hibernate Session
	 * @see Session#disconnect()
	 */
	protected void disconnectOnCompletion(Session session) {
		session.disconnect();
	}

	/**
	 * 将给定的 HibernateException 转换为 {@code org.springframework.dao} 层次结构中
	 * 相应的异常。
	 * <p>会自动将指定的 SQLExceptionTranslator 应用于
	 * Hibernate JDBCException，否则依赖 Hibernate 的默认转换。
	 * @param ex 发生的 HibernateException
	 * @return 相应的 DataAccessException
	 * @see SessionFactoryUtils#convertHibernateAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}


	/**
	 * Hibernate 事务对象，表示一个 SessionHolder。
	 * 由 HibernateTransactionManager 用作事务对象。
	 */
	private class HibernateTransactionObject extends JdbcTransactionObjectSupport {

		@Nullable
		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private boolean newSession;

		private boolean needsConnectionReset;

		@Nullable
		private Integer previousHoldability;

		public void setSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = true;
		}

		public void setExistingSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = false;
		}

		public void setSessionHolder(@Nullable SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = false;
			this.newSession = false;
		}

		public SessionHolder getSessionHolder() {
			Assert.state(this.sessionHolder != null, "No SessionHolder available");
			return this.sessionHolder;
		}

		public boolean hasSessionHolder() {
			return (this.sessionHolder != null);
		}

		public boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		public boolean isNewSession() {
			return this.newSession;
		}

		public void connectionPrepared() {
			this.needsConnectionReset = true;
		}

		public boolean needsConnectionReset() {
			return this.needsConnectionReset;
		}

		public void setPreviousHoldability(@Nullable Integer previousHoldability) {
			this.previousHoldability = previousHoldability;
		}

		@Nullable
		public Integer getPreviousHoldability() {
			return this.previousHoldability;
		}

		public boolean hasSpringManagedTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.getTransaction() != null);
		}

		public boolean hasHibernateManagedTransaction() {
			return (this.sessionHolder != null &&
					this.sessionHolder.getSession().getTransaction().getStatus() == TransactionStatus.ACTIVE);
		}

		public void setRollbackOnly() {
			getSessionHolder().setRollbackOnly();
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			return getSessionHolder().isRollbackOnly() ||
					(hasConnectionHolder() && getConnectionHolder().isRollbackOnly());
		}

		@Override
		public void flush() {
			try {
				getSessionHolder().getSession().flush();
			}
			catch (HibernateException ex) {
				throw convertHibernateAccessException(ex);
			}
			catch (PersistenceException ex) {
				if (ex.getCause() instanceof HibernateException) {
					throw convertHibernateAccessException((HibernateException) ex.getCause());
				}
				throw ex;
			}
		}
	}


	/**
	 * 挂起资源的持有器。
	 * 供 {@code doSuspend} 和 {@code doResume} 内部使用。
	 */
	private static final class SuspendedResourcesHolder {

		private final SessionHolder sessionHolder;

		@Nullable
		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(SessionHolder sessionHolder, @Nullable ConnectionHolder conHolder) {
			this.sessionHolder = sessionHolder;
			this.connectionHolder = conHolder;
		}

		private SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		@Nullable
		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}

}
