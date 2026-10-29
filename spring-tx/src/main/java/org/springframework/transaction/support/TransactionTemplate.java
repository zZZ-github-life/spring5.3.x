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

package org.springframework.transaction.support;

import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.Assert;

/**
 * 简化编程式事务划分和事务异常处理的模板类。
 *
 * <p>核心方法是 {@link #execute}，支持实现 {@link TransactionCallback} 接口的事务性代码。
 * 此模板处理事务生命周期和可能出现的异常，因此 TransactionCallback 实现和调用方都不需要
 * 显式处理事务。
 *
 * <p>典型用法：允许编写使用 JDBC DataSource 等资源但自身不感知事务的底层数据访问对象。
 * 相反，它们可以通过内部类回调对象调用底层服务，从而隐式参与由使用此类的上层应用服务
 * 所处理的事务。
 *
 * <p>可以在服务实现中通过直接实例化并引用事务管理器来使用，也可以在应用上下文中准备好
 * 并作为 bean 引用传递给服务。注意：事务管理器应始终在应用上下文中配置为 bean：在第一种
 * 情况下直接提供给服务，在第二种情况下提供给准备好的模板。
 *
 * <p>支持按名称设置传播行为和隔离级别，便于在上下文定义中进行配置。
 *
 * @author Juergen Hoeller
 * @since 17.03.2003
 * @see #execute
 * @see #setTransactionManager
 * @see org.springframework.transaction.PlatformTransactionManager
 */
@SuppressWarnings("serial")
public class TransactionTemplate extends DefaultTransactionDefinition
		implements TransactionOperations, InitializingBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private PlatformTransactionManager transactionManager;


	/**
	 * 构造一个新的 TransactionTemplate 以供 bean 使用。
	 * <p>注意：必须在任何 {@code execute} 调用之前设置 PlatformTransactionManager。
	 * @see #setTransactionManager
	 */
	public TransactionTemplate() {
	}

	/**
	 * 使用给定的事务管理器构造一个新的 TransactionTemplate。
	 * @param transactionManager the transaction management strategy to be used
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 使用给定的事务管理器构造一个新的 TransactionTemplate，并从给定的事务定义中获取其默认设置。
	 * @param transactionManager the transaction management strategy to be used
	 * @param transactionDefinition the transaction definition to copy the
	 * default settings from. Local properties can still be set to change values.
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		super(transactionDefinition);
		this.transactionManager = transactionManager;
	}


	/**
	 * 设置要使用的事务管理策略。
	 */
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回要使用的事务管理策略。
	 */
	@Nullable
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.transactionManager == null) {
			throw new IllegalArgumentException("Property 'transactionManager' is required");
		}
	}


	@Override
	@Nullable
	public <T> T execute(TransactionCallback<T> action) throws TransactionException {
		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

		if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
			return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
		}
		else {
			TransactionStatus status = this.transactionManager.getTransaction(this);
			T result;
			try {
				result = action.doInTransaction(status);
			}
			catch (RuntimeException | Error ex) {
				// Transactional code threw application exception -> rollback
				rollbackOnException(status, ex);
				throw ex;
			}
			catch (Throwable ex) {
				// Transactional code threw unexpected exception -> rollback
				rollbackOnException(status, ex);
				throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
			}
			this.transactionManager.commit(status);
			return result;
		}
	}

	/**
	 * 执行回滚，并妥善处理回滚异常。
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of a rollback error
	 */
	private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

		logger.debug("Initiating transaction rollback on application exception", ex);
		try {
			this.transactionManager.rollback(status);
		}
		catch (TransactionSystemException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			ex2.initApplicationException(ex);
			throw ex2;
		}
		catch (RuntimeException | Error ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			throw ex2;
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate) ||
				getTransactionManager() == ((TransactionTemplate) other).getTransactionManager())));
	}

}
