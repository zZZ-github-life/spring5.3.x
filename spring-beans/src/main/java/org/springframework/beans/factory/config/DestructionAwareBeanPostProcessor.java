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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * {@link BeanPostProcessor} 的子接口，增加了销毁前回调。
 *
 * <p>典型用法是在特定 Bean 类型上调用自定义销毁回调，与相应的初始化回调相匹配。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在给定 Bean 实例销毁之前应用此 BeanPostProcessor，例如调用自定义销毁回调。
	 * <p>与 DisposableBean 的 {@code destroy} 和自定义销毁方法类似，此回调仅适用于容器完全管理其生命周
	 * 期的 Bean。通常单例和作用域 Bean 属于这种情况。
	 * @param bean 要销毁的 Bean 实例
	 * @param beanName 该 Bean 的名称
	 * @throws org.springframework.beans.BeansException 如果出错则抛出
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#setDestroyMethodName(String)
	 */
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

	/**
	 * 确定给定的 Bean 实例是否需要由此后处理器进行销毁。
	 * <p>默认实现返回 {@code true}。如果某个 pre-5 版本的 {@code DestructionAwareBeanPostProcessor} 实
	 * 现未提供此方法的具体实现，Spring 也会静默地假定为 {@code true}。
	 * @param bean 要检查的 Bean 实例
	 * @return 如果最终应为该 Bean 实例调用 {@link #postProcessBeforeDestruction}
	 * 则返回 {@code true}；如果不需要则返回 {@code false}
	 * @since 4.3
	 */
	default boolean requiresDestruction(Object bean) {
		return true;
	}

}
