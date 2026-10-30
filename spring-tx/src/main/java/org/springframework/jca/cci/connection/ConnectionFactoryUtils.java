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

package org.springframework.jca.cci.connection;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 辅助类，提供从 {@link javax.resource.cci.ConnectionFactory} 获取 CCI Connection 的静态方法。
 * 包括对 Spring 管理的事务性 Connection 的特殊支持，
 * 例如由 {@link CciLocalTransactionManager} 或
 * {@link org.springframework.transaction.jta.JtaTransactionManager} 管理的 Connection。
 *
 * <p>由 {@link org.springframework.jca.cci.core.CciTemplate}、
 * Spring 的 CCI 操作对象和 {@link CciLocalTransactionManager} 内部使用。
 * 也可以直接在应用程序代码中使用。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 * @see #releaseConnection
 * @see CciLocalTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * 从给定的 ConnectionFactory 获取 Connection。将 ResourceException
	 * 转换为 Spring 未检查的通用数据访问异常层次结构，简化调用代码，
	 * 并使抛出的任何异常更有意义。
	 * <p>能感知绑定到当前线程的相应 Connection，例如在使用
	 * {@link CciLocalTransactionManager} 时。如果事务同步处于活动状态
	 * （例如在 JTA 事务中），会将 Connection 绑定到线程。
	 * @param cf 要从中获取 Connection 的 ConnectionFactory
	 * @return 来自给定 ConnectionFactory 的 CCI Connection
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果获取 Connection 的尝试失败
	 * @see #releaseConnection
	 */
	public static Connection getConnection(ConnectionFactory cf)
			throws org.springframework.jca.cci.CannotGetCciConnectionException {

		return getConnection(cf, null);
	}

	/**
	 * 从给定的 ConnectionFactory 获取 Connection。将 ResourceException
	 * 转换为 Spring 未检查的通用数据访问异常层次结构，简化调用代码，
	 * 并使抛出的任何异常更有意义。
	 * <p>能感知绑定到当前线程的相应 Connection，例如在使用
	 * {@link CciLocalTransactionManager} 时。如果事务同步处于活动状态
	 * （例如在 JTA 事务中），会将 Connection 绑定到线程。
	 * @param cf 要从中获取 Connection 的 ConnectionFactory
	 * @param spec 所需 Connection 的 ConnectionSpec（可以为 {@code null}）。
	 * 注意：如果指定了此项，每次调用都会获取新的 Connection，
	 * 而不参与共享的事务性 Connection。
	 * @return 来自给定 ConnectionFactory 的 CCI Connection
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果获取 Connection 的尝试失败
	 * @see #releaseConnection
	 */
	public static Connection getConnection(ConnectionFactory cf, @Nullable ConnectionSpec spec)
			throws org.springframework.jca.cci.CannotGetCciConnectionException {
		try {
			if (spec != null) {
				Assert.notNull(cf, "No ConnectionFactory specified");
				return cf.getConnection(spec);
			}
			else {
				return doGetConnection(cf);
			}
		}
		catch (ResourceException ex) {
			throw new org.springframework.jca.cci.CannotGetCciConnectionException("Could not get CCI Connection", ex);
		}
	}

	/**
	 * 实际从给定的 ConnectionFactory 获取 CCI Connection。
	 * 与 {@link #getConnection} 相同，但抛出原始的 ResourceException。
	 * <p>能感知绑定到当前线程的相应 Connection，例如在使用
	 * {@link CciLocalTransactionManager} 时。如果事务同步处于活动状态
	 * （例如在 JTA 事务中），会将 Connection 绑定到线程。
	 * <p>由 {@link TransactionAwareConnectionFactoryProxy} 直接访问。
	 * @param cf 要从中获取 Connection 的 ConnectionFactory
	 * @return 来自给定 ConnectionFactory 的 CCI Connection
	 * @throws ResourceException 如果由 CCI API 方法抛出
	 * @see #doReleaseConnection
	 */
	public static Connection doGetConnection(ConnectionFactory cf) throws ResourceException {
		Assert.notNull(cf, "No ConnectionFactory specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		if (conHolder != null) {
			return conHolder.getConnection();
		}

		logger.debug("Opening CCI Connection");
		Connection con = cf.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			conHolder = new ConnectionHolder(con);
			conHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(new ConnectionSynchronization(conHolder, cf));
			TransactionSynchronizationManager.bindResource(cf, conHolder);
		}

		return con;
	}

	/**
	 * 确定给定的 JCA CCI Connection 是否为事务性的，即由 Spring 的事务设施绑定到当前线程。
	 * @param con 要检查的 Connection
	 * @param cf 获取该 Connection 时所用的 ConnectionFactory
	 * （可以为 {@code null}）
	 * @return 该 Connection 是否为事务性的
	 */
	public static boolean isConnectionTransactional(Connection con, @Nullable ConnectionFactory cf) {
		if (cf == null) {
			return false;
		}
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		return (conHolder != null && conHolder.getConnection() == con);
	}

	/**
	 * 关闭从给定的 ConnectionFactory 获取的给定 Connection，
	 * 如果它不是外部管理的（即未绑定到线程）。
	 * @param con 如需要则要关闭的 Connection
	 * （如果为 {@code null}，则忽略该调用）
	 * @param cf 获取该 Connection 时所用的 ConnectionFactory
	 * （可以为 {@code null}）
	 * @see #getConnection
	 */
	public static void releaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf) {
		try {
			doReleaseConnection(con, cf);
		}
		catch (ResourceException ex) {
			logger.debug("Could not close CCI Connection", ex);
		}
		catch (Throwable ex) {
			// We don't trust the CCI driver: It might throw RuntimeException or Error.
			logger.debug("Unexpected exception on closing CCI Connection", ex);
		}
	}

	/**
	 * 实际关闭从给定的 ConnectionFactory 获取的给定 Connection。
	 * 与 {@link #releaseConnection} 相同，但抛出原始的 ResourceException。
	 * <p>由 {@link TransactionAwareConnectionFactoryProxy} 直接访问。
	 * @param con 如需要则要关闭的 Connection
	 * （如果为 {@code null}，则忽略该调用）
	 * @param cf 获取该 Connection 时所用的 ConnectionFactory
	 * （可以为 {@code null}）
	 * @throws ResourceException 如果由 JCA CCI 方法抛出
	 * @see #doGetConnection
	 */
	public static void doReleaseConnection(@Nullable Connection con, @Nullable ConnectionFactory cf)
			throws ResourceException {

		if (con == null || isConnectionTransactional(con, cf)) {
			return;
		}
		con.close();
	}


	/**
	 * 在非原生 CCI 事务结束时进行资源清理的回调
	 * （例如参与 JTA 事务时）。
	 */
	private static class ConnectionSynchronization
			extends ResourceHolderSynchronization<ConnectionHolder, ConnectionFactory> {

		public ConnectionSynchronization(ConnectionHolder connectionHolder, ConnectionFactory connectionFactory) {
			super(connectionHolder, connectionFactory);
		}

		@Override
		protected void releaseResource(ConnectionHolder resourceHolder, ConnectionFactory resourceKey) {
			releaseConnection(resourceHolder.getConnection(), resourceKey);
		}
	}

}
