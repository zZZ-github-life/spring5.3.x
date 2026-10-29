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
	 * @param name the name of the bean in the factory.
	 * Note that this name is the actual bean name used in the factory, which may
	 * differ from the originally specified name: in particular for inner bean
	 * names, the actual bean name might have been made unique through appending
	 * "#..." suffixes. Use the {@link BeanFactoryUtils#originalBeanName(String)}
	 * method to extract the original bean name (without suffix), if desired.
	 */
	void setBeanName(String name);

}
