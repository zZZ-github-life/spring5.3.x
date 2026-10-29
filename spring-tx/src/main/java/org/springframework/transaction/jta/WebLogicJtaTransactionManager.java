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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.Assert;

/**
 * 针对BEA WebLogic(9.0及更高版本)的特殊{@link JtaTransactionManager}变体。
 * 在WebLogic的事务协调器上支持Spring事务定义的全部功能, <i>超越标准JTA</i>:
 * 事务名称、每事务隔离级别, 以及在所有情况下正确恢复事务。
 *
 * <p>使用WebLogic的特殊{@code begin(name)}方法来启动JTA事务,
 * 以使<b>Spring驱动的事务在WebLogic的事务监视器中可见</b>。
 * 对于Spring的声明式事务, 暴露的名称(默认情况下)将是完全限定类名 + "." + 方法名。
 *
 * <p>通过WebLogic的相应JTA事务属性"ISOLATION LEVEL"支持<b>每事务隔离级别</b>。
 * 这会将指定的隔离级别(例如ISOLATION_SERIALIZABLE)应用于参与给定事务的所有JDBC连接。
 *
 * <p>如果标准JTA resume失败, 则调用WebLogic的特殊{@code forceResume}方法,
 * 以<b>在目标事务被标记为rollback-only时也能恢复</b>。
 * 如果您一开始就不依赖此事务挂起功能, Spring的标准JtaTransactionManager也能正常工作。
 *
 * <p>默认情况下, JTA UserTransaction和TransactionManager句柄直接从WebLogic的
 * {@code TransactionHelper}获取。这可以通过指定"userTransaction"/"userTransactionName"
 * 和"transactionManager"/"transactionManagerName"来覆盖, 传入现有句柄或指定要查找的相应JNDI位置。
 *
 * <p><b>注意: 此JtaTransactionManager旨在改进Spring端特定的事务界定行为。
 * 它将愉快地与您的持久化提供者中独立配置的WebLogic事务策略共存,
 * 无需以任何方式特别连接这些设置。</b>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.transaction.TransactionDefinition#getName
 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel
 * @see weblogic.transaction.UserTransaction#begin(String)
 * @see weblogic.transaction.Transaction#setProperty
 * @see weblogic.transaction.TransactionManager#forceResume
 * @see weblogic.transaction.TransactionHelper
 */
@SuppressWarnings("serial")
public class WebLogicJtaTransactionManager extends JtaTransactionManager {

	private static final String USER_TRANSACTION_CLASS_NAME = "weblogic.transaction.UserTransaction";

	private static final String CLIENT_TRANSACTION_MANAGER_CLASS_NAME = "weblogic.transaction.ClientTransactionManager";

	private static final String TRANSACTION_CLASS_NAME = "weblogic.transaction.Transaction";

	private static final String TRANSACTION_HELPER_CLASS_NAME = "weblogic.transaction.TransactionHelper";

	private static final String ISOLATION_LEVEL_KEY = "ISOLATION LEVEL";


	private boolean weblogicUserTransactionAvailable;

	@Nullable
	private Method beginWithNameMethod;

	@Nullable
	private Method beginWithNameAndTimeoutMethod;

	private boolean weblogicTransactionManagerAvailable;

	@Nullable
	private Method forceResumeMethod;

	@Nullable
	private Method setPropertyMethod;

