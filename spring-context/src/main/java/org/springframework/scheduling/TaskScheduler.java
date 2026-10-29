/*
 * Copyright 2002-2020 the original author or authors.
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * 任务调度器接口，抽象了基于不同类型触发器对 {@link Runnable Runnables} 的调度。
 *
 * <p>此接口与 {@link SchedulingTaskExecutor} 分开，因为它通常代表另一种后端，即具有不同特征和能力
 * 的线程池。如果实现能够处理这两种执行特征，则可以实现这两个接口。
 *
 * <p>“默认”实现是 {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler}，它包
 * 装了原生 {@link java.util.concurrent.ScheduledExecutorService}，并添加了扩展的触发器能力。
 *
 * <p>此接口大致相当于 Java EE 7 环境中所支持的 JSR-236 {@code ManagedScheduledExecutorService}，但
 * 与 Spring 的 {@code TaskExecutor} 模型保持一致。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ScheduledExecutorService
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
 */
public interface TaskScheduler {

	/**
	 * 返回用于调度目的的时钟。
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	default Clock getClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * 调度给定的 {@link Runnable}，每当触发器指示下一个执行时间时就调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param trigger an implementation of the {@link Trigger} interface,
	 * e.g. a {@link org.springframework.scheduling.support.CronTrigger} object
	 * wrapping a cron expression
	 * @return a {@link ScheduledFuture} representing pending completion of the task,
	 * or {@code null} if the given Trigger object never fires (i.e. returns
	 * {@code null} from {@link Trigger#nextExecutionTime})
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @see org.springframework.scheduling.support.CronTrigger
	 */
	@Nullable
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #schedule(Runnable, Date)
	 */
	default ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return schedule(task, Date.from(startTime));
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	ScheduledFuture<?> schedule(Runnable task, Date startTime);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, Date, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return scheduleAtFixedRate(task, Date.from(startTime), period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

	/**
	 * 调度给定的 {@link Runnable}，尽快启动并按给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return scheduleAtFixedRate(task, period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快启动并按给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的一次执行完成到下一次执行开始之
	 * 间的延迟调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, Date, long)
	 */
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return scheduleWithFixedDelay(task, Date.from(startTime), delay.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的一次执行完成到下一次执行开始之
	 * 间的延迟调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

	/**
	 * 调度给定的 {@link Runnable}，尽快启动，并按给定的一次执行完成到下一次执行开始之间的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return scheduleWithFixedDelay(task, delay.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快启动，并按给定的一次执行完成到下一次执行开始之间的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
