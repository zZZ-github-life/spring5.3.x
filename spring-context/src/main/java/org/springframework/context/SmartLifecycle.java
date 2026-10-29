/*
 * Copyright 2002-2019 the original author or authors.
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
 * 针对需要在 {@code ApplicationContext} 刷新和/或关闭时按特定顺序启动的那些对象，对
 * {@link Lifecycle} 接口的扩展。
 *
 * <p>{@link #isAutoStartup()} 的返回值指示此对象是否应在上下文刷新时启动。接受回调的
 * {@link #stop(Runnable)} 方法适用于具有异步关闭过程的对象。任何此接口的实现<i>必须</i>在关闭完成
 * 时调用回调的 {@code run()} 方法，以避免整体 {@code ApplicationContext} 关闭出现不必要的延迟。
 *
 * <p>此接口扩展了 {@link Phased}，{@link #getPhase()} 方法的返回值指示此 {@code Lifecycle} 组件应
 * 在哪个阶段启动和停止。启动过程从<i>最低</i>的阶段值开始，到<i>最高</i>的阶段值结束
 * （{@code Integer.MIN_VALUE} 是最低可能值，{@code Integer.MAX_VALUE} 是最高可能值）。关闭过程则采用
 * 相反的顺序。具有相同值的任何组件将在同一阶段内任意排序。
 *
 * <p>示例：如果组件 B 依赖于组件 A 已先启动，那么组件 A 的阶段值应低于组件 B。在关闭过程中，组件 B
 * 将先于组件 A 停止。
 *
 * <p>任何显式的 "depends-on" 关系都将优先于阶段顺序，使得依赖方 Bean 总是在其依赖项之后启动，并总
 * 是在其依赖项之前停止。
 *
 * <p>上下文中任何未同时实现 {@code SmartLifecycle} 的 {@code Lifecycle} 组件将被视为具有阶段值
 * {@code 0}。这样，如果 {@code SmartLifecycle} 组件具有负的阶段值，它可以在这些 {@code Lifecycle}
 * 组件之前启动；如果 {@code SmartLifecycle} 组件具有正的阶段值，它可以在这些 {@code Lifecycle} 组
 * 件之后启动。
 *
 * <p>请注意，由于 {@code SmartLifecycle} 中的自动启动支持，{@code SmartLifecycle} Bean 实例通常无
 * 论如何都会在应用上下文启动时被初始化。因此，Bean 定义的 lazy-init 标志对 {@code SmartLifecycle}
 * Bean 的实际影响非常有限。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see LifecycleProcessor
 * @see ConfigurableApplicationContext
 */
public interface SmartLifecycle extends Lifecycle, Phased {

	/**
	 * {@code SmartLifecycle} 的默认阶段：{@code Integer.MAX_VALUE}。
	 *
	 * <p>这与常规 {@link Lifecycle} 实现所关联的通用阶段 {@code 0} 不同，它使通常自动启动的
	 * {@code SmartLifecycle} Bean 处于更晚的启动阶段和更早的关闭阶段。
	 * @since 5.1
	 * @see #getPhase()
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	int DEFAULT_PHASE = Integer.MAX_VALUE;


	/**
	 * 如果此 {@code Lifecycle} 组件应由容器在包含它的 {@link ApplicationContext} 刷新时自动启动，则返
	 * 回 {@code true}。
	 *
	 * <p>值为 {@code false} 表示该组件打算改为通过显式的 {@link #start()} 调用启动，类似于普通的
	 * {@link Lifecycle} 实现。
	 *
	 * <p>默认实现返回 {@code true}。
	 * @see #start()
	 * @see #getPhase()
	 * @see LifecycleProcessor#onRefresh()
	 * @see ConfigurableApplicationContext#refresh()
	 */
	default boolean isAutoStartup() {
		return true;
	}

	/**
	 * 指示 Lifecycle 组件在正在运行时必须停止。
	 *
	 * <p>所提供的回调由 {@link LifecycleProcessor} 使用，以支持对具有相同关闭顺序值的所有组件进行有序
	 * 且可能并发的关闭。回调<b>必须</b>在 {@code SmartLifecycle} 组件确实停止之后执行。
	 *
	 * <p>{@link LifecycleProcessor} 将<i>仅</i>调用 {@code stop} 方法的此变体；也就是说，对于
	 * {@code SmartLifecycle} 实现，不会调用 {@link Lifecycle#stop()}，除非在此方法的实现中显式委托。
	 *
	 * <p>默认实现委托给 {@link #stop()}，并在调用线程中立即触发给定回调。请注意，两者之间没有同步，因
	 * 此自定义实现至少可能希望将相同的步骤放入其公共生命周期监视器（如果有）中。
	 * @see #stop()
	 * @see #getPhase()
	 */
	default void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * 返回此生命周期对象应运行所在的阶段。
	 *
	 * <p>默认实现返回 {@link #DEFAULT_PHASE}，以使 {@code stop()} 回调在常规 {@code Lifecycle} 实现之
	 * 后执行。
	 * @see #isAutoStartup()
	 * @see #start()
	 * @see #stop(Runnable)
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	@Override
	default int getPhase() {
		return DEFAULT_PHASE;
	}

}
