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

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 当要求 {@code BeanFactory} 提供某个 Bean 实例，而它找不到相应定义时抛出的异常。这可能指向不存在
 * 的 Bean、不唯一的 Bean，或没有关联 Bean 定义的手动注册单例实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanFactory#getBean(String)
 * @see BeanFactory#getBean(Class)
 * @see NoUniqueBeanDefinitionException
 */
@SuppressWarnings("serial")
public class NoSuchBeanDefinitionException extends BeansException {

	@Nullable
	private final String beanName;

	@Nullable
	private final ResolvableType resolvableType;


	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param name 缺失的 Bean 名称
	 */
	public NoSuchBeanDefinitionException(String name) {
		super("No bean named '" + name + "' available");
		this.beanName = name;
		this.resolvableType = null;
	}

	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param name 缺失的 Bean 名称
	 * @param message 描述该问题的详细消息
	 */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' available: " + message);
		this.beanName = name;
		this.resolvableType = null;
	}

	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param type 缺失的 Bean 的所需类型
	 */
	public NoSuchBeanDefinitionException(Class<?> type) {
		this(ResolvableType.forClass(type));
	}

	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param type 缺失的 Bean 的所需类型
	 * @param message 描述该问题的详细消息
	 */
	public NoSuchBeanDefinitionException(Class<?> type, String message) {
		this(ResolvableType.forClass(type), message);
	}

	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param type 缺失的 Bean 的完整类型声明
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type) {
		super("No qualifying bean of type '" + type + "' available");
		this.beanName = null;
		this.resolvableType = type;
	}

	/**
	 * 创建一个新的 {@code NoSuchBeanDefinitionException}。
	 * @param type 缺失的 Bean 的完整类型声明
	 * @param message 描述该问题的详细消息
	 * @since 4.3.4
	 */
	public NoSuchBeanDefinitionException(ResolvableType type, String message) {
		super("No qualifying bean of type '" + type + "' available: " + message);
		this.beanName = null;
		this.resolvableType = type;
	}


	/**
	 * 返回缺失 Bean 的名称（如果是<em>按名称</em>查找失败）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回缺失 Bean 的所需类型（如果是<em>按类型</em>查找失败）。
	 */
	@Nullable
	public Class<?> getBeanType() {
		return (this.resolvableType != null ? this.resolvableType.resolve() : null);
	}

	/**
	 * 返回缺失 Bean 的所需 {@link ResolvableType}（如果是<em>按类型</em>查找失败）。
	 * @since 4.3.4
	 */
	@Nullable
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回在仅预期一个匹配 Bean 时找到的 Bean 数量。对于常规的 NoSuchBeanDefinitionException，该值始终
	 * 为 0。
	 * @see NoUniqueBeanDefinitionException
	 */
	public int getNumberOfBeansFound() {
		return 0;
	}

}
