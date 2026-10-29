/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * 在 {@link BeanFactory} 启动期间，在单例预实例化阶段结束时触发的回调接口。单例 Bean 可以实现该接
 * 口，以便在常规的单例实例化算法之后执行一些初始化，同时避免因意外的提前初始化（例如来自
 * {@link ListableBeanFactory#getBeansOfType} 调用）而产生副作用。从这个意义上说，它是
 * {@link InitializingBean} 的替代方案，后者恰好在 Bean 本地构造阶段结束时触发。
 *
 * <p>该回调变体与 {@link org.springframework.context.event.ContextRefreshedEvent} 有些相似，但不要
 * 求实现 {@link org.springframework.context.ApplicationListener}，也无需跨上下文层次结构过滤上下文
 * 引用等。它还意味着仅对 {@code beans} 包有更小的依赖，并且独立的 {@link ListableBeanFactory} 实现
 * 也会支持它，而不仅限于 {@link org.springframework.context.ApplicationContext} 环境。
 *
 * <p><b>NOTE:</b> 如果打算启动/管理异步任务，最好实现
 * {@link org.springframework.context.Lifecycle}，它提供了更丰富的运行时管理模型，并允许分阶段启动/
 * 关闭。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

	/**
	 * 恰好在单例预实例化阶段结束时被调用，并保证所有常规单例 Bean 均已创建完成。此方法内部的
	 * {@link ListableBeanFactory#getBeansOfType} 调用不会在启动期间触发意外副作用。
	 * <p><b>NOTE:</b> 对于在 {@link BeanFactory} 启动后按需延迟初始化的单例 Bean，不会触发该回调，任何
	 * 其他 Bean 作用域也不会触发。请仅对具有预期启动语义的 Bean 谨慎使用。
	 */
	void afterSingletonsInstantiated();

}
