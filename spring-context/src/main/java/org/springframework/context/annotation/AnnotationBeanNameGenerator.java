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

package org.springframework.context.annotation;

import java.beans.Introspector;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于以 {@link org.springframework.stereotype.Component @Component} 注解标注的 Bean 类，或以本身
 * 带有 {@code @Component} 元注解的其他注解标注的 Bean 类的 {@link BeanNameGenerator} 实现。例如，
 * Spring 的构造型注解（如 {@link org.springframework.stereotype.Repository @Repository}）本身就带
 * 有 {@code @Component} 注解。
 *
 * <p>还支持 Java EE 6 的 {@link javax.annotation.ManagedBean} 和 JSR-330 的
 * {@link javax.inject.Named} 注解（如果可用）。请注意，Spring 组件注解始终会覆盖此类标准注解。
 *
 * <p>如果注解的值未指示 Bean 名称，则将基于类的短名称（首字母小写）构建合适的名称，除非前两个字母
 * 均为大写。例如：
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 * <pre class="code">com.xyz.URLFooServiceImpl -&gt; URLFooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.stereotype.Component#value()
 * @see org.springframework.stereotype.Repository#value()
 * @see org.springframework.stereotype.Service#value()
 * @see org.springframework.stereotype.Controller#value()
 * @see javax.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNameGenerator
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	/**
	 * 默认 {@code AnnotationBeanNameGenerator} 实例的便捷常量，用于组件扫描目的。
	 * @since 5.2
	 */
	public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

	private final Map<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();


	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		if (definition instanceof AnnotatedBeanDefinition) {
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			if (StringUtils.hasText(beanName)) {
				// Explicit bean name found.
				return beanName;
			}
		}
		// Fallback: generate a unique default bean name.
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * 从类上的某个注解派生 Bean 名称。
	 * @param annotatedDef 支持注解的 Bean 定义
	 * @return Bean 名称；如果未找到，则为 {@code null}
	 */
	@Nullable
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata amd = annotatedDef.getMetadata();
		Set<String> types = amd.getAnnotationTypes();
		String beanName = null;
		for (String type : types) {
			AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
			if (attributes != null) {
				Set<String> metaTypes = this.metaAnnotationTypesCache.computeIfAbsent(type, key -> {
					Set<String> result = amd.getMetaAnnotationTypes(key);
					return (result.isEmpty() ? Collections.emptySet() : result);
				});
				if (isStereotypeWithNameValue(type, metaTypes, attributes)) {
					Object value = attributes.get("value");
					if (value instanceof String) {
						String strVal = (String) value;
						if (StringUtils.hasLength(strVal)) {
							if (beanName != null && !strVal.equals(beanName)) {
								throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
										"component names: '" + beanName + "' versus '" + strVal + "'");
							}
							beanName = strVal;
						}
					}
				}
			}
		}
		return beanName;
	}

	/**
	 * 检查给定注解是否是一种构造型注解，允许其通过注解的 {@code value()} 建议组件名称。
	 * @param annotationType 要检查的注解类的名称
	 * @param metaAnnotationTypes 给定注解上的元注解名称
	 * @param attributes 给定注解的属性映射
	 * @return 该注解是否属于带组件名称的构造型注解
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
			Set<String> metaAnnotationTypes, @Nullable Map<String, Object> attributes) {

		boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
				metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("javax.inject.Named");

		return (isStereotype && attributes != null && attributes.containsKey("value"));
	}

	/**
	 * 从给定的 Bean 定义派生默认 Bean 名称。
	 *
	 * <p>默认实现委托给 {@link #buildDefaultBeanName(BeanDefinition)}。
	 * @param definition 要为其构建 Bean 名称的 Bean 定义
	 * @param registry 给定 Bean 定义正在注册到的注册表
	 * @return 默认 Bean 名称（永不为 {@code null}）
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 * 从给定的 Bean 定义派生默认 Bean 名称。
	 *
	 * <p>默认实现仅构建短类名的首字母小写版本：例如 "mypackage.MyJdbcDao" &rarr; "myJdbcDao"。
	 *
	 * <p>请注意，因此内部类将具有 "outerClassName.InnerClassName" 形式的名称，由于名称中含有句点，在按
	 * 名称自动注入时可能会成为问题。
	 * @param definition 要为其构建 Bean 名称的 Bean 定义
	 * @return 默认 Bean 名称（永不为 {@code null}）
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		String shortClassName = ClassUtils.getShortName(beanClassName);
		return Introspector.decapitalize(shortClassName);
	}

}
