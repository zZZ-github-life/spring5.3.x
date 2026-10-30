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

package org.springframework.beans.factory.wiring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于保存特定类相关 Bean 装配元数据信息的持有者。与
 * {@link org.springframework.beans.factory.annotation.Configurable} 注解及 AspectJ 的
 * {@code AnnotationBeanConfigurerAspect} 配合使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see BeanWiringInfoResolver
 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory
 * @see org.springframework.beans.factory.annotation.Configurable
 */
public class BeanWiringInfo {

	/**
	 * 表示按名称自动装配 Bean 属性的常量。
	 * @see #BeanWiringInfo(int, boolean)
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_NAME
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 表示按类型自动装配 Bean 属性的常量。
	 * @see #BeanWiringInfo(int, boolean)
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#AUTOWIRE_BY_TYPE
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;


	@Nullable
	private String beanName;

	private boolean isDefaultBeanName = false;

	private int autowireMode = AutowireCapableBeanFactory.AUTOWIRE_NO;

	private boolean dependencyCheck = false;


	/**
	 * 创建一个默认的 BeanWiringInfo，建议对 Bean 类可能期望的工厂回调和后处理器回调执行普通初始化。
	 */
	public BeanWiringInfo() {
	}

	/**
	 * 创建一个指向给定 Bean 名称的新 BeanWiringInfo。
	 * @param beanName 要从中获取属性值的 Bean 定义的名称
	 * @throws IllegalArgumentException 如果提供的 beanName 为 {@code null}、为空或完全由空白组成
	 */
	public BeanWiringInfo(String beanName) {
		this(beanName, false);
	}

	/**
	 * 创建一个指向给定 Bean 名称的新 BeanWiringInfo。
	 * @param beanName 要从中获取属性值的 Bean 定义的名称
	 * @param isDefaultBeanName 给定的 Bean 名称是否为建议的默认 Bean 名称
	 * 它不一定匹配实际的 Bean 定义
	 * @throws IllegalArgumentException 如果提供的 beanName 为 {@code null}、为空或完全由空白组成
	 */
	public BeanWiringInfo(String beanName, boolean isDefaultBeanName) {
		Assert.hasText(beanName, "'beanName' must not be empty");
		this.beanName = beanName;
		this.isDefaultBeanName = isDefaultBeanName;
	}

	/**
	 * 创建一个表示自动装配的新 BeanWiringInfo。
	 * @param autowireMode 常量 {@link #AUTOWIRE_BY_NAME} / {@link #AUTOWIRE_BY_TYPE} 之一
	 * @param dependencyCheck 是否对 Bean 实例中的对象引用执行依赖检查（在自动装配之后）
	 * @throws IllegalArgumentException 如果提供的 {@code autowireMode} 不是允许的值之一
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public BeanWiringInfo(int autowireMode, boolean dependencyCheck) {
		if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
			throw new IllegalArgumentException("Only constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE supported");
		}
		this.autowireMode = autowireMode;
		this.dependencyCheck = dependencyCheck;
	}


	/**
	 * 返回此 BeanWiringInfo 是否表示自动装配。
	 */
	public boolean indicatesAutowiring() {
		return (this.beanName == null);
	}

	/**
	 * 返回此 BeanWiringInfo 指向的具体 Bean 名称（如果有）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回该具体 Bean 名称是否为建议的默认 Bean 名称，它不一定要匹配工厂中实际的 Bean 定义。
	 */
	public boolean isDefaultBeanName() {
		return this.isDefaultBeanName;
	}

	/**
	 * 如果指示自动装配，则返回 {@link #AUTOWIRE_BY_NAME} / {@link #AUTOWIRE_BY_TYPE} 常量之一。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回是否对 Bean 实例中的对象引用执行依赖检查（在自动装配之后）。
	 */
	public boolean getDependencyCheck() {
		return this.dependencyCheck;
	}

}
