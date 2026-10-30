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

package org.springframework.transaction.jta;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.springframework.lang.Nullable;

/**
 * 基于指定的事务特征创建JTA {@link javax.transaction.Transaction}对象的策略接口。
 *
 * <p>默认实现{@link SimpleTransactionFactory}简单地包装标准的JTA
 * {@link javax.transaction.TransactionManager}。此策略接口允许更复杂的实现,
 * 以适配厂商特定的JTA扩展。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.transaction.TransactionManager#getTransaction()
 * @see SimpleTransactionFactory
 * @see JtaTransactionManager
 */
public interface TransactionFactory {

	/**
	 * 基于给定的名称和超时时间创建一个活动的Transaction对象。
	 * @param name 事务名称（可能为 {@code null}）
	 * @param timeout 事务超时时间（默认超时时间可用 -1）
	 * @return 活动的 Transaction 对象（永不为 {@code null}）
	 * @throws NotSupportedException 如果事务管理器不支持指定类型的事务
	 * @throws SystemException 如果事务管理器创建事务失败
	 */
	Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException;

	/**
	 * 确定底层事务管理器是否支持由资源适配器管理的XA事务
	 * (即无需显式的XA资源登记)。
	 * <p>通常为{@code false}。由
	 * {@link org.springframework.jca.endpoint.AbstractMessageEndpointFactory}检查,
	 * 以区分无效配置和有效的ResourceAdapter管理事务。
	 * @see javax.resource.spi.ResourceAdapter#endpointActivation
	 * @see javax.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
	 */
	boolean supportsResourceAdapterManagedTransactions();

}
