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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 {@link DataSourceLookup} 实现，依赖映射来进行查找。
 *
 * <p>适用于测试环境或需要将任意 {@link String} 名称
 * 与目标 {@link DataSource} 对象相匹配的应用程序。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
public class MapDataSourceLookup implements DataSourceLookup {

	private final Map<String, DataSource> dataSources = new HashMap<>(4);


	/**
	 * 创建 {@link MapDataSourceLookup} 类的新实例。
	 */
	public MapDataSourceLookup() {
	}

	/**
	 * 创建 {@link MapDataSourceLookup} 类的新实例。
	 * @param dataSources the {@link Map} of {@link DataSource DataSources}; the keys
	 * are {@link String Strings}, the values are actual {@link DataSource} instances.
	 */
	public MapDataSourceLookup(Map<String, DataSource> dataSources) {
		setDataSources(dataSources);
	}

	/**
	 * 创建 {@link MapDataSourceLookup} 类的新实例。
	 * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
	 * @param dataSource the {@link DataSource} to be added
	 */
	public MapDataSourceLookup(String dataSourceName, DataSource dataSource) {
		addDataSource(dataSourceName, dataSource);
	}


	/**
	 * 设置 {@link DataSource DataSource} 的 {@link Map}；键为
	 * {@link String String}，值为实际的 {@link DataSource} 实例。
	 * <p>如果提供的 {@link Map} 为 {@code null}，
	 * 则调用此方法实际上没有任何效果。
	 * @param dataSources said {@link Map} of {@link DataSource DataSources}
	 */
	public void setDataSources(@Nullable Map<String, DataSource> dataSources) {
		if (dataSources != null) {
			this.dataSources.putAll(dataSources);
		}
	}

	/**
	 * 获取此对象维护的 {@link DataSource DataSource} 的 {@link Map}。
	 * <p>返回的 {@link Map} 是 {@link Collections#unmodifiableMap(java.util.Map) 不可修改的}。
	 * @return said {@link Map} of {@link DataSource DataSources} (never {@code null})
	 */
	public Map<String, DataSource> getDataSources() {
		return Collections.unmodifiableMap(this.dataSources);
	}

	/**
	 * 将提供的 {@link DataSource} 添加到此对象维护的
	 * {@link DataSource DataSource} 映射中。
	 * @param dataSourceName the name under which the supplied {@link DataSource} is to be added
	 * @param dataSource the {@link DataSource} to be so added
	 */
	public void addDataSource(String dataSourceName, DataSource dataSource) {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSources.put(dataSourceName, dataSource);
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		DataSource dataSource = this.dataSources.get(dataSourceName);
		if (dataSource == null) {
			throw new DataSourceLookupFailureException(
					"No DataSource with name '" + dataSourceName + "' registered");
		}
		return dataSource;
	}

}
