/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 指示给定 Bean 的“角色”提示。
 *
 * <p>可用于任何直接或间接标注了 {@link org.springframework.stereotype.Component} 的类，或标注了
 * {@link Bean} 的方法。
 *
 * <p>如果组件或 Bean 定义上不存在此注解，则将应用 {@link BeanDefinition#ROLE_APPLICATION} 的默认值。
 *
 * <p>如果 {@link Configuration @Configuration} 类上存在 Role，则表示该配置类 Bean 定义的角色，且不
 * 会级联到其中定义的所有 @{@code Bean} 方法。例如，此行为与 @{@link Lazy} 注解的行为不同。
 *
 * @author Chris Beams
 * @since 3.1
 * @see BeanDefinition#ROLE_APPLICATION
 * @see BeanDefinition#ROLE_INFRASTRUCTURE
 * @see BeanDefinition#ROLE_SUPPORT
 * @see Bean
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Role {

	/**
	 * 设置关联 Bean 的角色提示。
	 * @see BeanDefinition#ROLE_APPLICATION
	 * @see BeanDefinition#ROLE_INFRASTRUCTURE
	 * @see BeanDefinition#ROLE_SUPPORT
	 */
	int value();

}
