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

package org.springframework.r2dbc.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Wrapped;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link DelegatingConnectionFactory} 的实现，包装单个在使用后不会被关闭的
 * R2DBC {@link Connection}。显然，它不支持多线程。
 *
 * <p>请注意，在关闭时，应当有人通过 {@code close()} 方法关闭底层的
 * {@code Connection}。如果客户端代码是 SmartConnectionFactory 感知的
 * （例如使用 {@link ConnectionFactoryUtils#releaseConnection(Connection, ConnectionFactory)}），
 * 则它绝不会在 {@code Connection} 句柄上调用 close。
 *
 * <p>如果客户端代码把 {@code Connection} 当作池化连接而调用
 * {@link Connection#close()}（例如使用持久化工具时），请将 "suppressClose"
 * 设置为 {@code true}。这样将返回一个抑制关闭操作的代理，而不是物理 Connection。
 *
 * <p>此实现主要用于测试以及连接管道的使用。例如，它可以让那些期望在
 * {@link ConnectionFactory} 上工作的代码，无需应用服务器即可轻松测试。
 * 请注意，此实现并不充当类似连接池的工具。
 * 连接池需要一个池化的 {@link ConnectionFactory}，例如来自
 * {@code r2dbc-pool} 的实现。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see #create()
 * @see Connection#close()
 * @see ConnectionFactoryUtils#releaseConnection(Connection, ConnectionFactory)
 */
public class SingleConnectionFactory extends DelegatingConnectionFactory
		implements DisposableBean {

	/** 是否创建抑制关闭操作的代理？ */
	private boolean suppressClose;

	/** 是否覆盖自动提交状态？ */
	private @Nullable Boolean autoCommit;

	/** 被包装的 Connection。 */
	private final AtomicReference<Connection> target = new AtomicReference<>();

	/** 代理 Connection。 */
	private @Nullable Connection connection;

	private final Mono<? extends Connection> connectionEmitter;


	/**
	 * 用于 bean 风格配置的构造函数。
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
		this.connectionEmitter = super.create().cache();
	}

	/**
	 * 使用 R2DBC 连接 URL 创建一个新的 {@code SingleConnectionFactory}。
	 * @param url 用于访问 {@link ConnectionFactory} 发现机制的 R2DBC URL
	 * @param suppressClose 返回的 {@link Connection} 应是
	 * 抑制关闭的代理，还是物理 {@code Connection}
	 * @see ConnectionFactories#get(String)
	 */
	public SingleConnectionFactory(String url, boolean suppressClose) {
		super(ConnectionFactories.get(url));
		this.suppressClose = suppressClose;
		this.connectionEmitter = super.create().cache();
	}

	/**
	 * 使用给定的 {@link Connection} 和 {@link ConnectionFactoryMetadata}
	 * 创建一个新的 {@code SingleConnectionFactory}。
	 * @param target 底层目标 {@code Connection}
	 * @param metadata 要与此 {@code ConnectionFactory} 关联的
	 * {@code ConnectionFactory} 元数据
	 * @param suppressClose 如果 {@code Connection} 应包装为抑制 {@code close()} 调用的
	 * {@code Connection}，则为 {@code true}（以便期望池化
	 * {@code Connection} 的应用可以正常使用 {@code close()}）
	 */
	public SingleConnectionFactory(Connection target, ConnectionFactoryMetadata metadata, boolean suppressClose) {
		super(new ConnectionFactory() {
			@Override
			public Publisher<? extends Connection> create() {
				return Mono.just(target);
			}
			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return metadata;
			}
		});
		Assert.notNull(target, "Connection must not be null");
		Assert.notNull(metadata, "ConnectionFactoryMetadata must not be null");
		this.target.set(target);
		this.connectionEmitter = Mono.just(target);
		this.suppressClose = suppressClose;
		this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
	}


	/**
	 * 设置返回的 {@link Connection} 是抑制关闭的代理，还是物理 {@code Connection}。
	 */
	public void setSuppressClose(boolean suppressClose) {
		this.suppressClose = suppressClose;
	}

	/**
	 * 返回返回的 {@link Connection} 是抑制关闭的代理，还是物理 {@code Connection}。
	 */
	protected boolean isSuppressClose() {
		return this.suppressClose;
	}

	/**
	 * 设置是否应覆盖返回的 {@link Connection} 的 "autoCommit" 设置。
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	/**
	 * 返回是否应覆盖返回的 {@link Connection} 的 "autoCommit" 设置。
	 * @return "autoCommit" 值，如果没有要应用的值则为 {@code null}
	 */
	@Nullable
	protected Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	public Mono<? extends Connection> create() {
		Connection connection = this.target.get();
		return this.connectionEmitter.map(connectionToUse -> {
			if (connection == null) {
				this.target.compareAndSet(null, connectionToUse);
				this.connection =
						(isSuppressClose() ? getCloseSuppressingConnectionProxy(connectionToUse) : connectionToUse);
			}
			return this.connection;
		}).flatMap(this::prepareConnection);
	}

	/**
	 * 关闭底层的 {@link Connection}。
	 * 此 {@link ConnectionFactory} 的提供方需要负责正确关闭。
	 * <p>由于此 bean 实现了 {@link DisposableBean}，bean 工厂会在销毁其缓存的单例时
	 * 自动调用此方法。
	 */
	@Override
	public void destroy() {
		resetConnection().block();
	}

	/**
	 * 重置底层的共享 Connection，以便在下次访问时重新初始化。
	 */
	public Mono<Void> resetConnection() {
		Connection connection = this.target.get();
		if (connection == null) {
			return Mono.empty();
		}
		return Mono.defer(() -> {
			if (this.target.compareAndSet(connection, null)) {
				this.connection = null;
				return Mono.from(connection.close());
			}
			return Mono.empty();
		});
	}

	/**
	 * 在使用 {@link Connection} 之前对其进行准备。
	 * 如果配置了 {@linkplain #getAutoCommitValue() 自动提交} 设置，则会应用该设置。
	 * @param connection 请求的 {@code Connection}
	 * @return 准备好的 {@code Connection}
	 */
	protected Mono<Connection> prepareConnection(Connection connection) {
		Boolean autoCommit = getAutoCommitValue();
		if (autoCommit != null) {
			return Mono.from(connection.setAutoCommit(autoCommit)).thenReturn(connection);
		}
		return Mono.just(connection);
	}

	/**
	 * 用代理包装给定的 {@link Connection}，该代理将所有方法调用委托给目标，
	 * 但抑制 close 调用。
	 * @param target 要包装的原始 {@code Connection}
	 * @return 包装后的 Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(SingleConnectionFactory.class.getClassLoader(),
				new Class<?>[] { Connection.class, Wrapped.class }, new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * 用于抑制 R2DBC Connection 上 close 调用的调用处理器。
	 *
	 * @see Connection#close()
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return proxy == args[0];
				case "hashCode":
					// Use hashCode of PersistenceManager proxy.
					return System.identityHashCode(proxy);
				case "unwrap":
					return this.target;
				case "close":
					// Handle close method: suppress, not valid.
					return Mono.empty();
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
