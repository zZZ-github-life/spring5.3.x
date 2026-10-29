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

package org.springframework.scheduling.commonj;

import commonj.timers.TimerListener;

import org.springframework.lang.Nullable;

/**
 * JavaBean，用于描述一个定时执行的 TimerListener，由
 * TimerListener 本身（或用于创建 TimerListener 的 Runnable）
 * 以及延迟和周期组成。需要指定周期；
 * 为其设置默认值没有意义。
 *
 * <p>CommonJ TimerManager 不提供比 cron 表达式更高级的调度
 * 选项。如有此类高级需求，请考虑使用 Quartz。
 *
 * <p>请注意，TimerManager 使用的 TimerListener 实例在重复执行之间
 * 是共享的，这与 Quartz 不同——Quartz 在每次执行时
 * 都会实例化一个新的 Job。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.1, in favor of EE 7's
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public class ScheduledTimerListener {

	@Nullable
	private TimerListener timerListener;

	private long delay = 0;

	private long period = -1;

	private boolean fixedRate = false;


	/**
	 * 创建一个新的 ScheduledTimerListener，
	 * 通过 bean 属性进行填充。
	 * @see #setTimerListener
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	public ScheduledTimerListener() {
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，默认
	 * 无延迟的一次性执行。
	 * @param timerListener the TimerListener to schedule
	 */
	public ScheduledTimerListener(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，默认
	 * 带指定延迟的一次性执行。
	 * @param timerListener the TimerListener to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay) {
		this.timerListener = timerListener;
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener。
	 * @param timerListener the TimerListener to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerListener(TimerListener timerListener, long delay, long period, boolean fixedRate) {
		this.timerListener = timerListener;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，默认
	 * 无延迟的一次性执行。
	 * @param timerTask the Runnable to schedule as TimerListener
	 */
	public ScheduledTimerListener(Runnable timerTask) {
		setRunnable(timerTask);
	}

	/**
	 * 创建一个新的 ScheduledTimerListener，默认
	 * 带指定延迟的一次性执行。
	 * @param timerTask the Runnable to schedule as TimerListener
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay) {
		setRunnable(timerTask);
		this.delay = delay;
	}

	/**
	 * 创建一个新的 ScheduledTimerListener。
	 * @param timerTask the Runnable to schedule as TimerListener
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	public ScheduledTimerListener(Runnable timerTask, long delay, long period, boolean fixedRate) {
		setRunnable(timerTask);
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * 设置要作为 TimerListener 调度的 Runnable。
	 * @see DelegatingTimerListener
	 */
	public void setRunnable(Runnable timerTask) {
		this.timerListener = new DelegatingTimerListener(timerTask);
	}

	/**
	 * 设置要调度的 TimerListener。
	 */
	public void setTimerListener(@Nullable TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	/**
	 * 返回要调度的 TimerListener。
	 */
	@Nullable
	public TimerListener getTimerListener() {
		return this.timerListener;
	}

	/**
	 * 设置首次启动任务前的延迟时间，
	 * 以毫秒为单位。默认为 0，即在成功调度后
	 * 立即启动任务。
	 * <p>如果指定了 "firstTime" 属性，则此属性将被忽略。
	 * 请指定其中一个，不要同时指定两者。
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * 返回首次启动任务前的延迟时间。
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * 设置重复任务执行之间的间隔，以毫秒为单位。
	 * <p>默认为 -1，即一次性执行。如果值为零或
	 * 正值，任务将被重复执行，每次执行之间
	 * 间隔给定的时间。
	 * <p>请注意，周期值的语义在固定速率
	 * 和固定延迟执行之间有所不同。
	 * <p><b>注意：</b>支持周期值为 0（例如作为固定延迟），
	 * 因为 CommonJ 规范将此定义为合法值。
	 * 因此，值为 0 将导致在任务完成后立即重新执行
	 * （不会像 {@code java.util.Timer} 那样只执行一次）。
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 */
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * 返回重复任务执行之间的间隔。
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
		return (this.period < 0);
	}

	/**
	 * 设置是否以固定速率执行方式调度，而不是
	 * 固定延迟执行方式。默认为 "false"，即固定延迟。
	 * <p>有关这些执行模式的详细信息，请参阅 TimerManager 的 javadoc。
	 * @see commonj.timers.TimerManager#schedule(commonj.timers.TimerListener, long, long)
	 * @see commonj.timers.TimerManager#scheduleAtFixedRate(commonj.timers.TimerListener, long, long)
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * 返回是否以固定速率执行方式调度。
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
