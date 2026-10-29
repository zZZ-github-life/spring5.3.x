/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSR-303 的 {@link javax.validation.Valid} 的变体，支持指定校验组。专为与 Spring 的 JSR-303 支持
 * 配合使用而设计，但并不特定于 JSR-303。
 *
 * <p>例如，可用于 Spring MVC 处理方法的参数。通过
 * {@link org.springframework.validation.SmartValidator} 的校验提示概念提供支持，由校验组类充当提示
 * 对象。
 *
 * <p>也可用于方法级校验，表示应当在方法级别校验某个特定的类（充当相应校验拦截器的切点），同时还可
 * 选择性地为被注解类中的方法级校验指定校验组。在方法级别应用此注解可以覆盖特定方法的校验组，但不会
 * 充当切点；要触发特定 Bean 的方法校验，仍然需要类级别的注解。还可以作为元注解用在自定义的构造型注
 * 解或自定义的、特定于校验组的已校验注解上。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see javax.validation.Validator#validate(Object, Class[])
 * @see org.springframework.validation.SmartValidator#validate(Object, org.springframework.validation.Errors, Object...)
 * @see org.springframework.validation.beanvalidation.SpringValidatorAdapter
 * @see org.springframework.validation.beanvalidation.MethodValidationPostProcessor
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

	/**
	 * 指定一个或多个校验组，应用于由此注解启动的校验步骤。
	 *
	 * <p>JSR-303 将校验组定义为应用程序声明的自定义注解，其唯一目的就是作为类型安全的组参数使用，如
	 * {@link org.springframework.validation.beanvalidation.SpringValidatorAdapter} 中所实现的那样。
	 *
	 * <p>其他 {@link org.springframework.validation.SmartValidator} 实现也可能以其他方式支持类参数。
	 */
	Class<?>[] value() default {};

}
