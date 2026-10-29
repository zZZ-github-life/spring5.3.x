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

package org.springframework.scheduling.support;

import java.time.Clock;
import java.util.Date;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TriggerContext;

/**
 * 实现 {@link TriggerContext} 接口的简单数据持有者。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SimpleTriggerContext implements TriggerContext {

	private final Clock clock;

	@Nullable
	private volatile Date lastScheduledExecutionTime;

	@Nullable
	private volatile Date lastActualExecutionTime;

	@Nullable
	private volatile Date lastCompletionTime;


	/**
	 * 创建一个将全部时间值设为 {@code null} 的 SimpleTriggerContext，并暴露默认时区的系统时钟。
	 */
	public SimpleTriggerContext() {
		this.clock = Clock.systemDefaultZone();
	}

	/**
	 * 使用给定的时间值创建一个 SimpleTriggerContext，并暴露默认时区的系统时钟。
	 * @param lastScheduledExecutionTime last <i>scheduled</i> execution time
	 * @param lastActualExecutionTime last <i>actual</i> execution time
	 * @param lastCompletionTime last completion time
	 */
	public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this();
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}

	/**
	 * 创建一个将全部时间值设为 {@code null} 的 SimpleTriggerContext，并暴露给定的时钟。
	 * @param clock the clock to use for trigger calculation
	 * @since 5.3
	 * @see #update(Date, Date, Date)
	 */
	public SimpleTriggerContext(Clock clock) {
		this.clock = clock;
	}


	/**
	 * 用最新的时间值更新此持有者的状态。
 	 * @param lastScheduledExecutionTime last <i>scheduled</i> execution time
	 * @param lastActualExecutionTime last <i>actual</i> execution time
	 * @param lastCompletionTime last completion time
	 */
	public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
		this.lastScheduledExecutionTime = lastScheduledExecutionTime;
		this.lastActualExecutionTime = lastActualExecutionTime;
		this.lastCompletionTime = lastCompletionTime;
	}


	@Override
	public Clock getClock() {
		return this.clock;
	}

	@Override
	@Nullable
	public Date lastScheduledExecutionTime() {
		return this.lastScheduledExecutionTime;
	}

	@Override
	@Nullable
	public Date lastActualExecutionTime() {
		return this.lastActualExecutionTime;
	}

	@Override
	@Nullable
	public Date lastCompletionTime() {
		return this.lastCompletionTime;
	}

}
