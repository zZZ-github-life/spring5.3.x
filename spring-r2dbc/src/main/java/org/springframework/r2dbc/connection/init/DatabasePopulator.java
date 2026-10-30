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

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.util.Assert;

/**
 * 用于填充、初始化或清理数据库的策略。
 *
 * @author Mark Paluch
 * @author Keith Donald
 * @author Sam Brannen
 * @since 5.3
 * @see ResourceDatabasePopulator
 * @see ConnectionFactoryInitializer
 */
@FunctionalInterface
public interface DatabasePopulator {

	/**
	 * 使用提供的 R2DBC {@link Connection} 填充、初始化或清理数据库。
	 * @param connection 用于填充数据库的 R2DBC 连接；
	 * 已配置并可直接使用，且不得为 {@code null}
	 * @return 启动脚本执行并在完成时收到通知的
	 * {@link Mono}
	 * @throws ScriptException 在所有其他错误情况下
	 */
	Mono<Void> populate(Connection connection) throws ScriptException;

	/**
	 * 针对给定的 {@link ConnectionFactory} 执行当前 {@link DatabasePopulator}。
	 * @param connectionFactory 要对其执行操作的 {@link ConnectionFactory}
	 * @return 启动 {@link DatabasePopulator#populate(Connection)}
	 * 并在完成时收到通知的 {@link Mono}
	 */
	default Mono<Void> populate(ConnectionFactory connectionFactory) throws DataAccessException {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		return Mono.usingWhen(ConnectionFactoryUtils.getConnection(connectionFactory), //
				this::populate, //
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory), //
				(connection, err) -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory),
				connection -> ConnectionFactoryUtils.releaseConnection(connection, connectionFactory))
				.onErrorMap(ex -> !(ex instanceof ScriptException),
						ex -> new UncategorizedScriptException("Failed to execute database script", ex));
	}

}
