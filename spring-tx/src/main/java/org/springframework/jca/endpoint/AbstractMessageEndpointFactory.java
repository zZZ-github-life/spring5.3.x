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

package org.springframework.jca.endpoint;

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.transaction.jta.SimpleTransactionFactory;
import org.springframework.transaction.jta.TransactionFactory;
import org.springframework.util.Assert;

/**
 * JCA 1.7 {@link javax.resource.spi.endpoint.MessageEndpointFactory} 接口的
 * 抽象基础实现，提供事务管理能力以及端点调用的 ClassLoader 暴露。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setTransactionManager
 */
public abstract class AbstractMessageEndpointFactory implements MessageEndpointFactory, BeanNameAware {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private TransactionFactory transactionFactory;

	@Nullable
	private String transactionName;

	private int transactionTimeout = -1;

	@Nullable
	private String beanName;


	/**
	 * 设置用于包装端点调用的 XA 事务管理器，在每个此类事务中登记端点资源。
	 * <p>传入的对象可以是实现 Spring {@link org.springframework.transaction.jta.TransactionFactory}
	 * 接口的事务管理器，也可以是普通的 {@link javax.transaction.TransactionManager}。
	 * <p>如果未指定事务管理器，端点调用将不会包装在 XA 事务中。
	 * 请查看你的资源提供者的 ActivationSpec 文档，了解特定提供者的本地事务选项。
	 * @see #setTransactionName
	 * @see #setTransactionTimeout
	 */
	public void setTransactionManager(Object transactionManager) {
		if (transactionManager instanceof TransactionFactory) {
			this.transactionFactory = (TransactionFactory) transactionManager;
		}
		else if (transactionManager instanceof TransactionManager) {
			this.transactionFactory = new SimpleTransactionFactory((TransactionManager) transactionManager);
		}
		else {
			throw new IllegalArgumentException("Transaction manager [" + transactionManager +
					"] is neither a [org.springframework.transaction.jta.TransactionFactory} nor a " +
					"[javax.transaction.TransactionManager]");
		}
	}

	/**
	 * 设置用于包装端点调用的 Spring TransactionFactory，在每个此类事务中登记端点资源。
	 * <p>或者，通过 {@link #setTransactionManager "transactionManager"} 属性指定适当的事务管理器。
	 * <p>如果未指定事务工厂，端点调用将不会包装在 XA 事务中。
	 * 请查看你的资源提供者的 ActivationSpec 文档，了解特定提供者的本地事务选项。
	 * @see #setTransactionName
	 * @see #setTransactionTimeout
	 */
	public void setTransactionFactory(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	/**
	 * 指定事务名称（如果有）。
	 * <p>默认为无。指定的名称将传递给事务管理器，允许在事务监视器中识别事务。
	 */
	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	/**
	 * 指定事务超时时间（如果有）。
	 * <p>默认为 -1：依赖事务管理器的默认超时。
	 * 指定具体的超时时间以限制每次端点调用的最大持续时间。
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * 设置此消息端点的名称。当在 Spring 的 bean 工厂中定义时，自动设置为 bean 名称。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	/**
	 * JCA 1.7 {@code #getActivationName()} 方法的实现，
	 * 返回设置在此 MessageEndpointFactory 上的 bean 名称。
	 * @see #setBeanName
	 */
	@Override
	@Nullable
	public String getActivationName() {
		return this.beanName;
	}

	/**
	 * JCA 1.7 {@code #getEndpointClass()} 方法的实现，
	 * 返回 {@code} null 以指示合成端点类型。
	 */
	@Override
	@Nullable
	public Class<?> getEndpointClass() {
		return null;
	}

	/**
	 * 如果指定了事务管理器则返回 {@code true}；否则返回 {@code false}。
	 * @see #setTransactionManager
	 * @see #setTransactionFactory
	 */
	@Override
	public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
		return (this.transactionFactory != null);
	}

	/**
	 * {@code createEndpoint} 的标准 JCA 1.5 版本。
	 * <p>此实现委托给 {@link #createEndpointInternal()}，
	 * 在端点被调用之前初始化端点的 XAResource。
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * {@code createEndpoint} 的替代 JCA 1.6 版本。
	 * <p>此实现委托给 {@link #createEndpointInternal()}，忽略指定的超时时间。
	 * 此方法仅为 JCA 1.6 合规性而存在。
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * 创建实际的端点实例，作为此工厂内部类 {@link AbstractMessageEndpoint} 的子类。
	 * @return the actual endpoint instance (never {@code null})
	 * @throws UnavailableException if no endpoint is available at present
	 */
	protected abstract AbstractMessageEndpoint createEndpointInternal() throws UnavailableException;


	/**
	 * 实际端点实现的内部类，基于模板方法以允许任何类型的具体端点实现。
	 */
	protected abstract class AbstractMessageEndpoint implements MessageEndpoint {

		@Nullable
		private TransactionDelegate transactionDelegate;

