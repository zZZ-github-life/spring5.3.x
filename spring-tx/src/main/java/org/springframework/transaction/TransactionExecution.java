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

/**
 * 事务当前状态的通用表示。
 * 作为 {@link TransactionStatus} 和 {@link ReactiveTransaction} 的基础接口。
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public interface TransactionExecution {

	/**
	 * 返回当前事务是否为新事务；否则表示参与了一个现有事务，
	 * 或者可能根本没有在实际事务中运行。
	 */
	boolean isNewTransaction();

	/**
	 * 将事务设置为仅回滚。这通知事务管理器，该事务唯一可能的结果是回滚，
	 * 作为抛出异常（反过来也会触发回滚）的替代方案。
	 */
	void setRollbackOnly();

	/**
	 * 返回事务是否已被标记为仅回滚
	 * （由应用程序或事务基础设施标记）。
	 */
	boolean isRollbackOnly();

	/**
	 * 返回此事务是否已完成，即是否已经提交或回滚。
	 */
	boolean isCompleted();

}
