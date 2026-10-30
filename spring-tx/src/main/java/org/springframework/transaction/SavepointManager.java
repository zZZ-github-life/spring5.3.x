/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.transaction;

/**
 * 此接口定义了以通用方式编程化管理事务保存点的 API。
 * 由 TransactionStatus 扩展，用于公开特定事务的保存点管理功能。
 *
 * <p>请注意，保存点只能在活跃的事务中使用。
 * 只在有高级需求时才使用这种编程式的保存点处理；
 * 否则，使用 PROPAGATION_NESTED 的子事务更为可取。
 *
 * <p>此接口的设计灵感来自 JDBC 3.0 的 Savepoint 机制，
 * 但不依赖于任何特定的持久化技术。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see TransactionStatus
 * @see TransactionDefinition#PROPAGATION_NESTED
 * @see java.sql.Savepoint
 */
public interface SavepointManager {

	/**
	 * 创建一个新的保存点。你可以通过 {@code rollbackToSavepoint} 回滚到特定的保存点，
	 * 并通过 {@code releaseSavepoint} 显式释放不再需要的保存点。
	 * <p>请注意，大多数事务管理器会在事务完成时自动释放保存点。
	 * @return 一个保存点对象，
	 * 将传入 {@link #rollbackToSavepoint} 或 {@link #releaseSavepoint}
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果无法创建保存点，
	 * 例如因为事务未处于适当状态
	 * @see java.sql.Connection#setSavepoint
	 */
	Object createSavepoint() throws TransactionException;

	/**
	 * 回滚到给定的保存点。
	 * <p>回滚后保存点<i>不会</i>被自动释放。
	 * 你可以显式调用 {@link #releaseSavepoint(Object)} 或依赖事务完成时自动释放。
	 * @param savepoint 要回滚到的保存点
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果回滚失败
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	void rollbackToSavepoint(Object savepoint) throws TransactionException;

	/**
	 * 显式释放给定的保存点。
	 * <p>请注意，大多数事务管理器会在事务完成时自动释放保存点。
	 * <p>如果适当的资源清理最终会在事务完成时发生，
	 * 则实现应尽可能安静地失败。
	 * @param savepoint 要释放的保存点
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果释放失败
	 * @see java.sql.Connection#releaseSavepoint
	 */
	void releaseSavepoint(Object savepoint) throws TransactionException;

}
