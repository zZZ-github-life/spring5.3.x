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

package org.springframework.transaction;

/**
 * 当事务超时时抛出的异常。
 *
 * <p>当根据为给定事务指定的超时时间，在尝试操作时已达到事务的截止时间，
 * Spring 的本地事务策略会抛出此异常。
 *
 * <p>除了在每次事务操作前进行此类检查之外，Spring 的本地事务策略还会
 * 将适当的超时值传递给资源操作（例如传递给 JDBC Statement，让 JDBC 驱动
 * 遵守超时时间）。此类操作如果操作超时已过期，通常会抛出本地资源异常
 * （例如 JDBC SQLException），然后在相应的 DAO 中被转换为 Spring 的
 * DataAccessException（DAO 可能使用 Spring 的 JdbcTemplate 等）。
 *
 * <p>在 JTA 环境中，由 JTA 事务协调器负责应用事务超时。通常，相应的
 * JTA 感知连接池将执行超时检查并抛出相应的本地资源异常
 * （例如 JDBC SQLException）。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.springframework.transaction.support.ResourceHolderSupport#getTimeToLiveInMillis
 * @see java.sql.Statement#setQueryTimeout
 * @see java.sql.SQLException
 */
@SuppressWarnings("serial")
public class TransactionTimedOutException extends TransactionException {

	/**
	 * TransactionTimedOutException 的构造函数。
	 * @param msg the detail message
	 */
	public TransactionTimedOutException(String msg) {
		super(msg);
	}

	/**
	 * TransactionTimedOutException 的构造函数。
	 * @param msg the detail message
	 * @param cause the root cause from the transaction API in use
	 */
	public TransactionTimedOutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
