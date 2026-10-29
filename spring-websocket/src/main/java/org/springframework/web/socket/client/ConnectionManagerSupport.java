/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 连接管理器的基类，用于结合 Spring ApplicationContext 生命周期自动完成
 * 连接到 WebSocket 服务器的过程。在 {@link #start()} 时连接到 WebSocket
 * 服务器，在 {@link #stop()} 时断开连接。
 * 如果将 {@link #setAutoStartup(boolean)} 设置为 {@code true}，则会在
 * Spring {@code ApplicationContext} 刷新时自动执行这些操作。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class ConnectionManagerSupport implements SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final URI uri;

	private boolean autoStartup = false;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public ConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
		this.uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(
				uriVariables).encode().toUri();
	}


	protected URI getUri() {
		return this.uri;
	}

	/**
	 * 设置在此连接管理器初始化且 Spring 上下文刷新之后，是否自动连接到
	 * 远程端点。
	 * <p>默认值为 "false"。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回 'autoStartup' 属性的值。如果为 "true"，此端点连接管理器将在
	 * ContextRefreshedEvent 时连接到远程端点。
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定建立到远程端点的连接以及随后关闭连接的阶段。启动顺序从低到高，
	 * 关闭顺序则相反。默认情况下，该值为
	 * Integer.MAX_VALUE，表示此端点连接工厂尽可能晚地连接，
	 * 并尽可能早地关闭。
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此端点连接工厂自动连接和停止所处的阶段。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}


	/**
	 * 启动 WebSocket 连接。如果已经连接，则此方法不产生任何影响。
	 */
	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				startInternal();
			}
		}
	}

	protected void startInternal() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting " + getClass().getSimpleName());
			}
			this.running = true;
			openConnection();
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping " + getClass().getSimpleName());
				}
				try {
					stopInternal();
				}
				catch (Throwable ex) {
					logger.error("Failed to stop WebSocket connection", ex);
				}
				finally {
					this.running = false;
				}
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	protected void stopInternal() throws Exception {
		if (isConnected()) {
			closeConnection();
		}
	}

	/**
	 * 返回此 ConnectionManager 是否已启动。
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 连接是打开状态（{@code true}）还是关闭状态（{@code false}）。
	 */
	public abstract boolean isConnected();

	/**
	 * 子类实现此方法以实际建立连接。
	 */
	protected abstract void openConnection();

	/**
	 * 子类实现此方法以关闭连接。
	 */
	protected abstract void closeConnection() throws Exception;

}
