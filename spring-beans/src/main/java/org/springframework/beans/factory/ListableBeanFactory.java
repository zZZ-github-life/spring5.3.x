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

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * {@link BeanFactory} 接口的扩展，由能够枚举其所有 Bean 实例的 Bean 工厂实现，而不是按客户端请求逐
 * 个按名称查找 Bean。预加载其所有 Bean 定义的 BeanFactory 实现（例如基于 XML 的工厂）可以实现此接
 * 口。
 *
 * <p>如果这是 {@link HierarchicalBeanFactory}，返回值<i>不会</i>考虑任何 BeanFactory 层次结构，而
 * 只与当前工厂中定义的 Bean 相关。要同时考虑祖先工厂中的 Bean，请使用 {@link BeanFactoryUtils} 辅
 * 助类。
 *
 * <p>此接口中的方法仅遵循本工厂的 Bean 定义。它们将忽略通过其他方式注册的任何单例 Bean，例如
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 的
 * {@code registerSingleton} 方法，但 {@code getBeanNamesForType} 和 {@code getBeansOfType} 例外，
 * 它们也会检查此类手动注册的单例。当然，BeanFactory 的 {@code getBean} 也允许透明访问此类特殊 Bean。
 * 不过，在典型场景中，所有 Bean 反正都会由外部 Bean 定义来定义，因此大多数应用无需担心这种区分。
 *
 * <p><b>注意：</b>除 {@code getBeanDefinitionCount} 和 {@code containsBeanDefinition} 之外，此接口
 * 中的方法并非为频繁调用而设计。实现可能很慢。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16 April 2001
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * 检查此 Bean 工厂是否包含具有给定名称的 Bean 定义。<p>不考虑此工厂可能参与的任何层次结构，并忽略
	 * 通过 Bean 定义以外的方式注册的任何单例 Bean。
	 * @param beanName 要查找的 Bean 名称
	 * @return 此 Bean 工厂是否包含具有给定名称的 Bean 定义
	 * @see #containsBean
	 *
	 * 仅检查当前 BeanFactory 中是否存在显式声明的、名为 beanName 的 Bean 定义，
	 *
	 * 不查父容器，也不认手动注册的单例对象
	 * 有些 bean 是直接以实例形式注册到容器中的，比如：applicationContext.getBeanFactory().registerSingleton("myBean", new MyBean());
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回工厂中定义的 Bean 数量。<p>不考虑此工厂可能参与的任何层次结构，并忽略通过 Bean 定义以外的方
	 * 式注册的任何单例 Bean。
	 * @return 工厂中定义的 Bean 数量
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有 Bean 的名称。<p>不考虑此工厂可能参与的任何层次结构，并忽略通过 Bean 定义
	 * 以外的方式注册的任何单例 Bean。
	 * @return 此工厂中定义的所有 Bean 名称；若未定义则为空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。
	 * @param requiredType Bean 必须匹配的类型；可以是接口或超类
	 * @param allowEagerInit 基于流的访问是否可以为了类型检查而初始化
	 * <i>lazy-init singletons</i> 和 <i>由 FactoryBeans 创建的对象</i>
	 * （或通过带有 "factory-bean" 引用的工厂方法创建的对象）
	 * @return 对应的提供者句柄
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。
	 * @param requiredType Bean 必须匹配的类型；可以是泛型类型声明。
	 * 注意，与反射注入点不同，此处不支持集合类型。
	 * 若要以编程方式获取匹配特定类型的 Bean 列表，请在此处指定实际的 Bean 类型，
	 * 随后使用 {@link ObjectProvider#orderedStream()} 或其惰性流式/迭代选项。
	 * @param allowEagerInit 基于流的访问是否可以为了类型检查而初始化
	 * <i>lazy-init singletons</i> 和 <i>由 FactoryBeans 创建的对象</i>
	 * （或通过带有 "factory-bean" 引用的工厂方法创建的对象）
	 * @return 对应的提供者句柄
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

	/**
	 * 返回与给定类型（包括子类）匹配的 Bean 的名称，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如
	 * 果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>不考虑此工厂可
	 * 能参与的任何层次结构。使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 可将祖
	 * 先工厂中的 Bean 也包含进来。<p>注意：<i>不会</i>忽略通过 Bean 定义以外的方式注册的单例 Bean。<p>
	 * 此版本的 {@code getBeanNamesForType} 匹配所有类型的 Bean，无论是单例、原型还是 FactoryBean。在大
	 * 多数实现中，结果与 {@code getBeanNamesForType(type, true, true)} 相同。<p>此方法返回的 Bean 名称
	 * 应尽可能始终按后端配置中的<i>定义顺序</i>返回。
	 * @param type 要匹配的泛型类或接口
	 * @return 与给定对象类型（包括子类）匹配的 Bean（或由 FactoryBeans 创建的对象）名称；
	 * 若没有则为空数组
	 * @since 4.2
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * 返回与给定类型（包括子类）匹配的 Bean 的名称，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，
	 * 这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本
	 * 身与该类型进行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个
	 * FactoryBean）。<p>不考虑此工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的
	 * {@code beanNamesForTypeIncludingAncestors} 可将祖先工厂中的 Bean 也包含进来。<p>注意：<i>不会
	 * </i>忽略通过 Bean 定义以外的方式注册的单例 Bean。<p>此方法返回的 Bean 名称应尽可能始终按后端配置
	 * 中的<i>定义顺序</i>返回。
	 * @param type 要匹配的泛型类或接口
	 * @param includeNonSingletons 是否也包含原型或作用域 Bean，还是仅包含单例（同样适用于 FactoryBeans）
	 * @param allowEagerInit 是否为了类型检查而初始化 <i>lazy-init singletons</i> 和
	 * <i>由 FactoryBeans 创建的对象</i>（或通过带有 "factory-bean" 引用的工厂方法创建的对象）。
	 * 注意，FactoryBeans 需要被急切初始化才能确定其类型：因此为此标志传入 "true"
	 * 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 * @return 与给定对象类型（包括子类）匹配的 Bean（或由 FactoryBeans 创建的对象）名称；
	 * 若没有则为空数组
	 * @since 5.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType, boolean, boolean)
	 */
	String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定类型（包括子类）匹配的 Bean 的名称，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如
	 * 果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>不考虑此工厂可
	 * 能参与的任何层次结构。使用 BeanFactoryUtils 的 {@code beanNamesForTypeIncludingAncestors} 可将祖
	 * 先工厂中的 Bean 也包含进来。<p>注意：<i>不会</i>忽略通过 Bean 定义以外的方式注册的单例 Bean。<p>
	 * 此版本的 {@code getBeanNamesForType} 匹配所有类型的 Bean，无论是单例、原型还是 FactoryBean。在大
	 * 多数实现中，结果与 {@code getBeanNamesForType(type, true, true)} 相同。<p>此方法返回的 Bean 名称
	 * 应尽可能始终按后端配置中的<i>定义顺序</i>返回。
	 * @param type 要匹配的类或接口；若为 {@code null} 则匹配所有 Bean 名称
	 * @return 与给定对象类型（包括子类）匹配的 Bean（或由 FactoryBeans 创建的对象）名称；
	 * 若没有则为空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type);

	/**
	 * 返回与给定类型（包括子类）匹配的 Bean 的名称，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，
	 * 这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本
	 * 身与该类型进行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个
	 * FactoryBean）。<p>不考虑此工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的
	 * {@code beanNamesForTypeIncludingAncestors} 可将祖先工厂中的 Bean 也包含进来。<p>注意：<i>不会
	 * </i>忽略通过 Bean 定义以外的方式注册的单例 Bean。<p>此方法返回的 Bean 名称应尽可能始终按后端配置
	 * 中的<i>定义顺序</i>返回。
	 * @param type 要匹配的类或接口；若为 {@code null} 则匹配所有 Bean 名称
	 * @param includeNonSingletons 是否也包含原型或作用域 Bean，还是仅包含单例（同样适用于 FactoryBeans）
	 * @param allowEagerInit 是否为了类型检查而初始化 <i>lazy-init singletons</i> 和
	 * <i>由 FactoryBeans 创建的对象</i>（或通过带有 "factory-bean" 引用的工厂方法创建的对象）。
	 * 注意，FactoryBeans 需要被急切初始化才能确定其类型：因此为此标志传入 "true"
	 * 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 * @return 与给定对象类型（包括子类）匹配的 Bean（或由 FactoryBeans 创建的对象）名称；
	 * 若没有则为空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * 返回与给定对象类型（包括子类）匹配的 Bean 实例，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如
	 * 果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>不考虑此工厂可
	 * 能参与的任何层次结构。使用 BeanFactoryUtils 的 {@code beansOfTypeIncludingAncestors} 可将祖先工
	 * 厂中的 Bean 也包含进来。<p>注意：<i>不会</i>忽略通过 Bean 定义以外的方式注册的单例 Bean。<p>此版
	 * 本的 getBeansOfType 匹配所有类型的 Bean，无论是单例、原型还是 FactoryBean。在大多数实现中，结果
	 * 与 {@code getBeansOfType(type, true, true)} 相同。<p>此方法返回的 Map 应尽可能始终按后端配置中的
	 * <i>定义顺序</i>返回 Bean 名称和对应的 Bean 实例。
	 * @param type 要匹配的类或接口；若为 {@code null} 则匹配所有具体 Bean
	 * @return 包含匹配 Bean 的 Map，以 Bean 名称为键，以对应的 Bean 实例为值
	 * @throws BeansException 如果某个 Bean 无法创建
	 * @since 1.1.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;

	/**
	 * 返回与给定对象类型（包括子类）匹配的 Bean 实例，对于 FactoryBean，则根据 Bean 定义或
	 * {@code getObjectType} 的值来判断。<p><b>注意：此方法仅内省顶层 Bean。</b>它<i>不会</i>检查可能也
	 * 匹配指定类型的嵌套 Bean。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，
	 * 这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本
	 * 身与该类型进行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个
	 * FactoryBean）。<p>不考虑此工厂可能参与的任何层次结构。使用 BeanFactoryUtils 的
	 * {@code beansOfTypeIncludingAncestors} 可将祖先工厂中的 Bean 也包含进来。<p>注意：<i>不会</i>忽略
	 * 通过 Bean 定义以外的方式注册的单例 Bean。<p>此方法返回的 Map 应尽可能始终按后端配置中的<i>定义顺
	 * 序</i>返回 Bean 名称和对应的 Bean 实例。
	 * @param type 要匹配的类或接口；若为 {@code null} 则匹配所有具体 Bean
	 * @param includeNonSingletons 是否也包含原型或作用域 Bean，还是仅包含单例（同样适用于 FactoryBeans）
	 * @param allowEagerInit 是否为了类型检查而初始化 <i>lazy-init singletons</i> 和
	 * <i>由 FactoryBeans 创建的对象</i>（或通过带有 "factory-bean" 引用的工厂方法创建的对象）。
	 * 注意，FactoryBeans 需要被急切初始化才能确定其类型：因此为此标志传入 "true"
	 * 将初始化 FactoryBeans 和 "factory-bean" 引用。
	 * @return 包含匹配 Bean 的 Map，以 Bean 名称为键，以对应的 Bean 实例为值
	 * @throws BeansException 如果某个 Bean 无法创建
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 查找所有以所提供的 {@link Annotation} 类型注解的 Bean 的名称，且不创建相应的 Bean 实例。<p>请注
	 * 意，此方法会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化，以确定其对象类型。
	 * @param annotationType 要查找的注解类型（在指定 Bean 的类、接口或工厂方法级别）
	 * @return 所有匹配的 Bean 名称
	 * @since 4.0
	 * @see #findAnnotationOnBean
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * 查找所有以所提供的 {@link Annotation} 类型注解的 Bean，返回 Bean 名称与对应 Bean 实例的 Map。<p>
	 * 请注意，此方法会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化，以确定其对象类型。
	 *
	 * @param annotationType 要查找的注解类型（在指定 Bean 的类、接口或工厂方法级别）
	 * @return 包含匹配 Bean 的 Map，以 Bean 名称为键，以对应的 Bean 实例为值
	 * @throws BeansException 如果某个 Bean 无法创建
	 * @since 3.0
	 * @see #findAnnotationOnBean
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 在指定 Bean 上查找 {@code annotationType} 的 {@link Annotation}；如果在给定类本身上找不到注解，
	 * 则遍历其接口和超类，同时还会检查该 Bean 的工厂方法（如果有）。
	 * @param beanName 要在其上查找注解的 Bean 名称
	 * @param annotationType 要查找的注解类型（在指定 Bean 的类、接口或工厂方法级别）
	 * @return 找到的给定类型注解；否则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 3.0
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 * @see #getType(String)
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

	/**
	 * 在指定 Bean 上查找 {@code annotationType} 的 {@link Annotation}；如果在给定类本身上找不到注解，
	 * 则遍历其接口和超类，同时还会检查该 Bean 的工厂方法（如果有）。
	 * @param beanName 要在其上查找注解的 Bean 名称
	 * @param annotationType 要查找的注解类型（在指定 Bean 的类、接口或工厂方法级别）
	 * @param allowFactoryBeanInit 是否允许仅为确定对象类型而初始化 {@code FactoryBean}
	 * @return 找到的给定类型注解；否则为 {@code null}
	 * @throws NoSuchBeanDefinitionException 如果没有具有给定名称的 Bean
	 * @since 5.3.14
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 * @see #getType(String, boolean)
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException;

}
