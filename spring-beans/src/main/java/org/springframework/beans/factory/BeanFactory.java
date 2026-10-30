/*
 * Copyright 2002-2021 the original author or authors.
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
 * 用于访问 Spring Bean 容器的根接口。
 *
 * <p>这是 Bean 容器的基本客户端视图；{@link ListableBeanFactory} 和
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 等进一步的接口可用于特
 * 定目的。
 *
 * <p>此接口由持有若干 Bean 定义的对象实现，每个 Bean 定义都由一个 String 名称唯一标识。取决于 Bean
 * 定义，工厂将返回所包含对象的独立实例（Prototype 设计模式），或者返回单个共享实例（Singleton 设计
 * 模式的上佳替代，其中该实例是工厂作用域内的单例）。返回哪种类型的实例取决于 Bean 工厂配置：API 是
 * 相同的。自 Spring 2.0 起，根据具体的应用上下文，还可以使用更多作用域（例如 Web 环境中的
 * "request" 和 "session" 作用域）。
 *
 * <p>这种做法的要点在于，BeanFactory 是应用组件的中心注册表，并集中管理应用组件的配置（例如，各个
 * 对象不再需要自行读取属性文件）。关于这种做法的优势的讨论，参见《Expert One-on-One J2EE Design
 * and Development》第 4 章和第 11 章。
 *
 * <p>请注意，通常更好的做法是依靠依赖注入（"push" 式配置）通过 setter 或构造器来配置应用对象，而不
 * 是使用 BeanFactory 查找这类 "pull" 式配置。Spring 的依赖注入功能正是使用此 BeanFactory 接口及其
 * 子接口实现的。
 *
 * <p>通常，BeanFactory 会加载存储在配置源（例如 XML 文档）中的 Bean 定义，并使用
 * {@code org.springframework.beans} 包来配置这些 Bean。不过，实现也可以直接在 Java 代码中按需创建
 * 并返回 Java 对象。对定义的存储方式没有任何限制：LDAP、RDBMS、XML、属性文件等。鼓励实现支持 Bean
 * 之间的引用（依赖注入）。
 *
 * <p>与 {@link ListableBeanFactory} 中的方法不同，如果本接口是 {@link HierarchicalBeanFactory}，那
 * 么此接口中的所有操作还会检查父工厂。如果在此工厂实例中找不到某个 Bean，则会询问直接父工厂。此工
 * 厂实例中的 Bean 应当覆盖任何父工厂中同名的 Bean。
 *
 * <p>Bean 工厂实现应尽可能支持标准的 Bean 生命周期接口。完整的初始化方法集合及其标准顺序为：
 * <ol>
 * <li>BeanNameAware 的 {@code setBeanName}
 * <li>BeanClassLoaderAware 的 {@code setBeanClassLoader}
 * <li>BeanFactoryAware 的 {@code setBeanFactory}
 * <li>EnvironmentAware 的 {@code setEnvironment}
 * <li>EmbeddedValueResolverAware 的 {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware 的 {@code setResourceLoader}
 * （仅在应用上下文中运行时适用）
 * <li>ApplicationEventPublisherAware 的 {@code setApplicationEventPublisher}
 * （仅在应用上下文中运行时适用）
 * <li>MessageSourceAware 的 {@code setMessageSource}
 * （仅在应用上下文中运行时适用）
 * <li>ApplicationContextAware 的 {@code setApplicationContext}
 * （仅在应用上下文中运行时适用）
 * <li>ServletContextAware 的 {@code setServletContext}
 * （仅在 Web 应用上下文中运行时适用）
 * <li>BeanPostProcessor 的 {@code postProcessBeforeInitialization} 方法
 * <li>InitializingBean 的 {@code afterPropertiesSet}
 * <li>自定义的 {@code init-method} 定义
 * <li>BeanPostProcessor 的 {@code postProcessAfterInitialization} 方法
 * </ol>
 *
 * <p>在 Bean 工厂关闭时，适用以下生命周期方法：
 * <ol>
 * <li>DestructionAwareBeanPostProcessor 的 {@code postProcessBeforeDestruction} 方法
 * <li>DisposableBean 的 {@code destroy}
 * <li>自定义的 {@code destroy-method} 定义
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13 April 2001
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 */
public interface BeanFactory {

