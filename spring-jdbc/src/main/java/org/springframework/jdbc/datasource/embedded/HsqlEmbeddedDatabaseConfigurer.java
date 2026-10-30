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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * HSQL 嵌入式数据库实例的 {@link EmbeddedDatabaseConfigurer}。
 *
 * <p>调用 {@link #getInstance()} 获取此类的单例实例。
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
final class HsqlEmbeddedDatabaseConfigurer extends AbstractEmbeddedDatabaseConfigurer {

	@Nullable
	private static HsqlEmbeddedDatabaseConfigurer instance;

	private final Class<? extends Driver> driverClass;


	/**
	 * 获取单例 {@link HsqlEmbeddedDatabaseConfigurer} 实例。
	 * @return 配置器实例
	 * @throws ClassNotFoundException 如果 HSQL 不在 classpath 上
	 */
	@SuppressWarnings("unchecked")
	public static synchronized HsqlEmbeddedDatabaseConfigurer getInstance() throws ClassNotFoundException {
		if (instance == null) {
			instance = new HsqlEmbeddedDatabaseConfigurer( (Class<? extends Driver>)
					ClassUtils.forName("org.hsqldb.jdbcDriver", HsqlEmbeddedDatabaseConfigurer.class.getClassLoader()));
		}
		return instance;
	}


	private HsqlEmbeddedDatabaseConfigurer(Class<? extends Driver> driverClass) {
		this.driverClass = driverClass;
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(this.driverClass);
		properties.setUrl("jdbc:hsqldb:mem:" + databaseName);
		properties.setUsername("sa");
		properties.setPassword("");
	}

}
