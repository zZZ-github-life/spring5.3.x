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

package org.springframework.scheduling;

import org.springframework.core.task.AsyncTaskExecutor;

/**
 * {@link org.springframework.core.task.TaskExecutor} 的扩展，暴露与潜在任务提交者相关的调度特征。
 *
 * <p>鼓励调度客户端提交与所用 {@code TaskExecutor} 实现所暴露的偏好相匹配的
 * {@link Runnable Runnables}。
 *
 * <p>注意：鼓励 {@link SchedulingTaskExecutor} 实现同时实现
 * {@link org.springframework.core.task.AsyncListenableTaskExecutor} 接口。但这不是必需的，因为它依
 * 赖于 Spring 4.0 新增的 {@link org.springframework.util.concurrent.ListenableFuture} 接口，这会使
 * 得第三方执行器实现无法同时兼容 Spring 4.0 和 Spring 3.x。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulingAwareRunnable
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

	/**
	 * 此 {@code TaskExecutor} 是否更偏好短期任务而非长期任务？
	 * <p>{@code SchedulingTaskExecutor} 实现可以表明它是否希望提交的任务在单次任务执行中尽可能少地完成
	 * 工作。例如，提交的任务可以将重复循环拆分为各个子任务，这些子任务随后（如果可行）再提交一个后续任
	 * 务。
	 * <p>这应被视为一种提示。当然，{@code TaskExecutor} 客户端可以自由地忽略此标志，从而也忽略整个
	 * {@code SchedulingTaskExecutor} 接口。不过，线程池通常会表示偏好短期任务，从而支持更细粒度的调度。
	 * @return {@code true} if this executor prefers short-lived tasks (the default),
	 * {@code false} otherwise (for treatment like a regular {@code TaskExecutor})
	 */
	default boolean prefersShortLivedTasks() {
		return true;
	}

}
