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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.util.Assert;

/**
 * 简单的 {@link ConnectionFactoryLookup} 实现，依赖映射完成查找。
 *
 * <p>适用于测试环境，或需要将任意 {@link String} 名称
 * 与目标 {@link ConnectionFactory} 对象进行匹配的应用程序。
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 5.3
 */
public class MapConnectionFactoryLookup implements ConnectionFactoryLookup {

	private final Map<String, ConnectionFactory> connectionFactories = new HashMap<>();


	/**
	 * 创建一个新的 {@link MapConnectionFactoryLookup} 类实例。
	 */
	public MapConnectionFactoryLookup() {}

	/**
	 * 创建一个新的 {@link MapConnectionFactoryLookup} 类实例。
	 * @param connectionFactories {@link ConnectionFactory} 的 {@link Map}。
	 * 键为 {@link String Strings}，值为实际的 {@link ConnectionFactory} 实例。
	 */
	public MapConnectionFactoryLookup(Map<String, ConnectionFactory> connectionFactories) {
		setConnectionFactories(connectionFactories);
	}

	/**
	 * 创建一个新的 {@link MapConnectionFactoryLookup} 类实例。
	 * @param connectionFactoryName 添加所提供 {@link ConnectionFactory} 时使用的名称
	 * @param connectionFactory 要添加的 {@link ConnectionFactory}
	 */
	public MapConnectionFactoryLookup(String connectionFactoryName, ConnectionFactory connectionFactory) {
		addConnectionFactory(connectionFactoryName, connectionFactory);
	}


	/**
	 * 设置 {@link ConnectionFactory ConnectionFactory} 的 {@link Map}。
	 * 键为 {@link String 字符串}，值为实际的 {@link ConnectionFactory} 实例。
	 * <p>如果所提供的 {@link Map} 为 {@code null}，则此方法调用实际上不产生任何效果。
	 * @param connectionFactories 所述的 {@link ConnectionFactory connectionFactories} {@link Map}
	 */
	public void setConnectionFactories(Map<String, ConnectionFactory> connectionFactories) {
		Assert.notNull(connectionFactories, "ConnectionFactories must not be null");
		this.connectionFactories.putAll(connectionFactories);
	}

	/**
	 * 获取此对象维护的 {@link ConnectionFactory ConnectionFactory} {@link Map}。
	 * <p>返回的 {@link Map} 是 {@link Collections#unmodifiableMap(Map) 不可修改的}。
	 * @return {@link ConnectionFactory connectionFactory} 的 {@link Map}（永不为 {@code null}）
	 */
	public Map<String, ConnectionFactory> getConnectionFactories() {
		return Collections.unmodifiableMap(this.connectionFactories);
	}

	/**
	 * 将所提供的 {@link ConnectionFactory} 添加到此对象维护的
	 * {@link ConnectionFactory ConnectionFactory} 实例映射中。
	 * @param connectionFactoryName 添加所提供 {@link ConnectionFactory} 时使用的名称
	 * @param connectionFactory 要以该方式添加的 {@link ConnectionFactory}
	 */
	public void addConnectionFactory(String connectionFactoryName, ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactoryName, "ConnectionFactory name must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactories.put(connectionFactoryName, connectionFactory);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		Assert.notNull(connectionFactoryName, "ConnectionFactory name must not be null");
		return this.connectionFactories.computeIfAbsent(connectionFactoryName, key -> {
			throw new ConnectionFactoryLookupFailureException(
					"No ConnectionFactory with name '" + connectionFactoryName + "' registered");
		});
	}

}
