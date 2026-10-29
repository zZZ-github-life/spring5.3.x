/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * 用于按名称查找 DataSource 的策略接口。
 *
 * <p>例如，用于解析 JPA {@code persistence.xml} 文件中的数据源名称。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager#setDataSourceLookup
 */
@FunctionalInterface
public interface DataSourceLookup {

	/**
	 * 获取由给定名称标识的 DataSource。
	 * @param dataSourceName the name of the DataSource
	 * @return the DataSource (never {@code null})
	 * @throws DataSourceLookupFailureException if the lookup failed
	 */
	DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException;

}
