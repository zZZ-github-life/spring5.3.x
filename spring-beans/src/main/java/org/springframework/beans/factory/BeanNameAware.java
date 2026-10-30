/*
 * Copyright 2002-2016 the original author or authors.
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
 * 由希望感知其在 Bean 工厂中 Bean 名称的 Bean 实现的接口。请注意，通常不建议对象依赖于其 Bean 名称，
 * 因为这代表了对可能脆弱的外部配置的依赖，以及对 Spring API 可能不必要的依赖。
 *
 * <p>有关所有 Bean 生命周期方法的列表，请参见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 01.11.2003
 * @see BeanClassLoaderAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
public interface BeanNameAware extends Aware {

	/**
	 * 设置该 Bean 在创建它的 Bean 工厂中的名称。<p>在填充普通 Bean 属性之后、在诸如
	 * {@link InitializingBean#afterPropertiesSet()} 之类的初始化回调或自定义 init-method 之前调用。
	 * @param name 工厂中该 Bean 的名称。注意此名称是工厂中实际使用的 Bean 名称，
	 * 可能与最初指定的名称不同：特别是对于内部 Bean 名称，实际 Bean 名称
	 * 可能通过追加 "#..." 后缀而变得唯一。如果需要，可使用
	 * {@link BeanFactoryUtils#originalBeanName(String)} 方法提取原始 Bean 名称（不含后缀）。
	 */
	void setBeanName(String name);

}
