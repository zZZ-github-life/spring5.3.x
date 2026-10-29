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

package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * DataSourceLookup 的一个实现，它只是包装单个给定的 DataSource，
 * 对任何数据源名称都返回该 DataSource。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SingleDataSourceLookup implements DataSourceLookup {

	private final DataSource dataSource;


	/**
	 * 创建 {@link SingleDataSourceLookup} 类的新实例。
	 * @param dataSource the single {@link DataSource} to wrap
	 */
	public SingleDataSourceLookup(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSource = dataSource;
	}


	@Override
	public DataSource getDataSource(String dataSourceName) {
		return this.dataSource;
	}

}
