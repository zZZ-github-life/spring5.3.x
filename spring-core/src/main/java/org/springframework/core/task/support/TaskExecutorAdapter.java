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

package org.springframework.core.task.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 接收 JDK {@code java.util.concurrent.Executor} 并为其暴露 Spring
 * {@link org.springframework.core.task.TaskExecutor} 的适配器。还会检测扩展的
 * {@code java.util.concurrent.ExecutorService}，并相应地适配
 * {@link org.springframework.core.task.AsyncTaskExecutor} 接口。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 */
public class TaskExecutorAdapter implements AsyncListenableTaskExecutor {

	private final Executor concurrentExecutor;

	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * 使用给定的 JDK 并发执行器创建一个新的 TaskExecutorAdapter。
	 * @param concurrentExecutor the JDK concurrent executor to delegate to
	 */
	public TaskExecutorAdapter(Executor concurrentExecutor) {
		Assert.notNull(concurrentExecutor, "Executor must not be null");
		this.concurrentExecutor = concurrentExecutor;
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
	 * 委托给指定的 JDK 并发执行器。
	 * @see java.util.concurrent.Executor#execute(Runnable)
	 */
	@Override
	public void execute(Runnable task) {
		try {
			doExecute(this.concurrentExecutor, this.taskDecorator, task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Deprecated
	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		try {
			if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
				return ((ExecutorService) this.concurrentExecutor).submit(task);
			}
			else {
				FutureTask<Object> future = new FutureTask<>(task, null);
				doExecute(this.concurrentExecutor, this.taskDecorator, future);
				return future;
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		try {
			if (this.taskDecorator == null && this.concurrentExecutor instanceof ExecutorService) {
				return ((ExecutorService) this.concurrentExecutor).submit(task);
			}
			else {
				FutureTask<T> future = new FutureTask<>(task);
				doExecute(this.concurrentExecutor, this.taskDecorator, future);
				return future;
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
			doExecute(this.concurrentExecutor, this.taskDecorator, future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
			doExecute(this.concurrentExecutor, this.taskDecorator, future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException(
					"Executor [" + this.concurrentExecutor + "] did not accept task: " + task, ex);
		}
	}


	/**
	 * 使用给定的执行器实际执行给定的 {@code Runnable}（它可能是用户提供的任务，也可能是对用户提供任务
	 * 的包装）。
	 * @param concurrentExecutor the underlying JDK concurrent executor to delegate to
	 * @param taskDecorator the specified decorator to be applied, if any
	 * @param runnable the runnable to execute
	 * @throws RejectedExecutionException if the given runnable cannot be accepted
	 * @since 4.3
	 */
	protected void doExecute(Executor concurrentExecutor, @Nullable TaskDecorator taskDecorator, Runnable runnable)
			throws RejectedExecutionException{

		concurrentExecutor.execute(taskDecorator != null ? taskDecorator.decorate(runnable) : runnable);
	}

}
