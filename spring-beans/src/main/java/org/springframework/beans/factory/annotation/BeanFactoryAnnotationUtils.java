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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 执行与 Spring 特定注解相关的 Bean 查找的便捷方法，例如 Spring 的 {@link Qualifier @Qualifier} 注
 * 解。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1.2
 * @see BeanFactoryUtils
 */
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * 从给定的 {@code BeanFactory} 中检索所有类型为 {@code T} 且声明了与给定限定符匹配的限定符（例如通
	 * 过 {@code <qualifier>} 或 {@code @Qualifier}），或者 Bean 名称与给定限定符匹配的 Bean。
	 * @param beanFactory 要从中获取目标 Bean 的工厂（也会搜索祖先）
	 * @param beanType 要获取的 Bean 类型
	 * @param qualifier 用于在所有类型匹配之间进行选择的限定符
	 * @return 类型为 {@code T} 的匹配 Bean
	 * @throws BeansException 如果任何匹配的 Bean 无法创建
	 * @since 5.1.1
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> Map<String, T> qualifiedBeansOfType(
			ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {

		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
		Map<String, T> result = new LinkedHashMap<>(4);
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
				result.put(beanName, beanFactory.getBean(beanName, beanType));
			}
		}
		return result;
	}

	/**
	 * 从给定的 {@code BeanFactory} 中获取一个类型为 {@code T} 且声明了与给定限定符匹配的限定符（例如通
	 * 过 {@code <qualifier>} 或 {@code @Qualifier}），或者 Bean 名称与给定限定符匹配的 Bean。
	 * @param beanFactory 要从中获取目标 Bean 的工厂（也会搜索祖先）
	 * @param beanType 要获取的 Bean 类型
	 * @param qualifier 用于在多个 Bean 匹配之间进行选择的限定符
	 * @return 类型为 {@code T} 的匹配 Bean（从不为 {@code null}）
	 * @throws NoUniqueBeanDefinitionException 如果找到多个类型为 {@code T} 的匹配 Bean 则抛出
	 * @throws NoSuchBeanDefinitionException 如果未找到类型为 {@code T} 的匹配 Bean 则抛出
	 * @throws BeansException 如果无法创建该 Bean 则抛出
	 * @see BeanFactoryUtils#beanOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ListableBeanFactory) {
			// Full qualifier matching supported.
			return qualifiedBeanOfType((ListableBeanFactory) beanFactory, beanType, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name.
			return beanFactory.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for bean name '" + qualifier +
					"'! (Note: Qualifier matching not supported because given " +
					"BeanFactory does not implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * 从给定的 {@code BeanFactory} 中获取一个类型为 {@code T} 且声明了与给定限定符匹配的限定符（例如
	 * {@code <qualifier>} 或 {@code @Qualifier}）的 Bean。
	 * @param bf 用于获取目标 Bean 的工厂
	 * @param beanType 要获取的 Bean 的类型
	 * @param qualifier 用于在多个 Bean 匹配之间进行选择的限定符
	 * @return 类型为 {@code T} 的匹配 Bean（永不为 {@code null}）
	 */
	private static <T> T qualifiedBeanOfType(ListableBeanFactory bf, Class<T> beanType, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
		String matchingBean = null;
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, bf)) {
				if (matchingBean != null) {
					throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
				}
				matchingBean = beanName;
			}
		}
		if (matchingBean != null) {
			return bf.getBean(matchingBean, beanType);
		}
		else if (bf.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name - probably a manually registered singleton.
			return bf.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * 检查指定名称的 Bean 是否声明了给定名称的限定符。
	 * @param qualifier 要匹配的限定符
	 * @param beanName 候选 Bean 的名称
	 * @param beanFactory 从中获取指定名称 Bean 的工厂
	 * @return 如果 Bean 定义（XML 情况）或 Bean 的工厂方法
	 * （{@code @Bean} 情况）通过 {@code <qualifier>} 或 {@code @Qualifier}
	 * 定义了匹配的限定符值，则返回 {@code true}
	 * @since 5.0
	 */
	public static boolean isQualifierMatch(
			Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

		// Try quick bean name or alias match first...
		if (qualifier.test(beanName)) {
			return true;
		}
		if (beanFactory != null) {
			for (String alias : beanFactory.getAliases(beanName)) {
				if (qualifier.test(alias)) {
					return true;
				}
			}
			try {
				Class<?> beanType = beanFactory.getType(beanName);
				if (beanFactory instanceof ConfigurableBeanFactory) {
					BeanDefinition bd = ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName);
					// Explicit qualifier metadata on bean definition? (typically in XML definition)
					if (bd instanceof AbstractBeanDefinition) {
						AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
						AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
						if (candidate != null) {
							Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
							if (value != null && qualifier.test(value.toString())) {
								return true;
							}
						}
					}
					// Corresponding qualifier on factory method? (typically in configuration class)
					if (bd instanceof RootBeanDefinition) {
						Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
						if (factoryMethod != null) {
							Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
							if (targetAnnotation != null) {
								return qualifier.test(targetAnnotation.value());
							}
						}
					}
				}
				// Corresponding qualifier on bean implementation class? (for custom user types)
				if (beanType != null) {
					Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
					if (targetAnnotation != null) {
						return qualifier.test(targetAnnotation.value());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore - can't compare qualifiers for a manually registered singleton object
			}
		}
		return false;
	}

}
