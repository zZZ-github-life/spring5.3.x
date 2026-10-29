/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.lang.Nullable;

/**
 * {@link EmbeddedDatabaseFactory} 的子类，实现了 {@link FactoryBean}，
 * 以便注册为 Spring Bean。将向 Spring 返回提供嵌入式数据库连接的
 * 实际 {@link DataSource}。
 *
 * <p>返回的是目标 {@link DataSource}，而不是 {@link EmbeddedDatabase}
 * 代理，因为 {@link FactoryBean} 将管理嵌入式数据库实例的初始化和销毁生命周期。
 *
 * <p>实现了 {@link DisposableBean}，以便在管理它的 Spring 容器关闭时
 * 关闭嵌入式数据库。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class EmbeddedDatabaseFactoryBean extends EmbeddedDatabaseFactory
		implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

	@Nullable
	private DatabasePopulator databaseCleaner;


	/**
	 * 设置要在 Bean 销毁回调中运行的脚本执行器，
	 * 用于清理数据库并为其他使用者留下已知状态。
	 * @param databaseCleaner the database script executor to run on destroy
	 * @see #setDatabasePopulator
	 * @see org.springframework.jdbc.datasource.init.DataSourceInitializer#setDatabaseCleaner
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	@Override
	public void afterPropertiesSet() {
		initDatabase();
	}


	@Override
	@Nullable
	public DataSource getObject() {
		return getDataSource();
	}

	@Override
	public Class<? extends DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.databaseCleaner != null && getDataSource() != null) {
			DatabasePopulatorUtils.execute(this.databaseCleaner, getDataSource());
		}
		shutdownDatabase();
	}

}
