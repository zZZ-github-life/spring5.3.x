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

package org.springframework.r2dbc.connection;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;


/**
 * 包装 R2DBC {@link Connection} 的资源持有器。
 * {@link R2dbcTransactionManager} 会将此类的实例绑定到订阅，
 * 并关联到特定的 {@link ConnectionFactory}。
 *
 * <p>继承基类对嵌套 R2DBC 事务的仅回滚（rollback-only）支持以及引用计数功能。
 *
 * <p>注意：这是一个 SPI 类，不打算供应用程序使用。
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 5.3
 * @see R2dbcTransactionManager
 * @see ConnectionFactoryUtils
 */
public class ConnectionHolder extends ResourceHolderSupport {

	@Nullable
	private Connection currentConnection;

	private boolean transactionActive;


	/**
	 * 为给定的 R2DBC {@link Connection} 创建一个新的 ConnectionHolder，
	 * 假定当前没有正在进行的事务。
	 * @param connection the R2DBC {@link Connection} to hold
	 * @see #ConnectionHolder(Connection, boolean)
	 */
	public ConnectionHolder(Connection connection) {
		this(connection, false);
	}

	/**
	 * 为给定的 R2DBC {@link Connection} 创建一个新的 ConnectionHolder。
	 * @param connection the R2DBC {@link Connection} to hold
	 * @param transactionActive whether the given {@link Connection} is involved
	 * in an ongoing transaction
	 */
	public ConnectionHolder(Connection connection, boolean transactionActive) {
		this.currentConnection = connection;
		this.transactionActive = transactionActive;
	}


	/**
	 * 返回此持有器当前是否持有一个 {@link Connection}。
	 */
	protected boolean hasConnection() {
		return (this.currentConnection != null);
	}

	/**
	 * 设置此持有器是否表示一个活动的、由 R2DBC 管理的事务。
	 * @see R2dbcTransactionManager
	 */
	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	/**
	 * 返回此持有器是否表示一个活动的、由 R2DBC 管理的事务。
	 */
	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	/**
	 * 用给定的 {@link Connection} 覆盖现有的 Connection。
	 * <p>用于在挂起时释放 {@link Connection}（传入 {@code null} 参数），
	 * 以及在恢复时设置新的 {@link Connection}。
	 */
	protected void setConnection(@Nullable Connection connection) {
		this.currentConnection = connection;
	}

	/**
	 * 返回此 {@link ConnectionHolder} 当前持有的 {@link Connection}。
	 * <p>在 {@link ConnectionHolder} 上调用 {@code released} 之前，这里始终是同一个
	 * {@link Connection}；调用之后将重置所持有的 {@link Connection}，
	 * 并按需获取新的 {@link Connection}。
	 * @see #released()
	 */
	public Connection getConnection() {
		Assert.notNull(this.currentConnection, "Active Connection is required");
		return this.currentConnection;
	}

	/**
	 * 释放当前的 {@link Connection}。
	 */
	@Override
	public void released() {
		super.released();
		if (!isOpen() && this.currentConnection != null) {
			this.currentConnection = null;
		}
	}

	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}

}
