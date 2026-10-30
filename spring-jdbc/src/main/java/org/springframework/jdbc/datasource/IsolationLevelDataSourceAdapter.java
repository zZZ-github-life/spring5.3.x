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

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 目标 {@link javax.sql.DataSource} 的适配器，会将当前 Spring 事务的
 * 隔离级别（以及可能指定的用户凭据）应用到每次 {@code getConnection} 调用。
 * 如果指定了只读标志，也会一并应用。
 *
 * <p>可用于代理未配置所需隔离级别（和用户凭据）的目标 JNDI DataSource。
 * 客户端代码可以像平常一样使用此 DataSource，而无需担心这些设置。
 *
 * <p>从父类 {@link UserCredentialsDataSourceAdapter} 继承了应用特定用户凭据
 * 的能力；有关该功能的详细信息（例如 {@link #setCredentialsForCurrentThread}），
 * 请参阅后者的 javadoc。
 *
 * <p><b>警告：</b>此适配器只是为其获取的每个 Connection 调用
 * {@link java.sql.Connection#setTransactionIsolation} 和/或
 * {@link java.sql.Connection#setReadOnly}。但它<i>不会</i>重置这些设置；
 * 相反，它期望目标 DataSource 在其连接池处理过程中执行此类重置。
 * <b>请确保目标 DataSource 能正确清理此类事务状态。</b>
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setIsolationLevel
 * @see #setIsolationLevelName
 * @see #setUsername
 * @see #setPassword
 */
public class IsolationLevelDataSourceAdapter extends UserCredentialsDataSourceAdapter {

	/** TransactionDefinition 的 Constants 实例。 */
	private static final Constants constants = new Constants(TransactionDefinition.class);

	@Nullable
	private Integer isolationLevel;


	/**
	 * 通过 {@link org.springframework.transaction.TransactionDefinition} 中
	 * 相应常量的名称设置默认隔离级别，例如 "ISOLATION_SERIALIZABLE"。
	 * <p>如果未指定，将使用目标 DataSource 的默认值。
	 * 请注意，事务特定的隔离值将始终覆盖在 DataSource 级别指定的任何隔离设置。
	 * @param constantName 常量的名称
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see #setIsolationLevel
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 按 JDBC {@link java.sql.Connection} 常量
	 * （等同于相应的 Spring
	 * {@link org.springframework.transaction.TransactionDefinition} 常量）
	 * 指定获取 Connection 时要使用的默认隔离级别。
	 * <p>如果未指定，将使用目标 DataSource 的默认值。
	 * 请注意，事务特定的隔离值将始终覆盖在 DataSource 级别指定的任何隔离设置。
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 */
	public void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(DefaultTransactionDefinition.PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevel : null);
	}

	/**
	 * 返回静态指定的隔离级别，
	 * 如果没有则返回 {@code null}。
	 */
	@Nullable
	protected Integer getIsolationLevel() {
		return this.isolationLevel;
	}


	/**
	 * 将当前的隔离级别值和只读标志应用到返回的 Connection。
	 * @see #getCurrentIsolationLevel()
	 * @see #getCurrentReadOnlyFlag()
	 */
	@Override
	protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
		Connection con = super.doGetConnection(username, password);
		Boolean readOnlyToUse = getCurrentReadOnlyFlag();
		if (readOnlyToUse != null) {
			con.setReadOnly(readOnlyToUse);
		}
		Integer isolationLevelToUse = getCurrentIsolationLevel();
		if (isolationLevelToUse != null) {
			con.setTransactionIsolation(isolationLevelToUse);
		}
		return con;
	}

	/**
	 * 确定当前的隔离级别：要么是事务的隔离级别，
	 * 要么是静态定义的隔离级别。
	 * @return 当前隔离级别，如果没有则为 {@code null}
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 * @see #setIsolationLevel
	 */
	@Nullable
	protected Integer getCurrentIsolationLevel() {
		Integer isolationLevelToUse = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
		if (isolationLevelToUse == null) {
			isolationLevelToUse = getIsolationLevel();
		}
		return isolationLevelToUse;
	}

	/**
	 * 确定当前的只读标志：默认为事务的只读提示。
	 * @return 当前作用域是否有只读提示
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	@Nullable
	protected Boolean getCurrentReadOnlyFlag() {
		boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
		return (txReadOnly ? Boolean.TRUE : null);
	}

}
