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

import java.util.Map;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 抽象的 {@link ConnectionFactory} 实现，它根据查找键将
 * {@link #create()} 调用路由到多个目标 {@link ConnectionFactory 工厂}之一。
 * 查找键通常（但不一定）由某个订阅者上下文确定。
 *
 * <p> 允许配置一个 {@link #setDefaultTargetConnectionFactory(Object)
 * 默认 ConnectionFactory} 作为回退。
 *
 * <p> 如果已配置，对 {@link #getMetadata()} 的调用将被路由到
 * {@link #setDefaultTargetConnectionFactory(Object) 默认 ConnectionFactory}。
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 5.3
 * @see #setTargetConnectionFactories
 * @see #setDefaultTargetConnectionFactory
 * @see #determineCurrentLookupKey()
 */
public abstract class AbstractRoutingConnectionFactory implements ConnectionFactory, InitializingBean {

	private static final Object FALLBACK_MARKER = new Object();


	@Nullable
	private Map<?, ?> targetConnectionFactories;

	@Nullable
	private Object defaultTargetConnectionFactory;

	private boolean lenientFallback = true;

	private ConnectionFactoryLookup connectionFactoryLookup = new MapConnectionFactoryLookup();

	@Nullable
	private Map<Object, ConnectionFactory> resolvedConnectionFactories;

	@Nullable
	private ConnectionFactory resolvedDefaultConnectionFactory;


	/**
	 * 指定目标 {@link ConnectionFactory ConnectionFactory} 的映射，
	 * 以查找键作为键。映射值可以是相应的 {@link ConnectionFactory} 实例，
	 * 也可以是连接工厂名称字符串（将通过
	 * {@link #setConnectionFactoryLookup ConnectionFactoryLookup} 解析）。
	 * <p>键可以是任意类型；此类只实现通用的查找流程。
	 * 具体的键表示由 {@link #resolveSpecifiedLookupKey(Object)}
	 * 和 {@link #determineCurrentLookupKey()} 处理。
	 */
	public void setTargetConnectionFactories(Map<?, ?> targetConnectionFactories) {
		this.targetConnectionFactories = targetConnectionFactories;
	}

	/**
	 * 指定默认的目标 {@link ConnectionFactory}（如果有）。
	 * <p>映射值可以是相应的 {@link ConnectionFactory} 实例，
	 * 也可以是连接工厂名称 {@link String}（将通过
	 * {@link #setConnectionFactoryLookup ConnectionFactoryLookup} 解析）。
	 * <p>如果键控的 {@link #setTargetConnectionFactories targetConnectionFactories}
	 * 中没有任何一项与 {@link #determineCurrentLookupKey() 当前查找键}匹配，
	 * 则将使用此 {@link ConnectionFactory} 作为目标。
	 */
	public void setDefaultTargetConnectionFactory(Object defaultTargetConnectionFactory) {
		this.defaultTargetConnectionFactory = defaultTargetConnectionFactory;
	}

	/**
	 * 指定当无法为当前查找键找到特定 {@link ConnectionFactory} 时，
	 * 是否对默认 {@link ConnectionFactory} 应用宽松回退。
	 * <p>默认值为 {@code true}，即接受在目标 {@link ConnectionFactory}
	 * 映射中没有对应条目的查找键 —— 在这种情况下直接回退到默认的
	 * {@link ConnectionFactory}。
	 * <p>如果你希望仅在没有发出任何查找键时才应用回退，
	 * 请将此标志切换为 {@code false}。此时，没有对应 {@link ConnectionFactory}
	 * 条目的查找键将导致 {@link IllegalStateException}。
	 * @see #setTargetConnectionFactories
	 * @see #setDefaultTargetConnectionFactory
	 * @see #determineCurrentLookupKey()
	 */
	public void setLenientFallback(boolean lenientFallback) {
		this.lenientFallback = lenientFallback;
	}

	/**
	 * 设置用于解析 {@link #setTargetConnectionFactories targetConnectionFactories}
	 * 映射中连接工厂名称字符串的 {@link ConnectionFactoryLookup} 实现。
	 */
	public void setConnectionFactoryLookup(ConnectionFactoryLookup connectionFactoryLookup) {
		Assert.notNull(connectionFactoryLookup, "ConnectionFactoryLookup must not be null");
		this.connectionFactoryLookup = connectionFactoryLookup;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.targetConnectionFactories, "Property 'targetConnectionFactories' must not be null");

		this.resolvedConnectionFactories = CollectionUtils.newHashMap(this.targetConnectionFactories.size());
		this.targetConnectionFactories.forEach((key, value) -> {
			Object lookupKey = resolveSpecifiedLookupKey(key);
			ConnectionFactory connectionFactory = resolveSpecifiedConnectionFactory(value);
			this.resolvedConnectionFactories.put(lookupKey, connectionFactory);
		});

		if (this.defaultTargetConnectionFactory != null) {
			this.resolvedDefaultConnectionFactory = resolveSpecifiedConnectionFactory(this.defaultTargetConnectionFactory);
		}
	}

	/**
	 * 将给定的查找键对象（如 {@link #setTargetConnectionFactories targetConnectionFactories}
	 * 映射中所指定）解析为用于与 {@link #determineCurrentLookupKey() 当前查找键}
	 * 进行匹配的实际查找键。
	 * <p>默认实现简单地按原样返回给定的键。
	 * @param lookupKey 用户指定的查找键对象
	 * @return 匹配所需的查找键。
	 */
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		return lookupKey;
	}

	/**
	 * 将指定的连接工厂对象解析为 {@link ConnectionFactory} 实例。
	 * <p>默认实现处理 {@link ConnectionFactory} 实例和连接工厂名称
	 * （将通过 {@link #setConnectionFactoryLookup ConnectionFactoryLookup} 解析）。
	 * @param connectionFactory {@link #setTargetConnectionFactories targetConnectionFactories}
	 * 映射中指定的连接工厂值对象
	 * @return 解析后的 {@link ConnectionFactory}（永不为 {@code null}）
	 * @throws IllegalArgumentException 在不支持的值类型情况下
	 */
	protected ConnectionFactory resolveSpecifiedConnectionFactory(Object connectionFactory)
			throws IllegalArgumentException {

		if (connectionFactory instanceof ConnectionFactory) {
			return (ConnectionFactory) connectionFactory;
		}
		else if (connectionFactory instanceof String) {
			return this.connectionFactoryLookup.getConnectionFactory((String) connectionFactory);
		}
		else {
			throw new IllegalArgumentException("Illegal connection factory value - " +
					"only 'io.r2dbc.spi.ConnectionFactory' and 'String' supported: " + connectionFactory);
		}
	}

	@Override
	public Mono<Connection> create() {
		return determineTargetConnectionFactory().map(ConnectionFactory::create).flatMap(Mono::from);
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		if (this.resolvedDefaultConnectionFactory != null) {
			return this.resolvedDefaultConnectionFactory.getMetadata();
		}
		throw new UnsupportedOperationException(
				"No default ConnectionFactory configured to retrieve ConnectionFactoryMetadata");
	}

	/**
	 * 获取当前的目标 {@link ConnectionFactory}。确定
	 * {@link #determineCurrentLookupKey() 当前查找键}，在
	 * {@link #setTargetConnectionFactories targetConnectionFactories} 映射中执行查找，
	 * 如有必要则回退到指定的
	 * {@link #setDefaultTargetConnectionFactory 默认目标 ConnectionFactory}。
	 * @return 按 {@link #determineCurrentLookupKey()} 发出
	 * 当前 {@link ConnectionFactory} 的 {@link Mono}
	 * @see #determineCurrentLookupKey()
	 */
	protected Mono<ConnectionFactory> determineTargetConnectionFactory() {
		Assert.state(this.resolvedConnectionFactories != null, "ConnectionFactory router not initialized");

		Mono<Object> lookupKey = determineCurrentLookupKey().defaultIfEmpty(FALLBACK_MARKER);

		return lookupKey.handle((key, sink) -> {
			ConnectionFactory connectionFactory = this.resolvedConnectionFactories.get(key);
			if (connectionFactory == null && (key == FALLBACK_MARKER || this.lenientFallback)) {
				connectionFactory = this.resolvedDefaultConnectionFactory;
			}
			if (connectionFactory == null) {
				sink.error(new IllegalStateException(String.format(
						"Cannot determine target ConnectionFactory for lookup key '%s'", key == FALLBACK_MARKER ? null : key)));
				return;
			}
			sink.next(connectionFactory);
		});
	}

	/**
	 * 确定当前的查找键。通常实现为检查订阅者上下文。
	 * 键可以是任意类型。返回的键需要与所存储的查找键类型匹配，
	 * 该类型由 {@link #resolveSpecifiedLookupKey} 方法解析。
	 * @return 发出查找键的 {@link Mono}。
	 * 如果没有可用的查找键，则可能不发出值而完成
	 */
	protected abstract Mono<Object> determineCurrentLookupKey();

}
