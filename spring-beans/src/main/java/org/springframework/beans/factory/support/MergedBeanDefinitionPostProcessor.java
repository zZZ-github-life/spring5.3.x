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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 运行时用于<i>合并</i>后的 Bean 定义的后处理器回调接口。{@link BeanPostProcessor} 实现类可以实现
 * 此子接口，以便对 Spring {@code BeanFactory} 创建 Bean 实例时使用的合并 Bean 定义（原始 Bean 定义
 * 经过处理后的副本）进行后处理。
 *
 * <p>例如，{@link #postProcessMergedBeanDefinition} 方法可以内省 Bean 定义，以便在对 Bean 的实际实
 * 例进行后处理之前准备一些缓存的元数据。也允许修改 Bean 定义，但<i>仅</i>限于确实用于并发修改的定
 * 义属性。本质上，这只适用于在 {@link RootBeanDefinition} 本身上定义的操作，而不适用于其基类的属性。
 *
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getMergedBeanDefinition
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * 对指定 Bean 的给定合并 Bean 定义进行后处理。
	 * @param beanDefinition 该 Bean 的合并后的 Bean 定义
	 * @param beanType 被管理 Bean 实例的实际类型
	 * @param beanName Bean 的名称
	 * @see AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

	/**
	 * 通知：指定名称的 Bean 定义已被重置，此处理器应清除受影响 Bean 的任何元数据。
	 * <p>默认实现为空。
	 * @param beanName Bean 的名称
	 * @since 5.1
	 * @see DefaultListableBeanFactory#resetBeanDefinition
	 */
	default void resetBeanDefinition(String beanName) {
	}

}
