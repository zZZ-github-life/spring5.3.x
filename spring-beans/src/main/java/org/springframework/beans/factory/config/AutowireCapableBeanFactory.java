/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.BeanFactory} 接口的扩展，由具备自动装配能力的 Bean 工厂
 * 实现，前提是它们希望为现有 Bean 实例暴露此功能。
 *
 * <p>BeanFactory 的这个子接口不应用于普通的应用代码：典型用例请坚持使用
 * {@link org.springframework.beans.factory.BeanFactory} 或
 * {@link org.springframework.beans.factory.ListableBeanFactory}。
 *
 * <p>其他框架的集成代码可以利用此接口来装配和填充 Spring 不控制其生命周期的现有 Bean 实例。例如，
 * 这对 WebWork Action 和 Tapestry Page 对象特别有用。
 *
 * <p>请注意，此接口并非由 {@link org.springframework.context.ApplicationContext} 门面实现，因为应
 * 用代码几乎不会使用它。尽管如此，它也可从应用上下文获取，通过 ApplicationContext 的
 * {@link org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()} 方法访问。
 *
 *
 * <p>你也可以实现 {@link org.springframework.beans.factory.BeanFactoryAware} 接口，即使在
 * ApplicationContext 中运行时也能暴露内部的 BeanFactory，从而获得对 AutowireCapableBeanFactory 的
 * 访问：只需将传入的 BeanFactory 强制转换为 AutowireCapableBeanFactory。
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

	/**
	 * 指示没有外部定义的自动装配的常量。请注意，BeanFactoryAware 等接口以及注解驱动的注入仍会被应用。
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_NO = 0;

	/**
	 * 指示按名称自动装配 Bean 属性（应用于所有 Bean 属性 setter）的常量。
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_NAME = 1;

	/**
	 * 指示按类型自动装配 Bean 属性（应用于所有 Bean 属性 setter）的常量。
	 * @see #createBean
	 * @see #autowire
	 * @see #autowireBeanProperties
	 */
	int AUTOWIRE_BY_TYPE = 2;

	/**
	 * 指示自动装配能够满足的、参数最多的构造方法（涉及解析合适的构造方法）的常量。
	 * @see #createBean
	 * @see #autowire
	 */
	int AUTOWIRE_CONSTRUCTOR = 3;

	/**
	 * 指示通过对 Bean 类进行内省来确定合适的自动装配策略的常量。
	 * @see #createBean
	 * @see #autowire
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	int AUTOWIRE_AUTODETECT = 4;

	/**
	 * 初始化现有 Bean 实例时 "original instance" 约定的后缀：追加到 Bean 类的完全限定名之后，例如
	 * "com.mypackage.MyClass.ORIGINAL"，以强制返回给定的实例，即不使用代理等。
	 * @since 5.1
	 * @see #initializeBean(Object, String)
	 * @see #applyBeanPostProcessorsBeforeInitialization(Object, String)
	 * @see #applyBeanPostProcessorsAfterInitialization(Object, String)
	 */
	String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	/**
	 * 完全创建给定类的新 Bean 实例。<p>对 Bean 执行完整初始化，包括所有适用的
	 * {@link BeanPostProcessor BeanPostProcessors}。<p>注意：这旨在创建全新的实例，填充被标注的字段和
	 * 方法，并应用所有标准的 Bean 初始化回调。它<i>不</i>意味着传统的按名称或按类型的属性自动装配；这
	 * 些用途请使用 {@link #createBean(Class, int, boolean)}。
	 * @param beanClass 要创建的 Bean 的类
	 * @return 新的 Bean 实例
	 * @throws BeansException 如果实例化或装配失败则抛出
	 */
	<T> T createBean(Class<T> beanClass) throws BeansException;

	/**
	 * 通过应用实例化后回调和 Bean 属性后处理（例如用于注解驱动的注入）来填充给定的 Bean 实例。<p>注意：
	 * 这主要用于（重新）填充被标注的字段和方法，无论是新实例还是反序列化的实例。它<i>不</i>意味着传
	 * 统的按名称或按类型的属性自动装配；这些用途请使用 {@link #autowireBeanProperties}。
	 * @param existingBean 现有的 Bean 实例
	 * @throws BeansException 如果装配失败则抛出
	 */
	void autowireBean(Object existingBean) throws BeansException;

	/**
	 * 配置给定的原始 Bean：自动装配 Bean 属性、应用 Bean 属性值、应用工厂回调（如 {@code setBeanName}
	 * 和 {@code setBeanFactory}），并且还应用所有 Bean 后处理器（包括可能包装给定原始 Bean 的后处理器）。
	 * <p>这实际上是 {@link #initializeBean} 所提供功能的超集，会完整应用相应 Bean 定义所指定的配置。
	 * <b>注意：此方法要求给定名称存在对应的 Bean 定义！</b>
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName 该 Bean 的名称，必要时会传递给它
	 * （该名称的 Bean 定义必须可用）
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的 Bean 定义则抛出
	 * @throws BeansException 如果初始化失败则抛出
	 * @see #initializeBean
	 */
	Object configureBean(Object existingBean, String beanName) throws BeansException;


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	/**
	 * 使用指定的自动装配策略完全创建给定类的新 Bean 实例。此处支持此接口中定义的所有常量。<p>对 Bean
	 * 执行完整初始化，包括所有适用的 {@link BeanPostProcessor BeanPostProcessors}。这实际上是
	 * {@link #autowire} 所提供功能的超集，增加了 {@link #initializeBean} 的行为。
	 * @param beanClass 要创建的 Bean 的类
	 * @param autowireMode 按名称还是按类型，使用此接口中的常量
	 * @param dependencyCheck 是否对对象执行依赖检查
	 * （不适用于构造方法自动装配，因此在该处被忽略）
	 * @return 新的 Bean 实例
	 * @throws BeansException 如果实例化或装配失败则抛出
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 */
	Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 使用指定的自动装配策略实例化给定类的新 Bean 实例。此处支持此接口中定义的所有常量。也可以使用
	 * {@code AUTOWIRE_NO} 调用，以便仅应用实例化前回调（例如用于注解驱动的注入）。<p><i>不</i>应用标准
	 * 的 {@link BeanPostProcessor BeanPostProcessors} 回调，也不对 Bean 执行任何进一步的初始化。此接口
	 * 为此类用途提供了独立的细粒度操作，例如 {@link #initializeBean}。不过，如果适用于实例的构造，则会
	 * 应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 * @param beanClass 要实例化的 Bean 的类
	 * @param autowireMode 按名称还是按类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 Bean 实例中的对象引用执行依赖检查
	 * （不适用于构造方法自动装配，因此在该处被忽略）
	 * @return 新的 Bean 实例
	 * @throws BeansException 如果实例化或装配失败则抛出
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #initializeBean
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException;

	/**
	 * 按名称或类型自动装配给定 Bean 实例的 Bean 属性。也可以使用 {@code AUTOWIRE_NO} 调用，以便仅应用
	 * 实例化后回调（例如用于注解驱动的注入）。<p><i>不</i>应用标准的
	 * {@link BeanPostProcessor BeanPostProcessors} 回调，也不对 Bean 执行任何进一步的初始化。此接口为
	 * 此类用途提供了独立的细粒度操作，例如 {@link #initializeBean}。不过，如果适用于实例的配置，则会应
	 * 用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 * @param existingBean 现有的 Bean 实例
	 * @param autowireMode 按名称还是按类型，使用此接口中的常量
	 * @param dependencyCheck 是否对 Bean 实例中的对象引用执行依赖检查
	 * @throws BeansException 如果装配失败则抛出
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_NO
	 */
	void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException;

	/**
	 * 将具有给定名称的 Bean 定义的属性值应用到给定的 Bean 实例。该 Bean 定义既可以定义完全自包含的
	 * Bean（复用其属性值），也可以只定义打算用于现有 Bean 实例的属性值。<p>此方法<i>不</i>自动装配
	 * Bean 属性；它只应用显式定义的属性值。要自动装配现有 Bean 实例，请使用
	 * {@link #autowireBeanProperties} 方法。<b>注意：此方法要求给定名称存在对应的 Bean 定义！</b><p>
	 * <i>不</i>应用标准的 {@link BeanPostProcessor BeanPostProcessors} 回调，也不对 Bean 执行任何进一
	 * 步的初始化。此接口为此类用途提供了独立的细粒度操作，例如 {@link #initializeBean}。不过，如果适用
	 * 于实例的配置，则会应用 {@link InstantiationAwareBeanPostProcessor} 回调。
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName Bean 工厂中 Bean 定义的名称
	 * （该名称的 Bean 定义必须可用）
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException 如果没有给定名称的 Bean 定义则抛出
	 * @throws BeansException 如果应用属性值失败则抛出
	 * @see #autowireBeanProperties
	 */
	void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException;

	/**
	 * 初始化给定的原始 Bean，应用工厂回调（如 {@code setBeanName} 和 {@code setBeanFactory}），还应用
	 * 所有 Bean 后处理器（包括可能包装给定原始 Bean 的后处理器）。<p>请注意，Bean 工厂中不必存在给定名
	 * 称的 Bean 定义。传入的 Bean 名称仅用于回调，而不会与已注册的 Bean 定义进行核对。
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName 该 Bean 的名称，必要时会传递给它
	 * （仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 * 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，
	 * 以强制返回给定的实例，即不返回代理等）
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果初始化失败则抛出
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object initializeBean(Object existingBean, String beanName) throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 Bean 实例，调用它们的
	 * {@code postProcessBeforeInitialization} 方法。返回的 Bean 实例可能是原始实例的包装。
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName 该 Bean 的名称，必要时会传递给它
	 * （仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 * 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，
	 * 以强制返回给定的实例，即不返回代理等）
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败则抛出
	 * @see BeanPostProcessor#postProcessBeforeInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 将 {@link BeanPostProcessor BeanPostProcessors} 应用于给定的现有 Bean 实例，调用它们的
	 * {@code postProcessAfterInitialization} 方法。返回的 Bean 实例可能是原始实例的包装。
	 * @param existingBean 现有的 Bean 实例
	 * @param beanName 该 Bean 的名称，必要时会传递给它
	 * （仅传递给 {@link BeanPostProcessor BeanPostProcessors}；
	 * 可以遵循 {@link #ORIGINAL_INSTANCE_SUFFIX} 约定，
	 * 以强制返回给定的实例，即不返回代理等）
	 * @return 要使用的 Bean 实例，可以是原始实例或包装后的实例
	 * @throws BeansException 如果任何后处理失败则抛出
	 * @see BeanPostProcessor#postProcessAfterInitialization
	 * @see #ORIGINAL_INSTANCE_SUFFIX
	 */
	Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException;

	/**
	 * 销毁给定的 Bean 实例（通常来自 {@link #createBean}），应用
	 * {@link org.springframework.beans.factory.DisposableBean} 契约以及已注册的
	 * {@link DestructionAwareBeanPostProcessor DestructionAwareBeanPostProcessors}。<p>销毁期间出现的
	 * 任何异常都应被捕获并记录，而不是传播给此方法的调用者。
	 * @param existingBean 要销毁的 Bean 实例
	 */
	void destroyBean(Object existingBean);


	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	//-------------------------------------------------------------------------

	/**
	 * 解析唯一匹配给定对象类型的 Bean 实例（如果有），包括其 Bean 名称。<p>这实际上是
	 * {@link #getBean(Class)} 的变体，会保留匹配实例的 Bean 名称。
	 * @param requiredType 该 Bean 必须匹配的类型；可以是接口或超类
	 * @return Bean 名称及 Bean 实例
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 Bean 则抛出
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 Bean 则抛出
	 * @throws BeansException 如果无法创建该 Bean 则抛出
	 * @since 4.3.3
	 * @see #getBean(Class)
	 */
	<T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

	/**
	 * 为给定 Bean 名称解析 Bean 实例，并提供一个依赖描述符以暴露给目标工厂方法。<p>这实际上是
	 * {@link #getBean(String, Class)} 的变体，支持带有
	 * {@link org.springframework.beans.factory.InjectionPoint} 参数的工厂方法。
	 * @param name 要查找的 Bean 的名称
	 * @param descriptor 发起注入点的依赖描述符
	 * @return 对应的 Bean 实例
	 * @throws NoSuchBeanDefinitionException 如果没有指定名称的 Bean 则抛出
	 * @throws BeansException 如果无法创建该 Bean 则抛出
	 * @since 5.1.5
	 * @see #getBean(String, Class)
	 */
	Object resolveBeanByName(String name, DependencyDescriptor descriptor) throws BeansException;

	/**
	 * 针对此工厂中定义的 Bean 解析指定的依赖。
	 * @param descriptor 该依赖（字段/方法/构造方法）的描述符
	 * @param requestingBeanName 声明给定依赖的 Bean 的名称
	 * @return 解析得到的对象；如果未找到则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 Bean 则抛出
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 Bean 则抛出
	 * @throws BeansException 如果因任何其他原因导致依赖解析失败则抛出
	 * @since 2.5
	 * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException;

	/**
	 * 针对此工厂中定义的 Bean 解析指定的依赖。
	 * @param descriptor 该依赖（字段/方法/构造方法）的描述符
	 * @param requestingBeanName 声明给定依赖的 Bean 的名称
	 * @param autowiredBeanNames 用于添加所有自动装配 Bean 名称（用于解析给定依赖）的 Set
	 * @param typeConverter 用于填充数组和集合的 TypeConverter
	 * @return 解析得到的对象；如果未找到则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果未找到匹配的 Bean 则抛出
	 * @throws NoUniqueBeanDefinitionException 如果找到多个匹配的 Bean 则抛出
	 * @throws BeansException 如果因任何其他原因导致依赖解析失败则抛出
	 * @since 2.5
	 * @see DependencyDescriptor
	 */
	@Nullable
	Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException;

}
