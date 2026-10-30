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
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param trigger {@link Trigger} 接口的实现，
	 * 例如包装 cron 表达式的
	 * {@link org.springframework.scheduling.support.CronTrigger} 对象
	 * @return 表示任务待完成的 {@link ScheduledFuture}，
	 * 如果给定的 Trigger 对象从不触发则为 {@code null}
	 * （即从 {@link Trigger#nextExecutionTime} 返回 {@code null}）
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 * @see org.springframework.scheduling.support.CronTrigger
	 */
	@Nullable
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 * @since 5.0
	 * @see #schedule(Runnable, Date)
	 */
	default ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return schedule(task, Date.from(startTime));
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 */
	ScheduledFuture<?> schedule(Runnable task, Date startTime);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的首次执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @param period 任务连续两次执行之间的间隔
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, Date, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return scheduleAtFixedRate(task, Date.from(startTime), period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的周期调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的首次执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @param period 任务连续两次执行之间的间隔（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

	/**
	 * 调度给定的 {@link Runnable}，尽快启动并按给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param period 任务连续两次执行之间的间隔
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return scheduleAtFixedRate(task, period.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快启动并按给定的周期调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param period 任务连续两次执行之间的间隔（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

	/**
	 * 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后按给定的一次执行完成到下一次执行开始之
	 * 间的延迟调用。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的首次执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @param delay 一次执行结束到下一次执行开始之间的延迟
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
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
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param startTime 任务期望的首次执行时间
	 * （如果该时间已过去，则任务将立即执行，即尽快执行）
	 * @param delay 一次执行结束到下一次执行开始之间的延迟（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

	/**
	 * 调度给定的 {@link Runnable}，尽快启动，并按给定的一次执行完成到下一次执行开始之间的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param delay 一次执行结束到下一次执行开始之间的延迟
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果由于内部原因（例如池过载处理策略或池正在关闭）
	 * 未接受给定的任务
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, long)
	 */
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return scheduleWithFixedDelay(task, delay.toMillis());
	}

	/**
	 * 调度给定的 {@link Runnable}，尽快启动，并按给定的一次执行完成到下一次执行开始之间的延迟调用它。
	 * <p>一旦调度器关闭或返回的 {@link ScheduledFuture} 被取消，执行就会结束。
	 * @param task 每次触发器触发时要执行的 Runnable
	 * @param delay 一次执行结束到下一次执行开始之间的延迟（以毫秒为单位）
	 * @return 表示任务待完成的 {@link ScheduledFuture}
	 * @throws org.springframework.core.task.TaskRejectedException 如果给定的任务因内部原因（例如线程池过载处理策略
	 * 或线程池正在关闭）而未被接受
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
