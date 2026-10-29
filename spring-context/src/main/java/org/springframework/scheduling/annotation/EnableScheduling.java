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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 启用 Spring 的定时任务执行能力，类似于 Spring 的 {@code <task:*>} XML 命名空间中的功能。按如下方
 * 式用于 {@link Configuration @Configuration} 类：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     // various &#064;Bean definitions
 * }</pre>
 *
 * <p>这会启用对容器中任何 Spring 托管 Bean 上 {@link Scheduled @Scheduled} 注解的检测。例如，给定
 * 一个类 {@code MyTask}：
 *
 * <pre class="code">
 * package com.myco.tasks;
 *
 * public class MyTask {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * <p>以下配置可确保每 1000 毫秒调用一次 {@code MyTask.work()}：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyTask task() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>或者，如果 {@code MyTask} 标注了 {@code @Component}，以下配置可确保其 {@code @Scheduled} 方法
 * 按所需间隔被调用：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * &#064;ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 * }</pre>
 *
 * <p>标注了 {@code @Scheduled} 的方法甚至可以直接声明在 {@code @Configuration} 类中：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * <p>默认情况下，Spring 会查找关联的调度器定义：上下文中的唯一
 * {@link org.springframework.scheduling.TaskScheduler} Bean，否则查找名为 "taskScheduler" 的
 * {@code TaskScheduler} Bean；对于 {@link java.util.concurrent.ScheduledExecutorService} Bean 也会
 * 执行相同的查找。如果两者都无法解析，将在注册器内创建并使用一个本地单线程的默认调度器。
 *
 * <p>如果需要更多控制，{@code @Configuration} 类可以实现 {@link SchedulingConfigurer}。这样可以访
 * 问底层的 {@link ScheduledTaskRegistrar} 实例。例如，以下示例演示如何自定义用于执行定时任务的
 * {@link Executor}：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskExecutor());
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskExecutor() {
 *         return Executors.newScheduledThreadPool(100);
 *     }
 * }</pre>
 *
 * <p>请注意上面示例中使用了 {@code @Bean(destroyMethod="shutdown")}。这可确保当 Spring 应用上下文
 * 本身关闭时，任务执行器被正确关闭。
 *
 * <p>实现 {@code SchedulingConfigurer} 还可以通过 {@code ScheduledTaskRegistrar} 对任务注册进行细
 * 粒度控制。例如，以下配置根据自定义 {@code Trigger} 实现来执行特定的 Bean 方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskScheduler());
 *         taskRegistrar.addTriggerTask(
 *             () -&gt; myTask().work(),
 *             new CustomTrigger()
 *         );
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskScheduler() {
 *         return Executors.newScheduledThreadPool(42);
 *     }
 *
 *     &#064;Bean
 *     public MyTask myTask() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven scheduler="taskScheduler"/&gt;
 *
 *     &lt;task:scheduler id="taskScheduler" pool-size="42"/&gt;
 *
 *     &lt;task:scheduled-tasks scheduler="taskScheduler"&gt;
 *         &lt;task:scheduled ref="myTask" method="work" fixed-rate="1000"/&gt;
 *     &lt;/task:scheduled-tasks&gt;
 *
 *     &lt;bean id="myTask" class="com.foo.MyTask"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>这些示例是等价的，只是在 XML 中使用了<em>固定速率</em>周期，而不是自定义的 <em>
 * {@code Trigger}</em> 实现；这是因为 {@code task:} 命名空间的 {@code scheduled} 不容易暴露此类支
 * 持。这只是一个示例，说明基于代码的方式如何通过直接访问实际组件来实现最大程度的可配置性。
 *
 * <p><b>注意：{@code @EnableScheduling} 仅适用于其本地应用上下文，允许在不同层级有选择地对 Bean 进
 * 行调度。</b>如果需要在多个层级应用其行为，请在每个单独上下文中重新声明 {@code @EnableScheduling}，
 * 例如公共的根 Web 应用上下文以及任何单独的 {@code DispatcherServlet} 应用上下文。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Scheduled
 * @see SchedulingConfiguration
 * @see SchedulingConfigurer
 * @see ScheduledTaskRegistrar
 * @see Trigger
 * @see ScheduledAnnotationBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
@Documented
public @interface EnableScheduling {

}
