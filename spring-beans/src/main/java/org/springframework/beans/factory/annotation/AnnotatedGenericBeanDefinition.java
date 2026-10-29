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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.support.GenericBeanDefinition} 类的扩展，增加了对通过
 * {@link AnnotatedBeanDefinition} 接口暴露的注解元数据的支持。
 *
 * <p>此 GenericBeanDefinition 变体主要用于测试那些期望操作 AnnotatedBeanDefinition 的代码，例如
 * Spring 组件扫描支持中的策略实现（其中的默认定义类是
 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition}，它也实现了
 * AnnotatedBeanDefinition 接口）。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotatedBeanDefinition#getMetadata()
 * @see org.springframework.core.type.StandardAnnotationMetadata
 */
@SuppressWarnings("serial")
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

	private final AnnotationMetadata metadata;

	@Nullable
	private MethodMetadata factoryMethodMetadata;


	/**
	 * 为给定的 Bean 类创建一个新的 AnnotatedGenericBeanDefinition。
	 * @param beanClass the loaded bean class
	 */
	public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
		setBeanClass(beanClass);
		this.metadata = AnnotationMetadata.introspect(beanClass);
	}

	/**
	 * 为给定的注解元数据创建一个新的 AnnotatedGenericBeanDefinition，从而支持基于 ASM 的处理并避免过早
	 * 加载 Bean 类。请注意，该构造器在功能上等同于
	 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition ScannedGenericBeanDefinition}，
	 * 但后者的语义表明某个 Bean 是通过组件扫描而非其他方式专门发现的。
	 * @param metadata the annotation metadata for the bean class in question
	 * @since 3.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		if (metadata instanceof StandardAnnotationMetadata) {
			setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		else {
			setBeanClassName(metadata.getClassName());
		}
		this.metadata = metadata;
	}

	/**
	 * 基于带注解的类及该类上的工厂方法，为给定的注解元数据创建一个新的 AnnotatedGenericBeanDefinition。
	 *
	 * @param metadata the annotation metadata for the bean class in question
	 * @param factoryMethodMetadata metadata for the selected factory method
	 * @since 4.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
		this(metadata);
		Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
		setFactoryMethodName(factoryMethodMetadata.getMethodName());
		this.factoryMethodMetadata = factoryMethodMetadata;
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	@Nullable
	public final MethodMetadata getFactoryMethodMetadata() {
		return this.factoryMethodMetadata;
	}

}
