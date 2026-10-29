/*
 * Copyright 2002-2021 the original author or authors.
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * 用于 cron 表达式的 {@link Trigger} 实现。包装一个 {@link CronExpression}。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 3.0
 * @see CronExpression
 */
public class CronTrigger implements Trigger {

	private final CronExpression expression;

	private final ZoneId zoneId;


	/**
	 * 使用默认时区中提供的模式构建一个 {@code CronTrigger}。
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 */
	public CronTrigger(String expression) {
		this(expression, ZoneId.systemDefault());
	}

	/**
	 * 使用给定时区中提供的模式构建一个 {@code CronTrigger}。
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param timeZone a time zone in which the trigger times will be generated
	 */
	public CronTrigger(String expression, TimeZone timeZone) {
		this(expression, timeZone.toZoneId());
	}

	/**
	 * 使用给定时区中提供的模式构建一个 {@code CronTrigger}。
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param zoneId a time zone in which the trigger times will be generated
	 * @since 5.3
	 * @see CronExpression#parse(String)
	 */
	public CronTrigger(String expression, ZoneId zoneId) {
		Assert.hasLength(expression, "Expression must not be empty");
		Assert.notNull(zoneId, "ZoneId must not be null");

		this.expression = CronExpression.parse(expression);
		this.zoneId = zoneId;
	}


	/**
	 * 返回此触发器所依据的 cron 模式。
	 */
	public String getExpression() {
		return this.expression.toString();
	}


	/**
	 * 根据给定的触发器上下文确定下一次执行时间。
	 * <p>下一次执行时间基于上一次执行的 {@linkplain TriggerContext#lastCompletionTime 完成时间} 计算；
	 * 因此不会发生重叠执行。
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date date = triggerContext.lastCompletionTime();
		if (date != null) {
			Date scheduled = triggerContext.lastScheduledExecutionTime();
			if (scheduled != null && date.before(scheduled)) {
				// Previous task apparently executed too early...
				// Let's simply use the last calculated execution time then,
				// in order to prevent accidental re-fires in the same second.
				date = scheduled;
			}
		}
		else {
			date = new Date(triggerContext.getClock().millis());
		}
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), this.zoneId);
		ZonedDateTime next = this.expression.next(dateTime);
		return (next != null ? Date.from(next.toInstant()) : null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof CronTrigger &&
				this.expression.equals(((CronTrigger) other).expression)));
	}

	@Override
	public int hashCode() {
		return this.expression.hashCode();
	}

	@Override
	public String toString() {
		return this.expression.toString();
	}

}
