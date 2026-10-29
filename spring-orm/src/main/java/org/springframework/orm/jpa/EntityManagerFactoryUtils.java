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

import java.util.Map;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.Ordered;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 辅助类，提供管理 JPA EntityManager 的方法，
 * 允许在事务内复用 EntityManager 实例。
 * 还提供异常转换支持。
 *
 * <p>主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class EntityManagerFactoryUtils {

	/**
	 * 用于清理 JPA EntityManager 的 TransactionSynchronization 对象的顺序值。
	 * 返回 DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100，
	 * 以便在 JDBC Connection 清理（如果有）之前执行 EntityManager 清理。
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int ENTITY_MANAGER_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	private static final Log logger = LogFactory.getLog(EntityManagerFactoryUtils.class);


	/**
	 * 在给定的 Spring 应用上下文（表示为 ListableBeanFactory）中
	 * 查找具有给定名称的 EntityManagerFactory。
	 * <p>如果发现的 EntityManagerFactory 实现了 {@link EntityManagerFactoryInfo}
	 * 接口，则指定的单元名称将与配置的持久化单元进行匹配。否则，
	 * 持久化单元名称将与 Spring bean 名称进行匹配，
	 * 假定 EntityManagerFactory 的 bean 名称遵循该约定。
	 * <p>如果未指定单元名称，此方法将通过
	 * {@link ListableBeanFactory#getBean(Class)} 搜索默认的 EntityManagerFactory。
	 * @param beanFactory the ListableBeanFactory to search
	 * @param unitName the name of the persistence unit (may be {@code null} or empty,
	 * in which case a single bean of type EntityManagerFactory will be searched for)
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
	 * @see EntityManagerFactoryInfo#getPersistenceUnitName()
	 */
	public static EntityManagerFactory findEntityManagerFactory(
			ListableBeanFactory beanFactory, @Nullable String unitName) throws NoSuchBeanDefinitionException {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		if (StringUtils.hasLength(unitName)) {
			// See whether we can find an EntityManagerFactory with matching persistence unit name.
			String[] candidateNames =
					BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class);
			for (String candidateName : candidateNames) {
				EntityManagerFactory emf = (EntityManagerFactory) beanFactory.getBean(candidateName);
				if (emf instanceof EntityManagerFactoryInfo &&
						unitName.equals(((EntityManagerFactoryInfo) emf).getPersistenceUnitName())) {
					return emf;
				}
			}
			// No matching persistence unit found - simply take the EntityManagerFactory
			// with the persistence unit name as bean name (by convention).
			return beanFactory.getBean(unitName, EntityManagerFactory.class);
		}
		else {
			// Find unique EntityManagerFactory bean in the context, falling back to parent contexts.
			return beanFactory.getBean(EntityManagerFactory.class);
		}
	}

	/**
	 * 从给定的工厂获取一个 JPA EntityManager。它能感知绑定到当前线程的
	 * 相应 EntityManager，例如在使用 JpaTransactionManager 时。
	 * <p>注意：如果找不到线程绑定的 EntityManager，将返回 {@code null}！
	 * @param emf the EntityManagerFactory to create the EntityManager with
	 * @return the EntityManager, or {@code null} if none found
	 * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
	 * @see JpaTransactionManager
	 */
	@Nullable
	public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf)
			throws DataAccessResourceFailureException {

		return getTransactionalEntityManager(emf, null);
	}

	/**
	 * 从给定的工厂获取一个 JPA EntityManager。它能感知绑定到当前线程的
	 * 相应 EntityManager，例如在使用 JpaTransactionManager 时。
	 * <p>注意：如果找不到线程绑定的 EntityManager，将返回 {@code null}！
	 * @param emf the EntityManagerFactory to create the EntityManager with
	 * @param properties the properties to be passed into the {@code createEntityManager}
	 * call (may be {@code null})
	 * @return the EntityManager, or {@code null} if none found
	 * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
	 * @see JpaTransactionManager
	 */
	@Nullable
	public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties)
			throws DataAccessResourceFailureException {
		try {
			return doGetTransactionalEntityManager(emf, properties, true);
		}
		catch (PersistenceException ex) {
			throw new DataAccessResourceFailureException("Could not obtain JPA EntityManager", ex);
		}
	}

	/**
	 * 从给定的工厂获取一个 JPA EntityManager。它能感知绑定到当前线程的
	 * 相应 EntityManager，例如在使用 JpaTransactionManager 时。
	 * <p>与 {@code getEntityManager} 相同，但会抛出原始的 PersistenceException。
	 * @param emf the EntityManagerFactory to create the EntityManager with
	 * @param properties the properties to be passed into the {@code createEntityManager}
	 * call (may be {@code null})
	 * @return the EntityManager, or {@code null} if none found
	 * @throws javax.persistence.PersistenceException if the EntityManager couldn't be created
	 * @see #getTransactionalEntityManager(javax.persistence.EntityManagerFactory)
	 * @see JpaTransactionManager
	 */
	@Nullable
	public static EntityManager doGetTransactionalEntityManager(EntityManagerFactory emf, Map<?, ?> properties)
			throws PersistenceException {

		return doGetTransactionalEntityManager(emf, properties, true);
	}

	/**
	 * 从给定的工厂获取一个 JPA EntityManager。它能感知绑定到当前线程的
	 * 相应 EntityManager，例如在使用 JpaTransactionManager 时。
	 * <p>与 {@code getEntityManager} 相同，但会抛出原始的 PersistenceException。
	 * @param emf the EntityManagerFactory to create the EntityManager with
	 * @param properties the properties to be passed into the {@code createEntityManager}
	 * call (may be {@code null})
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return the EntityManager, or {@code null} if none found
	 * @throws javax.persistence.PersistenceException if the EntityManager couldn't be created
	 * @see #getTransactionalEntityManager(javax.persistence.EntityManagerFactory)
	 * @see JpaTransactionManager
	 */
	@Nullable
	public static EntityManager doGetTransactionalEntityManager(
			EntityManagerFactory emf, @Nullable Map<?, ?> properties, boolean synchronizedWithTransaction)
			throws PersistenceException {

		Assert.notNull(emf, "No EntityManagerFactory specified");

		EntityManagerHolder emHolder =
				(EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
		if (emHolder != null) {
			if (synchronizedWithTransaction) {
				if (!emHolder.isSynchronizedWithTransaction()) {
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						// Try to explicitly synchronize the EntityManager itself
						// with an ongoing JTA transaction, if any.
						try {
							emHolder.getEntityManager().joinTransaction();
						}
						catch (TransactionRequiredException ex) {
							logger.debug("Could not join transaction because none was actually active", ex);
						}
					}
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						Object transactionData = prepareTransaction(emHolder.getEntityManager(), emf);
						TransactionSynchronizationManager.registerSynchronization(
								new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, false));
						emHolder.setSynchronizedWithTransaction(true);
					}
				}
				// Use holder's reference count to track synchronizedWithTransaction access.
				// isOpen() check used below to find out about it.
				emHolder.requested();
				return emHolder.getEntityManager();
			}
			else {
				// unsynchronized EntityManager demanded
				if (emHolder.isTransactionActive() && !emHolder.isOpen()) {
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						return null;
					}
					// EntityManagerHolder with an active transaction coming from JpaTransactionManager,
					// with no synchronized EntityManager having been requested by application code before.
					// Unbind in order to register a new unsynchronized EntityManager instead.
					TransactionSynchronizationManager.unbindResource(emf);
				}
				else {
					// Either a previously bound unsynchronized EntityManager, or the application
					// has requested a synchronized EntityManager before and therefore upgraded
					// this transaction's EntityManager to synchronized before.
					return emHolder.getEntityManager();
				}
			}
		}
		else if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			// Indicate that we can't obtain a transactional EntityManager.
			return null;
		}

		// Create a new EntityManager for use within the current transaction.
		logger.debug("Opening JPA EntityManager");
		EntityManager em = null;
		if (!synchronizedWithTransaction) {
			try {
				em = emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED, properties);
			}
			catch (AbstractMethodError err) {
				// JPA 2.1 API available but method not actually implemented in persistence provider:
				// falling back to regular createEntityManager method.
			}
		}
		if (em == null) {
			em = (!CollectionUtils.isEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
		}

		try {
			// Use same EntityManager for further JPA operations within the transaction.
			// Thread-bound object will get removed by synchronization at transaction completion.
			emHolder = new EntityManagerHolder(em);
			if (synchronizedWithTransaction) {
				Object transactionData = prepareTransaction(em, emf);
				TransactionSynchronizationManager.registerSynchronization(
						new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, true));
				emHolder.setSynchronizedWithTransaction(true);
			}
			else {
				// Unsynchronized - just scope it for the transaction, as demanded by the JPA 2.1 spec...
				TransactionSynchronizationManager.registerSynchronization(
						new TransactionScopedEntityManagerSynchronization(emHolder, emf));
			}
			TransactionSynchronizationManager.bindResource(emf, emHolder);
		}
		catch (RuntimeException ex) {
			// Unexpected exception from external delegation call -> close EntityManager and rethrow.
			closeEntityManager(em);
			throw ex;
		}

		return em;
	}

	/**
	 * 在给定的 EntityManager 上准备事务（如果可能）。
	 * @param em the EntityManager to prepare
	 * @param emf the EntityManagerFactory that the EntityManager has been created with
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into cleanupTransaction)
	 * @see JpaDialect#prepareTransaction
	 */
	@Nullable
	private static Object prepareTransaction(EntityManager em, EntityManagerFactory emf) {
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				return jpaDialect.prepareTransaction(em,
						TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
						TransactionSynchronizationManager.getCurrentTransactionName());
			}
		}
		return null;
	}

	/**
	 * 在给定的 EntityManager 上清理事务（如果可能）。
	 * @param transactionData arbitrary object that holds transaction data, if any
	 * (as returned by prepareTransaction)
	 * @param emf the EntityManagerFactory that the EntityManager has been created with
	 * @see JpaDialect#cleanupTransaction
	 */
	private static void cleanupTransaction(@Nullable Object transactionData, EntityManagerFactory emf) {
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				jpaDialect.cleanupTransaction(transactionData);
			}
		}
	}

	/**
	 * 将当前事务超时（如果有）应用于给定的 JPA Query 对象。
	 * <p>此方法会相应地设置 JPA 2.0 查询提示 "javax.persistence.query.timeout"。
	 * @param query the JPA Query object
	 * @param emf the JPA EntityManagerFactory that the Query was created for
	 */
	public static void applyTransactionTimeout(Query query, EntityManagerFactory emf) {
		EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
		if (emHolder != null && emHolder.hasTimeout()) {
			int timeoutValue = (int) emHolder.getTimeToLiveInMillis();
			try {
				query.setHint("javax.persistence.query.timeout", timeoutValue);
			}
			catch (IllegalArgumentException ex) {
				// oh well, at least we tried...
			}
		}
	}

	/**
	 * 将给定的运行时异常转换为 {@code org.springframework.dao} 层次结构中
	 * 的适当异常。如果不需要转换则返回 null：任何其他异常可能
	 * 源自用户代码，不应进行转换。
	 * <p>此处涵盖了最重要的情形，如对象未找到或乐观锁失败。
	 * 对于更细粒度的转换，JpaTransactionManager 等支持
	 * 通过 JpaDialect 进行复杂的异常转换。
	 * @param ex runtime exception that occurred
	 * @return the corresponding DataAccessException instance,
	 * or {@code null} if the exception should not be translated
	 */
	@Nullable
	public static DataAccessException convertJpaAccessExceptionIfPossible(RuntimeException ex) {
		// Following the JPA specification, a persistence provider can also
		// throw these two exceptions, besides PersistenceException.
		if (ex instanceof IllegalStateException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof IllegalArgumentException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		// Check for well-known PersistenceException subclasses.
		if (ex instanceof EntityNotFoundException) {
			return new JpaObjectRetrievalFailureException((EntityNotFoundException) ex);
		}
		if (ex instanceof NoResultException) {
			return new EmptyResultDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof QueryTimeoutException) {
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage(), ex);
		}
		if (ex instanceof LockTimeoutException) {
			return new CannotAcquireLockException(ex.getMessage(), ex);
		}
		if (ex instanceof PessimisticLockException) {
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof OptimisticLockException) {
			return new JpaOptimisticLockingFailureException((OptimisticLockException) ex);
		}
		if (ex instanceof EntityExistsException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof TransactionRequiredException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		// If we have another kind of PersistenceException, throw it.
		if (ex instanceof PersistenceException) {
			return new JpaSystemException(ex);
		}

		// If we get here, we have an exception that resulted from user code,
		// rather than the persistence provider, so we return null to indicate
		// that translation should not occur.
		return null;
	}

	/**
	 * 关闭给定的 JPA EntityManager，
	 * 并捕获和记录任何清理异常。
	 * @param em the JPA EntityManager to close (may be {@code null})
	 * @see javax.persistence.EntityManager#close()
	 */
	public static void closeEntityManager(@Nullable EntityManager em) {
		if (em != null) {
			try {
				if (em.isOpen()) {
					em.close();
				}
			}
			catch (Throwable ex) {
				logger.error("Failed to release JPA EntityManager", ex);
			}
		}
	}


	/**
	 * 用于在非 JPA 事务结束时进行资源清理的回调
	 * （例如在参与 JtaTransactionManager 事务时），
	 * 与正在进行的事务完全同步。
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class TransactionalEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
			implements Ordered {

		@Nullable
		private final Object transactionData;

		@Nullable
		private final JpaDialect jpaDialect;

		private final boolean newEntityManager;

		public TransactionalEntityManagerSynchronization(
				EntityManagerHolder emHolder, EntityManagerFactory emf, @Nullable Object txData, boolean newEm) {

			super(emHolder, emf);
			this.transactionData = txData;
			this.jpaDialect = (emf instanceof EntityManagerFactoryInfo ?
					((EntityManagerFactoryInfo) emf).getJpaDialect() : null);
			this.newEntityManager = newEm;
		}

		@Override
		public int getOrder() {
			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER;
		}

		@Override
		protected void flushResource(EntityManagerHolder resourceHolder) {
			EntityManager em = resourceHolder.getEntityManager();
			if (em instanceof EntityManagerProxy) {
				EntityManager target = ((EntityManagerProxy) em).getTargetEntityManager();
				if (TransactionSynchronizationManager.hasResource(target)) {
					// ExtendedEntityManagerSynchronization active after joinTransaction() call:
					// flush synchronization already registered.
					return;
				}
			}
			try {
				em.flush();
			}
			catch (RuntimeException ex) {
				DataAccessException dae;
				if (this.jpaDialect != null) {
					dae = this.jpaDialect.translateExceptionIfPossible(ex);
				}
				else {
					dae = convertJpaAccessExceptionIfPossible(ex);
				}
				throw (dae != null ? dae : ex);
			}
		}

		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newEntityManager;
		}

		@Override
		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
			closeEntityManager(resourceHolder.getEntityManager());
		}

		@Override
		protected void cleanupResource(
				EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey, boolean committed) {

			if (!committed) {
				// Clear all pending inserts/updates/deletes in the EntityManager.
				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
				resourceHolder.getEntityManager().clear();
			}
			cleanupTransaction(this.transactionData, resourceKey);
		}
	}


	/**
	 * 最简回调，仅在事务结束时关闭 EntityManager。
	 */
	private static class TransactionScopedEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
			implements Ordered {

		public TransactionScopedEntityManagerSynchronization(EntityManagerHolder emHolder, EntityManagerFactory emf) {
			super(emHolder, emf);
		}

		@Override
		public int getOrder() {
			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER + 1;
		}

		@Override
		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
			closeEntityManager(resourceHolder.getEntityManager());
		}
	}

}
