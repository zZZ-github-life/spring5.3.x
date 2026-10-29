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
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
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
	 * @return the number of beans defined in the factory
	 */
	int getBeanDefinitionCount();

	/**
	 * 返回此工厂中定义的所有 Bean 的名称。<p>不考虑此工厂可能参与的任何层次结构，并忽略通过 Bean 定义
	 * 以外的方式注册的任何单例 Bean。
	 * @return the names of all beans defined in this factory,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 * with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
	 * @since 5.3
	 * @see #getBeanProvider(ResolvableType, boolean)
	 * @see #getBeanProvider(Class)
	 * @see #getBeansOfType(Class, boolean, boolean)
	 * @see #getBeanNamesForType(Class, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

	/**
	 * 返回指定 Bean 的提供者，允许按需延迟检索实例，包括可用性和唯一性选项。
	 * @param requiredType type the bean must match; can be a generic type declaration.
	 * Note that collection types are not supported here, in contrast to reflective
	 * injection points. For programmatically retrieving a list of beans matching a
	 * specific type, specify the actual bean type as an argument here and subsequently
	 * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
	 * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
	 * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
	 * with a "factory-bean" reference) for the type check
	 * @return a corresponding provider handle
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
	 * @param type the generically typed class or interface to match
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
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
	 * @param type the generically typed class or interface to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
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
	 * @param type the class or interface to match, or {@code null} for all bean names
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
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
	 * @param type the class or interface to match, or {@code null} for all bean names
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
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
	 * @param type the class or interface to match, or {@code null} for all concrete beans
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
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
	 * @param type the class or interface to match, or {@code null} for all concrete beans
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * 查找所有以所提供的 {@link Annotation} 类型注解的 Bean 的名称，且不创建相应的 Bean 实例。<p>请注
	 * 意，此方法会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化，以确定其对象类型。
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the names of all matching beans
	 * @since 4.0
	 * @see #findAnnotationOnBean
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * 查找所有以所提供的 {@link Annotation} 类型注解的 Bean，返回 Bean 名称与对应 Bean 实例的 Map。<p>
	 * 请注意，此方法会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化，以确定其对象类型。
	 *
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @since 3.0
	 * @see #findAnnotationOnBean
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * 在指定 Bean 上查找 {@code annotationType} 的 {@link Annotation}；如果在给定类本身上找不到注解，
	 * 则遍历其接口和超类，同时还会检查该 Bean 的工厂方法（如果有）。
	 * @param beanName the name of the bean to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the annotation of the given type if found, or {@code null} otherwise
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
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
	 * @param beanName the name of the bean to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @param allowFactoryBeanInit whether a {@code FactoryBean} may get initialized
	 * just for the purpose of determining its object type
	 * @return the annotation of the given type if found, or {@code null} otherwise
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
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
