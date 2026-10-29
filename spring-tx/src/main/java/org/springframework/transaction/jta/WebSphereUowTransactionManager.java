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

package org.springframework.transaction.jta;

import java.util.List;

import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * WebSphere特定的PlatformTransactionManager实现, 委托给从WebSphere的JNDI环境中获取的
 * {@link com.ibm.wsspi.uow.UOWManager}实例。这允许Spring充分利用WebSphere事务协调器的全部功能,
 * 包括事务挂起, 且完全符合官方支持的WebSphere API。
 *
 * <p>此类实现的{@link CallbackPreferringPlatformTransactionManager}接口表明,
 * 调用者最好通过{@link #execute}方法传入{@link TransactionCallback},
 * 该回调将通过基于回调的WebSphere UOWManager API而不是通过标准JTA API
 * (UserTransaction / TransactionManager)进行处理。这避免了在WebSphere上使用非公开的
 * {@code javax.transaction.TransactionManager} API, 保持在受支持的WebSphere API边界内。
 *
 * <p>此事务管理器实现派生自Spring的标准{@link JtaTransactionManager},
 * 继承了通过JTA UserTransaction句柄支持使用{@code getTransaction} / {@code commit} /
 * {@code rollback}调用进行编程式事务界定的能力, 适用于不使用基于TransactionCallback的
 * {@link #execute}方法的调用者。但是, 在此{@code getTransaction}风格中<i>不</i>支持事务挂起
 * (除非您显式指定{@link #setTransactionManager}引用, 尽管有官方WebSphere建议)。
 * 对于任何可能需要事务挂起的代码, 请使用{@link #execute}风格。
 *
 * <p>此事务管理器与WebSphere 6.1.0.9及更高版本兼容。
 * UOWManager的默认JNDI位置是"java:comp/websphere/UOWManager"。
 * 如果根据您的WebSphere文档位置有所不同, 只需通过此事务管理器的"uowManagerName"
 * bean属性指定实际位置。
 *
 * <p><b>注意: 此JtaTransactionManager旨在改进Spring端特定的事务界定行为。
 * 它将愉快地与您的持久化提供者中独立配置的WebSphere事务策略共存,
 * 无需以任何方式特别连接这些设置。</b>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setUowManager
 * @see #setUowManagerName
 * @see com.ibm.wsspi.uow.UOWManager
 */
@SuppressWarnings("serial")
public class WebSphereUowTransactionManager extends JtaTransactionManager
		implements CallbackPreferringPlatformTransactionManager {

	/**
	 * WebSphere UOWManager的默认JNDI位置。
	 * @see #setUowManagerName
	 */
	public static final String DEFAULT_UOW_MANAGER_NAME = "java:comp/websphere/UOWManager";


	@Nullable
	private UOWManager uowManager;

	@Nullable
	private String uowManagerName;


	/**
	 * 创建一个新的WebSphereUowTransactionManager。
	 */
	public WebSphereUowTransactionManager() {
		setAutodetectTransactionManager(false);
	}

	/**
	 * 为给定的UOWManager创建一个新的WebSphereUowTransactionManager。
	 * @param uowManager the WebSphere UOWManager to use as direct reference
	 */
	public WebSphereUowTransactionManager(UOWManager uowManager) {
		this();
		this.uowManager = uowManager;
	}


	/**
	 * 设置要用作直接引用的WebSphere UOWManager。
	 * <p>通常仅用于测试设置; 在Java EE环境中, UOWManager将始终从JNDI获取。
	 * @see #setUserTransactionName
	 */
	public void setUowManager(UOWManager uowManager) {
		this.uowManager = uowManager;
	}

	/**
	 * 设置WebSphere UOWManager的JNDI名称。
	 * 如果未设置, 则使用默认值"java:comp/websphere/UOWManager"。
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 * @see #setUowManager
	 */
	public void setUowManagerName(String uowManagerName) {
		this.uowManagerName = uowManagerName;
	}


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();

		// Fetch UOWManager handle from JNDI, if necessary.
		if (this.uowManager == null) {
			if (this.uowManagerName != null) {
				this.uowManager = lookupUowManager(this.uowManagerName);
			}
			else {
				this.uowManager = lookupDefaultUowManager();
			}
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找WebSphere UOWManager。
	 * @param uowManagerName the JNDI name of the UOWManager
	 * @return the UOWManager object
	 * @throws TransactionSystemException if the JNDI lookup failed
	 * @see #setJndiTemplate
	 * @see #setUowManagerName
	 */
	protected UOWManager lookupUowManager(String uowManagerName) throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving WebSphere UOWManager from JNDI location [" + uowManagerName + "]");
			}
			return getJndiTemplate().lookup(uowManagerName, UOWManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"WebSphere UOWManager is not available at JNDI location [" + uowManagerName + "]", ex);
		}
	}

	/**
	 * 从默认JNDI位置"java:comp/websphere/UOWManager"获取WebSphere UOWManager。
	 * @return the UOWManager object
	 * @throws TransactionSystemException if the JNDI lookup failed
	 * @see #setJndiTemplate
	 */
	protected UOWManager lookupDefaultUowManager() throws TransactionSystemException {
		try {
			logger.debug("Retrieving WebSphere UOWManager from default JNDI location [" + DEFAULT_UOW_MANAGER_NAME + "]");
			return getJndiTemplate().lookup(DEFAULT_UOW_MANAGER_NAME, UOWManager.class);
		}
		catch (NamingException ex) {
			logger.debug("WebSphere UOWManager is not available at default JNDI location [" +
					DEFAULT_UOW_MANAGER_NAME + "] - falling back to UOWManagerFactory lookup");
			return UOWManagerFactory.getUOWManager();
		}
	}

	private UOWManager obtainUOWManager() {
		Assert.state(this.uowManager != null, "No UOWManager set");
		return this.uowManager;
	}


	/**
	 * 将同步注册为UOWManager上的交错JTA Synchronization。
	 */
	@Override
	protected void doRegisterAfterCompletionWithJtaTransaction(
			JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations) {

		obtainUOWManager().registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
	}

	/**
	 * 返回{@code true}, 因为WebSphere ResourceAdapter(在JNDI中暴露)
	 * 在MessageEndpointFactory的{@code isDeliveryTransacted}方法返回{@code true}时
	 * 会隐式执行事务登记。在这种情况下, 我们将简单地跳过{@link #createTransaction}调用。
	 * @see javax.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
	 * @see org.springframework.jca.endpoint.AbstractMessageEndpointFactory
	 * @see TransactionFactory#createTransaction
	 */
	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return true;
	}


	@Override
	@Nullable
	public <T> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
		}

		UOWManager uowManager = obtainUOWManager();
		int pb = def.getPropagationBehavior();
		boolean existingTx = (uowManager.getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE &&
				uowManager.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);

		int uowType = UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION;
		boolean joinTx = false;
		boolean newSynch = false;

		if (existingTx) {
			if (pb == TransactionDefinition.PROPAGATION_NEVER) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'never' but existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_NESTED) {
				throw new NestedTransactionNotSupportedException(
						"Transaction propagation 'nested' not supported for WebSphere UOW transactions");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_REQUIRED ||
					pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				joinTx = true;
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
			else if (pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}
		else {
			if (pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'mandatory' but no existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
					pb == TransactionDefinition.PROPAGATION_NEVER) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}

		boolean debug = logger.isDebugEnabled();
		if (debug) {
			logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
		}
		SuspendedResourcesHolder suspendedResources = (!joinTx ? suspend(null) : null);
		UOWActionAdapter<T> action = null;
		try {
			boolean actualTransaction = (uowType == UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
			if (actualTransaction && def.getTimeout() > TransactionDefinition.TIMEOUT_DEFAULT) {
				uowManager.setUOWTimeout(uowType, def.getTimeout());
			}
			if (debug) {
				logger.debug("Invoking WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			action = new UOWActionAdapter<>(def, callback, actualTransaction, !joinTx, newSynch, debug);
			uowManager.runUnderUOW(uowType, joinTx, action);
			if (debug) {
				logger.debug("Returned from WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			return action.getResult();
		}
		catch (UOWException | UOWActionException ex) {
			TransactionSystemException tse =
					new TransactionSystemException("UOWManager transaction processing failed", ex);
			Throwable appEx = action.getException();
			if (appEx != null) {
				logger.error("Application exception overridden by rollback exception", appEx);
				tse.initApplicationException(appEx);
			}
			throw tse;
		}
		finally {
			if (suspendedResources != null) {
				resume(null, suspendedResources);
			}
		}
	}


	/**
	 * 在WebSphere UOWAction形态内执行给定Spring事务的适配器。
	 */
	private class UOWActionAdapter<T> implements UOWAction, SmartTransactionObject {

		private final TransactionDefinition definition;

		private final TransactionCallback<T> callback;

		private final boolean actualTransaction;

		private final boolean newTransaction;

		private final boolean newSynchronization;

		private boolean debug;

		@Nullable
		private T result;

		@Nullable
		private Throwable exception;

		public UOWActionAdapter(TransactionDefinition definition, TransactionCallback<T> callback,
				boolean actualTransaction, boolean newTransaction, boolean newSynchronization, boolean debug) {

			this.definition = definition;
			this.callback = callback;
			this.actualTransaction = actualTransaction;
			this.newTransaction = newTransaction;
			this.newSynchronization = newSynchronization;
			this.debug = debug;
		}

		@Override
		public void run() {
			UOWManager uowManager = obtainUOWManager();
			DefaultTransactionStatus status = prepareTransactionStatus(
					this.definition, (this.actualTransaction ? this : null),
					this.newTransaction, this.newSynchronization, this.debug, null);
			try {
				this.result = this.callback.doInTransaction(status);
				triggerBeforeCommit(status);
			}
			catch (Throwable ex) {
				this.exception = ex;
				if (status.isDebug()) {
					logger.debug("Rolling back on application exception from transaction callback", ex);
				}
				uowManager.setRollbackOnly();
			}
			finally {
				if (status.isLocalRollbackOnly()) {
					if (status.isDebug()) {
						logger.debug("Transaction callback has explicitly requested rollback");
					}
					uowManager.setRollbackOnly();
				}
				triggerBeforeCompletion(status);
				if (status.isNewSynchronization()) {
					List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
					TransactionSynchronizationManager.clear();
					if (!synchronizations.isEmpty()) {
						uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
					}
				}
			}
		}

		@Nullable
		public T getResult() {
			if (this.exception != null) {
				ReflectionUtils.rethrowRuntimeException(this.exception);
			}
			return this.result;
		}

		@Nullable
		public Throwable getException() {
			return this.exception;
		}

		@Override
		public boolean isRollbackOnly() {
			return obtainUOWManager().getRollbackOnly();
		}

		@Override
		public void flush() {
			TransactionSynchronizationUtils.triggerFlush();
		}
	}

}
