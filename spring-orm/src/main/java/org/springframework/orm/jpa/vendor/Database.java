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

package org.springframework.orm.jpa.vendor;

/**
 * 常见数据库平台的枚举。可对数据库类型进行强类型定义，
 * 并在不同 JpaVendorDialect 实现之间实现可移植的配置。
 *
 * <p>如果某个 PersistenceProvider 支持此处未列出的数据库，
 * 仍可使用全限定类名指定策略类。
 * 此枚举仅提供便利。此处列出的数据库产品
 * 与 {@code sql-error-codes.xml} 中为 Spring JDBC 异常转换
 * 显式支持的产品相同。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see AbstractJpaVendorAdapter#setDatabase
 */
public enum Database {

	DEFAULT,

	DB2,

	DERBY,

	/** @since 2.5.5 */
	H2,

	/** @since 5.1 */
	HANA,

	HSQL,

	INFORMIX,

	MYSQL,

	ORACLE,

	POSTGRESQL,

	SQL_SERVER,

	SYBASE

}
