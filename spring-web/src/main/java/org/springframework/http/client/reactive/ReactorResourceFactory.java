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

package org.springframework.http.client.reactive;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在 Spring {@code ApplicationContext} 生命周期内管理 Reactor Netty 资源
 * （即用于事件循环线程的 {@link LoopResources}，以及用于连接池的
 * {@link ConnectionProvider}）的工厂。
 *
 * <p>此工厂实现 {@link InitializingBean} 和 {@link DisposableBean}，
 * 通常应声明为 Spring 管理的 bean。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.1
 */
public class ReactorResourceFactory implements InitializingBean, DisposableBean {

	private boolean useGlobalResources = true;

	@Nullable
	private Consumer<HttpResources> globalResourcesConsumer;

	private Supplier<ConnectionProvider> connectionProviderSupplier = () -> ConnectionProvider.create("webflux", 500);

	@Nullable
	private ConnectionProvider connectionProvider;

	private Supplier<LoopResources> loopResourcesSupplier = () -> LoopResources.create("webflux-http");

	@Nullable
	private LoopResources loopResources;

	private boolean manageConnectionProvider = false;

	private boolean manageLoopResources = false;

	private Duration shutdownQuietPeriod = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_QUIET_PERIOD);

	private Duration shutdownTimeout = Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_TIMEOUT);


	/**
	 * 是否通过 {@link HttpResources} 使用全局 Reactor Netty 资源。
	 * <p>默认值为 "true"，此时此工厂会在 Spring 的
	 * {@code ApplicationContext} 生命周期内初始化和停止全局 Reactor Netty 资源。
	 * 如果设置为 "false"，则工厂将独立于全局资源管理其资源。
	 * @param useGlobalResources 是否公开并管理全局资源
	 * @see #addGlobalResourcesConsumer(Consumer)
	 */
	public void setUseGlobalResources(boolean useGlobalResources) {
		this.useGlobalResources = useGlobalResources;
	}

	/**
	 * 此工厂是否公开全局
	 * {@link reactor.netty.http.HttpResources HttpResources} 持有者。
	 */
	public boolean isUseGlobalResources() {
		return this.useGlobalResources;
	}

	/**
	 * 添加一个 Consumer，用于在启动时配置全局 Reactor Netty 资源。
	 * 使用此选项时，也会启用 {@link #setUseGlobalResources}。
	 * @param consumer 要应用的 consumer
	 * @see #setUseGlobalResources(boolean)
	 */
	public void addGlobalResourcesConsumer(Consumer<HttpResources> consumer) {
		this.useGlobalResources = true;
		this.globalResourcesConsumer = (this.globalResourcesConsumer != null ?
				this.globalResourcesConsumer.andThen(consumer) : consumer);
	}

	/**
	 * 当你不想参与全局资源，并且希望自定义托管
	 * {@code ConnectionProvider} 的创建时，请使用此项。
	 * <p>默认使用 {@code ConnectionProvider.elastic("http")}。
	 * <p>请注意，如果 {@code userGlobalResources=false} 或
	 * 设置了 {@link #setConnectionProvider(ConnectionProvider)}，则忽略此选项。
	 * @param supplier 要使用的 supplier
	 */
	public void setConnectionProviderSupplier(Supplier<ConnectionProvider> supplier) {
		this.connectionProviderSupplier = supplier;
	}

	/**
	 * 当你希望提供外部管理的
	 * {@link ConnectionProvider} 实例时，请使用此项。
	 * @param connectionProvider 要按原样使用的连接提供者
	 */
	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * 返回已配置的 {@link ConnectionProvider}。
	 */
	public ConnectionProvider getConnectionProvider() {
		Assert.state(this.connectionProvider != null, "ConnectionProvider not initialized yet");
		return this.connectionProvider;
	}

	/**
	 * 当你不想参与全局资源，并且希望自定义托管
	 * {@code LoopResources} 的创建时，请使用此项。
	 * <p>默认使用 {@code LoopResources.create("reactor-http")}。
	 * <p>请注意，如果 {@code userGlobalResources=false} 或
	 * 设置了 {@link #setLoopResources(LoopResources)}，则忽略此选项。
	 * @param supplier 要使用的 supplier
	 */
	public void setLoopResourcesSupplier(Supplier<LoopResources> supplier) {
		this.loopResourcesSupplier = supplier;
	}

	/**
	 * 当你希望提供外部管理的
	 * {@link LoopResources} 实例时，请使用此选项。
	 * @param loopResources 要按原样使用的 loop 资源
	 */
	public void setLoopResources(LoopResources loopResources) {
		this.loopResources = loopResources;
	}

	/**
	 * 返回已配置的 {@link LoopResources}。
	 */
	public LoopResources getLoopResources() {
		Assert.state(this.loopResources != null, "LoopResources not initialized yet");
		return this.loopResources;
	}

	/**
	 * 配置在关闭资源之前等待的时间量。
	 * 如果在 {@code shutdownQuietPeriod} 期间提交任务，则保证
	 * 接受该任务，并且 {@code shutdownQuietPeriod} 将重新开始。
	 * <p>默认设置为
	 * {@link LoopResources#DEFAULT_SHUTDOWN_QUIET_PERIOD}，即 2 秒，
	 * 但也可以通过系统属性
	 * {@link reactor.netty.ReactorNetty#SHUTDOWN_QUIET_PERIOD
	 * ReactorNetty.SHUTDOWN_QUIET_PERIOD} 覆盖。
	 * @since 5.2.4
	 * @see #setShutdownTimeout(Duration)
	 */
	public void setShutdownQuietPeriod(Duration shutdownQuietPeriod) {
		Assert.notNull(shutdownQuietPeriod, "shutdownQuietPeriod should not be null");
		this.shutdownQuietPeriod = shutdownQuietPeriod;
	}

	/**
	 * 配置等待底层资源释放的最长时间，无论
	 * 在 {@code shutdownQuietPeriod} 期间是否提交了任务。
	 * <p>默认设置为
	 * {@link LoopResources#DEFAULT_SHUTDOWN_TIMEOUT}，即 15 秒，
	 * 但也可以通过系统属性
	 * {@link reactor.netty.ReactorNetty#SHUTDOWN_TIMEOUT
	 * ReactorNetty.SHUTDOWN_TIMEOUT} 覆盖。
	 * @since 5.2.4
	 * @see #setShutdownQuietPeriod(Duration)
	 */
	public void setShutdownTimeout(Duration shutdownTimeout) {
		Assert.notNull(shutdownTimeout, "shutdownTimeout should not be null");
		this.shutdownTimeout = shutdownTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.useGlobalResources) {
			Assert.isTrue(this.loopResources == null && this.connectionProvider == null,
					"'useGlobalResources' is mutually exclusive with explicitly configured resources");
			HttpResources httpResources = HttpResources.get();
			if (this.globalResourcesConsumer != null) {
				this.globalResourcesConsumer.accept(httpResources);
			}
			this.connectionProvider = httpResources;
			this.loopResources = httpResources;
		}
		else {
			if (this.loopResources == null) {
				this.manageLoopResources = true;
				this.loopResources = this.loopResourcesSupplier.get();
			}
			if (this.connectionProvider == null) {
				this.manageConnectionProvider = true;
				this.connectionProvider = this.connectionProviderSupplier.get();
			}
		}
	}

	@Override
	public void destroy() {
		if (this.useGlobalResources) {
			HttpResources.disposeLoopsAndConnectionsLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
		}
		else {
			try {
				ConnectionProvider provider = this.connectionProvider;
				if (provider != null && this.manageConnectionProvider) {
					provider.disposeLater().block();
				}
			}
			catch (Throwable ex) {
				// ignore
			}

			try {
				LoopResources resources = this.loopResources;
				if (resources != null && this.manageLoopResources) {
					resources.disposeLater(this.shutdownQuietPeriod, this.shutdownTimeout).block();
				}
			}
			catch (Throwable ex) {
				// ignore
			}
		}
	}

}
