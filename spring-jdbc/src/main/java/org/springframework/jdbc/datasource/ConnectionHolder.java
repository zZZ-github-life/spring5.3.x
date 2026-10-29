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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * 包装 JDBC {@link Connection} 的资源持有器。
 * {@link DataSourceTransactionManager} 会将此类的实例
 * 针对特定的 {@link javax.sql.DataSource} 绑定到当前线程。
 *
 * <p>从基类继承了嵌套 JDBC 事务的仅回滚（rollback-only）支持
 * 以及引用计数功能。
 *
 * <p>注意：这是一个 SPI 类，不打算供应用程序使用。
 *
 * @author Juergen Hoeller
 * @since 06.05.2003
 * @see DataSourceTransactionManager
 * @see DataSourceUtils
 */
public class ConnectionHolder extends ResourceHolderSupport {

	/**
	 * 保存点名称的前缀。
	 */
	public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";


	@Nullable
	private ConnectionHandle connectionHandle;

	@Nullable
	private Connection currentConnection;

	private boolean transactionActive = false;

	@Nullable
	private Boolean savepointsSupported;

	private int savepointCounter = 0;


	/**
	 * 为给定的 ConnectionHandle 创建一个新的 ConnectionHolder。
	 * @param connectionHandle the ConnectionHandle to hold
	 */
	public ConnectionHolder(ConnectionHandle connectionHandle) {
		Assert.notNull(connectionHandle, "ConnectionHandle must not be null");
		this.connectionHandle = connectionHandle;
	}

	/**
	 * 为给定的 JDBC Connection 创建一个新的 ConnectionHolder，
	 * 并将其包装为 {@link SimpleConnectionHandle}，
	 * 假定当前没有正在进行的事务。
	 * @param connection the JDBC Connection to hold
	 * @see SimpleConnectionHandle
	 * @see #ConnectionHolder(java.sql.Connection, boolean)
	 */
	public ConnectionHolder(Connection connection) {
		this.connectionHandle = new SimpleConnectionHandle(connection);
	}

	/**
	 * 为给定的 JDBC Connection 创建一个新的 ConnectionHolder，
	 * 并将其包装为 {@link SimpleConnectionHandle}。
	 * @param connection the JDBC Connection to hold
	 * @param transactionActive whether the given Connection is involved
	 * in an ongoing transaction
	 * @see SimpleConnectionHandle
	 */
	public ConnectionHolder(Connection connection, boolean transactionActive) {
		this(connection);
		this.transactionActive = transactionActive;
	}


	/**
	 * 返回此 ConnectionHolder 持有的 ConnectionHandle。
	 */
	@Nullable
	public ConnectionHandle getConnectionHandle() {
		return this.connectionHandle;
	}

	/**
	 * 返回此持有器当前是否持有一个 Connection。
	 */
	protected boolean hasConnection() {
		return (this.connectionHandle != null);
	}

	/**
	 * 设置此持有器是否代表一个活动的、由 JDBC 管理的事务。
	 * @see DataSourceTransactionManager
	 */
	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	/**
	 * 返回此持有器是否代表一个活动的、由 JDBC 管理的事务。
	 */
	protected boolean isTransactionActive() {
		return this.transactionActive;
	}


	/**
	 * 用给定的 Connection 覆盖现有的 Connection 句柄。
	 * 如果给定 {@code null}，则重置句柄。
	 * <p>用于在挂起时释放 Connection（传入 {@code null}
	 * 参数），并在恢复时设置新的 Connection。
	 */
	protected void setConnection(@Nullable Connection connection) {
		if (this.currentConnection != null) {
			if (this.connectionHandle != null) {
				this.connectionHandle.releaseConnection(this.currentConnection);
			}
			this.currentConnection = null;
		}
		if (connection != null) {
			this.connectionHandle = new SimpleConnectionHandle(connection);
		}
		else {
			this.connectionHandle = null;
		}
	}

	/**
	 * 返回此 ConnectionHolder 当前持有的 Connection。
	 * <p>在 ConnectionHolder 上调用 {@code released} 之前，返回的将是同一个
	 * Connection；调用之后将重置所持有的 Connection，并在需要时获取新的
	 * Connection。
	 * @see ConnectionHandle#getConnection()
	 * @see #released()
	 */
	public Connection getConnection() {
		Assert.notNull(this.connectionHandle, "Active Connection is required");
		if (this.currentConnection == null) {
			this.currentConnection = this.connectionHandle.getConnection();
		}
		return this.currentConnection;
	}

	/**
	 * 返回是否支持 JDBC 3.0 保存点（Savepoint）。
	 * 该标志会在 ConnectionHolder 的整个生命周期内缓存。
	 * @throws SQLException if thrown by the JDBC driver
	 */
	public boolean supportsSavepoints() throws SQLException {
		if (this.savepointsSupported == null) {
			this.savepointsSupported = getConnection().getMetaData().supportsSavepoints();
		}
		return this.savepointsSupported;
	}

	/**
	 * 为当前 Connection 创建一个新的 JDBC 3.0 保存点（Savepoint），
	 * 使用对该 Connection 唯一的自动生成保存点名称。
	 * @return the new Savepoint
	 * @throws SQLException if thrown by the JDBC driver
	 */
	public Savepoint createSavepoint() throws SQLException {
		this.savepointCounter++;
		return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
	}

	/**
	 * 释放此 ConnectionHolder 当前持有的 Connection。
	 * <p>对于期望“Connection 借用”的 ConnectionHandle 而言，这是必要的：
	 * 每个返回的 Connection 只是临时租用，在数据操作完成后需要归还，
	 * 以便该 Connection 可用于同一事务内的其他操作。
	 */
	@Override
	public void released() {
		super.released();
		if (!isOpen() && this.currentConnection != null) {
			if (this.connectionHandle != null) {
				this.connectionHandle.releaseConnection(this.currentConnection);
			}
			this.currentConnection = null;
		}
	}


	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
		this.savepointsSupported = null;
		this.savepointCounter = 0;
	}

}
