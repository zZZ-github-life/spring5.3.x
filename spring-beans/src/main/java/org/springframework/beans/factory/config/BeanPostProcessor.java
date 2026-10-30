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
import org.springframework.lang.Nullable;

/**
 * 工厂钩子，允许对新 Bean 实例进行自定义修改 &mdash; 例如，检查标记接口或用代理包装 Bean。
 *
 * <p>通常，通过标记接口等填充 Bean 的后处理器会实现 {@link #postProcessBeforeInitialization}，而用
 * 代理包装 Bean 的后处理器通常会实现 {@link #postProcessAfterInitialization}。
 *
 * <h3>Registration</h3>
 * <p>{@code ApplicationContext} 可以在其 Bean 定义中自动检测 {@code BeanPostProcessor} Bean，并将
 * 这些后处理器应用于此后创建的任何 Bean。普通的 {@code BeanFactory} 允许以编程方式注册后处理器，并
 * 将它们应用于通过该 Bean 工厂创建的所有 Bean。
 *
 * <h3>Ordering</h3>
 * <p>在 {@code ApplicationContext} 中自动检测到的 {@code BeanPostProcessor} Bean 将按照
 * {@link org.springframework.core.PriorityOrdered} 和 {@link org.springframework.core.Ordered} 的
 * 语义排序。相比之下，通过 {@code BeanFactory} 以编程方式注册的 {@code BeanPostProcessor} Bean 将
 * 按照注册顺序应用；通过实现 {@code PriorityOrdered} 或 {@code Ordered} 接口所表达的任何排序语义，
 * 对于以编程方式注册的后处理器都将被忽略。此外，{@code BeanPostProcessor} Bean 不会考虑
 * {@link org.springframework.core.annotation.Order @Order} 注解。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface BeanPostProcessor {

	/**
	 * 在任何 Bean 初始化回调（如 InitializingBean 的 {@code afterPropertiesSet} 或自定义 init-method）
	 * <i>之前</i>，将此 {@code BeanPostProcessor} 应用于给定的新 Bean 实例。此时 Bean 已填充好属性值。
	 * 返回的 Bean 实例可能是原始实例的包装器。
	 * <p>默认实现会原样返回给定的 {@code bean}。
	 * @param bean 新的 Bean 实例
	 * @param beanName 该 Bean 的名称
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例；
	 * 如果为 {@code null}，则不会调用后续的 BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 如果出错则抛出
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在任何 Bean 初始化回调（如 InitializingBean 的 {@code afterPropertiesSet} 或自定义 init-method）
	 * <i>之后</i>，将此 {@code BeanPostProcessor} 应用于给定的新 Bean 实例。此时 Bean 已填充好属性值。
	 * 返回的 Bean 实例可能是原始实例的包装器。
	 * <p>对于 FactoryBean，此回调将同时针对 FactoryBean 实例和由该 FactoryBean 创建的对象（自 Spring
	 * 2.0 起）调用。后处理器可以通过相应的 {@code bean instanceof FactoryBean} 检查来决定是应用于
	 * FactoryBean 还是创建的对象，抑或两者。
	 * <p>与所有其他 {@code BeanPostProcessor} 回调不同，此回调也会在由
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} 方法触发的短路之后调
	 * 用。
	 * <p>默认实现会原样返回给定的 {@code bean}。
	 * @param bean 新的 Bean 实例
	 * @param beanName 该 Bean 的名称
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例；
	 * 如果为 {@code null}，则不会调用后续的 BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 如果出错则抛出
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
