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

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer}，用于获取给定 PostgreSQL 序列的下一个值。
 *
 * <p>感谢 Tomislav Urban 的建议！
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 */
public class PostgresSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	/**
	 * 用于 bean 属性风格用法的默认构造函数。
	 * @see #setDataSource
	 * @see #setIncrementerName
	 */
	public PostgresSequenceMaxValueIncrementer() {
	}

	/**
	 * 便捷构造函数。
	 * @param dataSource 要使用的 DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public PostgresSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select nextval('" + getIncrementerName() + "')";
	}

}
