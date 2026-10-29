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

package org.springframework.core.task;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 为每个任务启动一个新 Thread 并异步执行该任务的 {@link TaskExecutor} 实现。
 *
 * <p>支持通过“concurrencyLimit”Bean 属性限制并发线程数。默认情况下，并发线程数不受限制。
 *
 * <p><b>注意：此实现不会复用线程！</b>请考虑改用线程池化的 TaskExecutor 实现，特别是在执行大量短生
 * 命周期任务时。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConcurrencyLimit
 * @see SyncTaskExecutor
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
		implements AsyncListenableTaskExecutor, Serializable {

	/**
	 * 允许任意数量的并发调用：也就是说，不限制并发。
	 * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
	 */
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * 关闭并发：也就是说，不允许任何并发调用。
	 * @see ConcurrencyThrottleSupport#NO_CONCURRENCY
	 */
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/** 此执行器使用的内部并发节流器。 */
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

	@Nullable
	private ThreadFactory threadFactory;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * 使用默认线程名前缀创建一个新的 SimpleAsyncTaskExecutor。
	 */
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * 使用给定的线程名前缀创建一个新的 SimpleAsyncTaskExecutor。
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	/**
	 * 使用给定的外部线程工厂创建一个新的 SimpleAsyncTaskExecutor。
	 * @param threadFactory the factory to use for creating new Threads
	 */
	public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}


	/**
	 * 指定用于创建新 Thread 的外部工厂，而不是依赖此执行器的本地属性。
	 *
	 * <p>你可以指定一个内部的 ThreadFactory Bean，也可以指定从 JNDI（在 Java EE 6 服务器上）或其他某种
	 * 查找机制获得的 ThreadFactory 引用。
	 * @see #setThreadNamePrefix
	 * @see #setThreadPriority
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * 返回用于创建新 Thread 的外部工厂（如果有）。
	 */
	@Nullable
	public final ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}

	/**
	 * 指定一个自定义 {@link TaskDecorator}，将其应用于任何即将执行的 {@link Runnable}。
	 *
	 * <p>请注意，此类装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，而是应用于实际执
	 * 行回调（该回调可能是对用户提供任务的包装）。
	 *
	 * <p>主要用例是在任务调用前后设置某些执行上下文，或者为任务执行提供某些监控/统计信息。
	 *
	 * <p><b>注意：</b>{@code TaskDecorator} 实现中的异常处理仅限于通过 {@code execute} 调用执行的普通
	 * {@code Runnable}。对于 {@code #submit} 调用，暴露的 {@code Runnable} 将是一个 {@code FutureTask}，
	 * 它不会传播任何异常；你可能需要对其进行强制类型转换并调用 {@code Future#get} 来获取异常。
	 * @since 4.3
	 */
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * 设置允许的最大并行访问数。-1 表示完全不限制并发。
	 * <p>原则上，此限制可以在运行时更改，尽管它通常被设计为配置时设置。注意：不要在运行时在 -1 与任何
	 * 具体限制之间切换，因为这会导致并发计数不一致：-1 的限制实际上会完全关闭并发计数。
	 * @see #UNBOUNDED_CONCURRENCY
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * 返回允许的最大并行访问数。
	 */
	public final int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * 返回当前是否启用了此节流。
	 * @return {@code true} if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 * @see #setConcurrencyLimit
	 */
	public final boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * 在并发节流限制内（如果通过超类的设置进行了配置）执行给定任务。
	 * @see #doExecute(Runnable)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * 在并发节流限制内（如果通过超类的设置进行了配置）执行给定任务。
	 * <p>直接执行紧急任务（超时为“立即”），绕过并发节流（如果处于活动状态）。所有其他任务都受节流限制。
	 *
	 * @see #TIMEOUT_IMMEDIATE
	 * @see #doExecute(Runnable)
	 */
	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new ConcurrencyThrottlingRunnable(taskToUse));
		}
		else {
			doExecute(taskToUse);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * 用于实际执行任务的模板方法。
	 * <p>默认实现会创建一个新 Thread 并启动它。
	 * @param task the Runnable to execute
	 * @see #setThreadFactory
	 * @see #createThread
	 * @see java.lang.Thread#start()
	 */
	protected void doExecute(Runnable task) {
		Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		thread.start();
	}


	/**
	 * 通用 ConcurrencyThrottleSupport 类的子类，使 {@code beforeAccess()} 和 {@code afterAccess()} 对
	 * 周围类可见。
	 */
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		@Override
		protected void beforeAccess() {
			super.beforeAccess();
		}

		@Override
		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * 此 Runnable 在目标 Runnable 完成执行后调用 {@code afterAccess()}。
	 */
	private class ConcurrencyThrottlingRunnable implements Runnable {

		private final Runnable target;

		public ConcurrencyThrottlingRunnable(Runnable target) {
			this.target = target;
		}

		@Override
		public void run() {
			try {
				this.target.run();
			}
			finally {
				concurrencyThrottle.afterAccess();
			}
		}
	}

}
