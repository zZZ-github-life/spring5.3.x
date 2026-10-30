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

package org.springframework.beans.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * 以 JavaBeans 风格处理注解的通用工具方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.2, in favor of custom annotation attribute processing
 */
@Deprecated
public abstract class AnnotationBeanUtils {

	/**
	 * 将所提供 {@link Annotation} 的属性复制到所提供目标 Bean。在 {@code excludedProperties} 中定义的
	 * 任何属性都不会被复制。
	 * @param ann 要从中复制的注解
	 * @param bean 要复制到的 Bean 实例
	 * @param excludedProperties 被排除的属性名称（如果有）
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, String... excludedProperties) {
		copyPropertiesToBean(ann, bean, null, excludedProperties);
	}

	/**
	 * 将所提供 {@link Annotation} 的属性复制到所提供目标 Bean。在 {@code excludedProperties} 中定义的
	 * 任何属性都不会被复制。
	 * <p>例如，指定的值解析器可以解析属性值中的占位符。
	 * @param ann 要从中复制的注解
	 * @param bean 要复制到的 Bean 实例
	 * @param valueResolver 用于对 String 属性值进行后处理的解析器（可能为 {@code null}）
	 * @param excludedProperties 被排除的属性名称（如果有）
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, @Nullable StringValueResolver valueResolver,
			String... excludedProperties) {

		Set<String> excluded = (excludedProperties.length == 0 ? Collections.emptySet() :
				new HashSet<>(Arrays.asList(excludedProperties)));
		Method[] annotationProperties = ann.annotationType().getDeclaredMethods();
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		for (Method annotationProperty : annotationProperties) {
			String propertyName = annotationProperty.getName();
			if (!excluded.contains(propertyName) && bw.isWritableProperty(propertyName)) {
				Object value = ReflectionUtils.invokeMethod(annotationProperty, ann);
				if (valueResolver != null && value instanceof String) {
					value = valueResolver.resolveStringValue((String) value);
				}
				bw.setPropertyValue(propertyName, value);
			}
		}
	}

}
