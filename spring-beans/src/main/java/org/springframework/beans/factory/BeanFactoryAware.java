/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;

/**
 * 由希望感知其所拥有的 {@link BeanFactory} 的 Bean 实现的接口。
 *
 * <p>例如，Bean 可以通过工厂查找协作 Bean（依赖查找）。请注意，大多数 Bean 会选择通过相应的 Bean
 * 属性或构造器参数（依赖注入）接收对协作 Bean 的引用。
 *
 * <p>有关所有 Bean 生命周期方法的列表，请参见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 11.03.2003
 * @see BeanNameAware
 * @see BeanClassLoaderAware
 * @see InitializingBean
 * @see org.springframework.context.ApplicationContextAware
 */
public interface BeanFactoryAware extends Aware {

	/**
	 * 向 Bean 实例提供其所拥有工厂的回调。<p>在填充普通 Bean 属性之后、在诸如
	 * {@link InitializingBean#afterPropertiesSet()} 之类的初始化回调或自定义 init-method 之前调用。
	 * @param beanFactory 所属的 BeanFactory（从不为 {@code null}）。
	 * 该 Bean 可以立即调用工厂上的方法。
	 * @throws BeansException 如果发生初始化错误
	 * @see BeanInitializationException
	 */
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
