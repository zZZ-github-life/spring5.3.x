/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import io.r2dbc.spi.ConnectionFactory;

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
 * @author Mark Paluch
 * @since 5.3
 * @see DatabasePopulator
 */
public class ConnectionFactoryInitializer implements InitializingBean, DisposableBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private DatabasePopulator databasePopulator;

	@Nullable
	private DatabasePopulator databaseCleaner;

	private boolean enabled = true;


	/**
	 * 用于在组件初始化时填充、并在组件关闭时清理的数据库 {@link ConnectionFactory}。
	 * <p>此属性为必需项，没有提供默认值。
	 * @param connectionFactory R2DBC 的 {@link ConnectionFactory}
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 设置要在 bean 初始化阶段执行的 {@link DatabasePopulator}。
	 * @param databasePopulator 初始化期间要使用的 {@link DatabasePopulator}
	 * @see #setDatabaseCleaner
	 */
	public void setDatabasePopulator(DatabasePopulator databasePopulator) {
		this.databasePopulator = databasePopulator;
	}

	/**
	 * 设置要在 bean 销毁阶段执行的 {@link DatabasePopulator}，
	 * 用于清理数据库，并使其恢复为供其他使用者使用的已知状态。
	 * @param databaseCleaner 销毁期间要使用的 {@code DatabasePopulator}
	 * @see #setDatabasePopulator
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	/**
	 * 用于显式启用或禁用 {@linkplain #setDatabasePopulator database populator}
	 * 和 {@linkplain #setDatabaseCleaner database cleaner} 的标志。
	 * @param enabled 如果数据库填充器和数据库清理器应分别在启动和
	 * 关闭时被调用，则为 {@code true}
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	/**
	 * 使用 {@linkplain #setDatabasePopulator database populator} 设置数据库。
	 */
	@Override
	public void afterPropertiesSet() {
		execute(this.databasePopulator);
	}

	/**
	 * 使用 {@linkplain #setDatabaseCleaner database cleaner} 清理数据库。
	 */
	@Override
	public void destroy() {
		execute(this.databaseCleaner);
	}

	private void execute(@Nullable DatabasePopulator populator) {
		Assert.state(this.connectionFactory != null, "ConnectionFactory must be set");
		if (this.enabled && populator != null) {
			populator.populate(this.connectionFactory).block();
		}
	}

}
