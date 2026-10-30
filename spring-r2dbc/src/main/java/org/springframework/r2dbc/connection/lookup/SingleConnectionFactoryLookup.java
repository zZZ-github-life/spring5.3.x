/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.util.Assert;

/**
 * {@link ConnectionFactoryLookup} 的一个实现，
 * 它简单包装单个给定的 {@link ConnectionFactory}，
 * 对任何连接工厂名称都返回该实例。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public class SingleConnectionFactoryLookup implements ConnectionFactoryLookup {

	private final ConnectionFactory connectionFactory;


	/**
	 * 创建一个新的 {@link SingleConnectionFactoryLookup} 类实例。
	 * @param connectionFactory 要包装的单个 {@link ConnectionFactory}
	 */
	public SingleConnectionFactoryLookup(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}


	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		return this.connectionFactory;
	}

}
