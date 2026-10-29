/*
 * Copyright 2002-2018 the original author or authors.
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


import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在 Spring {@code ApplicationContext} 生命周期内管理 Jetty 资源（即
 * {@link Executor}、{@link ByteBufferPool} 和 {@link Scheduler}）的工厂。
 *
 * <p>此工厂实现 {@link InitializingBean} 和 {@link DisposableBean}，
 * 通常应声明为 Spring 管理的 bean。
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public class JettyResourceFactory implements InitializingBean, DisposableBean {

	@Nullable
	private Executor executor;

	@Nullable
	private ByteBufferPool byteBufferPool;

	@Nullable
	private Scheduler scheduler;

	private String threadPrefix = "jetty-http";


	/**
	 * 配置要使用的 {@link Executor}。
	 * <p>默认使用 {@link QueuedThreadPool} 初始化。
	 * @param executor the executor to use
	 */
	public void setExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * 配置要使用的 {@link ByteBufferPool}。
	 * <p>默认使用 {@link MappedByteBufferPool} 初始化。
	 * @param byteBufferPool the {@link ByteBuffer} pool to use
	 */
	public void setByteBufferPool(@Nullable ByteBufferPool byteBufferPool) {
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * 配置要使用的 {@link Scheduler}。
	 * <p>默认使用 {@link ScheduledExecutorScheduler} 初始化。
	 * @param scheduler the {@link Scheduler} to use
	 */
	public void setScheduler(@Nullable Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * 配置用于初始化 {@link QueuedThreadPool} 执行器的线程前缀。
	 * 仅当未{@link #setExecutor(Executor) 提供} {@link Executor} 实例时
	 * 使用此项。
	 * <p>默认设置为 "jetty-http"。
	 * @param threadPrefix the thread prefix to use
	 */
	public void setThreadPrefix(String threadPrefix) {
		Assert.notNull(threadPrefix, "Thread prefix is required");
		this.threadPrefix = threadPrefix;
	}

	/**
	 * 返回已配置的 {@link Executor}。
	 */
	@Nullable
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * 返回已配置的 {@link ByteBufferPool}。
	 */
	@Nullable
	public ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * 返回已配置的 {@link Scheduler}。
	 */
	@Nullable
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());
		if (this.executor == null) {
			QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName(name);
			this.executor = threadPool;
		}
		if (this.byteBufferPool == null) {
			this.byteBufferPool = new MappedByteBufferPool(2048,
					this.executor instanceof ThreadPool.SizedThreadPool
							? ((ThreadPool.SizedThreadPool) this.executor).getMaxThreads() / 2
							: ProcessorUtils.availableProcessors() * 2);
		}
		if (this.scheduler == null) {
			this.scheduler = new ScheduledExecutorScheduler(name + "-scheduler", false);
		}

		if (this.executor instanceof LifeCycle) {
			((LifeCycle)this.executor).start();
		}
		this.scheduler.start();
	}

	@Override
	public void destroy() throws Exception {
		try {
			if (this.executor instanceof LifeCycle) {
				((LifeCycle)this.executor).stop();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
		try {
			if (this.scheduler != null) {
				this.scheduler.stop();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
	}

}
