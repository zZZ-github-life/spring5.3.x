/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionStatus;

/**
 * TransactionCallback 实现的简单便捷类。
 * 允许实现无返回值的 doInTransaction 版本，即不需要 return 语句。
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 * @see TransactionTemplate
 */
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {

	@Override
	@Nullable
	public final Object doInTransaction(TransactionStatus status) {
		doInTransactionWithoutResult(status);
		return null;
	}

	/**
	 * 由 {@code TransactionTemplate.execute} 在事务上下文中调用。
	 * 不需要关心事务本身，虽然它可以通过给定的状态对象检索和影响当前事务的状态，
	 * 例如设置 rollback-only。
	 * <p>回调抛出的 RuntimeException 被视为强制回滚的应用程序异常。
	 * 异常将传播到模板的调用者。
	 * <p>使用 JTA 时请注意：JTA 事务仅适用于事务性 JNDI 资源，
	 * 因此如果实现需要事务支持，就必须使用此类资源。
	 * @param status 关联的事务状态
	 * @see TransactionTemplate#execute
	 */
	protected abstract void doInTransactionWithoutResult(TransactionStatus status);

}
