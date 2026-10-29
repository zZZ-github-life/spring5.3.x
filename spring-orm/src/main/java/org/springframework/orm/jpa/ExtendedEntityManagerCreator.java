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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 创建各种 {@link javax.persistence.EntityManager} 代理的委托类，
 * 这些代理遵循 JPA 规范中 "扩展"（extended）EntityManager 的语义。
 *
 * <p>支持多种 "扩展" EntityManager 变体：特别是
 * {@link javax.persistence.EntityManagerFactory#createEntityManager()} 所定义的
 * "应用程序管理的扩展 EntityManager"，以及
 * {@link javax.persistence.PersistenceContextType#EXTENDED} 所定义的
 * "容器管理的扩展 EntityManager"。
 *
 * <p>"应用程序管理" 与 "容器管理" 之间的原始区别在于：
 * 在 "应用程序" 场景中需要通过 {@link EntityManager#joinTransaction()} 方法
 * 显式加入外部管理的事务，而在 "容器" 场景中，每次用户级 EntityManager 操作
 * 都会自动加入事务。自 JPA 2.1 起，两种 EntityManager 都支持这两种加入模式，
 * 因此 "应用程序管理" 与 "容器管理" 之间的区别现在主要在于加入模式的默认值，
 * 以及容器管理 EntityManager 受限的生命周期
 * （即与其被注入的对象绑定）。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Paluch
 * @since 2.0
 * @see javax.persistence.EntityManagerFactory#createEntityManager()
 * @see javax.persistence.PersistenceContextType#EXTENDED
 * @see javax.persistence.EntityManager#joinTransaction()
 * @see SharedEntityManagerCreator
 */
public abstract class ExtendedEntityManagerCreator {

	private static final Map<Class<?>, Class<?>[]> cachedEntityManagerInterfaces = new ConcurrentReferenceHashMap<>(4);


	/**
	 * 创建一个应用程序管理的扩展 EntityManager 代理。
	 * @param rawEntityManager the raw EntityManager to decorate
	 * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
	 * and PersistenceUnitInfo from
	 * @return an application-managed EntityManager that can join transactions
	 * but does not participate in them automatically
	 */
	public static EntityManager createApplicationManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

		return createProxy(rawEntityManager, emfInfo, false, false);
	}

	/**
	 * 创建一个应用程序管理的扩展 EntityManager 代理。
	 * @param rawEntityManager the raw EntityManager to decorate
	 * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
	 * and PersistenceUnitInfo from
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return an application-managed EntityManager that can join transactions
	 * but does not participate in them automatically
	 * @since 4.0
	 */
	public static EntityManager createApplicationManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo, boolean synchronizedWithTransaction) {

		return createProxy(rawEntityManager, emfInfo, false, synchronizedWithTransaction);
	}

	/**
	 * 创建一个容器管理的扩展 EntityManager 代理。
	 * @param rawEntityManager the raw EntityManager to decorate
	 * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
	 * and PersistenceUnitInfo from
	 * @return a container-managed EntityManager that will automatically participate
	 * in any managed transaction
	 */
	public static EntityManager createContainerManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

		return createProxy(rawEntityManager, emfInfo, true, true);
	}

	/**
	 * 创建一个容器管理的扩展 EntityManager 代理。
	 * @param emf the EntityManagerFactory to create the EntityManager with.
	 * If this implements the EntityManagerFactoryInfo interface, the corresponding
	 * JpaDialect and PersistenceUnitInfo will be detected accordingly.
	 * @return a container-managed EntityManager that will automatically participate
	 * in any managed transaction
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 */
	public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf) {
		return createContainerManagedEntityManager(emf, null, true);
	}

	/**
	 * 创建一个容器管理的扩展 EntityManager 代理。
	 * @param emf the EntityManagerFactory to create the EntityManager with.
	 * If this implements the EntityManagerFactoryInfo interface, the corresponding
	 * JpaDialect and PersistenceUnitInfo will be detected accordingly.
	 * @param properties the properties to be passed into the {@code createEntityManager}
	 * call (may be {@code null})
	 * @return a container-managed EntityManager that will automatically participate
	 * in any managed transaction
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties) {
		return createContainerManagedEntityManager(emf, properties, true);
	}

	/**
	 * 创建一个容器管理的扩展 EntityManager 代理。
	 * @param emf the EntityManagerFactory to create the EntityManager with.
	 * If this implements the EntityManagerFactoryInfo interface, the corresponding
	 * JpaDialect and PersistenceUnitInfo will be detected accordingly.
	 * @param properties the properties to be passed into the {@code createEntityManager}
	 * call (may be {@code null})
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return a container-managed EntityManager that expects container-driven lifecycle
	 * management but may opt out of automatic transaction synchronization
	 * @since 4.0
	 * @see javax.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	public static EntityManager createContainerManagedEntityManager(
			EntityManagerFactory emf, @Nullable Map<?, ?> properties, boolean synchronizedWithTransaction) {

		Assert.notNull(emf, "EntityManagerFactory must not be null");
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			EntityManager rawEntityManager = emfInfo.createNativeEntityManager(properties);
			return createProxy(rawEntityManager, emfInfo, true, synchronizedWithTransaction);
		}
		else {
			EntityManager rawEntityManager = (!CollectionUtils.isEmpty(properties) ?
					emf.createEntityManager(properties) : emf.createEntityManager());
			return createProxy(rawEntityManager, null, null, null, null, true, synchronizedWithTransaction);
		}
	}


	/**
	 * 实际创建 EntityManager 代理。
	 * @param rawEntityManager raw EntityManager
	 * @param emfInfo the EntityManagerFactoryInfo to obtain the JpaDialect
	 * and PersistenceUnitInfo from
	 * @param containerManaged whether to follow container-managed EntityManager
	 * or application-managed EntityManager semantics
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return the EntityManager proxy
	 */
	private static EntityManager createProxy(EntityManager rawEntityManager,
			EntityManagerFactoryInfo emfInfo, boolean containerManaged, boolean synchronizedWithTransaction) {

		Assert.notNull(emfInfo, "EntityManagerFactoryInfo must not be null");
		JpaDialect jpaDialect = emfInfo.getJpaDialect();
		PersistenceUnitInfo pui = emfInfo.getPersistenceUnitInfo();
		Boolean jta = (pui != null ? pui.getTransactionType() == PersistenceUnitTransactionType.JTA : null);
		return createProxy(rawEntityManager, emfInfo.getEntityManagerInterface(),
				emfInfo.getBeanClassLoader(), jpaDialect, jta, containerManaged, synchronizedWithTransaction);
	}

	/**
	 * 实际创建 EntityManager 代理。
	 * @param rawEm raw EntityManager
	 * @param emIfc the (potentially vendor-specific) EntityManager
	 * interface to proxy, or {@code null} for default detection of all interfaces
	 * @param cl the ClassLoader to use for proxy creation (maybe {@code null})
	 * @param exceptionTranslator the PersistenceException translator to use
	 * @param jta whether to create a JTA-aware EntityManager
	 * (or {@code null} if not known in advance)
	 * @param containerManaged whether to follow container-managed EntityManager
	 * or application-managed EntityManager semantics
	 * @param synchronizedWithTransaction whether to automatically join ongoing
	 * transactions (according to the JPA 2.1 SynchronizationType rules)
	 * @return the EntityManager proxy
	 */
	private static EntityManager createProxy(
			EntityManager rawEm, @Nullable Class<? extends EntityManager> emIfc, @Nullable ClassLoader cl,
			@Nullable PersistenceExceptionTranslator exceptionTranslator, @Nullable Boolean jta,
			boolean containerManaged, boolean synchronizedWithTransaction) {

		Assert.notNull(rawEm, "EntityManager must not be null");
		Class<?>[] interfaces;

		if (emIfc != null) {
			interfaces = cachedEntityManagerInterfaces.computeIfAbsent(emIfc, key -> {
				if (EntityManagerProxy.class.equals(key)) {
					return new Class<?>[] {key};
				}
				return new Class<?>[] {key, EntityManagerProxy.class};
			});
		}
		else {
			interfaces = cachedEntityManagerInterfaces.computeIfAbsent(rawEm.getClass(), key -> {
				Set<Class<?>> ifcs = new LinkedHashSet<>(ClassUtils.getAllInterfacesForClassAsSet(key, cl));
				ifcs.add(EntityManagerProxy.class);
				return ClassUtils.toClassArray(ifcs);
			});
		}

		return (EntityManager) Proxy.newProxyInstance(
				(cl != null ? cl : ExtendedEntityManagerCreator.class.getClassLoader()),
				interfaces,
				new ExtendedEntityManagerInvocationHandler(
						rawEm, exceptionTranslator, jta, containerManaged, synchronizedWithTransaction));
	}


	/**
	 * JPA 规范中定义的扩展 EntityManager 的 InvocationHandler。
	 */
	@SuppressWarnings("serial")
	private static final class ExtendedEntityManagerInvocationHandler implements InvocationHandler, Serializable {

		private static final Log logger = LogFactory.getLog(ExtendedEntityManagerInvocationHandler.class);

		private final EntityManager target;

		@Nullable
		private final PersistenceExceptionTranslator exceptionTranslator;

		private final boolean jta;

		private final boolean containerManaged;

		private final boolean synchronizedWithTransaction;

		private ExtendedEntityManagerInvocationHandler(EntityManager target,
				@Nullable PersistenceExceptionTranslator exceptionTranslator, @Nullable Boolean jta,
				boolean containerManaged, boolean synchronizedWithTransaction) {

			this.target = target;
			this.exceptionTranslator = exceptionTranslator;
			this.jta = (jta != null ? jta : isJtaEntityManager());
			this.containerManaged = containerManaged;
			this.synchronizedWithTransaction = synchronizedWithTransaction;
		}

		private boolean isJtaEntityManager() {
			try {
				this.target.getTransaction();
				return false;
			}
			catch (IllegalStateException ex) {
				logger.debug("Cannot access EntityTransaction handle - assuming we're in a JTA environment");
				return true;
			}
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on EntityManager interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of EntityManager proxy.
					return hashCode();
				case "getTargetEntityManager":
					// Handle EntityManagerProxy interface.
					return this.target;
				case "unwrap":
					// Handle JPA 2.0 unwrap method - could be a proxy match.
					Class<?> targetClass = (Class<?>) args[0];
					if (targetClass == null) {
						return this.target;
					}
					else if (targetClass.isInstance(proxy)) {
						return proxy;
					}
					break;
				case "isOpen":
					if (this.containerManaged) {
						return true;
					}
					break;
				case "close":
					if (this.containerManaged) {
						throw new IllegalStateException("Invalid usage: Cannot close a container-managed EntityManager");
					}
					ExtendedEntityManagerSynchronization synch = (ExtendedEntityManagerSynchronization)
							TransactionSynchronizationManager.getResource(this.target);
					if (synch != null) {
						// Local transaction joined - don't actually call close() before transaction completion
						synch.closeOnCompletion = true;
						return null;
					}
					break;
				case "getTransaction":
					if (this.synchronizedWithTransaction) {
						throw new IllegalStateException(
								"Cannot obtain local EntityTransaction from a transaction-synchronized EntityManager");
					}
					break;
				case "joinTransaction":
					doJoinTransaction(true);
					return null;
				case "isJoinedToTransaction":
					// Handle JPA 2.1 isJoinedToTransaction method for the non-JTA case.
					if (!this.jta) {
						return TransactionSynchronizationManager.hasResource(this.target);
					}
					break;
			}

			// Do automatic joining if required. Excludes toString, equals, hashCode calls.
			if (this.synchronizedWithTransaction && method.getDeclaringClass().isInterface()) {
				doJoinTransaction(false);
			}

			// Invoke method on current EntityManager.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * 加入现有事务（如果尚未加入）。
		 * @param enforce whether to enforce the transaction
		 * (i.e. whether failure to join is considered fatal)
		 */
		private void doJoinTransaction(boolean enforce) {
			if (this.jta) {
				// Let's try whether we're in a JTA transaction.
				try {
					this.target.joinTransaction();
					logger.debug("Joined JTA transaction");
				}
				catch (TransactionRequiredException ex) {
					if (!enforce) {
						logger.debug("No JTA transaction to join: " + ex);
					}
					else {
						throw ex;
					}
				}
			}
			else {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					if (!TransactionSynchronizationManager.hasResource(this.target) &&
							!this.target.getTransaction().isActive()) {
						enlistInCurrentTransaction();
					}
					logger.debug("Joined local transaction");
				}
				else {
					if (!enforce) {
						logger.debug("No local transaction to join");
					}
					else {
						throw new TransactionRequiredException("No local transaction to join");
					}
				}
			}
		}

		/**
		 * 将此应用程序管理的 EntityManager 登记到当前事务中。
		 */
		private void enlistInCurrentTransaction() {
			// Resource local transaction, need to acquire the EntityTransaction,
			// start a transaction now and enlist a synchronization for commit or rollback later.
			EntityTransaction et = this.target.getTransaction();
			et.begin();
			if (logger.isDebugEnabled()) {
				logger.debug("Starting resource-local transaction on application-managed " +
						"EntityManager [" + this.target + "]");
			}
			ExtendedEntityManagerSynchronization extendedEntityManagerSynchronization =
					new ExtendedEntityManagerSynchronization(this.target, this.exceptionTranslator);
			TransactionSynchronizationManager.bindResource(this.target, extendedEntityManagerSynchronization);
			TransactionSynchronizationManager.registerSynchronization(extendedEntityManagerSynchronization);
		}
	}


	/**
	 * 将一个扩展 EntityManager 登记到当前 Spring 事务中的
	 * TransactionSynchronization。
	 */
	private static class ExtendedEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManager>
			implements Ordered {

		private final EntityManager entityManager;

		@Nullable
		private final PersistenceExceptionTranslator exceptionTranslator;

		public volatile boolean closeOnCompletion;

		public ExtendedEntityManagerSynchronization(
				EntityManager em, @Nullable PersistenceExceptionTranslator exceptionTranslator) {

			super(new EntityManagerHolder(em), em);
			this.entityManager = em;
			this.exceptionTranslator = exceptionTranslator;
		}

		@Override
		public int getOrder() {
			return EntityManagerFactoryUtils.ENTITY_MANAGER_SYNCHRONIZATION_ORDER - 1;
		}

		@Override
		protected void flushResource(EntityManagerHolder resourceHolder) {
			try {
				this.entityManager.flush();
			}
			catch (RuntimeException ex) {
				throw convertException(ex);
			}
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return false;
		}

		@Override
		public void afterCommit() {
			super.afterCommit();
			// Trigger commit here to let exceptions propagate to the caller.
			try {
				this.entityManager.getTransaction().commit();
			}
			catch (RuntimeException ex) {
				throw convertException(ex);
			}
		}

		@Override
		public void afterCompletion(int status) {
			try {
				super.afterCompletion(status);
				if (status != STATUS_COMMITTED) {
					// Haven't had an afterCommit call: trigger a rollback.
					try {
						this.entityManager.getTransaction().rollback();
					}
					catch (RuntimeException ex) {
						throw convertException(ex);
					}
				}
			}
			finally {
				if (this.closeOnCompletion) {
					EntityManagerFactoryUtils.closeEntityManager(this.entityManager);
				}
			}
		}

		private RuntimeException convertException(RuntimeException ex) {
			DataAccessException dae = (this.exceptionTranslator != null) ?
					this.exceptionTranslator.translateExceptionIfPossible(ex) :
					EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
			return (dae != null ? dae : ex);
		}
	}

}
