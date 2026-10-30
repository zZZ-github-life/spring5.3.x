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

/**
 * 用于按名称查找 {@link ConnectionFactory} 的策略接口。
 *
 * @author Mark Paluch
 * @since 5.3
 */
@FunctionalInterface
public interface ConnectionFactoryLookup {

	/**
	 * 获取由给定名称标识的 {@link ConnectionFactory}。
	 * @param connectionFactoryName {@link ConnectionFactory} 的名称
	 * @return {@link ConnectionFactory}（永不为 {@code null}）
	 * @throws ConnectionFactoryLookupFailureException 如果查找失败
	 */
	ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException;

}
