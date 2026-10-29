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

package org.springframework.beans.factory.config;

import java.util.Iterator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.lang.Nullable;

/**
 * 供大多数可列举 Bean 工厂实现的配置接口。除 {@link ConfigurableBeanFactory} 外，它还提供分析和修
 * 改 Bean 定义以及预实例化 singleton 的功能。
 *
 * <p>此 {@link org.springframework.beans.factory.BeanFactory} 的子接口并不打算在普通的应用代码中使
 * 用：典型用例请坚持使用 {@link org.springframework.beans.factory.BeanFactory} 或
 * {@link org.springframework.beans.factory.ListableBeanFactory}。此接口只是为了支持框架内部的即插
 * 即用，即使在需要访问 Bean 工厂配置方法时也是如此。
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * 忽略给定的依赖类型以进行自动装配：例如 String。默认不忽略任何类型。
	 * @param type the dependency type to ignore
	 */
	void ignoreDependencyType(Class<?> type);

	/**
	 * 忽略给定的依赖接口以进行自动装配。
	 * <p>应用上下文通常用其注册以其他方式解析的依赖，例如通过 BeanFactoryAware 解析 BeanFactory，或通
	 * 过 ApplicationContextAware 解析 ApplicationContext。
	 * <p>默认情况下，只忽略 BeanFactoryAware 接口。要忽略更多类型，请为每种类型调用此方法。
	 * @param ifc the dependency interface to ignore
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * 注册一个特殊依赖类型及其对应的自动装配值。
	 * <p>这适用于那些应可被自动装配但未在工厂中定义为 Bean 的工厂/上下文引用：例如，将
	 * ApplicationContext 类型的依赖解析为 Bean 所在的那个 ApplicationContext 实例。
	 * <p>注意：在普通的 BeanFactory 中没有注册任何此类默认类型，甚至对 BeanFactory 接口本身也没有。
	 * @param dependencyType the dependency type to register. This will typically
	 * be a base interface such as BeanFactory, with extensions of it resolved
	 * as well if declared as an autowiring dependency (e.g. ListableBeanFactory),
	 * as long as the given value actually implements the extended interface.
	 * @param autowiredValue the corresponding autowired value. This may also be an
	 * implementation of the {@link org.springframework.beans.factory.ObjectFactory}
	 * interface, which allows for lazy resolution of the actual target value.
	 */
	void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

	/**
	 * 确定指定 Bean 是否有资格作为自动装配候选，以便注入到声明了匹配类型依赖的其他 Bean 中。
	 * <p>此方法也会检查祖先工厂。
	 * @param beanName the name of the bean to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @return whether the bean should be considered as autowire candidate
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 */
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * 返回指定 Bean 注册的 BeanDefinition，允许访问其属性值和构造函数参数值（这些值可在 Bean 工厂后处
	 * 理期间修改）。
	 * <p>返回的 BeanDefinition 对象不应是副本，而应是工厂中注册的原始定义对象。这意味着如有必要，它应
	 * 可强制转换为更具体的实现类型。
	 * <p><b>注意：</b>此方法<i>不</i>考虑祖先工厂。它仅用于访问此工厂的本地 Bean 定义。
	 * @param beanName the name of the bean
	 * @return the registered BeanDefinition
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * defined in this factory
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回此工厂管理的所有 Bean 名称的统一视图。
	 * <p>包括 Bean 定义名称以及手动注册的 singleton 实例的名称，Bean 定义名称始终排在前面，类似于按类
	 * 型/注解检索 Bean 名称的工作方式。
	 * @return the composite iterator for the bean names view
	 * @since 4.1.2
	 * @see #containsBeanDefinition
	 * @see #registerSingleton
	 * @see #getBeanNamesForType
	 * @see #getBeanNamesForAnnotation
	 */
	Iterator<String> getBeanNamesIterator();

	/**
	 * 清除合并 Bean 定义缓存，移除那些尚不符合完整元数据缓存条件的 Bean 的条目。
	 * <p>通常在原始 Bean 定义发生更改后触发，例如应用 {@link BeanFactoryPostProcessor} 之后。注意，此
	 * 时已经创建的 Bean 的元数据将被保留。
	 * @since 4.2
	 * @see #getBeanDefinition
	 * @see #getMergedBeanDefinition
	 */
	void clearMetadataCache();

	/**
	 * 冻结所有 Bean 定义，表示已注册的 Bean 定义将不再被修改或后处理。
	 * <p>这使工厂能够积极地缓存 Bean 定义元数据。
	 */
	void freezeConfiguration();

	/**
	 * 返回此工厂的 Bean 定义是否已冻结，即是否不应再被修改或后处理。
	 * @return {@code true} if the factory's configuration is considered frozen
	 */
	boolean isConfigurationFrozen();

	/**
	 * 确保所有非延迟初始化的 singleton 都被实例化，同时也会考虑
	 * {@link org.springframework.beans.factory.FactoryBean FactoryBeans}。如果需要，通常在工厂设置结束
	 * 时调用。
	 * @throws BeansException if one of the singleton beans could not be created.
	 * Note: This may have left the factory with some beans already initialized!
	 * Call {@link #destroySingletons()} for full cleanup in this case.
	 * @see #destroySingletons()
	 */
	void preInstantiateSingletons() throws BeansException;

}
