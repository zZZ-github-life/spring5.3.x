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

package org.springframework.jdbc.datasource.init;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在初始化时{@linkplain #setDatabasePopulator 设置}数据库，
 * 并在销毁时{@link #setDatabaseCleaner 清理}数据库。
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @since 3.0
 * @see DatabasePopulator
 */
public class DataSourceInitializer implements InitializingBean, DisposableBean {

	@Nullable
	private DataSource dataSource;

	@Nullable
	private DatabasePopulator databasePopulator;

	@Nullable
	private DatabasePopulator databaseCleaner;

	private boolean enabled = true;


	/**
	 * 此组件初始化时要填充、关闭时要清理的数据库的 {@link DataSource}。
	 * <p>此属性是必需的，没有提供默认值。
	 * @param dataSource the DataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置要在 Bean 初始化阶段执行的 {@link DatabasePopulator}。
	 * @param databasePopulator the {@code DatabasePopulator} to use during initialization
	 * @see #setDatabaseCleaner
	 */
	public void setDatabasePopulator(DatabasePopulator databasePopulator) {
		this.databasePopulator = databasePopulator;
	}

	/**
	 * 设置要在 Bean 销毁阶段执行的 {@link DatabasePopulator}，
	 * 用于清理数据库并为其他使用者留下已知状态。
	 * @param databaseCleaner the {@code DatabasePopulator} to use during destruction
	 * @see #setDatabasePopulator
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	/**
	 * 是否显式启用或禁用{@linkplain #setDatabasePopulator 数据库填充器}
	 * 和{@linkplain #setDatabaseCleaner 数据库清理器}。
	 * @param enabled {@code true} if the database populator and database cleaner
	 * should be called on startup and shutdown, respectively
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	/**
	 * 使用{@linkplain #setDatabasePopulator 数据库填充器}设置数据库。
	 */
	@Override
	public void afterPropertiesSet() {
		execute(this.databasePopulator);
	}

	/**
	 * 使用{@linkplain #setDatabaseCleaner 数据库清理器}清理数据库。
	 */
	@Override
	public void destroy() {
		execute(this.databaseCleaner);
	}

	private void execute(@Nullable DatabasePopulator populator) {
		Assert.state(this.dataSource != null, "DataSource must be set");
		if (this.enabled && populator != null) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

}
