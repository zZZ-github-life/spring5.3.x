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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * 工厂钩子，允许对应用上下文的 Bean 定义进行自定义修改，调整上下文底层 Bean 工厂的 Bean 属性值。
 *
 * <p>对于面向系统管理员、用于覆盖应用上下文中配置的 Bean 属性的自定义配置文件非常有用。有关开箱即
 * 用地满足此类配置需求的方案，请参阅 {@link PropertyResourceConfigurer} 及其具体实现。
 *
 * <p>{@code BeanFactoryPostProcessor} 可以与 Bean 定义交互并修改它们，但绝不会与 Bean 实例交互。否
 * 则可能导致 Bean 过早实例化，破坏容器并造成意外的副作用。如果需要与 Bean 实例交互，请考虑改为实现
 * {@link BeanPostProcessor}。
 *
 * <h3>Registration</h3>
 * <p>{@code ApplicationContext} 会在其 Bean 定义中自动检测 {@code BeanFactoryPostProcessor} Bean，
 * 并在创建任何其他 Bean 之前应用它们。{@code BeanFactoryPostProcessor} 也可以通过
 * {@code ConfigurableApplicationContext} 以编程方式注册。
 *
 * <h3>Ordering</h3>
 * <p>在 {@code ApplicationContext} 中自动检测到的 {@code BeanFactoryPostProcessor} Bean 将按照
 * {@link org.springframework.core.PriorityOrdered} 和 {@link org.springframework.core.Ordered} 的
 * 语义排序。相比之下，通过 {@code ConfigurableApplicationContext} 以编程方式注册的
 * {@code BeanFactoryPostProcessor} Bean 将按照注册顺序应用；通过实现 {@code PriorityOrdered} 或
 * {@code Ordered} 接口所表达的任何排序语义，对于以编程方式注册的后处理器都将被忽略。此外，
 * {@code BeanFactoryPostProcessor} Bean 不会考虑
 * {@link org.springframework.core.annotation.Order @Order} 注解。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * 在标准初始化之后修改应用上下文的内部 Bean 工厂。此时所有 Bean 定义都已加载，但尚未实例化任何
	 * Bean。这允许覆盖或添加属性，甚至对预先初始化的 Bean 也是如此。
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
