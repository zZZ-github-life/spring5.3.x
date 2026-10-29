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

package org.springframework.scheduling.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 允许以 Bean 风格（通过其 "corePoolSize"、"maxPoolSize"、"keepAliveSeconds"、"queueCapacity" 属性）
 * 配置 {@link java.util.concurrent.ThreadPoolExecutor}，并将其作为 Spring
 * {@link org.springframework.core.task.TaskExecutor} 暴露的 JavaBean。此类也非常适合管理与监控（例
 * 如通过 JMX），提供若干有用的属性："corePoolSize"、"maxPoolSize"、"keepAliveSeconds"（均支持运行
 * 时更新）；"poolSize"、"activeCount"（仅用于内省）。
 *
 * <p>默认配置是核心池大小为 1，最大池大小不限，队列容量不限。这大致相当于
 * {@link java.util.concurrent.Executors#newSingleThreadExecutor()}，所有任务共享单个线程。将
 * {@link #setQueueCapacity "queueCapacity"} 设为 0 可模拟
 * {@link java.util.concurrent.Executors#newCachedThreadPool()}，池中的线程会立即扩展至可能非常多的
 * 数量。此时还应考虑设置 {@link #setMaxPoolSize "maxPoolSize"}，也可能需要设置更高的
 * {@link #setCorePoolSize "corePoolSize"}（另请参阅
 * {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} 扩展模式）。
 *
 * <p><b>NOTE:</b> 此类实现 Spring 的 {@link org.springframework.core.task.TaskExecutor} 接口以及
 * {@link java.util.concurrent.Executor} 接口，其中前者是主要接口，后者仅作为次要的便利接口。因此，
 * 异常处理遵循 TaskExecutor 约定而非 Executor 约定，特别是在
 * {@link org.springframework.core.task.TaskRejectedException} 方面。
 *
 * <p>作为替代方案，你可以使用构造器注入直接设置 ThreadPoolExecutor 实例，或使用指向
 * {@link java.util.concurrent.Executors} 类的工厂方法定义。要将这样的原始 Executor 暴露为 Spring
 * {@link org.springframework.core.task.TaskExecutor}，只需用
 * {@link org.springframework.scheduling.concurrent.ConcurrentTaskExecutor} 适配器将其包装即可。
 *
 * @author Juergen Hoeller
 * @author Rémy Guihard
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see ThreadPoolExecutorFactoryBean
 * @see ConcurrentTaskExecutor
 */
@SuppressWarnings("serial")
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	private final Object poolSizeMonitor = new Object();

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	@Nullable
	private TaskDecorator taskDecorator;

	@Nullable
	private ThreadPoolExecutor threadPoolExecutor;

	// Runnable decorator to user-level FutureTask, if different
	private final Map<Runnable, Object> decoratedTaskMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * 设置 ThreadPoolExecutor 的核心池大小。默认值为 1。
	 * <p><b>此设置可在运行时修改，例如通过 JMX。</b>
	 */
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
			this.corePoolSize = corePoolSize;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的核心池大小。
	 */
	public int getCorePoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.corePoolSize;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的最大池大小。默认值为 {@code Integer.MAX_VALUE}。
	 * <p><b>此设置可在运行时修改，例如通过 JMX。</b>
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
			this.maxPoolSize = maxPoolSize;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的最大池大小。
	 */
	public int getMaxPoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.maxPoolSize;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的保持存活秒数。
	 * <p>默认值为 60。
	 * <p><b>此设置可在运行时修改，例如通过 JMX。</b>
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
			this.keepAliveSeconds = keepAliveSeconds;
		}
	}

	/**
	 * 返回 ThreadPoolExecutor 的保持存活秒数。
	 */
	public int getKeepAliveSeconds() {
		synchronized (this.poolSizeMonitor) {
			return this.keepAliveSeconds;
		}
	}

	/**
	 * 设置 ThreadPoolExecutor 的 BlockingQueue 的容量。
	 * <p>默认值为 {@code Integer.MAX_VALUE}。
	 * <p>任何正值都会使用 LinkedBlockingQueue 实例；任何其他值都会使用 SynchronousQueue 实例。
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * 返回 ThreadPoolExecutor 的 BlockingQueue 的容量。
	 * @since 5.3.21
	 * @see #setQueueCapacity(int)
	 */
	public int getQueueCapacity() {
		return this.queueCapacity;
	}

	/**
	 * 指定是否允许核心线程超时。这即使在配合非零队列时也能实现动态增长和收缩（因为只有在队列满时最大池
	 * 大小才会增长）。
	 * <p>默认值为 "false"。
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * 指定是否启动所有核心线程，使它们空闲等待工作。
	 * <p>默认值为 "false"。
	 * @since 5.3.14
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads
	 */
	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	/**
	 * 指定一个自定义 {@link TaskDecorator}，应用于任何即将执行的 {@link Runnable}。
	 * <p>请注意，此类装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，而是应用于实际执
	 * 行回调（它可能是对用户所提供任务的包装）。
	 * <p>主要用例是在任务调用周围设置某种执行上下文，或为任务执行提供监控/统计信息。
	 * <p><b>NOTE:</b> {@code TaskDecorator} 实现中的异常处理仅限于通过 {@code execute} 调用执行普通
	 * {@code Runnable} 的情况。对于 {@code #submit} 调用，暴露的 {@code Runnable} 将是一个不会传播任何
	 * 异常的 {@code FutureTask}；你可能需要对其进行强制类型转换并调用 {@code Future#get} 来评估异常。
	 * 有关在此类 {@code Future} 情况下如何访问异常的示例，请参阅
	 * {@code ThreadPoolExecutor#afterExecute} 的 javadoc。
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}


	/**
	 * 注意：此方法向其基类暴露一个 {@link ExecutorService}，但在内部存储实际的
	 * {@link ThreadPoolExecutor} 句柄。不要为了替换执行器而重写此方法，只应为了装饰其
	 * {@code ExecutorService} 句柄或存储自定义状态而重写。
	 */
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		ThreadPoolExecutor executor;
		if (this.taskDecorator != null) {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler) {
				@Override
				public void execute(Runnable command) {
					Runnable decorated = taskDecorator.decorate(command);
					if (decorated != command) {
						decoratedTaskMap.put(decorated, command);
					}
					super.execute(decorated);
				}
			};
		}
		else {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler);

		}

		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}
		if (this.prestartAllCoreThreads) {
			executor.prestartAllCoreThreads();
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	/**
	 * 创建供 ThreadPoolExecutor 使用的 BlockingQueue。
	 * <p>容量值为正时会创建 LinkedBlockingQueue 实例；否则创建 SynchronousQueue。
	 * @param queueCapacity the specified queue capacity
	 * @return the BlockingQueue instance
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<>(queueCapacity);
		}
		else {
			return new SynchronousQueue<>();
		}
	}

	/**
	 * 返回底层的 ThreadPoolExecutor，以便进行原生访问。
	 * @return the underlying ThreadPoolExecutor (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * 返回当前池大小。
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	public int getPoolSize() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume core pool size.
			return this.corePoolSize;
		}
		return this.threadPoolExecutor.getPoolSize();
	}

	/**
	 * 返回当前队列大小。
	 * @since 5.3.21
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public int getQueueSize() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume no queued tasks.
			return 0;
		}
		return this.threadPoolExecutor.getQueue().size();
	}

	/**
	 * 返回当前活动线程数。
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume no active threads.
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}


	@Override
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// Cancel associated user-level Future handle as well
		Object original = this.decoratedTaskMap.get(task);
		if (original instanceof Future) {
			((Future<?>) original).cancel(true);
		}
	}

}
