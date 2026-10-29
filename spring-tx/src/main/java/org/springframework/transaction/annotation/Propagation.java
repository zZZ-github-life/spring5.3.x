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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * 枚举类, 表示与{@link Transactional}注解一起使用的事务传播行为,
 * 对应{@link TransactionDefinition}接口。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Propagation {

	/**
	 * 支持当前事务, 如果不存在则创建新事务。
	 * 类似于同名的EJB事务属性。
	 * <p>这是事务注解的默认设置。
	 */
	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

	/**
	 * 支持当前事务, 如果不存在则以非事务方式执行。
	 * 类似于同名的EJB事务属性。
	 * <p>注意: 对于具有事务同步的事务管理器, {@code SUPPORTS}与完全没有事务略有不同,
	 * 因为它定义了同步将适用的事务范围。因此, 相同的资源(JDBC Connection、Hibernate Session等)
	 * 将在整个指定范围内共享。请注意, 这取决于事务管理器的实际同步配置。
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 */
	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

	/**
	 * 支持当前事务, 如果不存在则抛出异常。
	 * 类似于同名的EJB事务属性。
	 */
	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

	/**
	 * 创建新事务, 如果存在当前事务则挂起当前事务。
	 * 类似于同名的EJB事务属性。
	 * <p><b>注意:</b> 实际的事务挂起不会在所有事务管理器上开箱即用。
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它要求将{@code javax.transaction.TransactionManager}提供给它(在标准Java EE中是特定于服务器的)。
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

	/**
	 * 以非事务方式执行, 如果存在当前事务则挂起当前事务。
	 * 类似于同名的EJB事务属性。
	 * <p><b>注意:</b> 实际的事务挂起不会在所有事务管理器上开箱即用。
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它要求将{@code javax.transaction.TransactionManager}提供给它(在标准Java EE中是特定于服务器的)。
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

	/**
	 * 以非事务方式执行, 如果存在事务则抛出异常。
	 * 类似于同名的EJB事务属性。
	 */
	NEVER(TransactionDefinition.PROPAGATION_NEVER),

	/**
	 * 如果当前事务存在, 则在嵌套事务中执行, 否则行为类似{@code REQUIRED}。
	 * EJB中没有类似的功能。
	 * <p>注意: 嵌套事务的实际创建只会在特定的事务管理器上生效。
	 * 开箱即用的情况下, 这仅适用于JDBC DataSourceTransactionManager。
	 * 一些JTA提供者可能也支持嵌套事务。
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	NESTED(TransactionDefinition.PROPAGATION_NESTED);


	private final int value;


	Propagation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}
