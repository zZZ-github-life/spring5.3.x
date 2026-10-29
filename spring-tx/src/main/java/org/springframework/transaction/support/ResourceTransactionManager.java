/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager} 接口的扩展，
 * 表示在单个目标资源上运行的本地资源事务管理器。此类事务管理器与 JTA 事务管理器的区别在于，
 * 它们不对开放数量的资源使用 XA 事务列举，而是专注于利用单个目标资源的原生能力和简洁性。
 *
 * <p>此接口主要用于事务管理器的抽象内省，为客户端提供关于他们获得的是哪种事务管理器
 * 以及事务管理器在哪个具体资源上操作的提示。
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 * @see TransactionSynchronizationManager
 */
public interface ResourceTransactionManager extends PlatformTransactionManager {

	/**
	 * 返回此事务管理器操作的资源工厂，例如 JDBC DataSource 或 JMS ConnectionFactory。
	 * <p>此目标资源工厂通常用作 {@link TransactionSynchronizationManager}
	 * 每线程资源绑定的资源键。
	 * @return 目标资源工厂（不为 {@code null}）
	 * @see TransactionSynchronizationManager#bindResource
	 * @see TransactionSynchronizationManager#getResource
	 */
	Object getResourceFactory();

}
