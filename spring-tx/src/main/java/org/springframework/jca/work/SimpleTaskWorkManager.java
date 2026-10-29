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

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkAdapter;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 JCA 1.7 {@link javax.resource.spi.work.WorkManager} 实现，
 * 委托给 Spring {@link org.springframework.core.task.TaskExecutor}。
 * 提供简单的任务执行，包括启动超时，但不支持 JCA ExecutionContext
 * （即不支持导入的事务）。
 *
 * <p>默认使用 {@link org.springframework.core.task.SyncTaskExecutor} 处理
 * {@link #doWork} 调用，使用 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
 * 处理 {@link #startWork} 和 {@link #scheduleWork} 调用。
 * 可以通过配置覆盖这些默认的任务执行器。
 *
 * <p><b>注意：此 WorkManager 默认不提供线程池！</b>
 * 指定 {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
 * （或任何其他线程池 TaskExecutor）作为 "asyncTaskExecutor" 以实现实际的线程池。
 *
 * <p>此 WorkManager 自动检测指定的
 * {@link org.springframework.core.task.AsyncTaskExecutor} 实现，
 * 并在适当的地方使用其扩展的超时功能。在任何情况下都完全支持 JCA WorkListener。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setSyncTaskExecutor
 * @see #setAsyncTaskExecutor
 */
public class SimpleTaskWorkManager implements WorkManager {

	@Nullable
	private TaskExecutor syncTaskExecutor = new SyncTaskExecutor();

	@Nullable
	private AsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * 指定用于<i>同步</i>工作执行（即 {@link #doWork} 调用）的 TaskExecutor。
	 * <p>默认为 {@link org.springframework.core.task.SyncTaskExecutor}。
	 */
	public void setSyncTaskExecutor(TaskExecutor syncTaskExecutor) {
		this.syncTaskExecutor = syncTaskExecutor;
	}

	/**
	 * 指定用于<i>异步</i>工作执行（即 {@link #startWork} 和 {@link #scheduleWork} 调用）的 TaskExecutor。
	 * <p>这通常（但不一定）是 {@link org.springframework.core.task.AsyncTaskExecutor} 实现。
	 * 默认为 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}。
	 */
	public void setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
		this.asyncTaskExecutor = asyncTaskExecutor;
	}


	@Override
	public void doWork(Work work) throws WorkException {
		doWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void doWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.syncTaskExecutor != null, "No 'syncTaskExecutor' set");
		executeWork(this.syncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}

	@Override
	public long startWork(Work work) throws WorkException {
		return startWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public long startWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		return executeWork(this.asyncTaskExecutor, work, startTimeout, true, executionContext, workListener);
	}

	@Override
	public void scheduleWork(Work work) throws WorkException {
		scheduleWork(work, WorkManager.INDEFINITE, null, null);
	}

	@Override
	public void scheduleWork(Work work, long startTimeout, @Nullable ExecutionContext executionContext, @Nullable WorkListener workListener)
			throws WorkException {

		Assert.state(this.asyncTaskExecutor != null, "No 'asyncTaskExecutor' set");
		executeWork(this.asyncTaskExecutor, work, startTimeout, false, executionContext, workListener);
	}


	/**
	 * 在指定的 TaskExecutor 上执行给定的 Work。
	 * @param taskExecutor the TaskExecutor to use
	 * @param work the Work to execute
	 * @param startTimeout the time duration within which the Work is supposed to start
	 * @param blockUntilStarted whether to block until the Work has started
	 * @param executionContext the JCA ExecutionContext for the given Work
	 * @param workListener the WorkListener to clal for the given Work
	 * @return the time elapsed from Work acceptance until start of execution
	 * (or -1 if not applicable or not known)
	 * @throws WorkException if the TaskExecutor did not accept the Work
	 */
	@SuppressWarnings("deprecation")
	protected long executeWork(TaskExecutor taskExecutor, Work work, long startTimeout, boolean blockUntilStarted,
			@Nullable ExecutionContext executionContext, @Nullable WorkListener workListener) throws WorkException {

		if (executionContext != null && executionContext.getXid() != null) {
			throw new WorkException("SimpleTaskWorkManager does not supported imported XIDs: " + executionContext.getXid());
		}
		WorkListener workListenerToUse = workListener;
		if (workListenerToUse == null) {
			workListenerToUse = new WorkAdapter();
		}

		boolean isAsync = (taskExecutor instanceof AsyncTaskExecutor);
		DelegatingWorkAdapter workHandle = new DelegatingWorkAdapter(work, workListenerToUse, !isAsync);
		try {
			if (isAsync) {
				((AsyncTaskExecutor) taskExecutor).execute(workHandle, startTimeout);
			}
			else {
				taskExecutor.execute(workHandle);
			}
		}
		catch (org.springframework.core.task.TaskTimeoutException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work because of timeout: " + work, ex);
			wex.setErrorCode(WorkException.START_TIMED_OUT);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (TaskRejectedException ex) {
			WorkException wex = new WorkRejectedException("TaskExecutor rejected Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			workListenerToUse.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED, work, wex));
			throw wex;
		}
		catch (Throwable ex) {
			WorkException wex = new WorkException("TaskExecutor failed to execute Work: " + work, ex);
			wex.setErrorCode(WorkException.INTERNAL);
			throw wex;
		}
		if (isAsync) {
			workListenerToUse.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, work, null));
		}

		if (blockUntilStarted) {
			long acceptanceTime = System.currentTimeMillis();
			synchronized (workHandle.monitor) {
				try {
					while (!workHandle.started) {
						workHandle.monitor.wait();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			return (System.currentTimeMillis() - acceptanceTime);
		}
		else {
			return WorkManager.UNKNOWN;
		}
	}


	/**
	 * Work 适配器，支持给定 Work 的启动超时和 WorkListener 回调。
	 */
	private static class DelegatingWorkAdapter implements Work {

		private final Work work;

		private final WorkListener workListener;

		private final boolean acceptOnExecution;

		public final Object monitor = new Object();

		public boolean started = false;

		public DelegatingWorkAdapter(Work work, WorkListener workListener, boolean acceptOnExecution) {
			this.work = work;
			this.workListener = workListener;
			this.acceptOnExecution = acceptOnExecution;
		}

		@Override
		public void run() {
			if (this.acceptOnExecution) {
				this.workListener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED, this.work, null));
			}
			synchronized (this.monitor) {
				this.started = true;
				this.monitor.notify();
			}
			this.workListener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED, this.work, null));
			try {
				this.work.run();
			}
			catch (RuntimeException | Error ex) {
				this.workListener.workCompleted(
						new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, new WorkCompletedException(ex)));
				throw ex;
			}
			this.workListener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED, this.work, null));
		}

		@Override
		public void release() {
			this.work.release();
		}
	}

}
