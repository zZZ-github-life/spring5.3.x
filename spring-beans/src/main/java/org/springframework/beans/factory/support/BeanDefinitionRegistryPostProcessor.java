/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * 对标准 {@link BeanFactoryPostProcessor} SPI 的扩展，允许在常规 BeanFactoryPostProcessor 检测开始
 * <i>之前</i>注册更多 Bean 定义。特别地，BeanDefinitionRegistryPostProcessor 可以注册更多 Bean 定
 * 义，这些定义又会定义 BeanFactoryPostProcessor 实例。
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * 在应用上下文的标准初始化之后，修改其内部 Bean 定义注册表。此时所有常规 Bean 定义都已加载，但尚未
	 * 实例化任何 bean。这样可以在下一个后处理阶段开始之前添加更多 Bean 定义。
	 * @param registry the bean definition registry used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
