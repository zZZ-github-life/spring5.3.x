/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer}，通过相当于自增列的方式递增给定 SQL Server 表的最大值。
 * 注意：如果使用此类，你的表键列应 <i>不</i> 定义为 IDENTITY 列，因为序列表会完成这项工作。
 *
 * <p>此类旨在与 Microsoft SQL Server 配合使用。
 *
 * <p>序列保存在一张表中。每个需要自动生成键的表都应有一张对应的序列表。
 *
 * <p>示例：
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100))
 * create table tab_sequence (id bigint identity)
 * insert into tab_sequence default values</pre>
 *
 * 如果设置了 "cacheSize"，中间值将直接从缓存提供，而无需查询数据库。
 * 如果服务器或应用停止、崩溃，或者事务回滚，未使用的值将永远不会被提供。
 * 因此编号中的最大空洞大小即为 cacheSize 的值。
 *
 * <b>提示：</b> 由于 Microsoft SQL Server 支持 JDBC 3.0 的 {@code getGeneratedKeys} 方法，
 * 建议直接在表中使用 IDENTITY 列，然后使用
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcInsert}，或者在调用
 * {@link org.springframework.jdbc.core.JdbcTemplate}
 * 的 {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}
 * 方法时使用 {@link org.springframework.jdbc.support.KeyHolder}。
 *
 * <p>感谢 Preben Nilsson 的建议！
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public class SqlServerMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	/**
	 * 用于 bean 属性风格用法的默认构造函数。
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public SqlServerMaxValueIncrementer() {
	}

	/**
	 * 便捷构造函数。
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public SqlServerMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " default values";
	}

	@Override
	protected String getIdentityStatement() {
		return "select @@identity";
	}

}
