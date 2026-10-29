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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * 枚举类, 表示与{@link Transactional}注解一起使用的事务隔离级别,
 * 对应{@link TransactionDefinition}接口。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Isolation {

	/**
	 * 使用底层数据存储的默认隔离级别。所有其他级别对应于JDBC隔离级别。
	 * @see java.sql.Connection
	 */
	DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

	/**
	 * 表示可能发生脏读、不可重复读和幻读的常量。此级别允许一个事务在行更改提交之前
	 * 被另一个事务读取(即"脏读")。如果任何更改被回滚, 第二个事务将检索到无效的行。
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

	/**
	 * 表示防止脏读; 可能发生不可重复读和幻读的常量。此级别仅禁止事务读取
	 * 包含未提交更改的行。
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

	/**
	 * 表示防止脏读和不可重复读; 可能发生幻读的常量。此级别禁止事务读取包含未提交更改的行,
	 * 同时还禁止以下情况: 一个事务读取一行, 第二个事务修改该行, 第一个事务再次读取该行,
	 * 第二次获得不同的值(即"不可重复读")。
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

	/**
	 * 表示防止脏读、不可重复读和幻读的常量。此级别包含{@code ISOLATION_REPEATABLE_READ}
	 * 中的禁止项, 并进一步禁止以下情况: 一个事务读取满足{@code WHERE}条件的所有行,
	 * 第二个事务插入满足该{@code WHERE}条件的一行, 第一个事务再次按相同条件读取,
	 * 在第二次读取时检索到额外的"幻"行。
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);


	private final int value;


	Isolation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}
