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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean}，用于设置一个
 * {@link java.util.concurrent.ScheduledExecutorService}（默认情况下是一个
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}），并将其暴露以供 Bean 引用。
 *
 * <p>允许注册 {@link ScheduledExecutorTask ScheduledExecutorTasks}，在初始化时自动启动
 * {@link ScheduledExecutorService}，并在上下文销毁时取消它。在只需在启动时静态注册任务的场景中，应
 * 用代码完全不需要访问 {@link ScheduledExecutorService} 实例本身；此时
 * {@code ScheduledExecutorFactoryBean} 仅用于生命周期集成。
 *
 * <p>作为替代方案，你可以使用构造器注入直接设置 {@link ScheduledThreadPoolExecutor} 实例，或使用指
 * 向 {@link java.util.concurrent.Executors} 类的工厂方法定义。<b>对于配置类中的常规 {@code @Bean}
 * 方法，尤其强烈推荐这样做，因为这种 {@code FactoryBean} 变体会迫使你返回 {@code FactoryBean} 类型，
 * 而不是 {@code ScheduledExecutorService}。</b>
 *
 * <p>请注意，{@link java.util.concurrent.ScheduledExecutorService} 使用在重复执行之间共享的
 * {@link Runnable} 实例，这与 Quartz 不同，后者在每次执行时都会实例化一个新的 Job。
 *
 * <p><b>WARNING:</b> 通过原生 {@link java.util.concurrent.ScheduledExecutorService} 提交的
 * {@link Runnable Runnables} 一旦抛出异常，就会从执行调度中移除。如果你希望在此类异常之后继续执行，
 * 请将此 FactoryBean 的
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
 * 属性切换为 "true"。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setThreadFactory
 * @see ScheduledExecutorTask
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
@SuppressWarnings("serial")
public class ScheduledExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ScheduledExecutorService> {

	private int poolSize = 1;

	@Nullable
	private ScheduledExecutorTask[] scheduledExecutorTasks;

	private boolean removeOnCancelPolicy = false;

	private boolean continueScheduledExecutionAfterException = false;

	private boolean exposeUnconfigurableExecutor = false;

	@Nullable
	private ScheduledExecutorService exposedExecutor;


	/**
	 * 设置 ScheduledExecutorService 的池大小。默认值为 1。
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
	}

	/**
	 * 向此 FactoryBean 创建的 ScheduledExecutorService 注册一组 ScheduledExecutorTask 对象。根据每个
	 * ScheduledExecutorTask 的设置，它将通过 ScheduledExecutorService 的某个 schedule 方法进行注册。
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setScheduledExecutorTasks(ScheduledExecutorTask... scheduledExecutorTasks) {
		this.scheduledExecutorTasks = scheduledExecutorTasks;
	}

	/**
	 * 设置 {@link ScheduledThreadPoolExecutor} 的取消时移除模式。
	 * <p>默认值为 {@code false}。如果设为 {@code true}，目标执行器将切换为取消时移除模式（如果可能，否
	 * 则会进行软回退）。
	 */
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
	}

	/**
	 * 指定在已调度任务抛出异常后是否继续执行该任务。
	 * <p>默认值为 "false"，与 {@link java.util.concurrent.ScheduledExecutorService} 的原生行为一致。将
	 * 此标志切换为 "true"，可使每个任务都能不受异常影响地执行，并像成功执行时那样继续进行调度执行。
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate
	 */
	public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
		this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
	}

	/**
	 * 指定此 FactoryBean 是否应为创建的执行器暴露一个不可配置的装饰器。
	 * <p>默认值为 "false"，即把原始执行器作为 Bean 引用暴露。将此标志切换为 "true" 可严格防止客户端修
	 * 改执行器的配置。
	 * @see java.util.concurrent.Executors#unconfigurableScheduledExecutorService
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		ScheduledExecutorService executor =
				createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (executor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.debug("Could not apply remove-on-cancel policy - not a ScheduledThreadPoolExecutor");
			}
		}

		// Register specified ScheduledExecutorTasks, if necessary.
		if (!ObjectUtils.isEmpty(this.scheduledExecutorTasks)) {
			registerTasks(this.scheduledExecutorTasks, executor);
		}

		// Wrap executor with an unconfigurable decorator.
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableScheduledExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * 创建一个新的 {@link ScheduledExecutorService} 实例。
	 * <p>默认实现创建一个 {@link ScheduledThreadPoolExecutor}。可在子类中重写，以提供自定义的
	 * {@link ScheduledExecutorService} 实例。
	 * @param poolSize the specified pool size
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ScheduledExecutorService instance
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 在给定的 {@link ScheduledExecutorService} 上注册指定的
	 * {@link ScheduledExecutorTask ScheduledExecutorTasks}。
	 * @param tasks the specified ScheduledExecutorTasks (never empty)
	 * @param executor the ScheduledExecutorService to register the tasks on.
	 */
	protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
		for (ScheduledExecutorTask task : tasks) {
			Runnable runnable = getRunnableToSchedule(task);
			if (task.isOneTimeTask()) {
				executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
			}
			else {
				if (task.isFixedRate()) {
					executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
				else {
					executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
			}
		}
	}

	/**
	 * 确定为给定任务要调度的实际 Runnable。
	 * <p>将任务的 Runnable 包装在
	 * {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable} 中，该包装会捕获
	 * 并记录 Exception。如有必要，它会根据
	 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
	 * 标志抑制该 Exception。
	 * @param task the ScheduledExecutorTask to schedule
	 * @return the actual Runnable to schedule (may be a decorator)
	 */
	protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
		return (this.continueScheduledExecutionAfterException ?
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER) :
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
	}


	@Override
	@Nullable
	public ScheduledExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ScheduledExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ScheduledExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
