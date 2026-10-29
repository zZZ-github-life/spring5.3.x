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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 一个 Spring {@link FactoryBean}，用于构建并暴露预先配置好的 {@link ForkJoinPool}。
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ForkJoinPoolFactoryBean implements FactoryBean<ForkJoinPool>, InitializingBean, DisposableBean {

	private boolean commonPool = false;

	private int parallelism = Runtime.getRuntime().availableProcessors();

	private ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

	@Nullable
	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	private boolean asyncMode = false;

	private int awaitTerminationSeconds = 0;

	@Nullable
	private ForkJoinPool forkJoinPool;


	/**
	 * 设置是否暴露 JDK 8 的 'common' {@link ForkJoinPool}。
	 * <p>默认值为 "false"，即基于此 FactoryBean 上的 {@link #setParallelism "parallelism"}、
	 * {@link #setThreadFactory "threadFactory"}、
	 * {@link #setUncaughtExceptionHandler "uncaughtExceptionHandler"} 和
	 * {@link #setAsyncMode "asyncMode"} 属性创建一个本地 {@link ForkJoinPool} 实例。
	 * <p><b>NOTE:</b> 将此标志设为 "true" 会实际忽略此 FactoryBean 上的所有其他属性，转而复用共享的
	 * JDK 公共 {@link ForkJoinPool}。这在 JDK 8 上是个不错的选择，但会使应用无法自定义 ForkJoinPool 的
	 * 行为，特别是无法使用自定义线程。
	 * @since 3.2
	 * @see java.util.concurrent.ForkJoinPool#commonPool()
	 */
	public void setCommonPool(boolean commonPool) {
		this.commonPool = commonPool;
	}

	/**
	 * 指定并行度级别。默认值为 {@link Runtime#availableProcessors()}。
	 */
	public void setParallelism(int parallelism) {
		this.parallelism = parallelism;
	}

	/**
	 * 设置用于创建新 ForkJoinWorkerThread 的工厂。默认值为
	 * {@link ForkJoinPool#defaultForkJoinWorkerThreadFactory}。
	 */
	public void setThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 设置内部工作线程的处理器，这些线程因执行任务时遇到无法恢复的错误而终止。默认无。
	 */
	public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	/**
	 * 指定是否为那些从不被 join 的分叉任务建立本地的先进先出调度模式。在工作线程只处理事件式异步任务的
	 * 应用中，此模式（asyncMode = {@code true}）可能比默认的本地基于栈的模式更合适。默认值为
	 * {@code false}。
	 */
	public void setAsyncMode(boolean asyncMode) {
		this.asyncMode = asyncMode;
	}

	/**
	 * 设置此 ForkJoinPool 在关闭时最多应阻塞多少秒，以等待剩余任务完成执行，然后容器其余部分才继续关闭。
	 * 如果剩余任务可能需要访问同样由容器管理的其他资源，这一点尤其有用。
	 * <p>默认情况下，此 ForkJoinPool 根本不会等待任务终止。它会在容器其余部分关闭的同时，继续完整执行
	 * 所有正在执行的任务以及队列中的所有剩余任务。相反，如果你通过此属性指定了等待终止的期限，此执行器
	 * 将最多等待给定的时间，以等待任务终止。
	 * <p>请注意，此特性对 {@link #setCommonPool "commonPool"} 模式同样有效。在这种情况下，底层
	 * ForkJoinPool 实际上不会终止，但会等待所有任务终止。
	 * @see java.util.concurrent.ForkJoinPool#shutdown()
	 * @see java.util.concurrent.ForkJoinPool#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	public void afterPropertiesSet() {
		this.forkJoinPool = (this.commonPool ? ForkJoinPool.commonPool() :
				new ForkJoinPool(this.parallelism, this.threadFactory, this.uncaughtExceptionHandler, this.asyncMode));
	}


	@Override
	@Nullable
	public ForkJoinPool getObject() {
		return this.forkJoinPool;
	}

	@Override
	public Class<?> getObjectType() {
		return ForkJoinPool.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.forkJoinPool != null) {
			// Ignored for the common pool.
			this.forkJoinPool.shutdown();

			// Wait for all tasks to terminate - works for the common pool as well.
			if (this.awaitTerminationSeconds > 0) {
				try {
					this.forkJoinPool.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
