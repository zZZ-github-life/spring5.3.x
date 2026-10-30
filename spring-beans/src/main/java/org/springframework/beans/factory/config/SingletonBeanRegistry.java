/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.lang.Nullable;

/**
 * 定义共享 Bean 实例注册表的接口。该接口可由 {@link org.springframework.beans.factory.BeanFactory}
 * 的实现类实现，以便以统一的方式暴露其单例管理功能。
 *
 * <p>{@link ConfigurableBeanFactory} 接口扩展了此接口。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 */
public interface SingletonBeanRegistry {

	/**
	 * 将给定的现有对象以给定 Bean 名称注册为 Bean 注册表中的单例。
	 * <p>给定的实例应已完全初始化；注册表不会执行任何初始化回调（特别是不会调用 InitializingBean 的
	 * {@code afterPropertiesSet} 方法）。给定实例也不会收到任何销毁回调（如 DisposableBean 的
	 * {@code destroy} 方法）。
	 * <p>在完整的 BeanFactory 中运行时：<b>如果希望 Bean 接收初始化和/或销毁回调，请注册 Bean 定义，而
	 * 不是现有实例。</b>
	 * <p>通常在注册表配置期间调用，但也可用于在运行时注册单例。因此，注册表实现应同步单例访问；如果它
	 * 支持 BeanFactory 对单例的延迟初始化，则无论如何都必须这样做。
	 * @param beanName 该 Bean 的名称
	 * @param singletonObject 现有的单例对象
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.DisposableBean#destroy
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * 返回以给定名称注册的（原始）单例对象。
	 * <p>仅检查已实例化的单例；对于尚未实例化的单例 Bean 定义，不返回 Object。
	 * <p>此方法的主要目的是访问手动注册的单例（参见 {@link #registerSingleton}）。也可用于以原始方式访
	 * 问由已创建的 Bean 定义所定义的单例。
	 * <p><b>注意：</b> 此查找方法不感知 FactoryBean 前缀或别名。在获取单例实例之前，需要先解析规范
	 * Bean 名称。
	 * @param beanName 要查找的 Bean 的名称
	 * @return 已注册的单例对象；如果未找到则为 {@code null}
	 * @see ConfigurableListableBeanFactory#getBeanDefinition
	 */
	@Nullable
	Object getSingleton(String beanName);

	/**
	 * 检查此注册表是否包含具有给定名称的单例实例。
	 * <p>仅检查已实例化的单例；对于尚未实例化的单例 Bean 定义，不返回 {@code true}。
	 * <p>此方法的主要目的是检查手动注册的单例（参见 {@link #registerSingleton}）。也可用于检查由 Bean
	 * 定义所定义的单例是否已创建。
	 * <p>要检查 Bean 工厂是否包含具有给定名称的 Bean 定义，请使用 ListableBeanFactory 的
	 * {@code containsBeanDefinition}。同时调用 {@code containsBeanDefinition} 和
	 * {@code containsSingleton} 可以回答特定 Bean 工厂是否包含具有给定名称的本地 Bean 实例。
	 * <p>使用 BeanFactory 的 {@code containsBean} 进行一般性检查，判断工厂是否知道具有给定名称的 Bean
	 * （无论是手动注册的单例实例还是由 Bean 定义创建），同时还会检查祖先工厂。
	 * <p><b>注意：</b> 此查找方法不感知 FactoryBean 前缀或别名。在检查单例状态之前，需要先解析规范
	 * Bean 名称。
	 * @param beanName 要查找的 Bean 的名称
	 * @return 此 Bean 工厂是否包含给定名称的单例实例
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 * @see org.springframework.beans.factory.BeanFactory#containsBean
	 */
	boolean containsSingleton(String beanName);

	/**
	 * 返回在此注册表中注册的单例 Bean 的名称。
	 * <p>仅检查已实例化的单例；对于尚未实例化的单例 Bean 定义，不返回名称。
	 * <p>此方法的主要目的是检查手动注册的单例（参见 {@link #registerSingleton}）。也可用于检查由 Bean
	 * 定义所定义的哪些单例已创建。
	 * @return 名称列表的 String 数组（永不为 {@code null}）
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionNames
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames
	 */
	String[] getSingletonNames();

	/**
	 * 返回在此注册表中注册的单例 Bean 数量。
	 * <p>仅检查已实例化的单例；不统计尚未实例化的单例 Bean 定义。
	 * <p>此方法的主要目的是检查手动注册的单例（参见 {@link #registerSingleton}）。也可用于统计由 Bean
	 * 定义所定义的、已创建的单例数量。
	 * @return 单例 Bean 的数量
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionCount
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount
	 */
	int getSingletonCount();

	/**
	 * 返回此注册表使用的单例互斥锁（供外部协作者使用）。
	 * @return 互斥对象（永不为 {@code null}）
	 * @since 4.2
	 */
	Object getSingletonMutex();

}