		private boolean beforeDeliveryCalled = false;

		@Nullable
		private ClassLoader previousContextClassLoader;

		/**
		 * 初始化此端点的 TransactionDelegate。
		 * @param xaResource the XAResource for this endpoint
		 */
		void initXAResource(XAResource xaResource) {
			this.transactionDelegate = new TransactionDelegate(xaResource);
		}

		/**
		 * 此 {@code beforeDelivery} 实现在必要时启动事务，
		 * 并将端点 ClassLoader 暴露为当前线程上下文 ClassLoader。
		 * <p>注意，JCA 1.7 规范不要求 ResourceAdapter 在调用具体端点之前调用此方法。
		 * 如果未调用此方法（检查 {@link #hasBeforeDeliveryBeenCalled()}），
		 * 具体端点方法应作为其自身处理的一部分显式调用 {@code beforeDelivery}
		 * 及其兄弟方法 {@link #afterDelivery()}。
		 */
		@Override
		public void beforeDelivery(@Nullable Method method) throws ResourceException {
			this.beforeDeliveryCalled = true;
			Assert.state(this.transactionDelegate != null, "Not initialized");
			try {
				this.transactionDelegate.beginTransaction();
			}
			catch (Throwable ex) {
				throw new ApplicationServerInternalException("Failed to begin transaction", ex);
			}
			Thread currentThread = Thread.currentThread();
			this.previousContextClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getEndpointClassLoader());
		}

		/**
		 * 用于暴露端点 ClassLoader 的模板方法
		 * （通常是加载消息监听器类的 ClassLoader）。
		 * @return the endpoint ClassLoader (never {@code null})
		 */
		protected abstract ClassLoader getEndpointClassLoader();

		/**
		 * 返回此端点的 {@link #beforeDelivery} 方法是否已被调用。
		 */
		protected final boolean hasBeforeDeliveryBeenCalled() {
			return this.beforeDeliveryCalled;
		}

		/**
		 * 通知端点基类具体端点调用导致异常的回调方法。
		 * <p>由子类在具体端点抛出异常时调用。
		 * @param ex the exception thrown from the concrete endpoint
		 */
		protected void onEndpointException(Throwable ex) {
			Assert.state(this.transactionDelegate != null, "Not initialized");
			this.transactionDelegate.setRollbackOnly();
			logger.debug("Transaction marked as rollback-only after endpoint exception", ex);
		}

		/**
		 * 此 {@code afterDelivery} 实现重置线程上下文 ClassLoader 并完成事务（如果有的话）。
		 * <p>注意，JCA 1.7 规范不要求 ResourceAdapter 在调用具体端点之后调用此方法。
		 * 参见 {@link #beforeDelivery} 的 javadoc 中的说明。
		 */
		@Override
		public void afterDelivery() throws ResourceException {
			Assert.state(this.transactionDelegate != null, "Not initialized");
			this.beforeDeliveryCalled = false;
			Thread.currentThread().setContextClassLoader(this.previousContextClassLoader);
			this.previousContextClassLoader = null;
			try {
				this.transactionDelegate.endTransaction();
			}
			catch (Throwable ex) {
				logger.warn("Failed to complete transaction after endpoint delivery", ex);
				throw new ApplicationServerInternalException("Failed to complete transaction", ex);
			}
		}

		@Override
		public void release() {
			if (this.transactionDelegate != null) {
				try {
					this.transactionDelegate.setRollbackOnly();
					this.transactionDelegate.endTransaction();
				}
				catch (Throwable ex) {
					logger.warn("Could not complete unfinished transaction on endpoint release", ex);
				}
			}
		}
	}


	/**
	 * 执行实际事务处理的私有内部类，包括端点 XAResource 的登记。
	 */
	private class TransactionDelegate {

		@Nullable
		private final XAResource xaResource;

		@Nullable
		private Transaction transaction;

		private boolean rollbackOnly;

		public TransactionDelegate(@Nullable XAResource xaResource) {
			if (xaResource == null && transactionFactory != null &&
					!transactionFactory.supportsResourceAdapterManagedTransactions()) {
				throw new IllegalStateException("ResourceAdapter-provided XAResource is required for " +
						"transaction management. Check your ResourceAdapter's configuration.");
			}
			this.xaResource = xaResource;
		}

		public void beginTransaction() throws Exception {
			if (transactionFactory != null && this.xaResource != null) {
				this.transaction = transactionFactory.createTransaction(transactionName, transactionTimeout);
				this.transaction.enlistResource(this.xaResource);
			}
		}

		public void setRollbackOnly() {
			if (this.transaction != null) {
				this.rollbackOnly = true;
			}
		}

		public void endTransaction() throws Exception {
			if (this.transaction != null) {
				try {
					if (this.rollbackOnly) {
						this.transaction.rollback();
					}
					else {
						this.transaction.commit();
					}
				}
				finally {
					this.transaction = null;
					this.rollbackOnly = false;
				}
			}
		}
	}

}
