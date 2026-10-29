/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 描述已调度执行器任务的 JavaBean，由 {@link Runnable} 以及延迟和周期组成。周期需要明确指定；为它
 * 提供默认值没有意义。
 *
 * <p>{@link java.util.concurrent.ScheduledExecutorService} 不提供更复杂的调度选项，例如 cron 表达
 * 式。如有此类需求，可考虑使用 {@link ThreadPoolTaskScheduler}。
 *
 * <p>请注意，{@link java.util.concurrent.ScheduledExecutorService} 机制使用在重复执行之间共享的
 * {@link Runnable} 实例，这与 Quartz 不同，后者在每次执行时都会创建一个新的 Job 实例。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 */
public class ScheduledExecutorTask {

	@Nullable
	private Runnable runnable;

	private long delay = 0;

	private long period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;


	/**
	 * 创建一个新的 ScheduledExecutorTask，通过 Bean 属性填充。
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledExecutorTask() {
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask，默认为无延迟的一次性执行。
	 * @param executorTask the Runnable to schedule
	 */
	public ScheduledExecutorTask(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask，默认为带给定延迟的一次性执行。
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay) {
		this.runnable = executorTask;
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledExecutorTask。
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
		this.runnable = executorTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 设置要作为执行器任务调度的 Runnable。
	 */
	public void setRunnable(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * 返回要作为执行器任务调度的 Runnable。
	 */
	public Runnable getRunnable() {
		Assert.state(this.runnable != null, "No Runnable set");
		return this.runnable;
	}

	/**
	 * 设置任务首次启动前的延迟，单位为毫秒。默认值为 0，即在调度成功后立即启动任务。
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回作业首次启动前的延迟。
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置重复任务执行之间的周期，单位为毫秒。
	 * <p>默认值为 -1，表示一次性执行。如果为正值，任务将被重复执行，两次执行之间间隔给定的时间。
	 * <p>请注意，周期值的语义在固定速率执行和固定延迟执行之间有所不同。
	 * <p><b>Note:</b> <i>不</i>支持周期为 0（例如作为固定延迟），原因很简单：
	 * {@code java.util.concurrent.ScheduledExecutorService} 本身就不支持。因此，值 0 会被视为一次性执
	 * 行；不过，本来就不应显式指定该值！
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复任务执行之间的周期。
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * 此任务是否只会执行一次？
	 * @return {@code true} if this task is only ever going to execute once
	 * @see #getPeriod()
	 */
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * 指定延迟和周期值的时间单位。默认值为毫秒（{@code TimeUnit.MILLISECONDS}）。
	 * @see java.util.concurrent.TimeUnit#MILLISECONDS
	 * @see java.util.concurrent.TimeUnit#SECONDS
	 */
	public void setTimeUnit(@Nullable TimeUnit timeUnit) {
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
	}

	/**
	 * 返回延迟和周期值的时间单位。
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * 设置是否按固定速率执行调度，而不是固定延迟执行。默认值为 "false"，即固定延迟。
	 * <p>有关这些执行模式的详细信息，请参阅 ScheduledExecutorService 的 javadoc。
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否按固定速率执行调度。
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
