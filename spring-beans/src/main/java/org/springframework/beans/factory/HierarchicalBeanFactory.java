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

import org.springframework.lang.Nullable;

/**
 * 由可以成为层次结构一部分的 Bean 工厂实现的子接口。
 *
 * <p>对于允许以可配置方式设置父工厂的 Bean 工厂，其对应的 {@code setParentBeanFactory} 方法可在
 * ConfigurableBeanFactory 接口中找到。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 07.07.2003
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 返回父 Bean 工厂，如果没有则返回 {@code null}。
	 */
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * 返回本地 Bean 工厂是否包含具有给定名称的 Bean，忽略在祖先上下文中定义的 Bean。<p>这是
	 * {@code containsBean} 的替代方法，会忽略来自祖先 Bean 工厂的给定名称的 Bean。
	 * @param name 要查询的 Bean 名称
	 * @return 给定名称的 Bean 是否定义在本地工厂中
	 * @see BeanFactory#containsBean
	 */
	boolean containsLocalBean(String name);

}
