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

package org.springframework.context;

/**
 * 定义启动/停止生命周期控制方法的通用接口。其典型用例是控制异步处理。<b>注意：此接口并不蕴含特定的
 * 自动启动语义。若有此需要，请考虑实现 {@link SmartLifecycle}。</b>
 *
 * <p>组件（通常是在 Spring 上下文中定义的 Spring Bean）和容器（通常就是 Spring
 * {@link ApplicationContext} 本身）都可以实现该接口。容器会将启动/停止信号传播给每个容器内适用的所
 * 有组件，例如用于运行时的停止/重启场景。
 *
 * <p>可用于直接调用，也可用于通过 JMX 进行管理操作。在后一种情况下，
 * {@link org.springframework.jmx.export.MBeanExporter} 通常会与
 * {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler} 一同定义，从而
 * 将受活动控制的组件的可见性限制在 Lifecycle 接口上。
 *
 * <p>请注意，当前的 {@code Lifecycle} 接口仅在<b>顶层单例 Bean</b> 上受支持。在任何其他组件上，
 * {@code Lifecycle} 接口都将无法被检测到，因而被忽略。另外请注意，扩展的 {@link SmartLifecycle} 接
 * 口提供了与应用上下文的启动和关闭阶段的高级集成。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer
 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean
 */
public interface Lifecycle {

	/**
	 * 启动此组件。
	 *
	 * <p>如果组件已在运行，则不应抛出异常。
	 *
	 * <p>对于容器而言，这会将启动信号传播给所有适用的组件。
	 * @see SmartLifecycle#isAutoStartup()
	 */
	void start();

	/**
	 * 停止此组件，通常采用同步方式，使得该方法返回时组件已完全停止。当需要异步停止行为时，请考虑实现
	 * {@link SmartLifecycle} 及其 {@code stop(Runnable)} 变体。
	 *
	 * <p>请注意，此停止通知并不保证一定在销毁之前到达：在常规关闭时，{@code Lifecycle} Bean 会先收到停
	 * 止通知，然后才会传播一般的销毁回调；然而，在上下文生命周期内的热刷新或中止的刷新尝试中，给定
	 * Bean 的 destroy 方法将在完全不考虑停止信号的情况下被调用。
	 *
	 * <p>如果组件未在运行（尚未启动），则不应抛出异常。
	 *
	 * <p>对于容器而言，这会将停止信号传播给所有适用的组件。
	 * @see SmartLifecycle#stop(Runnable)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	void stop();

	/**
	 * 检查此组件当前是否正在运行。
	 *
	 * <p>对于容器而言，仅当<i>所有</i>适用的组件当前都在运行时，才会返回 {@code true}。
	 * @return whether the component is currently running
	 */
	boolean isRunning();

}
