/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.Flushable;

/**
 * 由能够返回内部 rollback-only 标记的事务对象实现的接口，
 * 通常来自另一个已参与并将其标记为 rollback-only 的事务。
 *
 * <p>由 DefaultTransactionStatus 自动检测，即使不是来自当前 TransactionStatus，
 * 也能始终返回当前的 rollbackOnly 标志。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultTransactionStatus#isRollbackOnly
 */
public interface SmartTransactionObject extends Flushable {

	/**
	 * 返回事务是否在内部被标记为 rollback-only。
	 * 例如，可以检查 JTA UserTransaction。
	 * @see javax.transaction.UserTransaction#getStatus
	 * @see javax.transaction.Status#STATUS_MARKED_ROLLBACK
	 */
	boolean isRollbackOnly();

	/**
	 * 如果适用，将底层会话刷新到数据存储：例如，所有受影响的 Hibernate/JPA 会话。
	 */
	@Override
	void flush();

}