	/**
	 * 用于解引用 {@link FactoryBean} 实例，并将其与由该 FactoryBean <i>创建</i>的 Bean 区分开。例如，
	 * 如果名为 {@code myJndiObject} 的 Bean 是一个 FactoryBean，那么获取 {@code &myJndiObject} 将返回
	 * 该工厂本身，而不是该工厂返回的实例。
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 返回指定 Bean 的一个实例，该实例可能是共享的，也可能是独立的。<p>此方法允许将 Spring BeanFactory
	 * 用作 Singleton 或 Prototype 设计模式的替代。对于 Singleton Bean，调用方可以保留对返回对象的引用。
	 * <p>将别名转换回对应的规范 Bean 名称。<p>如果在此工厂实例中找不到该 Bean，将询问父工厂。
	 * @param name 要获取的 Bean 名称
	 * @return Bean 的实例
	 * @throws NoSuchBeanDefinitionException 如果没有指定名称的 Bean
	 * @throws BeansException 如果无法获取该 Bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 返回指定 Bean 的一个实例，该实例可能是共享的，也可能是独立的。<p>行为与 {@link #getBean(String)}
	 * 相同，但提供了一定的类型安全性：如果 Bean 不是所需类型，则抛出 BeanNotOfRequiredTypeException。
	 * 这意味着在对结果进行正确转换时不会抛出 ClassCastException，而在使用 {@link #getBean(String)} 时
	 * 可能会发生这种情况。<p>将别名转换回对应的规范 Bean 名称。<p>如果在此工厂实例中找不到该 Bean，将
	 * 询问父工厂。
	 * @param name 要获取的 Bean 名称
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @return Bean 的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的 Bean 定义
	 * @throws BeanNotOfRequiredTypeException 如果该 Bean 不是所需的类型
	 * @throws BeansException 如果无法创建该 Bean
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定 Bean 的一个实例，该实例可能是共享的，也可能是独立的。<p>允许指定显式的构造器参数 / 工厂
	 * 方法参数，覆盖 Bean 定义中指定的默认参数（如果有）。
	 * @param name 要获取的 Bean 名称
	 * @param args 在使用显式参数创建 Bean 实例时使用的参数
	 * （仅在创建新实例时适用，而非获取已有实例）
	 * @return Bean 的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的 Bean 定义
	 * @throws BeanDefinitionStoreException 如果给定了参数但受影响的 Bean 不是原型
	 * @throws BeansException 如果无法创建该 Bean
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 返回唯一匹配给定对象类型的 Bean 实例（如果有）。<p>此方法进入了 {@link ListableBeanFactory} 按类
	 * 型查找的范畴，但也可以转换为基于给定类型名称的常规按名称查找。对于跨 Bean 集合的更广泛的检索操作，
	 * 请使用 {@link ListableBeanFactory} 和/或 {@link BeanFactoryUtils}。
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @return 与所需类型匹配的唯一 Bean 实例
	 * @throws NoSuchBeanDefinitionException 如果未找到给定类型的 Bean
	 * @throws NoUniqueBeanDefinitionException 如果找到多个给定类型的 Bean
	 * @throws BeansException 如果无法创建该 Bean
	 * @since 3.0
	 * @see ListableBeanFactory
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 返回指定 Bean 的一个实例，该实例可能是共享的，也可能是独立的。<p>允许指定显式的构造器参数 / 工厂
	 * 方法参数，覆盖 Bean 定义中指定的默认参数（如果有）。<p>此方法进入了 {@link ListableBeanFactory}
	 * 按类型查找的范畴，但也可以转换为基于给定类型名称的常规按名称查找。对于跨 Bean 集合的更广泛的检索
	 * 操作，请使用 {@link ListableBeanFactory} 和/或 {@link BeanFactoryUtils}。
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @param args 在使用显式参数创建 Bean 实例时使用的参数
	 * （仅在创建新实例时适用，而非获取已有实例）
	 * @return Bean 的实例
	 * @throws NoSuchBeanDefinitionException 如果没有这样的 Bean 定义
	 * @throws BeanDefinitionStoreException 如果给定了参数但受影响的 Bean 不是原型
	 * @throws BeansException 如果无法创建该 Bean
	 * @since 4.1
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。<p>要匹配泛型类型，请考虑
	 * 使用 {@link #getBeanProvider(ResolvableType)}。
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @return 对应的提供者句柄
	 * @since 5.1
	 * @see #getBeanProvider(ResolvableType)
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。此变体允许指定要匹配的泛型
	 * 类型，类似于方法/构造器参数中带有泛型类型声明的反射式注入点。<p>请注意，与反射式注入点不同，此处
	 * 不支持 Bean 的集合。要以编程方式检索匹配特定类型的 Bean 列表，请在此处将实际的 Bean 类型作为参数
	 * 指定，然后使用 {@link ObjectProvider#orderedStream()} 或其延迟流式/迭代选项。<p>此外，这里的泛型
	 * 匹配是严格的，遵循 Java 赋值规则。对于具有未检查语义的宽松回退匹配（类似于 ´unchecked´ Java 编译
	 * 器警告），如果此变体下没有可用的完整泛型匹配 {@link ObjectProvider#getIfAvailable()}，可考虑第二
	 * 步以原始类型调用 {@link #getBeanProvider(Class)}。
	 * @return 对应的提供者句柄
	 * @param requiredType Bean 必须匹配的类型；可以是泛型类型声明
	 * @since 5.1
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * 此 Bean 工厂是否包含具有给定名称的 Bean 定义或外部注册的单例实例？<p>如果给定名称是别名，将转换
	 * 回对应的规范 Bean 名称。<p>如果此工厂是分层的，当在此工厂实例中找不到该 Bean 时，将询问任何父工
	 * 厂。<p>如果找到与给定名称匹配的 Bean 定义或单例实例，此方法将返回 {@code true}，无论该命名 Bean
	 * 定义是具体还是抽象的、是延迟还是急切的、是否在作用域内。因此请注意，此方法返回 {@code true} 并不
	 * 一定表示 {@link #getBean} 能够为同一名称获取实例。
	 * @param name 要查询的 Bean 名称
	 * @return 是否存在具有给定名称的 Bean
	 */
	boolean containsBean(String name);

