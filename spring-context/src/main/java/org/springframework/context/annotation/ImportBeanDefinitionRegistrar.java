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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 在处理 @{@link Configuration} 类时注册额外 Bean 定义的类型应实现此接口。当希望或需要在 Bean 定义
 * 级别（而非 {@code @Bean} 方法/实例级别）进行操作时很有用。
 *
 * <p>与 {@code @Configuration} 和 {@link ImportSelector} 一样，此类型的类可以提供给 @
 * {@link Import} 注解（也可以从 {@code ImportSelector} 返回）。
 *
 * <p>{@link ImportBeanDefinitionRegistrar} 可以实现以下任意
 * {@link org.springframework.beans.factory.Aware Aware} 接口，其各自的方法将在
 * {@link #registerBeanDefinitions} 之前调用：
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>或者，该类可以提供单个构造器，其参数为以下受支持参数类型中的一种或多种：
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>有关用法示例，请参见相关实现及关联的单元测试。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * 根据导入方 {@code @Configuration} 类的给定注解元数据，按需注册 Bean 定义。
	 * <p>请注意，由于与 {@code @Configuration} 类处理相关的生命周期约束，
	 * {@link BeanDefinitionRegistryPostProcessor} 类型<em>不能</em>在此处注册。
	 * <p>默认实现会委托给 {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}。
	 *
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 * @param importBeanNameGenerator the bean name generator strategy for imported beans:
	 * {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR} by default, or a
	 * user-provided one if {@link ConfigurationClassPostProcessor#setBeanNameGenerator}
	 * has been set. In the latter case, the passed-in strategy will be the same used for
	 * component scanning in the containing application context (otherwise, the default
	 * component-scan naming strategy is {@link AnnotationBeanNameGenerator#INSTANCE}).
	 * @since 5.2
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 * @see ConfigurationClassPostProcessor#setBeanNameGenerator
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {

		registerBeanDefinitions(importingClassMetadata, registry);
	}

	/**
	 * 根据导入方 {@code @Configuration} 类的给定注解元数据，按需注册 Bean 定义。
	 * <p>请注意，由于与 {@code @Configuration} 类处理相关的生命周期约束，
	 * {@link BeanDefinitionRegistryPostProcessor} 类型<em>不能</em>在此处注册。
	 * <p>默认实现为空。
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	}

}
