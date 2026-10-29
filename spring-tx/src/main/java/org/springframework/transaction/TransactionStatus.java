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

package org.springframework.transaction;

import java.io.Flushable;

/**
 * 事务状态的表示。
 *
 * <p>事务代码可以使用它来获取状态信息，
 * 并以编程方式请求回滚（而不是抛出导致隐式回滚的异常）。
 *
 * <p>包含 {@link SavepointManager} 接口，以提供对保存点管理功能的访问。
 * 请注意，保存点管理仅在底层事务管理器支持时可用。
 *
 * @author Juergen Hoeller
 * @since 27.03.2003
 * @see #setRollbackOnly()
 * @see PlatformTransactionManager#getTransaction
 * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
 * @see org.springframework.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
 */
public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {

	/**
	 * 返回此事务内部是否携带保存点，
	 * 即是否基于保存点创建为嵌套事务。
	 * <p>此方法主要用于诊断目的，与 {@link #isNewTransaction()} 一起使用。
	 * 对于自定义保存点的编程式处理，请使用 {@link SavepointManager} 提供的操作。
	 * @see #isNewTransaction()
	 * @see #createSavepoint()
	 * @see #rollbackToSavepoint(Object)
	 * @see #releaseSavepoint(Object)
	 */
	boolean hasSavepoint();

	/**
	 * 如果适用，将底层会话刷新到数据存储中：
	 * 例如，所有受影响的 Hibernate/JPA 会话。
	 * <p>这实际上只是一个提示，如果底层事务管理器没有刷新概念，
	 * 则可能是空操作。刷新信号可能会应用于主要资源或事务同步，
	 * 具体取决于底层资源。
	 */
	@Override
	void flush();

}
