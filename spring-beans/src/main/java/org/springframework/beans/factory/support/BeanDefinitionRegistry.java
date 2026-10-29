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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * 用于存放 Bean 定义（例如 RootBeanDefinition 和 ChildBeanDefinition 实例）的注册表接口。通常由在
 * 内部使用 AbstractBeanDefinition 层次结构的 BeanFactory 实现。
 *
 * <p>这是 Spring 的 Bean 工厂包中唯一封装了 Bean 定义<i>注册</i>的接口。标准的 BeanFactory 接口仅
 * 涵盖对<i>完全配置的工厂实例</i>的访问。
 *
 * <p>Spring 的 Bean 定义读取器期望作用于该接口的实现。Spring 核心中的已知实现类有
 * DefaultListableBeanFactory 和 GenericApplicationContext。
 *
 * @author Juergen Hoeller
 * @since 26.11.2003
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * 向此注册表注册新的 Bean 定义。必须支持 RootBeanDefinition 和 ChildBeanDefinition。
	 * @param beanName the name of the bean instance to register
	 * @param beanDefinition definition of the bean instance to register
	 * @throws BeanDefinitionStoreException if the BeanDefinition is invalid
	 * @throws BeanDefinitionOverrideException if there is already a BeanDefinition
	 * for the specified bean name and we are not allowed to override it
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * 移除给定名称对应的 BeanDefinition。
	 * @param beanName the name of the bean instance to register
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定 bean 名称对应的 BeanDefinition。
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for the given name (never {@code null})
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 检查此注册表中是否包含给定名称的 Bean 定义。
	 * @param beanName the name of the bean to look for
	 * @return if this registry contains a bean definition with the given name
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回此注册表中定义的所有 bean 的名称。
	 * @return the names of all beans defined in this registry,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回注册表中定义的 bean 数量。
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定 bean 名称是否已在此注册表中使用，即该名称下是否注册了本地 bean 或别名。
	 * @param beanName the name to check
	 * @return whether the given bean name is already in use
	 */
	boolean isBeanNameInUse(String beanName);

}
