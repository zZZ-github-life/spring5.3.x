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

package org.springframework.jca.work;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.naming.NamingException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jca.context.BootstrapContextAware;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * {@link org.springframework.core.task.TaskExecutor} 实现，
 * 委托给 JCA 1.7 WorkManager，实现 {@link javax.resource.spi.work.WorkManager} 接口。
 *
 * <p>主要用于 JCA ResourceAdapter 实现内部，但也可在独立环境中使用，
 * 委托给本地嵌入的 WorkManager 实现（如 Geronimo 的实现）。
 *
 * <p>还实现了 JCA 1.7 WorkManager 接口本身，将所有调用委托给目标 WorkManager。
 * 因此，调用者可以选择是通过 Spring TaskExecutor 接口还是 WorkManager 接口与此执行器通信。
 *
 * <p>此适配器还能够从 JNDI 获取 JCA WorkManager。
 * 例如，这在 Geronimo 应用服务器上是合适的，其中 WorkManager GBean
 * （例如 Geronimo 的默认 "DefaultWorkManager" GBean）
 * 可以通过 {@code geronimo-web.xml} 部署描述符中的 "gbean-ref" 条目
 * 链接到 Java EE 环境中。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setWorkManager
 * @see javax.resource.spi.work.WorkManager#scheduleWork
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor, WorkManager, BootstrapContextAware, InitializingBean {

	@Nullable
	private WorkManager workManager;

	@Nullable
	private String workManagerName;

	private boolean blockUntilStarted = false;

	private boolean blockUntilCompleted = false;

	@Nullable
	private WorkListener workListener;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * 创建新的 WorkManagerTaskExecutor，期望 bean 风格配置。
	 * @see #setWorkManager
	 */
	public WorkManagerTaskExecutor() {
	}

	/**
	 * 为给定的 WorkManager 创建新的 WorkManagerTaskExecutor。
	 * @param workManager 要委托给的 JCA WorkManager
	 */
	public WorkManagerTaskExecutor(WorkManager workManager) {
		setWorkManager(workManager);
	}


	/**
	 * 指定要委托的 JCA WorkManager 实例。
	 */
	public void setWorkManager(WorkManager workManager) {
		Assert.notNull(workManager, "WorkManager must not be null");
		this.workManager = workManager;
	}

	/**
	 * 设置 JCA WorkManager 的 JNDI 名称。
	 * <p>这可以是完全限定的 JNDI 名称，也可以是当前环境命名上下文的相对 JNDI 名称
	 * （如果 "resourceRef" 设置为 "true"）。
	 * @see #setWorkManager
	 * @see #setResourceRef
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * 指定包含要委托的 WorkManager 的 JCA BootstrapContext。
	 */
	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.workManager = bootstrapContext.getWorkManager();
	}

	/**
	 * 设置是否让 {@link #execute} 阻塞直到工作实际启动。
	 * <p>底层使用 JCA {@code startWork} 操作，而不是默认的 {@code scheduleWork}。
	 * @see javax.resource.spi.work.WorkManager#startWork
	 * @see javax.resource.spi.work.WorkManager#scheduleWork
	 */
	public void setBlockUntilStarted(boolean blockUntilStarted) {
		this.blockUntilStarted = blockUntilStarted;
	}

	/**
	 * 设置是否让 {@link #execute} 阻塞直到工作完成。
	 * <p>底层使用 JCA {@code doWork} 操作，而不是默认的 {@code scheduleWork}。
	 * @see javax.resource.spi.work.WorkManager#doWork
	 * @see javax.resource.spi.work.WorkManager#scheduleWork
	 */
	public void setBlockUntilCompleted(boolean blockUntilCompleted) {
		this.blockUntilCompleted = blockUntilCompleted;
	}

	/**
	 * 指定要应用的 JCA WorkListener（如果有）。
	 * <p>此共享的 WorkListener 实例将被此 TaskExecutor 的所有 {@link #execute} 调用
	 * 传递给 WorkManager。
	 */
	public void setWorkListener(@Nullable WorkListener workListener) {
		this.workListener = workListener;
	}

	/**
	 * 指定要应用于即将执行的 {@link Runnable} 的自定义 {@link TaskDecorator}。
	 * <p>注意，此类装饰器不一定应用于用户提供的 {@code Runnable}/{@code Callable}，
	 * 而是应用于实际的执行回调（可能是用户提供的任务的包装器）。
	 * <p>主要用例是在任务调用周围设置一些执行上下文，或为任务执行提供监控/统计信息。
	 * <p><b>注意：</b> {@code TaskDecorator} 实现中的异常处理仅限于通过 {@code execute} 调用
	 * 执行普通的 {@code Runnable}。在 {@code #submit} 调用的情况下，暴露的 {@code Runnable}
	 * 将是一个 {@code FutureTask}，它不传播任何异常；你可能需要将其强制转换并调用
	 * {@code Future#get} 来评估异常。
	 * @since 4.3
	 */
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName != null) {
				this.workManager = lookup(this.workManagerName, WorkManager.class);
			}
			else {
				this.workManager = getDefaultWorkManager();
			}
		}
	}

	/**
	 * 获取默认的 WorkManager 进行委托。
	 * 在未指定显式的 WorkManager 或 WorkManager JNDI 名称时调用。
	 * <p>默认实现返回 {@link SimpleTaskWorkManager}。可以在子类中重写。
	 */
	protected WorkManager getDefaultWorkManager() {
		return new SimpleTaskWorkManager();
	}

	private WorkManager obtainWorkManager() {
		Assert.state(this.workManager != null, "No WorkManager specified");
		return this.workManager;
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		Work work = new DelegatingWork(this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		try {
			if (this.blockUntilCompleted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().doWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().doWork(work);
				}
			}
			else if (this.blockUntilStarted) {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().startWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().startWork(work);
				}
			}
			else {
				if (startTimeout != TIMEOUT_INDEFINITE || this.workListener != null) {
					obtainWorkManager().scheduleWork(work, startTimeout, null, this.workListener);
				}
				else {
					obtainWorkManager().scheduleWork(work);
				}
			}
		}
		catch (WorkRejectedException ex) {
			if (WorkException.START_TIMED_OUT.equals(ex.getErrorCode())) {
				throw new org.springframework.core.task.TaskTimeoutException(
						"JCA WorkManager rejected task because of timeout: " + task, ex);
			}
			else {
				throw new TaskRejectedException("JCA WorkManager rejected task: " + task, ex);
			}
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on JCA WorkManager", ex);
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


	//-------------------------------------------------------------------------
	// Implementation of the JCA WorkManager interface
	//-------------------------------------------------------------------------

	@Override
	public void doWork(Work work) throws WorkException {
		obtainWorkManager().doWork(work);
	}

	@Override
	public void doWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		obtainWorkManager().doWork(work, delay, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return obtainWorkManager().startWork(work);
	}

	@Override
	public long startWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		return obtainWorkManager().startWork(work, delay, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		obtainWorkManager().scheduleWork(work);
	}

	@Override
	public void scheduleWork(Work work, long delay, ExecutionContext executionContext, WorkListener workListener)
			throws WorkException {

		obtainWorkManager().scheduleWork(work, delay, executionContext, workListener);
	}

}
