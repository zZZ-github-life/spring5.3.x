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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 用于异步 {@link TaskExecutor} 实现的扩展接口，提供对 {@link java.util.concurrent.Callable} 的支
 * 持。
 *
 * <p>注意：{@link java.util.concurrent.Executors} 类提供了一组方法，可以在执行之前将其他一些常见的
 * 类似闭包的对象（例如 {@link java.security.PrivilegedAction}）转换为 {@link Callable}。
 *
 * <p>实现此接口还表明，{@link #execute(Runnable)} 方法不会在调用方线程中执行其 Runnable，而是会在
 * 其他某个线程中异步执行。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.Executors
 */
public interface AsyncTaskExecutor extends TaskExecutor {

	/**
	 * 表示立即执行的常量。
	 * @deprecated as of 5.3.16 along with {@link #execute(Runnable, long)}
	 */
	@Deprecated
	long TIMEOUT_IMMEDIATE = 0;

	/**
	 * 表示没有时间限制的常量。
	 * @deprecated as of 5.3.16 along with {@link #execute(Runnable, long)}
	 */
	@Deprecated
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * 执行给定的 {@code task}。
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @param startTimeout the time duration (milliseconds) within which the task is
	 * supposed to start. This is intended as a hint to the executor, allowing for
	 * preferred handling of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE}
	 * or {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
	 * @throws TaskTimeoutException in case of the task being rejected because
	 * of the timeout (i.e. it cannot be started in time)
	 * @throws TaskRejectedException if the given task was not accepted
	 * @see #execute(Runnable)
	 * @deprecated as of 5.3.16 since the common executors do not support start timeouts
	 */
	@Deprecated
	void execute(Runnable task, long startTimeout);

	/**
	 * 提交一个 Runnable 任务以供执行，并返回代表该任务的 Future。该 Future 在完成时将返回 {@code null}
	 * 结果。
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	Future<?> submit(Runnable task);

	/**
	 * 提交一个 Callable 任务以供执行，并返回代表该任务的 Future。该 Future 在完成时将返回 Callable 的
	 * 结果。
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	<T> Future<T> submit(Callable<T> task);

}
