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

package org.springframework.beans.factory;

/**
 * 由需要在其所有属性都已由 {@link BeanFactory} 设置后做出反应的 Bean 实现的接口：例如，执行自定义
 * 初始化，或仅检查所有必需属性是否都已设置。
 *
 * <p>实现 {@code InitializingBean} 的替代方案是指定自定义 init 方法，例如在 XML Bean 定义中。有关
 * 所有 Bean 生命周期方法的列表，请参见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
public interface InitializingBean {

	/**
	 * 在设置完所有 Bean 属性并满足 {@link BeanFactoryAware}、{@code ApplicationContextAware} 等之后，
	 * 由包含它的 {@code BeanFactory} 调用。<p>此方法允许 Bean 实例在所有 Bean 属性都已设置时对其整体配
	 * 置进行校验并进行最终初始化。
	 * @throws Exception in the event of misconfiguration (such as failure to set an
	 * essential property) or if initialization fails for any other reason
	 */
	void afterPropertiesSet() throws Exception;

}
