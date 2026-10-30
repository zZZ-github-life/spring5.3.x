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

import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 可选接口，由标注了 {@link EnableScheduling @EnableScheduling} 的
 * {@link org.springframework.context.annotation.Configuration @Configuration} 类实现。通常用于设置
 * 执行定时任务时要使用的特定 {@link org.springframework.scheduling.TaskScheduler TaskScheduler}
 * Bean，或者以<em>编程式</em>方式注册定时任务，而不是使用 {@link Scheduled @Scheduled} 注解的<em>
 * 声明式</em>方式。例如，在实现基于 {@link org.springframework.scheduling.Trigger Trigger} 的任务
 * 时可能需要这样做，因为 {@code @Scheduled} 注解不支持此类任务。
 *
 * <p>详细用法示例请参见 {@link EnableScheduling @EnableScheduling}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableScheduling
 * @see ScheduledTaskRegistrar
 */
@FunctionalInterface
public interface SchedulingConfigurer {

	/**
	 * 回调，允许将 {@link org.springframework.scheduling.TaskScheduler TaskScheduler} 和特定的
	 * {@link org.springframework.scheduling.config.Task Task} 实例注册到给定的
	 * {@link ScheduledTaskRegistrar}。
	 * @param taskRegistrar 要配置的注册器。
	 */
	void configureTasks(ScheduledTaskRegistrar taskRegistrar);

}
