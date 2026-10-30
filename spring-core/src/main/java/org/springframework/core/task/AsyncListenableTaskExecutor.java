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

package org.springframework.core.task;

import java.util.concurrent.Callable;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link AsyncTaskExecutor} 接口的扩展，增加了提交任务以获得
 * {@link ListenableFuture ListenableFutures} 的能力。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see ListenableFuture
 */
public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

	/**
	 * 提交一个 {@code Runnable} 任务以供执行，并返回代表该任务的 {@code ListenableFuture}。该 Future
	 * 在完成时将返回 {@code null} 结果。
	 * @param task 要执行的 {@code Runnable}（永不为 {@code null}）
	 * @return 表示任务待完成状态的 {@code ListenableFuture}
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	ListenableFuture<?> submitListenable(Runnable task);

	/**
	 * 提交一个 {@code Callable} 任务以供执行，并返回代表该任务的 {@code ListenableFuture}。该 Future
	 * 在完成时将返回 Callable 的结果。
	 * @param task 要执行的 {@code Callable}（永不为 {@code null}）
	 * @return 表示任务待完成状态的 {@code ListenableFuture}
	 * @throws TaskRejectedException 如果给定的任务未被接受
	 */
	<T> ListenableFuture<T> submitListenable(Callable<T> task);

}
