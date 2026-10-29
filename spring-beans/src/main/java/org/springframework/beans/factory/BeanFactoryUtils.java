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

package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于操作 Bean 工厂的便捷方法，特别是针对 {@link ListableBeanFactory} 接口。
 *
 * <p>返回 Bean 数量、Bean 名称或 Bean 实例，并考虑 Bean 工厂的嵌套层次结构（ListableBeanFactory 接
 * 口上定义的方法不考虑这一点，这与 BeanFactory 接口上定义的方法相反）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2003
 */
public abstract class BeanFactoryUtils {

	/**
	 * 生成的 Bean 名称的分隔符。如果类名或父名称不唯一，将附加 "#1"、"#2" 等，直到名称唯一为止。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

	/**
	 * 从带工厂 Bean 前缀的名称到去除解引用后的名称的缓存。
	 * @since 5.1
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	private static final Map<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();


	/**
	 * 返回给定名称是否为工厂解引用（以工厂解引用前缀开头）。
	 * @param name the name of the bean
	 * @return whether the given name is a factory dereference
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static boolean isFactoryDereference(@Nullable String name) {
		return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
	}

	/**
	 * 返回实际的 Bean 名称，去除工厂解引用前缀（如果有，若发现重复的工厂前缀也一并去除）。
	 * @param name the name of the bean
	 * @return the transformed name
	 * @see BeanFactory#FACTORY_BEAN_PREFIX
	 */
	public static String transformedBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
			return name;
		}
		return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
			do {
				beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
			}
			while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
			return beanName;
		});
	}

	/**
	 * 返回给定名称是否为默认命名策略生成的 Bean 名称（包含 "#..." 部分）。
	 * @param name the name of the bean
	 * @return whether the given name is a generated bean name
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#generateBeanName
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
	 */
	public static boolean isGeneratedBeanName(@Nullable String name) {
		return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
	}

	/**
	 * 从给定（可能是生成的）Bean 名称中提取 "raw" Bean 名称，排除任何可能为唯一性而添加的 "#..." 后缀。
	 *
	 * @param name the potentially generated bean name
	 * @return the raw bean name
	 * @see #GENERATED_BEAN_NAME_SEPARATOR
	 */
	public static String originalBeanName(String name) {
		Assert.notNull(name, "'name' must not be null");
		int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
		return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
	}


	// Retrieval of bean names

	/**
	 * 统计此工厂参与的任何层次结构中的所有 Bean。包括对祖先 Bean 工厂的统计。<p>"被覆盖"的 Bean（在子
	 * 工厂中以相同名称指定）只统计一次。
	 * @param lbf the bean factory
	 * @return count of beans including those defined in ancestor factories
	 * @see #beanNamesIncludingAncestors
	 */
	public static int countBeansIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesIncludingAncestors(lbf).length;
	}

	/**
	 * 返回工厂中的所有 Bean 名称，包括祖先工厂。
	 * @param lbf the bean factory
	 * @return the array of matching bean names, or an empty array if none
	 * @see #beanNamesForTypeIncludingAncestors
	 */
	public static String[] beanNamesIncludingAncestors(ListableBeanFactory lbf) {
		return beanNamesForTypeIncludingAncestors(lbf, Object.class);
	}

	/**
	 * 获取给定类型的所有 Bean 名称，包括在祖先工厂中定义的那些。在 Bean 定义被覆盖的情况下将返回唯一的
	 * 名称。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建
	 * 的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>此版本的
	 * {@code beanNamesForTypeIncludingAncestors} 自动包含原型和 FactoryBean。
	 * @param lbf the bean factory
	 * @param type the type that beans must match (as a {@code ResolvableType})
	 * @return the array of matching bean names, or an empty array if none
	 * @since 4.2
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, ResolvableType type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有 Bean 名称，包括在祖先工厂中定义的那些。在 Bean 定义被覆盖的情况下将返回唯一的
	 * 名称。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，这意味着
	 * FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进
	 * 行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个 FactoryBean）。
	 *
	 * @param lbf the bean factory
	 * @param type the type that beans must match (as a {@code ResolvableType})
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the array of matching bean names, or an empty array if none
	 * @since 5.2
	 * @see ListableBeanFactory#getBeanNamesForType(ResolvableType, boolean, boolean)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(
			ListableBeanFactory lbf, ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有 Bean 名称，包括在祖先工厂中定义的那些。在 Bean 定义被覆盖的情况下将返回唯一的
	 * 名称。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建
	 * 的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>此版本的
	 * {@code beanNamesForTypeIncludingAncestors} 自动包含原型和 FactoryBean。
	 * @param lbf the bean factory
	 * @param type the type that beans must match (as a {@code Class})
	 * @return the array of matching bean names, or an empty array if none
	 * @see ListableBeanFactory#getBeanNamesForType(Class)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(ListableBeanFactory lbf, Class<?> type) {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取给定类型的所有 Bean 名称，包括在祖先工厂中定义的那些。在 Bean 定义被覆盖的情况下将返回唯一的
	 * 名称。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，这意味着
	 * FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进
	 * 行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个 FactoryBean）。
	 *
	 * @param lbf the bean factory
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @param type the type that beans must match
	 * @return the array of matching bean names, or an empty array if none
	 * @see ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
	 */
	public static String[] beanNamesForTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}

	/**
	 * 获取其 {@code Class} 具有所提供的 {@link Annotation} 类型的所有 Bean 名称，包括在祖先工厂中定义
	 * 的那些，且不创建任何 Bean 实例。在 Bean 定义被覆盖的情况下将返回唯一的名称。
	 * @param lbf the bean factory
	 * @param annotationType the type of annotation to look for
	 * @return the array of matching bean names, or an empty array if none
	 * @since 5.0
	 * @see ListableBeanFactory#getBeanNamesForAnnotation(Class)
	 */
	public static String[] beanNamesForAnnotationIncludingAncestors(
			ListableBeanFactory lbf, Class<? extends Annotation> annotationType) {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		String[] result = lbf.getBeanNamesForAnnotation(annotationType);
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				String[] parentResult = beanNamesForAnnotationIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), annotationType);
				result = mergeNamesWithParent(result, parentResult, hbf);
			}
		}
		return result;
	}


	// Retrieval of bean instances

	/**
	 * 返回给定类型或其子类型的所有 Bean；如果当前 Bean 工厂是 HierarchicalBeanFactory，还会拾取在祖先
	 * Bean 工厂中定义的 Bean。返回的 Map 将只包含此类型的 Bean。<p>会考虑由 FactoryBean 创建的对象，这
	 * 意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与
	 * 该类型进行匹配。<p><b>注意：同名 Bean 将在“最低”工厂层级上优先，即此类 Bean 将从找到它们的最低工
	 * 厂返回，并隐藏祖先工厂中的对应 Bean。</b>此特性允许通过在子工厂中显式选择相同的 Bean 名称来“替换”
	 * Bean；此时祖先工厂中的该 Bean 将不可见，即使按类型查找也不可见。
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @return the Map of matching bean instances, or an empty Map if none
	 * @throws BeansException if a bean could not be created
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或其子类型的所有 Bean；如果当前 Bean 工厂是 HierarchicalBeanFactory，还会拾取在祖先
	 * Bean 工厂中定义的 Bean。返回的 Map 将只包含此类型的 Bean。<p>如果设置了 "allowEagerInit" 标志，
	 * 则会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象
	 * 不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。如果未设置 "allowEagerInit"，则只检查原始
	 * FactoryBean（这不需要初始化每个 FactoryBean）。<p><b>注意：同名 Bean 将在“最低”工厂层级上优先，
	 * 即此类 Bean 将从找到它们的最低工厂返回，并隐藏祖先工厂中的对应 Bean。</b>此特性允许通过在子工厂
	 * 中显式选择相同的 Bean 名称来“替换”Bean；此时祖先工厂中的该 Bean 将不可见，即使按类型查找也不可见。
	 *
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the Map of matching bean instances, or an empty Map if none
	 * @throws BeansException if a bean could not be created
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> Map<String, T> beansOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> result = new LinkedHashMap<>(4);
		result.putAll(lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit));
		if (lbf instanceof HierarchicalBeanFactory) {
			HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
			if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
				Map<String, T> parentResult = beansOfTypeIncludingAncestors(
						(ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
				parentResult.forEach((beanName, beanInstance) -> {
					if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
						result.put(beanName, beanInstance);
					}
				});
			}
		}
		return result;
	}

	/**
	 * 返回给定类型或其子类型的单个 Bean；如果当前 Bean 工厂是 HierarchicalBeanFactory，还会拾取在祖先
	 * Bean 工厂中定义的 Bean。当我们期望单个 Bean 且不关心 Bean 名称时，这是一个有用的便捷方法。<p>会
	 * 考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹
	 * 配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>此版本的
	 * {@code beanOfTypeIncludingAncestors} 自动包含原型和 FactoryBean。<p><b>注意：同名 Bean 将在“最低”
	 * 工厂层级上优先，即此类 Bean 将从找到它们的最低工厂返回，并隐藏祖先工厂中的对应 Bean。</b>此特性
	 * 允许通过在子工厂中显式选择相同的 Bean 名称来“替换”Bean；此时祖先工厂中的该 Bean 将不可见，即使按
	 * 类型查找也不可见。
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @return the matching bean instance
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException if the bean could not be created
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T beanOfTypeIncludingAncestors(ListableBeanFactory lbf, Class<T> type)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或其子类型的单个 Bean；如果当前 Bean 工厂是 HierarchicalBeanFactory，还会拾取在祖先
	 * Bean 工厂中定义的 Bean。当我们期望单个 Bean 且不关心 Bean 名称时，这是一个有用的便捷方法。<p>如
	 * 果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始
	 * 化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。如果未设置
	 * "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个 FactoryBean）。<p><b>注意：同名
	 * Bean 将在“最低”工厂层级上优先，即此类 Bean 将从找到它们的最低工厂返回，并隐藏祖先工厂中的对应
	 * Bean。</b>此特性允许通过在子工厂中显式选择相同的 Bean 名称来“替换”Bean；此时祖先工厂中的该 Bean
	 * 将不可见，即使按类型查找也不可见。
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the matching bean instance
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException if the bean could not be created
	 * @see #beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	public static <T> T beanOfTypeIncludingAncestors(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或其子类型的单个 Bean，不查找祖先工厂。当我们期望单个 Bean 且不关心 Bean 名称时，这
	 * 是一个有用的便捷方法。<p>会考虑由 FactoryBean 创建的对象，这意味着 FactoryBean 将被初始化。如果
	 * FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与该类型进行匹配。<p>此版本的
	 * {@code beanOfType} 自动包含原型和 FactoryBean。
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @return the matching bean instance
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException if the bean could not be created
	 * @see ListableBeanFactory#getBeansOfType(Class)
	 */
	public static <T> T beanOfType(ListableBeanFactory lbf, Class<T> type) throws BeansException {
		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type);
		return uniqueBean(type, beansOfType);
	}

	/**
	 * 返回给定类型或其子类型的单个 Bean，不查找祖先工厂。当我们期望单个 Bean 且不关心 Bean 名称时，这
	 * 是一个有用的便捷方法。<p>如果设置了 "allowEagerInit" 标志，则会考虑由 FactoryBean 创建的对象，这
	 * 意味着 FactoryBean 将被初始化。如果 FactoryBean 创建的对象不匹配，则会将原始 FactoryBean 本身与
	 * 该类型进行匹配。如果未设置 "allowEagerInit"，则只检查原始 FactoryBean（这不需要初始化每个
	 * FactoryBean）。
	 * @param lbf the bean factory
	 * @param type type of bean to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too
	 * or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
	 * <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
	 * eagerly initialized to determine their type: So be aware that passing in "true"
	 * for this flag will initialize FactoryBeans and "factory-bean" references.
	 * @return the matching bean instance
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException if the bean could not be created
	 * @see ListableBeanFactory#getBeansOfType(Class, boolean, boolean)
	 */
	public static <T> T beanOfType(
			ListableBeanFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		Assert.notNull(lbf, "ListableBeanFactory must not be null");
		Map<String, T> beansOfType = lbf.getBeansOfType(type, includeNonSingletons, allowEagerInit);
		return uniqueBean(type, beansOfType);
	}


	/**
	 * 将给定的 Bean 名称结果与给定的父结果合并。
	 * @param result the local bean name result
	 * @param parentResult the parent bean name result (possibly empty)
	 * @param hbf the local bean factory
	 * @return the merged result (possibly the local result as-is)
	 * @since 4.3.15
	 */
	private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
		if (parentResult.length == 0) {
			return result;
		}
		List<String> merged = new ArrayList<>(result.length + parentResult.length);
		merged.addAll(Arrays.asList(result));
		for (String beanName : parentResult) {
			if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
				merged.add(beanName);
			}
		}
		return StringUtils.toStringArray(merged);
	}

	/**
	 * 从给定的匹配 Bean Map 中提取给定类型的唯一 Bean。
	 * @param type type of bean to match
	 * @param matchingBeans all matching beans found
	 * @return the unique bean instance
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 */
	private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
		int count = matchingBeans.size();
		if (count == 1) {
			return matchingBeans.values().iterator().next();
		}
		else if (count > 1) {
			throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
		}
		else {
			throw new NoSuchBeanDefinitionException(type);
		}
	}

}
