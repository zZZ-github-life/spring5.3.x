/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 对 Bean 定义读取器实现有用的工具方法。主要供内部使用。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 */
public abstract class BeanDefinitionReaderUtils {

	/**
	 * 生成的 Bean 名称的分隔符。如果类名或父名称不唯一，将追加“#1”、“#2”等，直到名称变得唯一。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;


	/**
	 * 为给定父名称和类名创建一个新的 GenericBeanDefinition，如果指定了 ClassLoader，则急切加载 Bean 类。
	 *
	 * @param parentName 父 Bean 的名称（如果有）
	 * @param className Bean 类的名称（如果有）
	 * @param classLoader 用于加载 Bean 类的 ClassLoader
	 * （可以为 {@code null}，仅按名称注册 Bean 类）
	 * @return Bean 定义
	 * @throws ClassNotFoundException 如果无法加载 Bean 类
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName(parentName);
		if (className != null) {
			if (classLoader != null) {
				bd.setBeanClass(ClassUtils.forName(className, classLoader));
			}
			else {
				bd.setBeanClassName(className);
			}
		}
		return bd;
	}

	/**
	 * 为给定顶层 Bean 定义生成一个在给定 Bean 工厂内唯一的 Bean 名称。
	 * @param beanDefinition 要为其生成 Bean 名称的 Bean 定义
	 * @param registry 该定义将要注册到的 Bean 工厂
	 * （用于检查现有的 Bean 名称）
	 * @return 生成的 Bean 名称
	 * @throws BeanDefinitionStoreException 如果无法为给定 Bean 定义生成唯一名称
	 * @see #generateBeanName(BeanDefinition, BeanDefinitionRegistry, boolean)
	 */
	public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		return generateBeanName(beanDefinition, registry, false);
	}

	/**
	 * 为给定 Bean 定义生成一个在给定 Bean 工厂内唯一的 Bean 名称。
	 * @param definition 要为其生成 Bean 名称的 Bean 定义
	 * @param registry 该定义将要注册到的 Bean 工厂（用于检查已存在的 Bean 名称）
	 * @param isInnerBean 给定的 Bean 定义将注册为内部 Bean 还是顶级 Bean
	 * （这会影响内部 Bean 与顶级 Bean 的特殊名称生成）
	 * @return 生成的 Bean 名称
	 * @throws BeanDefinitionStoreException 如果无法为给定的 Bean 定义生成唯一名称
	 */
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {

		String generatedBeanName = definition.getBeanClassName();
		if (generatedBeanName == null) {
			if (definition.getParentName() != null) {
				generatedBeanName = definition.getParentName() + "$child";
			}
			else if (definition.getFactoryBeanName() != null) {
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}
		if (!StringUtils.hasText(generatedBeanName)) {
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
					"'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		if (isInnerBean) {
			// Inner bean: generate identity hashcode suffix.
			return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
		}

		// Top-level bean: use plain class name with unique suffix if necessary.
		return uniqueBeanName(generatedBeanName, registry);
	}

	/**
	 * 将给定 Bean 名称转换为给定 Bean 工厂中的唯一 Bean 名称，如有必要追加唯一计数器作为后缀。
	 * @param beanName 原始 Bean 名称
	 * @param registry 该定义将要注册到的 Bean 工厂（用于检查已存在的 Bean 名称）
	 * @return 要使用的唯一 Bean 名称
	 * @since 5.1
	 */
	public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
		String id = beanName;
		int counter = -1;

		// Increase counter until the id is unique.
		String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR;
		while (counter == -1 || registry.containsBeanDefinition(id)) {
			counter++;
			id = prefix + counter;
		}
		return id;
	}

	/**
	 * 将给定 Bean 定义注册到给定 Bean 工厂。
	 * @param definitionHolder 包含名称和别名的 Bean 定义
	 * @param registry 要注册到的 Bean 工厂
	 * @throws BeanDefinitionStoreException 如果注册失败
	 */
	public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		//通过给定的主名称注册bean到工厂
		String beanName = definitionHolder.getBeanName();
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// 如果bean有别名的话就注册别名
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				registry.registerAlias(beanName, alias);
			}
		}
	}

	/**
	 * 使用生成的名称将给定 Bean 定义注册到给定 Bean 工厂，该名称在给定 Bean 工厂内唯一。
	 * @param definition 要为其生成 Bean 名称的 Bean 定义
	 * @param registry 要注册到的 Bean 工厂
	 * @return 生成的 Bean 名称
	 * @throws BeanDefinitionStoreException 如果无法为给定的 Bean 定义生成唯一名称，或该定义无法注册
	 */
	public static String registerWithGeneratedName(
			AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		String generatedName = generateBeanName(definition, registry, false);
		registry.registerBeanDefinition(generatedName, definition);
		return generatedName;
	}

}
