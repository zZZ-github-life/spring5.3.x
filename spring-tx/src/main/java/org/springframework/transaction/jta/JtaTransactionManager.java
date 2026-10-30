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

package org.springframework.transaction.jta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于JTA的{@link org.springframework.transaction.PlatformTransactionManager}实现,
 * 委托给后端JTA提供者。通常用于委托给Java EE服务器的事务协调器,
 * 但也可以配置嵌入在应用程序中的本地JTA提供者。
 *
 * <p>此事务管理器适用于处理分布式事务, 即跨越多个资源的事务,
 * 以及通常用于控制应用服务器资源(例如JNDI中可用的JDBC DataSource)上的事务。
 * 对于单个JDBC DataSource, DataSourceTransactionManager完全足够;
 * 例如, 对于使用Hibernate访问单个资源(包括事务缓存), HibernateTransactionManager是合适的。
 *
 * <p><b>对于典型的JTA事务(REQUIRED、SUPPORTS、MANDATORY、NEVER), 一个简单的
 * JtaTransactionManager定义就是你所需要的, 可在所有Java EE服务器之间移植。</b>
 * 这对应于JTA UserTransaction的功能, Java EE为其指定了标准JNDI名称
 * ("java:comp/UserTransaction")。对于此类JTA用法, 无需配置服务器特定的TransactionManager查找。
 *
 * <p><b>事务挂起(REQUIRES_NEW、NOT_SUPPORTED)仅在注册了JTA TransactionManager时可用。</b>
 * 常见的TransactionManager位置由JtaTransactionManager自动检测, 前提是
 * "autodetectTransactionManager"标志设置为"true"(默认情况下如此)。
 *
 * <p>注意: Java EE不要求支持JTA TransactionManager接口。几乎所有Java EE服务器都暴露它,
 * 但作为EE的扩展。尽管TransactionManager接口是JTA的一部分, 但可能存在一些兼容性问题。
 * 因此, Spring提供了各种厂商特定的PlatformTransactionManager,
 * 在适当时建议使用: {@link WebLogicJtaTransactionManager}和{@link WebSphereUowTransactionManager}。
 * 对于所有其他Java EE服务器, 标准的JtaTransactionManager就足够了。
 *
 * <p>这个纯JtaTransactionManager类支持超时但不支持每事务隔离级别。
 * 自定义子类可以重写{@link #doJtaBegin}方法以提供特定JTA扩展来实现此功能;
 * Spring为WebLogic Server包含了一个相应的{@link WebLogicJtaTransactionManager}类。
 * 此类特定Java EE事务协调器的适配器也可以暴露事务名称用于监控;
 * 对于标准JTA, 事务名称将被简单地忽略。
 *
 * <p><b>考虑使用Spring的{@code tx:jta-transaction-manager}配置元素来自动选择适当的
 * JTA平台事务管理器(自动检测WebLogic和WebSphere)。</b>
 *
 * <p>JTA 1.1添加了TransactionSynchronizationRegistry功能, 作为标准JTA UserTransaction句柄
 * 之外的公共Java EE 5 API。从Spring 2.5开始, 此JtaTransactionManager自动检测
 * TransactionSynchronizationRegistry, 并在参与现有JTA事务(例如由EJB CMT控制)时
 * 使用它来注册Spring管理的同步。如果没有可用的TransactionSynchronizationRegistry,
 * 则此类同步将通过(非EE)JTA TransactionManager句柄进行注册。
 *
 * <p>此类是可序列化的。但是, 活动同步在序列化后不会保留。
 *
 * @author Juergen Hoeller
 * @since 24.03.2003
 * @see javax.transaction.UserTransaction
 * @see javax.transaction.TransactionManager
 * @see javax.transaction.TransactionSynchronizationRegistry
 * @see #setUserTransactionName
 * @see #setUserTransaction
 * @see #setTransactionManagerName
 * @see #setTransactionManager
 * @see WebLogicJtaTransactionManager
 */
@SuppressWarnings("serial")
public class JtaTransactionManager extends AbstractPlatformTransactionManager
		implements TransactionFactory, InitializingBean, Serializable {

	/**
	 * JTA UserTransaction的默认JNDI位置。许多Java EE服务器也在此处提供对JTA TransactionManager接口的支持。
	 * @see #setUserTransactionName
	 * @see #setAutodetectTransactionManager
	 */
	public static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

	/**
	 * JTA TransactionManager的回退JNDI位置。如果JTA UserTransaction没有实现JTA TransactionManager接口,
	 * 且"autodetectTransactionManager"标志为"true", 则应用此位置。
	 * @see #setTransactionManagerName
	 * @see #setAutodetectTransactionManager
	 */
	public static final String[] FALLBACK_TRANSACTION_MANAGER_NAMES =
			new String[] {"java:comp/TransactionManager", "java:appserver/TransactionManager",
					"java:pm/TransactionManager", "java:/TransactionManager"};

	/**
	 * JTA TransactionSynchronizationRegistry的标准Java EE 5 JNDI位置。
	 * 可用时自动检测。
	 */
	public static final String DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME =
			"java:comp/TransactionSynchronizationRegistry";


	private transient JndiTemplate jndiTemplate = new JndiTemplate();

	@Nullable
	private transient UserTransaction userTransaction;

	@Nullable
	private String userTransactionName;

	private boolean autodetectUserTransaction = true;

	private boolean cacheUserTransaction = true;

	private boolean userTransactionObtainedFromJndi = false;

	@Nullable
	private transient TransactionManager transactionManager;

	@Nullable
	private String transactionManagerName;

	private boolean autodetectTransactionManager = true;

	@Nullable
	private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;

	@Nullable
	private String transactionSynchronizationRegistryName;

	private boolean autodetectTransactionSynchronizationRegistry = true;

	private boolean allowCustomIsolationLevels = false;


	/**
	 * 创建一个新的JtaTransactionManager实例, 作为bean进行配置。
	 * 调用{@code afterPropertiesSet}以激活配置。
	 * @see #setUserTransactionName
	 * @see #setUserTransaction
	 * @see #setTransactionManagerName
	 * @see #setTransactionManager
	 * @see #afterPropertiesSet()
	 */
	public JtaTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * 创建一个新的JtaTransactionManager实例。
	 * @param userTransaction 用作直接引用的 JTA UserTransaction
	 */
	public JtaTransactionManager(UserTransaction userTransaction) {
		this();
		Assert.notNull(userTransaction, "UserTransaction must not be null");
		this.userTransaction = userTransaction;
	}

	/**
	 * 创建一个新的JtaTransactionManager实例。
	 * @param userTransaction 用作直接引用的 JTA UserTransaction
	 * @param transactionManager 用作直接引用的 JTA TransactionManager
	 */
	public JtaTransactionManager(UserTransaction userTransaction, TransactionManager transactionManager) {
		this();
		Assert.notNull(userTransaction, "UserTransaction must not be null");
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.userTransaction = userTransaction;
		this.transactionManager = transactionManager;
	}

	/**
	 * 创建一个新的JtaTransactionManager实例。
	 * @param transactionManager 用作直接引用的 JTA TransactionManager
	 */
	public JtaTransactionManager(TransactionManager transactionManager) {
		this();
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
		this.userTransaction = buildUserTransaction(transactionManager);
	}


	/**
	 * 设置用于JNDI查找的JndiTemplate。如果未设置, 则使用默认值。
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		Assert.notNull(jndiTemplate, "JndiTemplate must not be null");
		this.jndiTemplate = jndiTemplate;
	}

	/**
	 * 返回用于JNDI查找的JndiTemplate。
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * 设置用于JNDI查找的JNDI环境。使用给定的环境设置创建JndiTemplate。
	 * @see #setJndiTemplate
	 */
	public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 返回用于JNDI查找的JNDI环境。
	 */
	@Nullable
	public Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}


	/**
	 * 设置要用作直接引用的JTA UserTransaction。
	 * <p>通常仅用于本地JTA设置; 在Java EE环境中, UserTransaction将始终从JNDI获取。
	 * @see #setUserTransactionName
	 * @see #setAutodetectUserTransaction
	 */
	public void setUserTransaction(@Nullable UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * 返回此事务管理器使用的JTA UserTransaction。
	 */
	@Nullable
	public UserTransaction getUserTransaction() {
		return this.userTransaction;
	}

	/**
	 * 设置JTA UserTransaction的JNDI名称。
	 * <p>请注意, 如果未显式指定, UserTransaction将在Java EE默认位置
	 * "java:comp/UserTransaction"自动检测。
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 * @see #setUserTransaction
	 * @see #setAutodetectUserTransaction
	 */
	public void setUserTransactionName(String userTransactionName) {
		this.userTransactionName = userTransactionName;
	}

	/**
	 * 设置是否在Java EE指定的默认JNDI位置"java:comp/UserTransaction"自动检测JTA UserTransaction。
	 * 如果未找到, 将在没有UserTransaction的情况下继续。
	 * <p>默认为"true", 自动检测UserTransaction, 除非已显式指定。
	 * 关闭此标志以允许JtaTransactionManager仅针对TransactionManager运行,
	 * 即使默认的UserTransaction可用。
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 */
	public void setAutodetectUserTransaction(boolean autodetectUserTransaction) {
		this.autodetectUserTransaction = autodetectUserTransaction;
	}

	/**
	 * 设置是否缓存从JNDI获取的JTA UserTransaction对象。
	 * <p>默认为"true": UserTransaction查找仅在启动时发生, 对所有线程的所有事务重用相同的UserTransaction句柄。
	 * 对于提供共享UserTransaction对象(典型情况)的所有应用服务器, 这是最有效的选择。
	 * <p>关闭此标志以强制为每个事务重新查找UserTransaction。
	 * 这仅对于为每个事务返回新UserTransaction的应用服务器是必要的,
	 * 这些服务器将状态绑定到UserTransaction对象本身而不是当前线程。
	 * @see #setUserTransactionName
	 */
	public void setCacheUserTransaction(boolean cacheUserTransaction) {
		this.cacheUserTransaction = cacheUserTransaction;
	}

	/**
	 * 设置要用作直接引用的JTA TransactionManager。
	 * <p>TransactionManager对于挂起和恢复事务是必需的, 因为UserTransaction接口不支持此功能。
	 * <p>请注意, 如果JTA UserTransaction对象也实现了JTA TransactionManager接口,
	 * 则TransactionManager将被自动检测, 以及在各已知的回退JNDI位置自动检测。
	 * @see #setTransactionManagerName
	 * @see #setAutodetectTransactionManager
	 */
	public void setTransactionManager(@Nullable TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回此事务管理器使用的JTA TransactionManager(如果有)。
	 */
	@Nullable
	public TransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 设置JTA TransactionManager的JNDI名称。
	 * <p>TransactionManager对于挂起和恢复事务是必需的, 因为UserTransaction接口不支持此功能。
	 * <p>请注意, 如果JTA UserTransaction对象也实现了JTA TransactionManager接口,
	 * 则TransactionManager将被自动检测, 以及在各已知的回退JNDI位置自动检测。
	 * @see #setTransactionManager
	 * @see #setAutodetectTransactionManager
	 */
	public void setTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
	}

	/**
	 * 设置是否自动检测同时实现了JTA TransactionManager接口的JTA UserTransaction对象
	 * (即TransactionManager的JNDI位置与UserTransaction相同, 为"java:comp/UserTransaction")。
	 * 还会检查回退JNDI位置"java:comp/TransactionManager"和"java:/TransactionManager"。
	 * 如果未找到, 将在没有TransactionManager的情况下继续。
	 * <p>默认为"true", 自动检测TransactionManager, 除非已显式指定。
	 * 可以关闭此标志以故意忽略可用的TransactionManager, 例如当挂起/恢复存在已知问题
	 * 且任何使用REQUIRES_NEW或NOT_SUPPORTED的尝试都应快速失败时。
	 * @see #FALLBACK_TRANSACTION_MANAGER_NAMES
	 */
	public void setAutodetectTransactionManager(boolean autodetectTransactionManager) {
		this.autodetectTransactionManager = autodetectTransactionManager;
	}

	/**
	 * 设置要用作直接引用的JTA 1.1 TransactionSynchronizationRegistry。
	 * <p>TransactionSynchronizationRegistry允许交错注册事务同步,
	 * 作为JTA TransactionManager API上常规注册方法的替代方案。
	 * 此外, 与JTA TransactionManager本身不同, 它是Java EE 5平台的正式组成部分。
	 * <p>请注意, TransactionSynchronizationRegistry将在JNDI中自动检测,
	 * 如果在UserTransaction/TransactionManager对象中也实现了该接口, 也会从那里自动检测。
	 * @see #setTransactionSynchronizationRegistryName
	 * @see #setAutodetectTransactionSynchronizationRegistry
	 */
	public void setTransactionSynchronizationRegistry(@Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
		this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
	}

	/**
	 * 返回此事务管理器使用的JTA 1.1 TransactionSynchronizationRegistry(如果有)。
	 */
	@Nullable
	public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return this.transactionSynchronizationRegistry;
	}

	/**
	 * 设置JTA 1.1 TransactionSynchronizationRegistry的JNDI名称。
	 * <p>请注意, 如果未显式指定, TransactionSynchronizationRegistry将在Java EE 5默认位置
	 * "java:comp/TransactionSynchronizationRegistry"自动检测。
	 * @see #DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME
	 */
	public void setTransactionSynchronizationRegistryName(String transactionSynchronizationRegistryName) {
		this.transactionSynchronizationRegistryName = transactionSynchronizationRegistryName;
	}

	/**
	 * 设置是否在默认JNDI位置("java:comp/TransactionSynchronizationRegistry")
	 * 自动检测JTA 1.1 TransactionSynchronizationRegistry对象(如果UserTransaction也是从JNDI获取的),
	 * 以及是否回退检查JTA UserTransaction/TransactionManager对象是否也实现了JTA TransactionSynchronizationRegistry接口。
	 * <p>默认为"true", 自动检测TransactionSynchronizationRegistry, 除非已显式指定。
	 * 可以关闭此标志以将同步注册委托给常规的JTA TransactionManager API。
	 */
	public void setAutodetectTransactionSynchronizationRegistry(boolean autodetectTransactionSynchronizationRegistry) {
		this.autodetectTransactionSynchronizationRegistry = autodetectTransactionSynchronizationRegistry;
	}

	/**
	 * 设置是否允许指定自定义隔离级别。
	 * <p>默认为"false", 如果为事务指定了非默认隔离级别则抛出异常。
	 * 如果受影响的资源适配器检查线程绑定的事务上下文并单独应用指定的隔离级别
	 * (例如通过IsolationLevelDataSourceAdapter), 则打开此标志。
	 * @see org.springframework.jdbc.datasource.IsolationLevelDataSourceAdapter
	 * @see org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter
	 */
	public void setAllowCustomIsolationLevels(boolean allowCustomIsolationLevels) {
		this.allowCustomIsolationLevels = allowCustomIsolationLevels;
	}


	/**
	 * 初始化UserTransaction以及TransactionManager句柄。
	 * @see #initUserTransactionAndTransactionManager()
	 */
	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();
		checkUserTransactionAndTransactionManager();
		initTransactionSynchronizationRegistry();
	}

	/**
	 * 初始化UserTransaction以及TransactionManager句柄。
	 * @throws TransactionSystemException 如果初始化失败
	 */
	protected void initUserTransactionAndTransactionManager() throws TransactionSystemException {
		if (this.userTransaction == null) {
			// Fetch JTA UserTransaction from JNDI, if necessary.
			if (StringUtils.hasLength(this.userTransactionName)) {
				this.userTransaction = lookupUserTransaction(this.userTransactionName);
				this.userTransactionObtainedFromJndi = true;
			}
			else {
				this.userTransaction = retrieveUserTransaction();
				if (this.userTransaction == null && this.autodetectUserTransaction) {
					// Autodetect UserTransaction at its default JNDI location.
					this.userTransaction = findUserTransaction();
				}
			}
		}

		if (this.transactionManager == null) {
			// Fetch JTA TransactionManager from JNDI, if necessary.
			if (StringUtils.hasLength(this.transactionManagerName)) {
				this.transactionManager = lookupTransactionManager(this.transactionManagerName);
			}
			else {
				this.transactionManager = retrieveTransactionManager();
				if (this.transactionManager == null && this.autodetectTransactionManager) {
					// Autodetect UserTransaction object that implements TransactionManager,
					// and check fallback JNDI locations otherwise.
					this.transactionManager = findTransactionManager(this.userTransaction);
				}
			}
		}

		// If only JTA TransactionManager specified, create UserTransaction handle for it.
		if (this.userTransaction == null && this.transactionManager != null) {
			this.userTransaction = buildUserTransaction(this.transactionManager);
		}
	}

	/**
	 * 检查UserTransaction以及TransactionManager句柄, 假定标准JTA要求。
	 * @throws IllegalStateException 如果没有可用的足够句柄
	 */
	protected void checkUserTransactionAndTransactionManager() throws IllegalStateException {
		// We at least need the JTA UserTransaction.
		if (this.userTransaction != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using JTA UserTransaction: " + this.userTransaction);
			}
		}
		else {
			throw new IllegalStateException("No JTA UserTransaction available - specify either " +
					"'userTransaction' or 'userTransactionName' or 'transactionManager' or 'transactionManagerName'");
		}

		// For transaction suspension, the JTA TransactionManager is necessary too.
		if (this.transactionManager != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using JTA TransactionManager: " + this.transactionManager);
			}
		}
		else {
			logger.warn("No JTA TransactionManager found: transaction suspension not available");
		}
	}

	/**
	 * 初始化JTA 1.1 TransactionSynchronizationRegistry(如果可用)。
	 * <p>应在{@link #initUserTransactionAndTransactionManager()}之后调用,
	 * 因为它可能会检查UserTransaction和TransactionManager句柄。
	 * @throws TransactionSystemException 如果初始化失败
	 */
	protected void initTransactionSynchronizationRegistry() {
		if (this.transactionSynchronizationRegistry == null) {
			// Fetch JTA TransactionSynchronizationRegistry from JNDI, if necessary.
			if (StringUtils.hasLength(this.transactionSynchronizationRegistryName)) {
				this.transactionSynchronizationRegistry =
						lookupTransactionSynchronizationRegistry(this.transactionSynchronizationRegistryName);
			}
			else {
				this.transactionSynchronizationRegistry = retrieveTransactionSynchronizationRegistry();
				if (this.transactionSynchronizationRegistry == null && this.autodetectTransactionSynchronizationRegistry) {
					// Autodetect in JNDI if applicable, and check UserTransaction/TransactionManager
					// object that implements TransactionSynchronizationRegistry otherwise.
					this.transactionSynchronizationRegistry =
							findTransactionSynchronizationRegistry(this.userTransaction, this.transactionManager);
				}
			}
		}

		if (this.transactionSynchronizationRegistry != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using JTA TransactionSynchronizationRegistry: " + this.transactionSynchronizationRegistry);
			}
		}
	}


	/**
	 * 基于给定的TransactionManager构建UserTransaction句柄。
	 * @param transactionManager 该 TransactionManager
	 * @return 对应的 UserTransaction 句柄
	 */
	protected UserTransaction buildUserTransaction(TransactionManager transactionManager) {
		if (transactionManager instanceof UserTransaction) {
			return (UserTransaction) transactionManager;
		}
		else {
			return new UserTransactionAdapter(transactionManager);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA UserTransaction。
	 * <p>如果未设置直接的UserTransaction引用, 则由{@code afterPropertiesSet}调用。
	 * 子类可以重写此方法以提供不同的UserTransaction对象。
	 * @param userTransactionName UserTransaction 的 JNDI 名称
	 * @return UserTransaction 对象
	 * @throws TransactionSystemException 如果 JNDI 查找失败
	 * @see #setJndiTemplate
	 * @see #setUserTransactionName
	 */
	protected UserTransaction lookupUserTransaction(String userTransactionName)
			throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA UserTransaction from JNDI location [" + userTransactionName + "]");
			}
			return getJndiTemplate().lookup(userTransactionName, UserTransaction.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA UserTransaction is not available at JNDI location [" + userTransactionName + "]", ex);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA TransactionManager。
	 * <p>如果未设置直接的TransactionManager引用, 则由{@code afterPropertiesSet}调用。
	 * 子类可以重写此方法以提供不同的TransactionManager对象。
	 * @param transactionManagerName TransactionManager 的 JNDI 名称
	 * @return UserTransaction 对象
	 * @throws TransactionSystemException 如果 JNDI 查找失败
	 * @see #setJndiTemplate
	 * @see #setTransactionManagerName
	 */
	protected TransactionManager lookupTransactionManager(String transactionManagerName)
			throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA TransactionManager from JNDI location [" + transactionManagerName + "]");
			}
			return getJndiTemplate().lookup(transactionManagerName, TransactionManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA TransactionManager is not available at JNDI location [" + transactionManagerName + "]", ex);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA 1.1 TransactionSynchronizationRegistry。
	 * <p>子类可以重写此方法以提供不同的TransactionManager对象。
	 * @param registryName TransactionSynchronizationRegistry 的 JNDI 名称
	 * @return TransactionSynchronizationRegistry 对象
	 * @throws TransactionSystemException 如果 JNDI 查找失败
	 * @see #setJndiTemplate
	 * @see #setTransactionSynchronizationRegistryName
	 */
	protected TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry(String registryName) throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA TransactionSynchronizationRegistry from JNDI location [" + registryName + "]");
			}
			return getJndiTemplate().lookup(registryName, TransactionSynchronizationRegistry.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA TransactionSynchronizationRegistry is not available at JNDI location [" + registryName + "]", ex);
		}
	}

	/**
	 * 允许子类以厂商特定的方式检索JTA UserTransaction。
	 * 仅在未指定"userTransaction"或"userTransactionName"时调用。
	 * <p>默认实现简单地返回{@code null}。
	 * @return 要使用的 JTA UserTransaction 句柄；如果未找到，则为 {@code null}
	 * @throws TransactionSystemException 发生错误时
	 * @see #setUserTransaction
	 * @see #setUserTransactionName
	 */
	@Nullable
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		return null;
	}

	/**
	 * 允许子类以厂商特定的方式检索JTA TransactionManager。
	 * 仅在未指定"transactionManager"或"transactionManagerName"时调用。
	 * <p>默认实现简单地返回{@code null}。
	 * @return 要使用的 JTA TransactionManager 句柄；如果未找到，则为 {@code null}
	 * @throws TransactionSystemException 发生错误时
	 * @see #setTransactionManager
	 * @see #setTransactionManagerName
	 */
	@Nullable
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		return null;
	}

	/**
	 * 允许子类以厂商特定的方式检索JTA 1.1 TransactionSynchronizationRegistry。
	 * <p>默认实现简单地返回{@code null}。
	 * @return 要使用的 JTA TransactionSynchronizationRegistry 句柄，
	 * 如果未找到则为 {@code null}
	 * @throws TransactionSystemException 发生错误时
	 */
	@Nullable
	protected TransactionSynchronizationRegistry retrieveTransactionSynchronizationRegistry() throws TransactionSystemException {
		return null;
	}

	/**
	 * 通过默认JNDI查找"java:comp/UserTransaction"来查找JTA UserTransaction。
	 * @return JTA UserTransaction 引用；如果未找到，则为 {@code null}
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 */
	@Nullable
	protected UserTransaction findUserTransaction() {
		String jndiName = DEFAULT_USER_TRANSACTION_NAME;
		try {
			UserTransaction ut = getJndiTemplate().lookup(jndiName, UserTransaction.class);
			if (logger.isDebugEnabled()) {
				logger.debug("JTA UserTransaction found at default JNDI location [" + jndiName + "]");
			}
			this.userTransactionObtainedFromJndi = true;
			return ut;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No JTA UserTransaction found at default JNDI location [" + jndiName + "]", ex);
			}
			return null;
		}
	}

	/**
	 * 通过自动检测查找JTA TransactionManager: 检查UserTransaction对象是否实现了TransactionManager,
	 * 并检查回退JNDI位置。
	 * @param ut JTA UserTransaction 对象
	 * @return JTA TransactionManager 引用；如果未找到，则为 {@code null}
	 * @see #FALLBACK_TRANSACTION_MANAGER_NAMES
	 */
	@Nullable
	protected TransactionManager findTransactionManager(@Nullable UserTransaction ut) {
		if (ut instanceof TransactionManager) {
			if (logger.isDebugEnabled()) {
				logger.debug("JTA UserTransaction object [" + ut + "] implements TransactionManager");
			}
			return (TransactionManager) ut;
		}

		// Check fallback JNDI locations.
		for (String jndiName : FALLBACK_TRANSACTION_MANAGER_NAMES) {
			try {
				TransactionManager tm = getJndiTemplate().lookup(jndiName, TransactionManager.class);
				if (logger.isDebugEnabled()) {
					logger.debug("JTA TransactionManager found at fallback JNDI location [" + jndiName + "]");
				}
				return tm;
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No JTA TransactionManager found at fallback JNDI location [" + jndiName + "]", ex);
				}
			}
		}

		// OK, so no JTA TransactionManager is available...
		return null;
	}

	/**
	 * 通过自动检测查找JTA 1.1 TransactionSynchronizationRegistry:
	 * 检查UserTransaction对象或TransactionManager对象是否实现了它,
	 * 并检查Java EE 5的标准JNDI位置。
	 * <p>默认实现简单地返回{@code null}。
	 * @param ut JTA UserTransaction 对象
	 * @param tm JTA TransactionManager 对象
	 * @return 要使用的 JTA TransactionSynchronizationRegistry 句柄，
	 * 如果未找到则为 {@code null}
	 * @throws TransactionSystemException 发生错误时
	 */
	@Nullable
	protected TransactionSynchronizationRegistry findTransactionSynchronizationRegistry(
			@Nullable UserTransaction ut, @Nullable TransactionManager tm) throws TransactionSystemException {

		if (this.userTransactionObtainedFromJndi) {
			// UserTransaction has already been obtained from JNDI, so the
			// TransactionSynchronizationRegistry probably sits there as well.
			String jndiName = DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME;
			try {
				TransactionSynchronizationRegistry tsr = getJndiTemplate().lookup(jndiName, TransactionSynchronizationRegistry.class);
				if (logger.isDebugEnabled()) {
					logger.debug("JTA TransactionSynchronizationRegistry found at default JNDI location [" + jndiName + "]");
				}
				return tsr;
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No JTA TransactionSynchronizationRegistry found at default JNDI location [" + jndiName + "]", ex);
				}
			}
		}
		// Check whether the UserTransaction or TransactionManager implements it...
		if (ut instanceof TransactionSynchronizationRegistry) {
			return (TransactionSynchronizationRegistry) ut;
		}
		if (tm instanceof TransactionSynchronizationRegistry) {
			return (TransactionSynchronizationRegistry) tm;
		}
		// OK, so no JTA 1.1 TransactionSynchronizationRegistry is available...
		return null;
	}


	/**
	 * 此实现为JTA UserTransaction返回一个JtaTransactionObject实例。
	 * <p>UserTransaction对象将为当前事务进行全新查找, 或者使用启动时查找的缓存对象。
	 * 后者是默认行为: 大多数应用服务器使用可缓存的共享单例UserTransaction。
	 * 关闭"cacheUserTransaction"标志以强制为每个事务进行全新查找。
	 * @see #setCacheUserTransaction
	 */
	@Override
	protected Object doGetTransaction() {
		UserTransaction ut = getUserTransaction();
		if (ut == null) {
			throw new CannotCreateTransactionException("No JTA UserTransaction available - " +
					"programmatic PlatformTransactionManager.getTransaction usage not supported");
		}
		if (!this.cacheUserTransaction) {
			ut = lookupUserTransaction(
					this.userTransactionName != null ? this.userTransactionName : DEFAULT_USER_TRANSACTION_NAME);
		}
		return doGetJtaTransaction(ut);
	}

	/**
	 * 为给定的当前UserTransaction获取JTA事务对象。
	 * <p>子类可以重写此方法以提供JtaTransactionObject子类,
	 * 例如持有某些所需的额外JTA句柄。
	 * @param ut 用于当前事务的 UserTransaction 句柄
	 * @return 持有该 UserTransaction 的 JtaTransactionObject
	 */
	protected JtaTransactionObject doGetJtaTransaction(UserTransaction ut) {
		return new JtaTransactionObject(ut);
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			return (txObject.getUserTransaction().getStatus() != Status.STATUS_NO_TRANSACTION);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * 此实现返回false, 以便在已存在事务的情况下进一步调用doBegin。
	 * <p>JTA实现可能通过进一步的{@code UserTransaction.begin()}调用来支持嵌套事务,
	 * 但从不支持保存点。
	 * @see #doBegin
	 * @see javax.transaction.UserTransaction#begin()
	 */
	@Override
	protected boolean useSavepointForNestedTransaction() {
		return false;
	}


	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			doJtaBegin(txObject, definition);
		}
		catch (NotSupportedException | UnsupportedOperationException ex) {
			throw new NestedTransactionNotSupportedException(
					"JTA implementation does not support nested transactions", ex);
		}
		catch (SystemException ex) {
			throw new CannotCreateTransactionException("JTA failure on begin", ex);
		}
	}

	/**
	 * 在JTA UserTransaction或TransactionManager上执行JTA begin操作。
	 * <p>此实现仅支持标准JTA功能: 即不支持每事务隔离级别和事务名称。
	 * 子类可以针对特定的JTA实现重写此方法。
	 * <p>在调用UserTransaction的{@code begin}方法之前调用{@code applyIsolationLevel}和{@code applyTimeout}。
	 * @param txObject 包含 UserTransaction 的 JtaTransactionObject
	 * @param definition TransactionDefinition 实例，描述传播行为、隔离级别、
	 * 只读标志、超时和事务名称
	 * @throws NotSupportedException 如果由 JTA 方法抛出
	 * @throws SystemException 如果由 JTA 方法抛出
	 * @see #getUserTransaction
	 * @see #getTransactionManager
	 * @see #applyIsolationLevel
	 * @see #applyTimeout
	 * @see JtaTransactionObject#getUserTransaction()
	 * @see javax.transaction.UserTransaction#setTransactionTimeout
	 * @see javax.transaction.UserTransaction#begin
	 */
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		applyIsolationLevel(txObject, definition.getIsolationLevel());
		int timeout = determineTimeout(definition);
		applyTimeout(txObject, timeout);
		txObject.getUserTransaction().begin();
	}

	/**
	 * 应用给定的事务隔离级别。默认实现对于ISOLATION_DEFAULT以外的任何级别都将抛出异常。
	 * <p>子类可以针对特定的JTA实现重写此方法, 作为重写完整{@link #doJtaBegin}方法的替代方案。
	 * @param txObject 包含 UserTransaction 的 JtaTransactionObject
	 * @param isolationLevel 取自事务定义的隔离级别
	 * @throws InvalidIsolationLevelException 如果给定的隔离级别无法应用
	 * @throws SystemException 如果由 JTA 实现抛出
	 * @see #doJtaBegin
	 * @see JtaTransactionObject#getUserTransaction()
	 * @see #getTransactionManager()
	 */
	protected void applyIsolationLevel(JtaTransactionObject txObject, int isolationLevel)
			throws InvalidIsolationLevelException, SystemException {

		if (!this.allowCustomIsolationLevels && isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(
					"JtaTransactionManager does not support custom isolation levels by default - " +
					"switch 'allowCustomIsolationLevels' to 'true'");
		}
	}

	/**
	 * 应用给定的事务超时。默认实现对于非默认超时值将调用{@code UserTransaction.setTransactionTimeout}。
	 * @param txObject 包含 UserTransaction 的 JtaTransactionObject
	 * @param timeout 取自事务定义的超时值
	 * @throws SystemException 如果由 JTA 实现抛出
	 * @see #doJtaBegin
	 * @see JtaTransactionObject#getUserTransaction()
	 * @see javax.transaction.UserTransaction#setTransactionTimeout(int)
	 */
	protected void applyTimeout(JtaTransactionObject txObject, int timeout) throws SystemException {
		if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
			txObject.getUserTransaction().setTransactionTimeout(timeout);
			if (timeout > 0) {
				txObject.resetTransactionTimeout = true;
			}
		}
	}


	@Override
	protected Object doSuspend(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			return doJtaSuspend(txObject);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on suspend", ex);
		}
	}

	/**
	 * 在JTA TransactionManager上执行JTA suspend操作。
	 * <p>子类可以针对特定的JTA实现重写此方法。
	 * @param txObject 包含 UserTransaction 的 JtaTransactionObject
	 * @return 被挂起的 JTA Transaction 对象
	 * @throws SystemException 如果由 JTA 方法抛出
	 * @see #getTransactionManager()
	 * @see javax.transaction.TransactionManager#suspend()
	 */
	protected Object doJtaSuspend(JtaTransactionObject txObject) throws SystemException {
		if (getTransactionManager() == null) {
			throw new TransactionSuspensionNotSupportedException(
					"JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
					"specify the 'transactionManager' or 'transactionManagerName' property");
		}
		return getTransactionManager().suspend();
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			doJtaResume(txObject, suspendedResources);
		}
		catch (InvalidTransactionException ex) {
			throw new IllegalTransactionStateException("Tried to resume invalid JTA transaction", ex);
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on resume", ex);
		}
	}

	/**
	 * 在JTA TransactionManager上执行JTA resume操作。
	 * <p>子类可以针对特定的JTA实现重写此方法。
	 * @param txObject 包含 UserTransaction 的 JtaTransactionObject
	 * @param suspendedTransaction 被挂起的 JTA Transaction 对象
	 * @throws InvalidTransactionException 如果由 JTA 方法抛出
	 * @throws SystemException 如果由 JTA 方法抛出
	 * @see #getTransactionManager()
	 * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
	 */
	protected void doJtaResume(@Nullable JtaTransactionObject txObject, Object suspendedTransaction)
		throws InvalidTransactionException, SystemException {

		if (getTransactionManager() == null) {
			throw new TransactionSuspensionNotSupportedException(
					"JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
					"specify the 'transactionManager' or 'transactionManagerName' property");
		}
		getTransactionManager().resume((Transaction) suspendedTransaction);
	}


	/**
	 * 此实现返回"true": JTA提交将正确处理在全局级别被标记为rollback-only的事务。
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
				// Should never happen... would have thrown an exception before
				// and as a consequence led to a rollback, not to a commit call.
				// In any case, the transaction is already fully cleaned up.
				throw new UnexpectedRollbackException("JTA transaction already completed - probably rolled back");
			}
			if (jtaStatus == Status.STATUS_ROLLEDBACK) {
				// Only really happens on JBoss 4.2 in case of an early timeout...
				// Explicit rollback call necessary to clean up the transaction.
				// IllegalStateException expected on JBoss; call still necessary.
				try {
					txObject.getUserTransaction().rollback();
				}
				catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
					}
				}
				throw new UnexpectedRollbackException("JTA transaction already rolled back (probably due to a timeout)");
			}
			txObject.getUserTransaction().commit();
		}
		catch (RollbackException ex) {
			throw new UnexpectedRollbackException(
					"JTA transaction unexpectedly rolled back (maybe due to a timeout)", ex);
		}
		catch (HeuristicMixedException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, ex);
		}
		catch (HeuristicRollbackException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, ex);
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on commit", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus != Status.STATUS_NO_TRANSACTION) {
				try {
					txObject.getUserTransaction().rollback();
				}
				catch (IllegalStateException ex) {
					if (jtaStatus == Status.STATUS_ROLLEDBACK) {
						// Only really happens on JBoss 4.2 in case of an early timeout...
						if (logger.isDebugEnabled()) {
							logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
						}
					}
					else {
						throw new TransactionSystemException("Unexpected internal transaction state", ex);
					}
				}
			}
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on rollback", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JTA transaction rollback-only");
		}
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus != Status.STATUS_NO_TRANSACTION && jtaStatus != Status.STATUS_ROLLEDBACK) {
				txObject.getUserTransaction().setRollbackOnly();
			}
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on setRollbackOnly", ex);
		}
	}


	@Override
	protected void registerAfterCompletionWithExistingTransaction(
			Object transaction, List<TransactionSynchronization> synchronizations) {

		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		logger.debug("Registering after-completion synchronization with existing JTA transaction");
		try {
			doRegisterAfterCompletionWithJtaTransaction(txObject, synchronizations);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on registerSynchronization", ex);
		}
		catch (Exception ex) {
			// Note: JBoss throws plain RuntimeException with RollbackException as cause.
			if (ex instanceof RollbackException || ex.getCause() instanceof RollbackException) {
				logger.debug("Participating in existing JTA transaction that has been marked for rollback: " +
						"cannot register Spring after-completion callbacks with outer JTA transaction - " +
						"immediately performing Spring after-completion callbacks with outcome status 'rollback'. " +
						"Original exception: " + ex);
				invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			else {
				logger.debug("Participating in existing JTA transaction, but unexpected internal transaction " +
						"state encountered: cannot register Spring after-completion callbacks with outer JTA " +
						"transaction - processing Spring after-completion callbacks with outcome status 'unknown'" +
						"Original exception: " + ex);
				invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
			}
		}
	}

	/**
	 * 在JTA TransactionManager上注册JTA同步, 用于在给定的Spring TransactionSynchronization上
	 * 调用{@code afterCompletion}。
	 * <p>默认实现在JTA 1.1 TransactionSynchronizationRegistry(如果可用)上注册同步,
	 * 或者在JTA TransactionManager的当前Transaction上注册(同样, 如果可用)。
	 * 如果两者都不可用, 将记录警告日志。
	 * <p>子类可以针对特定的JTA实现重写此方法。
	 * @param txObject 当前事务对象
	 * @param synchronizations TransactionSynchronization 对象的 List
	 * @throws RollbackException 如果由 JTA 方法抛出
	 * @throws SystemException 如果由 JTA 方法抛出
	 * @see #getTransactionManager()
	 * @see javax.transaction.Transaction#registerSynchronization
	 * @see javax.transaction.TransactionSynchronizationRegistry#registerInterposedSynchronization
	 */
	protected void doRegisterAfterCompletionWithJtaTransaction(
			JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations)
			throws RollbackException, SystemException {

		int jtaStatus = txObject.getUserTransaction().getStatus();
		if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
			throw new RollbackException("JTA transaction already completed - probably rolled back");
		}
		if (jtaStatus == Status.STATUS_ROLLEDBACK) {
			throw new RollbackException("JTA transaction already rolled back (probably due to a timeout)");
		}

		if (this.transactionSynchronizationRegistry != null) {
			// JTA 1.1 TransactionSynchronizationRegistry available - use it.
			this.transactionSynchronizationRegistry.registerInterposedSynchronization(
					new JtaAfterCompletionSynchronization(synchronizations));
		}

		else if (getTransactionManager() != null) {
			// At least the JTA TransactionManager available - use that one.
			Transaction transaction = getTransactionManager().getTransaction();
			if (transaction == null) {
				throw new IllegalStateException("No JTA Transaction available");
			}
			transaction.registerSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
		}

		else {
			// No JTA TransactionManager available - log a warning.
			logger.warn("Participating in existing JTA transaction, but no JTA TransactionManager available: " +
					"cannot register Spring after-completion callbacks with outer JTA transaction - " +
					"processing Spring after-completion callbacks with outcome status 'unknown'");
			invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		if (txObject.resetTransactionTimeout) {
			try {
				txObject.getUserTransaction().setTransactionTimeout(0);
			}
			catch (SystemException ex) {
				logger.debug("Failed to reset transaction timeout after JTA completion", ex);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of TransactionFactory interface
	//---------------------------------------------------------------------

	@Override
	public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
		TransactionManager tm = getTransactionManager();
		Assert.state(tm != null, "No JTA TransactionManager available");
		if (timeout >= 0) {
			tm.setTransactionTimeout(timeout);
		}
		tm.begin();
		return new ManagedTransactionAdapter(tm);
	}

	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return false;
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Create template for client-side JNDI lookup.
		this.jndiTemplate = new JndiTemplate();

		// Perform a fresh lookup for JTA handles.
		initUserTransactionAndTransactionManager();
		initTransactionSynchronizationRegistry();
	}

}