	/**
	 * 此 Bean 是共享单例吗？也就是说，{@link #getBean} 是否总是返回同一实例？<p>注意：此方法返回
	 * {@code false} 并不能明确表示是独立实例。它表示非单例实例，也可能对应作用域 Bean。请使用
	 * {@link #isPrototype} 操作显式检查独立实例。<p>将别名转换回对应的规范 Bean 名称。<p>如果在此工厂
	 * 实例中找不到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @return 该 Bean 是否对应单例实例
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @see #getBean
	 * @see #isPrototype
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 此 Bean 是原型吗？也就是说，{@link #getBean} 是否总是返回独立实例？<p>注意：此方法返回
	 * {@code false} 并不能明确表示是单例对象。它表示非独立实例，也可能对应作用域 Bean。请使用
	 * {@link #isSingleton} 操作显式检查共享单例实例。<p>将别名转换回对应的规范 Bean 名称。<p>如果在此
	 * 工厂实例中找不到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @return 该 Bean 是否始终提供独立实例
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 2.0.3
	 * @see #getBean
	 * @see #isSingleton
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的 Bean 是否匹配指定类型。更具体地说，检查对给定名称的 {@link #getBean} 调用是否
	 * 会返回可赋值给指定目标类型的对象。<p>将别名转换回对应的规范 Bean 名称。<p>如果在此工厂实例中找不
	 * 到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @param typeToMatch 要匹配的类型（作为 {@code ResolvableType}）
	 * @return 如果 Bean 类型匹配则返回 {@code true}，
	 * 不匹配或尚无法确定则返回 {@code false}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 4.2
	 * @see #getBean
	 * @see #getType
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 检查具有给定名称的 Bean 是否匹配指定类型。更具体地说，检查对给定名称的 {@link #getBean} 调用是否
	 * 会返回可赋值给指定目标类型的对象。<p>将别名转换回对应的规范 Bean 名称。<p>如果在此工厂实例中找不
	 * 到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @param typeToMatch 要匹配的类型（作为 {@code Class}）
	 * @return 如果 Bean 类型匹配则返回 {@code true}，
	 * 不匹配或尚无法确定则返回 {@code false}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 2.0.1
	 * @see #getBean
	 * @see #getType
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的 Bean 的类型。更具体地说，确定 {@link #getBean} 会为给定名称返回的对象类型。
	 * <p>对于 {@link FactoryBean}，返回该 FactoryBean 所创建对象的类型，如
	 * {@link FactoryBean#getObjectType()} 所公开的。这可能会导致先前未初始化的 {@code FactoryBean} 被
	 * 初始化（参见 {@link #getType(String, boolean)}）。<p>将别名转换回对应的规范 Bean 名称。<p>如果在
	 * 此工厂实例中找不到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @return Bean 的类型；若无法确定为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 1.1.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 确定具有给定名称的 Bean 的类型。更具体地说，确定 {@link #getBean} 会为给定名称返回的对象类型。
	 * <p>对于 {@link FactoryBean}，返回该 FactoryBean 所创建对象的类型，如
	 * {@link FactoryBean#getObjectType()} 所公开的。根据 {@code allowFactoryBeanInit} 标志，如果没有可
	 * 用的早期类型信息，这可能会导致先前未初始化的 {@code FactoryBean} 被初始化。<p>将别名转换回对应的
	 * 规范 Bean 名称。<p>如果在此工厂实例中找不到该 Bean，将询问父工厂。
	 * @param name 要查询的 Bean 名称
	 * @param allowFactoryBeanInit 是否允许仅为确定对象类型而初始化 {@code FactoryBean}
	 * @return Bean 的类型；若无法确定为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 5.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定 Bean 名称的别名（如果有）。<p>在 {@link #getBean} 调用中使用时，所有这些别名都指向同一
	 * 个 Bean。<p>如果给定名称是别名，将返回对应的原始 Bean 名称和其他别名（如果有），其中原始 Bean 名
	 * 称是数组中的第一个元素。<p>如果在此工厂实例中找不到该 Bean，将询问父工厂。
	 * @param name 要检查别名的 Bean 名称
	 * @return 别名；若没有则为空数组
	 * @see #getBean
	 */
	String[] getAliases(String name);

}