	@Nullable
	private Object transactionHelper;


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		super.afterPropertiesSet();
		loadWebLogicTransactionClasses();
	}

	@Override
	@Nullable
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		Object helper = loadWebLogicTransactionHelper();
		try {
			logger.trace("Retrieving JTA UserTransaction from WebLogic TransactionHelper");
			Method getUserTransactionMethod = helper.getClass().getMethod("getUserTransaction");
			return (UserTransaction) getUserTransactionMethod.invoke(this.transactionHelper);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper.getUserTransaction() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper.getUserTransaction() method", ex);
		}
	}

	@Override
	@Nullable
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		Object helper = loadWebLogicTransactionHelper();
		try {
			logger.trace("Retrieving JTA TransactionManager from WebLogic TransactionHelper");
			Method getTransactionManagerMethod = helper.getClass().getMethod("getTransactionManager");
			return (TransactionManager) getTransactionManagerMethod.invoke(this.transactionHelper);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper.getTransactionManager() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper.getTransactionManager() method", ex);
		}
	}

	private Object loadWebLogicTransactionHelper() throws TransactionSystemException {
		Object helper = this.transactionHelper;
		if (helper == null) {
			try {
				Class<?> transactionHelperClass = getClass().getClassLoader().loadClass(TRANSACTION_HELPER_CLASS_NAME);
				Method getTransactionHelperMethod = transactionHelperClass.getMethod("getTransactionHelper");
				helper = getTransactionHelperMethod.invoke(null);
				this.transactionHelper = helper;
				logger.trace("WebLogic TransactionHelper found");
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's TransactionHelper.getTransactionHelper() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
						ex);
			}
		}
		return helper;
	}

	private void loadWebLogicTransactionClasses() throws TransactionSystemException {
		try {
			Class<?> userTransactionClass = getClass().getClassLoader().loadClass(USER_TRANSACTION_CLASS_NAME);
			this.weblogicUserTransactionAvailable = userTransactionClass.isInstance(getUserTransaction());
			if (this.weblogicUserTransactionAvailable) {
				this.beginWithNameMethod = userTransactionClass.getMethod("begin", String.class);
				this.beginWithNameAndTimeoutMethod = userTransactionClass.getMethod("begin", String.class, int.class);
				logger.debug("Support for WebLogic transaction names available");
			}
			else {
				logger.debug("Support for WebLogic transaction names not available");
			}

			// Obtain WebLogic ClientTransactionManager interface.
			Class<?> transactionManagerClass =
					getClass().getClassLoader().loadClass(CLIENT_TRANSACTION_MANAGER_CLASS_NAME);
			logger.trace("WebLogic ClientTransactionManager found");

			this.weblogicTransactionManagerAvailable = transactionManagerClass.isInstance(getTransactionManager());
			if (this.weblogicTransactionManagerAvailable) {
				Class<?> transactionClass = getClass().getClassLoader().loadClass(TRANSACTION_CLASS_NAME);
				this.forceResumeMethod = transactionManagerClass.getMethod("forceResume", Transaction.class);
				this.setPropertyMethod = transactionClass.getMethod("setProperty", String.class, Serializable.class);
				logger.debug("Support for WebLogic forceResume available");
			}
			else {
				logger.debug("Support for WebLogic forceResume not available");
			}
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
					ex);
		}
	}

	private TransactionManager obtainTransactionManager() {
		TransactionManager tm = getTransactionManager();
		Assert.state(tm != null, "No TransactionManager set");
		return tm;
	}


	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		int timeout = determineTimeout(definition);

		// Apply transaction name (if any) to WebLogic transaction.
		if (this.weblogicUserTransactionAvailable && definition.getName() != null) {
			try {
				if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName(), timeout);
					*/
					Assert.state(this.beginWithNameAndTimeoutMethod != null, "WebLogic JTA API not initialized");
					this.beginWithNameAndTimeoutMethod.invoke(txObject.getUserTransaction(), definition.getName(), timeout);
				}
				else {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName());
					*/
					Assert.state(this.beginWithNameMethod != null, "WebLogic JTA API not initialized");
					this.beginWithNameMethod.invoke(txObject.getUserTransaction(), definition.getName());
				}
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's UserTransaction.begin() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not invoke WebLogic's UserTransaction.begin() method", ex);
			}
		}
		else {
			// No WebLogic UserTransaction available or no transaction name specified
			// -> standard JTA begin call.
			applyTimeout(txObject, timeout);
			txObject.getUserTransaction().begin();
		}

		// Specify isolation level, if any, through corresponding WebLogic transaction property.
		if (this.weblogicTransactionManagerAvailable) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				try {
					Transaction tx = obtainTransactionManager().getTransaction();
					Integer isolationLevel = definition.getIsolationLevel();
					/*
					weblogic.transaction.Transaction wtx = (weblogic.transaction.Transaction) tx;
					wtx.setProperty(ISOLATION_LEVEL_KEY, isolationLevel);
					*/
					Assert.state(this.setPropertyMethod != null, "WebLogic JTA API not initialized");
					this.setPropertyMethod.invoke(tx, ISOLATION_LEVEL_KEY, isolationLevel);
				}
				catch (InvocationTargetException ex) {
					throw new TransactionSystemException(
							"WebLogic's Transaction.setProperty(String, Serializable) method failed", ex.getTargetException());
				}
				catch (Exception ex) {
					throw new TransactionSystemException(
							"Could not invoke WebLogic's Transaction.setProperty(String, Serializable) method", ex);
				}
			}
		}
		else {
			applyIsolationLevel(txObject, definition.getIsolationLevel());
		}
	}

	@Override
	protected void doJtaResume(@Nullable JtaTransactionObject txObject, Object suspendedTransaction)
			throws InvalidTransactionException, SystemException {

		try {
			obtainTransactionManager().resume((Transaction) suspendedTransaction);
		}
		catch (InvalidTransactionException ex) {
			if (!this.weblogicTransactionManagerAvailable) {
				throw ex;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Standard JTA resume threw InvalidTransactionException: " + ex.getMessage() +
					" - trying WebLogic JTA forceResume");
			}
			/*
			weblogic.transaction.TransactionManager wtm =
					(weblogic.transaction.TransactionManager) getTransactionManager();
			wtm.forceResume(suspendedTransaction);
			*/
			try {
				Assert.state(this.forceResumeMethod != null, "WebLogic JTA API not initialized");
				this.forceResumeMethod.invoke(getTransactionManager(), suspendedTransaction);
			}
			catch (InvocationTargetException ex2) {
				throw new TransactionSystemException(
						"WebLogic's TransactionManager.forceResume(Transaction) method failed", ex2.getTargetException());
			}
			catch (Exception ex2) {
				throw new TransactionSystemException(
						"Could not access WebLogic's TransactionManager.forceResume(Transaction) method", ex2);
			}
		}
	}

	@Override
	public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
		if (this.weblogicUserTransactionAvailable && name != null) {
			try {
				if (timeout >= 0) {
					Assert.state(this.beginWithNameAndTimeoutMethod != null, "WebLogic JTA API not initialized");
					this.beginWithNameAndTimeoutMethod.invoke(getUserTransaction(), name, timeout);
				}
				else {
					Assert.state(this.beginWithNameMethod != null, "WebLogic JTA API not initialized");
					this.beginWithNameMethod.invoke(getUserTransaction(), name);
				}
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof NotSupportedException) {
					throw (NotSupportedException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof SystemException) {
					throw (SystemException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) ex.getTargetException();
				}
				else {
					throw new SystemException(
							"WebLogic's begin() method failed with an unexpected error: " + ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new SystemException("Could not invoke WebLogic's UserTransaction.begin() method: " + ex);
			}
			return new ManagedTransactionAdapter(obtainTransactionManager());
		}

		else {
			// No name specified - standard JTA is sufficient.
			return super.createTransaction(name, timeout);
		}
	}

}
