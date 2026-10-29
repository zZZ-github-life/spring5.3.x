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

package org.springframework.r2dbc.connection;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * R2DBC {@link ConnectionFactory} 实现，将所有调用委托给给定的目标 {@link ConnectionFactory}。
 *
 * <p>此类设计为可被继承，子类只需覆盖那些不应简单委托给目标
 * {@link ConnectionFactory} 的方法（例如 {@link #create()}）。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see #create()
 */
public class DelegatingConnectionFactory implements ConnectionFactory, Wrapped<ConnectionFactory> {

	private final ConnectionFactory targetConnectionFactory;


	/**
	 * 创建一个新的 DelegatingConnectionFactory。
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public DelegatingConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * 返回此 ConnectionFactory 所委托的目标 ConnectionFactory。
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	@Override
	public Mono<? extends Connection> create() {
		return Mono.from(this.targetConnectionFactory.create());
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		return this.targetConnectionFactory.getMetadata();
	}

	@Override
	public ConnectionFactory unwrap() {
		return this.targetConnectionFactory;
	}

}
