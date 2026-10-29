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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * 用于处理 {@link ComponentScan @ComponentScan} {@linkplain ComponentScan.Filter type filters} 的
 * 工具集合。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.3.13
 * @see ComponentScan.Filter
 * @see org.springframework.core.type.filter.TypeFilter
 */
public abstract class TypeFilterUtils {

	/**
	 * 从提供的 {@link AnnotationAttributes} 创建 {@linkplain TypeFilter type filters}，例如来自
	 * {@link ComponentScan#includeFilters()} 或 {@link ComponentScan#excludeFilters()} 的那些。
	 *
	 * <p>每个 {@link TypeFilter} 都将使用适当的构造函数实例化，如果该类型过滤器实现了
	 * {@code BeanClassLoaderAware}、{@code BeanFactoryAware}、{@code EnvironmentAware} 和
	 * {@code ResourceLoaderAware} 契约，则会调用这些契约。
	 * @param filterAttributes {@code AnnotationAttributes} for a
	 * {@link ComponentScan.Filter @Filter} declaration
	 * @param environment the {@code Environment} to make available to filters
	 * @param resourceLoader the {@code ResourceLoader} to make available to filters
	 * @param registry the {@code BeanDefinitionRegistry} to make available to filters
	 * as a {@link org.springframework.beans.factory.BeanFactory} if applicable
	 * @return a list of instantiated and configured type filters
	 * @see TypeFilter
	 * @see AnnotationTypeFilter
	 * @see AssignableTypeFilter
	 * @see AspectJTypeFilter
	 * @see RegexPatternTypeFilter
	 * @see org.springframework.beans.factory.BeanClassLoaderAware
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.EnvironmentAware
	 * @see org.springframework.context.ResourceLoaderAware
	 */
	public static List<TypeFilter> createTypeFiltersFor(AnnotationAttributes filterAttributes, Environment environment,
			ResourceLoader resourceLoader, BeanDefinitionRegistry registry) {

		List<TypeFilter> typeFilters = new ArrayList<>();
		FilterType filterType = filterAttributes.getEnum("type");

		for (Class<?> filterClass : filterAttributes.getClassArray("classes")) {
			switch (filterType) {
				case ANNOTATION:
					Assert.isAssignable(Annotation.class, filterClass,
							"@ComponentScan ANNOTATION type filter requires an annotation type");
					@SuppressWarnings("unchecked")
					Class<Annotation> annotationType = (Class<Annotation>) filterClass;
					typeFilters.add(new AnnotationTypeFilter(annotationType));
					break;
				case ASSIGNABLE_TYPE:
					typeFilters.add(new AssignableTypeFilter(filterClass));
					break;
				case CUSTOM:
					Assert.isAssignable(TypeFilter.class, filterClass,
							"@ComponentScan CUSTOM type filter requires a TypeFilter implementation");
					TypeFilter filter = ParserStrategyUtils.instantiateClass(filterClass, TypeFilter.class,
							environment, resourceLoader, registry);
					typeFilters.add(filter);
					break;
				default:
					throw new IllegalArgumentException("Filter type not supported with Class value: " + filterType);
			}
		}

		for (String expression : filterAttributes.getStringArray("pattern")) {
			switch (filterType) {
				case ASPECTJ:
					typeFilters.add(new AspectJTypeFilter(expression, resourceLoader.getClassLoader()));
					break;
				case REGEX:
					typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
					break;
				default:
					throw new IllegalArgumentException("Filter type not supported with String pattern: " + filterType);
			}
		}

		return typeFilters;
	}

}
