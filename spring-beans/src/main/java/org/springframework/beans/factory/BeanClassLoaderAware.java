/*
 * Copyright 2002-2017 the original author or authors.
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
 * 允许 Bean 感知 Bean {@link ClassLoader class loader} 的回调；即当前 Bean 工厂用于加载 Bean 类的
 * 类加载器。
 *
 * <p>这主要适用于那些必须按名称获取应用类、而自身可能从共享类加载器加载的框架类来实现。
 *
 * <p>有关所有 Bean 生命周期方法的列表，请参阅 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 */
public interface BeanClassLoaderAware extends Aware {

	/**
	 * 向 Bean 实例提供 Bean {@link ClassLoader class loader} 的回调。
	 * <p>在填充普通 Bean 属性<i>之后</i>、但在初始化回调（如
	 * {@link InitializingBean InitializingBean's} 的 {@link InitializingBean#afterPropertiesSet()} 方
	 * 法或自定义 init-method）<i>之前</i>调用。
	 * @param classLoader 所属的类加载器
	 */
	void setBeanClassLoader(ClassLoader classLoader);

}
