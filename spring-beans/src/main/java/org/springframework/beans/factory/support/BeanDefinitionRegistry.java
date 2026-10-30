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
	 * @param beanName 要注册的 Bean 实例的名称
	 * @param beanDefinition 要注册的 Bean 实例的定义
	 * @throws BeanDefinitionStoreException 如果 BeanDefinition 无效
	 * @throws BeanDefinitionOverrideException 如果指定的 Bean 名称已存在 BeanDefinition 且不允许覆盖
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * 移除给定名称对应的 BeanDefinition。
	 * @param beanName 要注册的 Bean 实例的名称
	 * @throws NoSuchBeanDefinitionException 如果不存在这样的 Bean 定义
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定 bean 名称对应的 BeanDefinition。
	 * @param beanName 要查找定义的 Bean 名称
	 * @return 给定名称对应的 BeanDefinition（永不为 {@code null}）
	 * @throws NoSuchBeanDefinitionException 如果不存在这样的 Bean 定义
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 检查此注册表中是否包含给定名称的 Bean 定义。
	 * @param beanName 要查找的 Bean 名称
	 * @return 如果此注册表包含具有给定名称的 Bean 定义，则返回 true
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回此注册表中定义的所有 bean 的名称。
	 * @return 此注册表中定义的所有 Bean 的名称，如果没有定义任何 Bean 则为空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回注册表中定义的 bean 数量。
	 * @return 注册表中定义的 Bean 数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定 bean 名称是否已在此注册表中使用，即该名称下是否注册了本地 bean 或别名。
	 * @param beanName 要检查的名称
	 * @return 给定的 Bean 名称是否已被使用
	 */
	boolean isBeanNameInUse(String beanName);

}
