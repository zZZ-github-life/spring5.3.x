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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 将方法标记为需要调度的注解。必须恰好指定 {@link #cron}、{@link #fixedDelay} 或
 * {@link #fixedRate} 属性之一。
 *
 * <p>带注解的方法必须不接受任何参数。它通常具有 {@code void} 返回类型；如果不是，则通过调度器调用
 * 时返回值将被忽略。
 *
 * <p>{@code @Scheduled} 注解的处理通过注册 {@link ScheduledAnnotationBeanPostProcessor} 来完成。这
 * 可以手动完成，或者更方便地通过 {@code <task:annotation-driven/>} XML 元素或
 * {@link EnableScheduling @EnableScheduling} 注解完成。
 *
 * <p>该注解可用作<em>元注解</em>，以创建带有属性覆盖的自定义<em>组合注解</em>。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @author Victor Brown
 * @author Sam Brannen
 * @since 3.0
 * @see EnableScheduling
 * @see ScheduledAnnotationBeanPostProcessor
 * @see Schedules
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Schedules.class)
public @interface Scheduled {

	/**
	 * 一个特殊的 cron 表达式值，表示禁用的触发器：{@value}。
	 * <p>它主要用于 <code>${...}</code> 占位符，允许从外部禁用相应的定时方法。
	 * @since 5.1
	 * @see ScheduledTaskRegistrar#CRON_DISABLED
	 */
	String CRON_DISABLED = ScheduledTaskRegistrar.CRON_DISABLED;


	/**
	 * 类似 cron 的表达式，它扩展了通常的 UN*X 定义，包含秒、分、时、月内日、月和星期几上的触发器。
	 * <p>例如，{@code "0 * * * * MON-FRI"} 表示工作日每分钟一次（在每分钟开始时，即第 0 秒）。
	 * <p>从左到右读取的字段解释如下。
	 * <ul>
	 * <li>秒</li>
	 * <li>分</li>
	 * <li>时</li>
	 * <li>月内日</li>
	 * <li>月</li>
	 * <li>星期几</li>
	 * </ul>
	 * <p>特殊值 {@link #CRON_DISABLED "-"} 表示禁用的 cron 触发器，主要用于通过 <code>${...}</code> 占
	 * 位符解析的外部指定值。
	 * @return an expression that can be parsed to a cron schedule
	 * @see org.springframework.scheduling.support.CronExpression#parse(String)
	 */
	String cron() default "";

	/**
	 * 用于解析 cron 表达式的时区。默认情况下，该属性为空字符串（即使用服务器的本地时区）。
	 * @return a zone id accepted by {@link java.util.TimeZone#getTimeZone(String)},
	 * or an empty String to indicate the server's default time zone
	 * @since 4.0
	 * @see org.springframework.scheduling.support.CronTrigger#CronTrigger(String, java.util.TimeZone)
	 * @see java.util.TimeZone
	 */
	String zone() default "";

	/**
	 * 在最后一次调用结束与下一次调用开始之间以固定周期执行带注解的方法。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the delay
	 */
	long fixedDelay() default -1;

	/**
	 * 在最后一次调用结束与下一次调用开始之间以固定周期执行带注解的方法。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the delay as a String value &mdash; for example, a placeholder
	 * or a {@link java.time.Duration#parse java.time.Duration} compliant value
	 * @since 3.2.2
	 */
	String fixedDelayString() default "";

	/**
	 * 以调用之间的固定周期执行带注解的方法。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the period
	 */
	long fixedRate() default -1;

	/**
	 * 以调用之间的固定周期执行带注解的方法。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the period as a String value &mdash; for example, a placeholder
	 * or a {@link java.time.Duration#parse java.time.Duration} compliant value
	 * @since 3.2.2
	 */
	String fixedRateString() default "";

	/**
	 * 在 {@link #fixedRate} 或 {@link #fixedDelay} 任务首次执行之前延迟的时间单位数。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the initial
	 * @since 3.2
	 */
	long initialDelay() default -1;

	/**
	 * 在 {@link #fixedRate} 或 {@link #fixedDelay} 任务首次执行之前延迟的时间单位数。
	 * <p>时间单位默认为毫秒，但可以通过 {@link #timeUnit} 覆盖。
	 * @return the initial delay as a String value &mdash; for example, a placeholder
	 * or a {@link java.time.Duration#parse java.time.Duration} compliant value
	 * @since 3.2.2
	 */
	String initialDelayString() default "";

	/**
	 * 用于 {@link #fixedDelay}、{@link #fixedDelayString}、{@link #fixedRate}、
	 * {@link #fixedRateString}、{@link #initialDelay} 和 {@link #initialDelayString} 的
	 * {@link TimeUnit}。
	 * <p>默认值为 {@link TimeUnit#MILLISECONDS}。
	 * <p>对于 {@linkplain #cron() cron 表达式}以及通过 {@link #fixedDelayString}、
	 * {@link #fixedRateString} 或 {@link #initialDelayString} 提供的 {@link java.time.Duration} 值，此
	 * 属性会被忽略。
	 * @return the {@code TimeUnit} to use
	 * @since 5.3.10
	 */
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
