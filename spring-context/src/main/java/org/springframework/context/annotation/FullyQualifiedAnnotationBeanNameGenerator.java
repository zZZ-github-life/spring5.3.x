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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * {@code AnnotationBeanNameGenerator} 的扩展，在未通过受支持的类级注解（例如 {@code @Component}）
 * 提供显式 Bean 名称时，使用完全限定类名作为默认 Bean 名称（关于受支持的注解，请参见
 * {@link AnnotationBeanNameGenerator}）。
 *
 * <p>如果由于多个自动检测到的组件具有相同的非限定类名（即名称相同但位于不同包中的类）而遇到命名冲
 * 突，请优先使用此 Bean 命名策略，而不是 {@code AnnotationBeanNameGenerator}。
 *
 * <p>请注意，该类的一个实例默认用于配置级别的导入目的；而组件扫描目的默认使用的是普通的
 * {@code AnnotationBeanNameGenerator}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2.3
 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
 * @see AnnotationBeanNameGenerator
 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
 */
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

	/**
	 * 用于配置级别导入目的的默认 {@code FullyQualifiedAnnotationBeanNameGenerator} 实例的便捷常量。
	 * @since 5.2.11
	 */
	public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
			new FullyQualifiedAnnotationBeanNameGenerator();


	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		return beanClassName;
	}

}
