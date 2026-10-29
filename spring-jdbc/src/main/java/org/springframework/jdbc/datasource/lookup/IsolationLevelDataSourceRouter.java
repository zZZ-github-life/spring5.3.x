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

package org.springframework.jdbc.datasource.lookup;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 根据当前事务隔离级别路由到各个目标 DataSource 之一的 DataSource。
 * 目标 DataSource 需要以隔离级别名称作为键进行配置，
 * 这些名称定义在 {@link org.springframework.transaction.TransactionDefinition TransactionDefinition 接口} 上。
 *
 * <p>这在结合 JTA 事务管理（通常通过 Spring 的
 * {@link org.springframework.transaction.jta.JtaTransactionManager}）时特别有用。
 * 标准 JTA 不支持针对特定事务的隔离级别。一些 JTA 提供者以厂商特定扩展的形式
 * 支持隔离级别（例如 WebLogic），这是解决此问题的首选方式。作为替代方案
 * （例如在 WebSphere 上），目标数据库可以通过多个 JNDI DataSource 来表示，
 * 每个 DataSource 都配置了不同的隔离级别（针对整个 DataSource）。
 * 当前的 DataSource 路由器允许根据当前事务的隔离级别透明地切换到适当的 DataSource。
 *
 * <p>例如，假设目标 DataSource 被定义为名为
 * "myRepeatableReadDataSource"、"mySerializableDataSource" 和 "myDefaultDataSource"
 * 的独立 Spring bean，则配置可以如下所示：
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadDataSource"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableDataSource"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" ref="myDefaultDataSource"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 或者，带键的值也可以是数据源名称，通过 {@link #setDataSourceLookup DataSourceLookup}
 * 解析：默认情况下是用于标准 JNDI 查找的 JNDI 名称。
 * 这样只需一个简洁的定义，而无需单独的 DataSource bean 定义。
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value="java:comp/env/jdbc/myrrds"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value="java:comp/env/jdbc/myserds"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" value="java:comp/env/jdbc/mydefds"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 注意：如果将此路由器与 Spring 的
 * {@link org.springframework.transaction.jta.JtaTransactionManager} 结合使用，
 * 请不要忘记将 "allowCustomIsolationLevels" 标志切换为 "true"。
 * （默认情况下，由于标准 JTA 本身缺乏对隔离级别的支持，
 * JtaTransactionManager 只会接受默认的隔离级别。）
 *
 * <pre class="code">
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="allowCustomIsolationLevels" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
public class IsolationLevelDataSourceRouter extends AbstractRoutingDataSource {

	/** TransactionDefinition 的 Constants 实例。 */
	private static final Constants constants = new Constants(TransactionDefinition.class);


	/**
	 * 既支持隔离级别常量的 Integer 值，
	 * 也支持定义在 {@link org.springframework.transaction.TransactionDefinition TransactionDefinition 接口}
	 * 上的隔离级别名称。
	 */
	@Override
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		if (lookupKey instanceof Integer) {
			return lookupKey;
		}
		else if (lookupKey instanceof String) {
			String constantName = (String) lookupKey;
			if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
				throw new IllegalArgumentException("Only isolation constants allowed");
			}
			return constants.asNumber(constantName);
		}
		else {
			throw new IllegalArgumentException(
					"Invalid lookup key - needs to be isolation level Integer or isolation level name String: " + lookupKey);
		}
	}

	@Override
	@Nullable
	protected Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
	}

}
