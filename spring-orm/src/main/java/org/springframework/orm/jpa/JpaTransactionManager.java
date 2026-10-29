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

package org.springframework.orm.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 针对单个 JPA {@link javax.persistence.EntityManagerFactory} 的
 * {@link org.springframework.transaction.PlatformTransactionManager} 实现。
 * 它从指定的工厂中绑定一个 JPA EntityManager 到线程，可能允许每个工厂
 * 有一个线程绑定的 EntityManager。{@link SharedEntityManagerCreator} 和
 * {@code @PersistenceContext} 能感知线程绑定的 EntityManager，
 * 并自动参与此类事务。支持此事务管理机制的 JPA 访问代码
 * 需要使用其中之一。
 *
 * <p>此事务管理器适用于使用单个 JPA EntityManagerFactory
 * 进行事务性数据访问的应用程序。如果要在同一事务中访问多个事务性资源，
 * 则需要使用 JTA（通常通过
 * {@link org.springframework.transaction.jta.JtaTransactionManager}）。
 * 请注意，你需要相应地配置 JPA 提供者，使其能够参与 JTA 事务。
 *
 * <p>此事务管理器还支持在事务中直接访问 DataSource
 * （即使用同一 DataSource 的纯 JDBC 代码）。
 * 这样便可以混合使用访问 JPA 的服务和使用纯 JDBC 的服务（无需感知 JPA）！
 * 应用程序代码需要遵循与
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * 相同的简单 Connection 查找模式
 * （即 {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * 或通过 {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}）。
 * 请注意，这需要配置供应商特定的 {@link JpaDialect}。
 *
 * <p>注意：为了能为纯 JDBC 代码注册 DataSource 的 Connection，
 * 此实例需要感知 DataSource（{@link #setDataSource}）。
 * 显然，给定的 DataSource 应与给定的 EntityManagerFactory 所使用的 DataSource 一致。
 * 此事务管理器将自动检测用作 EntityManagerFactory 连接工厂的 DataSource，
 * 因此通常无需显式指定 "dataSource" 属性。
 *
 * <p>此事务管理器通过 JDBC 3.0 Savepoints 支持嵌套事务。
 * 不过 {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} 标志
 * 默认为 {@code false}，因为嵌套事务仅适用于 JDBC Connection，
 * 而不适用于 JPA EntityManager 及其缓存的实体对象和相关上下文。
 * 如果你希望对参与 JPA 事务的 JDBC 访问代码使用嵌套事务
 * （前提是你的 JDBC 驱动程序支持 Savepoints），可以手动将该标志设置为 {@code true}。
 * <i>请注意，JPA 本身并不支持嵌套事务！因此，不要期望 JPA 访问代码
 * 在语义上参与嵌套事务。</i>
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see #setDataSource
 * @see LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
@SuppressWarnings("serial")
public class JpaTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	@Nullable
	private EntityManagerFactory entityManagerFactory;

	@Nullable
	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<>();

	@Nullable
	private DataSource dataSource;

	private JpaDialect jpaDialect = new DefaultJpaDialect();

	@Nullable
	private Consumer<EntityManager> entityManagerInitializer;


	/**
	 * 创建一个新的 JpaTransactionManager 实例。
	 * <p>必须先设置 EntityManagerFactory 才能使用。
	 * @see #setEntityManagerFactory
	 */
	public JpaTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * 创建一个新的 JpaTransactionManager 实例。
	 * @param emf the EntityManagerFactory to manage transactions for
	 */
	public JpaTransactionManager(EntityManagerFactory emf) {
		this();
		this.entityManagerFactory = emf;
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的 EntityManagerFactory。
	 * <p>也可以指定目标 EntityManagerFactory 的持久化单元名称。
	 * 默认情况下，将通过查找所在 BeanFactory 中唯一的
	 * EntityManagerFactory 类型 bean 来获取默认的 EntityManagerFactory。
	 * @see #setPersistenceUnitName
	 */
	public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * 返回此实例应为其管理事务的 EntityManagerFactory。
	 */
	@Nullable
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * 获取供实际使用的 EntityManagerFactory。
	 * @return the EntityManagerFactory (never {@code null})
	 * @throws IllegalStateException in case of no EntityManagerFactory set
	 * @since 5.0
	 */
	protected final EntityManagerFactory obtainEntityManagerFactory() {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "No EntityManagerFactory set");
		return emf;
	}

	/**
	 * 设置要为其管理事务的持久化单元名称。
	 * <p>这是直接引用指定 EntityManagerFactory 的替代方式，
	 * 改为按持久化单元名称解析它。如果既未指定 EntityManagerFactory
	 * 也未指定持久化单元名称，则将通过查找唯一的
	 * EntityManagerFactory 类型 bean 来获取默认的 EntityManagerFactory。
	 * @see #setEntityManagerFactory
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回要为其管理事务的持久化单元名称（如果有）。
	 */
	@Nullable
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定要传入 {@code EntityManagerFactory.createEntityManager(Map)}
	 * 的 JPA 属性（如果有）。
	 * <p>可以通过 String 类型的 "value"（由 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素来填充。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaProperties(@Nullable Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 以 Map 形式指定要传入 {@code EntityManagerFactory.createEntityManager(Map)}
	 * 的 JPA 属性（如果有）。
	 * <p>可以通过 XML bean 定义中的 "map" 或 "props" 元素来填充。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public void setJpaPropertyMap(@Nullable Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问要传递给持久化提供者的 JPA 属性，
	 * 并可以选择添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过 {@code jpaPropertyMap[myKey]}。
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * 设置此实例应为其管理事务的 JDBC DataSource。
	 * 该 DataSource 应与 JPA EntityManagerFactory 所使用的 DataSource 一致：
	 * 例如，可以为两者指定同一个 JNDI DataSource。
	 * <p>如果 EntityManagerFactory 使用已知的 DataSource 作为其连接工厂，
	 * 则会自动检测该 DataSource：这种情况下你仍然可以显式指定 DataSource，
	 * 但不必这样做。
	 * <p>此 DataSource 的事务性 JDBC Connection 将提供给直接通过
	 * DataSourceUtils 或 JdbcTemplate 访问该 DataSource 的应用程序代码。
	 * 该 Connection 将从 JPA EntityManager 中获取。
	 * <p>请注意，需要为特定的 JPA 实现使用相应的 JPA 方言，
	 * 才能将 JPA 事务暴露为 JDBC 事务。
	 * <p>此处指定的 DataSource 应是用于管理事务的目标 DataSource，
	 * 而不是 TransactionAwareDataSourceProxy。只有数据访问代码
	 * 可以使用 TransactionAwareDataSourceProxy，而事务管理器需要
	 * 在底层的目标 DataSource 上工作。如果仍然传入了
	 * TransactionAwareDataSourceProxy，它将被解包以提取其目标 DataSource。
	 * @see EntityManagerFactoryInfo#getDataSource()
	 * @see #setJpaDialect
	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
	 * @see org.springframework.jdbc.datasource.DataSourceUtils
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
	 * 设置此事务管理器要使用的 JPA 方言。
	 * 用于供应商特定的事务管理和 JDBC 连接暴露。
	 * <p>如果 EntityManagerFactory 使用已知的 JpaDialect，则会自动检测：
	 * 这种情况下你仍然可以显式指定 DataSource，但不必这样做。
	 * <p>方言对象可用于获取底层 JDBC 连接，
	 * 从而允许将 JPA 事务暴露为 JDBC 事务。
	 * @see EntityManagerFactoryInfo#getJpaDialect()
	 * @see JpaDialect#beginTransaction
	 * @see JpaDialect#getJdbcConnection
	 */
	public void setJpaDialect(@Nullable JpaDialect jpaDialect) {
		this.jpaDialect = (jpaDialect != null ? jpaDialect : new DefaultJpaDialect());
	}

	/**
	 * 返回此事务管理器要使用的 JPA 方言。
	 */
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * 指定一个回调，用于自定义为此 {@code JpaTransactionManager} 管理的
	 * 新事务而创建的每个 {@code EntityManager} 资源。
	 * <p>这是工厂级 {@code EntityManager} 定制器和
	 * {@code JpaVendorAdapter} 级 {@code postProcessEntityManager} 回调的替代方式，
	 * 支持对事务性资源进行特定定制。
	 * @since 5.3
	 * @see #createEntityManagerForTransaction()
	 * @see AbstractEntityManagerFactoryBean#setEntityManagerInitializer
	 * @see JpaVendorAdapter#postProcessEntityManager
	 */
	public void setEntityManagerInitializer(Consumer<EntityManager> entityManagerInitializer) {
		this.entityManagerInitializer = entityManagerInitializer;
	}

	/**
	 * 如果未显式设置 EntityManagerFactory，则按持久化单元名称获取它。
	 * 如果未指定持久化单元，则回退到默认的 EntityManagerFactory bean。
	 * @see #setPersistenceUnitName
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}

	/**
	 * 尽早初始化 JPA 方言，如果未设置，则为指定的 EntityManagerFactory
	 * 创建一个默认方言。自动检测 EntityManagerFactory 的 DataSource（如果有）。
	 */
	@Override
	public void afterPropertiesSet() {
		if (getEntityManagerFactory() == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		if (getEntityManagerFactory() instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) getEntityManagerFactory();
			DataSource dataSource = emfInfo.getDataSource();
			if (dataSource != null) {
				setDataSource(dataSource);
			}
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				setJpaDialect(jpaDialect);
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return obtainEntityManagerFactory();
	}

	@Override
	protected Object doGetTransaction() {
		JpaTransactionObject txObject = new JpaTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		EntityManagerHolder emHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
		if (emHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound EntityManager [" + emHolder.getEntityManager() +
						"] for JPA transaction");
			}
			txObject.setEntityManagerHolder(emHolder, false);
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
		return ((JpaTransactionObject) transaction).hasTransaction();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! JpaTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single JpaTransactionManager for all transactions " +
					"on a single DataSource, no matter whether JPA or JDBC access.");
		}

		try {
			if (!txObject.hasEntityManagerHolder() ||
					txObject.getEntityManagerHolder().isSynchronizedWithTransaction()) {
				EntityManager newEm = createEntityManagerForTransaction();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new EntityManager [" + newEm + "] for JPA transaction");
				}
				txObject.setEntityManagerHolder(new EntityManagerHolder(newEm), true);
			}

			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

			// Delegate to JpaDialect for actual transaction begin.
			int timeoutToUse = determineTimeout(definition);
			Object transactionData = getJpaDialect().beginTransaction(em,
					new JpaTransactionDefinition(definition, timeoutToUse, txObject.isNewEntityManagerHolder()));
			txObject.setTransactionData(transactionData);
			txObject.setReadOnly(definition.isReadOnly());

			// Register transaction timeout.
			if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getEntityManagerHolder().setTimeoutInSeconds(timeoutToUse);
			}

			// Register the JPA EntityManager's JDBC Connection for the DataSource, if set.
			if (getDataSource() != null) {
				ConnectionHandle conHandle = getJpaDialect().getJdbcConnection(em, definition.isReadOnly());
				if (conHandle != null) {
					ConnectionHolder conHolder = new ConnectionHolder(conHandle);
					if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
						conHolder.setTimeoutInSeconds(timeoutToUse);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing JPA transaction as JDBC [" + conHandle + "]");
					}
					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
					txObject.setConnectionHolder(conHolder);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not exposing JPA transaction [" + em + "] as JDBC transaction because " +
								"JpaDialect [" + getJpaDialect() + "] does not support JDBC Connection retrieval");
					}
				}
			}

			// Bind the entity manager holder to the thread.
			if (txObject.isNewEntityManagerHolder()) {
				TransactionSynchronizationManager.bindResource(
						obtainEntityManagerFactory(), txObject.getEntityManagerHolder());
			}
			txObject.getEntityManagerHolder().setSynchronizedWithTransaction(true);
		}

		catch (TransactionException ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw ex;
		}
		catch (Throwable ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", ex);
		}
	}

	/**
	 * 创建一个用于事务的 JPA EntityManager。
	 * <p>默认实现会检查 EntityManagerFactory 是否为 Spring 代理，
	 * 如果可能，则委托给 {@link EntityManagerFactoryInfo#createNativeEntityManager}，
	 * 后者会进而应用
	 * {@link JpaVendorAdapter#postProcessEntityManager(EntityManager)}。
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 */
	protected EntityManager createEntityManagerForTransaction() {
		EntityManagerFactory emf = obtainEntityManagerFactory();
		Map<String, Object> properties = getJpaPropertyMap();
		EntityManager em;
		if (emf instanceof EntityManagerFactoryInfo) {
			em = ((EntityManagerFactoryInfo) emf).createNativeEntityManager(properties);
		}
		else {
			em = (!CollectionUtils.isEmpty(properties) ?
					emf.createEntityManager(properties) : emf.createEntityManager());
		}
		if (this.entityManagerInitializer != null) {
			this.entityManagerInitializer.accept(em);
		}
		return em;
	}

	/**
	 * 关闭当前事务的 EntityManager。
	 * 在事务开启尝试失败后调用。
	 * @param txObject the current transaction
	 */
	protected void closeEntityManagerAfterFailedBegin(JpaTransactionObject txObject) {
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			try {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
			catch (Throwable ex) {
				logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
			}
			finally {
				EntityManagerFactoryUtils.closeEntityManager(em);
			}
			txObject.setEntityManagerHolder(null, false);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
		txObject.setEntityManagerHolder(null, false);
		EntityManagerHolder entityManagerHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.unbindResource(obtainEntityManagerFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(entityManagerHolder, connectionHolder);
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(
				obtainEntityManagerFactory(), resourcesHolder.getEntityManagerHolder());
		if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	/**
	 * 此实现返回 "true"：JPA 提交将正确处理
	 * 在全局级别被标记为仅回滚的事务。
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			tx.commit();
		}
		catch (RollbackException ex) {
			if (ex.getCause() instanceof RuntimeException) {
				DataAccessException dae = getJpaDialect().translateExceptionIfPossible((RuntimeException) ex.getCause());
				if (dae != null) {
					throw dae;
				}
			}
			throw new TransactionSystemException("Could not commit JPA transaction", ex);
		}
		catch (RuntimeException ex) {
			// Assumably failed to flush changes to database.
			throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		catch (PersistenceException ex) {
			throw new TransactionSystemException("Could not roll back JPA transaction", ex);
		}
		finally {
			if (!txObject.isNewEntityManagerHolder()) {
				// Clear all pending inserts/updates/deletes in the EntityManager.
				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
				txObject.getEntityManagerHolder().getEntityManager().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		// Remove the entity manager holder from the thread, if still there.
		// (Could have been removed by EntityManagerFactoryUtils in order
		// to replace it with an unsynchronized EntityManager).
		if (txObject.isNewEntityManagerHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(obtainEntityManagerFactory());
		}
		txObject.getEntityManagerHolder().clear();

		// Remove the JDBC connection holder from the thread, if exposed.
		if (getDataSource() != null && txObject.hasConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
			ConnectionHandle conHandle = txObject.getConnectionHolder().getConnectionHandle();
			if (conHandle != null) {
				try {
					getJpaDialect().releaseJdbcConnection(conHandle,
							txObject.getEntityManagerHolder().getEntityManager());
				}
				catch (Throwable ex) {
					// Just log it, to keep a transaction-related exception.
					logger.error("Failed to release JDBC connection after transaction", ex);
				}
			}
		}

		getJpaDialect().cleanupTransaction(txObject.getTransactionData());

		// Remove the entity manager holder from the thread.
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing JPA EntityManager [" + em + "] after transaction");
			}
			EntityManagerFactoryUtils.closeEntityManager(em);
		}
		else {
			logger.debug("Not closing pre-bound JPA EntityManager after transaction");
		}
	}


	/**
	 * JPA 事务对象，表示一个 EntityManagerHolder。
	 * 由 JpaTransactionManager 用作事务对象。
	 */
	private class JpaTransactionObject extends JdbcTransactionObjectSupport {

		@Nullable
		private EntityManagerHolder entityManagerHolder;

		private boolean newEntityManagerHolder;

		@Nullable
		private Object transactionData;

		public void setEntityManagerHolder(
				@Nullable EntityManagerHolder entityManagerHolder, boolean newEntityManagerHolder) {

			this.entityManagerHolder = entityManagerHolder;
			this.newEntityManagerHolder = newEntityManagerHolder;
		}

		public EntityManagerHolder getEntityManagerHolder() {
			Assert.state(this.entityManagerHolder != null, "No EntityManagerHolder available");
			return this.entityManagerHolder;
		}

		public boolean hasEntityManagerHolder() {
			return (this.entityManagerHolder != null);
		}

		public boolean isNewEntityManagerHolder() {
			return this.newEntityManagerHolder;
		}

		public boolean hasTransaction() {
			return (this.entityManagerHolder != null && this.entityManagerHolder.isTransactionActive());
		}

		public void setTransactionData(@Nullable Object transactionData) {
			this.transactionData = transactionData;
			getEntityManagerHolder().setTransactionActive(true);
			if (transactionData instanceof SavepointManager) {
				getEntityManagerHolder().setSavepointManager((SavepointManager) transactionData);
			}
		}

		@Nullable
		public Object getTransactionData() {
			return this.transactionData;
		}

		public void setRollbackOnly() {
			EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.setRollbackOnly();
			}
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
			return tx.getRollbackOnly();
		}

		@Override
		public void flush() {
			try {
				getEntityManagerHolder().getEntityManager().flush();
			}
			catch (RuntimeException ex) {
				throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
			}
		}

		@Override
		public Object createSavepoint() throws TransactionException {
			if (getEntityManagerHolder().isRollbackOnly()) {
				throw new CannotCreateTransactionException(
						"Cannot create savepoint for transaction which is already marked as rollback-only");
			}
			return getSavepointManager().createSavepoint();
		}

		@Override
		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().rollbackToSavepoint(savepoint);
			getEntityManagerHolder().resetRollbackOnly();
		}

		@Override
		public void releaseSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().releaseSavepoint(savepoint);
		}

		private SavepointManager getSavepointManager() {
			if (!isSavepointAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions");
			}
			SavepointManager savepointManager = getEntityManagerHolder().getSavepointManager();
			if (savepointManager == null) {
				throw new NestedTransactionNotSupportedException(
						"JpaDialect does not support savepoints - check your JPA provider's capabilities");
			}
			return savepointManager;
		}
	}


	/**
	 * 要传递给 {@link JpaDialect#beginTransaction} 的 JPA 特定事务定义。
	 * @since 5.1
	 */
	private static class JpaTransactionDefinition extends DelegatingTransactionDefinition
			implements ResourceTransactionDefinition {

		private final int timeout;

		private final boolean localResource;

		public JpaTransactionDefinition(TransactionDefinition targetDefinition, int timeout, boolean localResource) {
			super(targetDefinition);
			this.timeout = timeout;
			this.localResource = localResource;
		}

		@Override
		public int getTimeout() {
			return this.timeout;
		}

		@Override
		public boolean isLocalResource() {
			return this.localResource;
		}
	}


	/**
	 * 挂起资源的持有器。
	 * 供 {@code doSuspend} 和 {@code doResume} 内部使用。
	 */
	private static final class SuspendedResourcesHolder {

		private final EntityManagerHolder entityManagerHolder;

		@Nullable
		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(EntityManagerHolder emHolder, @Nullable ConnectionHolder conHolder) {
			this.entityManagerHolder = emHolder;
			this.connectionHolder = conHolder;
		}

		private EntityManagerHolder getEntityManagerHolder() {
			return this.entityManagerHolder;
		}

		@Nullable
		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}

}
