/*
 * Copyright 2002-2019 the original author or authors.
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

import java.sql.SQLException;
import java.sql.Savepoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.util.Assert;

/**
 * JDBC 感知事务对象的便捷基类。可以包含一个带有 JDBC {@code Connection}
 * 的 {@link ConnectionHolder}，并基于该 {@code ConnectionHolder}
 * 实现 {@link SavepointManager} 接口。
 *
 * <p>允许以编程方式管理 JDBC {@link java.sql.Savepoint Savepoint}。
 * Spring 的 {@link org.springframework.transaction.support.DefaultTransactionStatus}
 * 会自动委托给它，因为它会自动检测实现了 {@link SavepointManager} 接口
 * 的事务对象。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DataSourceTransactionManager
 */
public abstract class JdbcTransactionObjectSupport implements SavepointManager, SmartTransactionObject {

	private static final Log logger = LogFactory.getLog(JdbcTransactionObjectSupport.class);


	@Nullable
	private ConnectionHolder connectionHolder;

	@Nullable
	private Integer previousIsolationLevel;

	private boolean readOnly = false;

	private boolean savepointAllowed = false;


	/**
	 * 为此事务对象设置 ConnectionHolder。
	 */
	public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
		this.connectionHolder = connectionHolder;
	}

	/**
	 * 返回此事务对象的 ConnectionHolder。
	 */
	public ConnectionHolder getConnectionHolder() {
		Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
		return this.connectionHolder;
	}

	/**
	 * 检查此事务对象是否拥有 ConnectionHolder。
	 */
	public boolean hasConnectionHolder() {
		return (this.connectionHolder != null);
	}

	/**
	 * 设置要保留的先前隔离级别（如果有）。
	 */
	public void setPreviousIsolationLevel(@Nullable Integer previousIsolationLevel) {
		this.previousIsolationLevel = previousIsolationLevel;
	}

	/**
	 * 返回所保留的先前隔离级别（如果有）。
	 */
	@Nullable
	public Integer getPreviousIsolationLevel() {
		return this.previousIsolationLevel;
	}

	/**
	 * 设置此事务的只读状态。
	 * 默认值为 {@code false}。
	 * @since 5.2.1
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * 返回此事务的只读状态。
	 * @since 5.2.1
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 设置此事务内是否允许保存点（Savepoint）。
	 * 默认值为 {@code false}。
	 */
	public void setSavepointAllowed(boolean savepointAllowed) {
		this.savepointAllowed = savepointAllowed;
	}

	/**
	 * 返回此事务内是否允许保存点（Savepoint）。
	 */
	public boolean isSavepointAllowed() {
		return this.savepointAllowed;
	}

	@Override
	public void flush() {
		// no-op
	}


	//---------------------------------------------------------------------
	// Implementation of SavepointManager
	//---------------------------------------------------------------------

	/**
	 * 此实现创建一个 JDBC 3.0 保存点（Savepoint）并将其返回。
	 * @see java.sql.Connection#setSavepoint
	 */
	@Override
	public Object createSavepoint() throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			if (!conHolder.supportsSavepoints()) {
				throw new NestedTransactionNotSupportedException(
						"Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
			}
			if (conHolder.isRollbackOnly()) {
				throw new CannotCreateTransactionException(
						"Cannot create savepoint for transaction which is already marked as rollback-only");
			}
			return conHolder.createSavepoint();
		}
		catch (SQLException ex) {
			throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
		}
	}

	/**
	 * 此实现回滚到给定的 JDBC 3.0 保存点（Savepoint）。
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			conHolder.getConnection().rollback((Savepoint) savepoint);
			conHolder.resetRollbackOnly();
		}
		catch (Throwable ex) {
			throw new TransactionSystemException("Could not roll back to JDBC savepoint", ex);
		}
	}

	/**
	 * 此实现释放给定的 JDBC 3.0 保存点（Savepoint）。
	 * @see java.sql.Connection#releaseSavepoint
	 */
	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		ConnectionHolder conHolder = getConnectionHolderForSavepoint();
		try {
			conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
		}
		catch (Throwable ex) {
			logger.debug("Could not explicitly release JDBC savepoint", ex);
		}
	}

	protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
		if (!isSavepointAllowed()) {
			throw new NestedTransactionNotSupportedException(
					"Transaction manager does not allow nested transactions");
		}
		if (!hasConnectionHolder()) {
			throw new TransactionUsageException(
					"Cannot create nested transaction when not exposing a JDBC transaction");
		}
		return getConnectionHolder();
	}

}
