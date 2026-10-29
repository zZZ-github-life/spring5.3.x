/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示被注解的类是一个“组件”。在使用基于注解的配置和类路径扫描时，此类类被视为自动检测的候选者。
 *
 * <p>其他类级别注解也可能被视为标识一个组件，通常是某种特殊类型的组件：例如
 * {@link Repository @Repository} 注解或 AspectJ 的
 * {@link org.aspectj.lang.annotation.Aspect @Aspect} 注解。
 *
 * @author Mark Fisher
 * @since 2.5
 * @see Repository
 * @see Service
 * @see Controller
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Component {

	/**
	 * 该值可以表示逻辑组件名称的建议，以便在自动检测到组件时将其转换为 Spring Bean。
	 * @return the suggested component name, if any (or empty String otherwise)
	 */
	String value() default "";

}
