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

import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.wiring.BeanWiringInfoResolver}，使用 Configurable 注解
 * 来识别哪些类需要自动装配。要查找的 Bean 名称将从 {@link Configurable} 注解中获取（如果指定了）；
 * 否则默认为被配置类的全限定名。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see Configurable
 * @see org.springframework.beans.factory.wiring.ClassNameBeanWiringInfoResolver
 */
public class AnnotationBeanWiringInfoResolver implements BeanWiringInfoResolver {

	@Override
	@Nullable
	public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
		Assert.notNull(beanInstance, "Bean instance must not be null");
		Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
		return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
	}

	/**
	 * 为给定的 {@link Configurable} 注解构建 {@link BeanWiringInfo}。
	 * @param beanInstance the bean instance
	 * @param annotation the Configurable annotation found on the bean class
	 * @return the resolved BeanWiringInfo
	 */
	protected BeanWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
		if (!Autowire.NO.equals(annotation.autowire())) {
			// Autowiring by name or by type
			return new BeanWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
		}
		else if (!annotation.value().isEmpty()) {
			// Explicitly specified bean name for bean definition to take property values from
			return new BeanWiringInfo(annotation.value(), false);
		}
		else {
			// Default bean name for bean definition to take property values from
			return new BeanWiringInfo(getDefaultBeanName(beanInstance), true);
		}
	}

	/**
	 * 确定指定 Bean 实例的默认 Bean 名称。
	 * <p>默认实现对于 CGLIB 代理返回其超类名称，否则返回普通 Bean 类的名称。
	 * @param beanInstance the bean instance to build a default name for
	 * @return the default bean name to use
	 * @see org.springframework.util.ClassUtils#getUserClass(Class)
	 */
	protected String getDefaultBeanName(Object beanInstance) {
		return ClassUtils.getUserClass(beanInstance).getName();
	}

}
