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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于字段或方法/构造方法参数级别的注解，指示被标注元素的默认值表达式。
 *
 * <p>通常用于表达式驱动或属性驱动的依赖注入。也支持处理器方法参数的动态解析 &mdash; 例如在 Spring
 * MVC 中。
 *
 * <p>一个常见用例是使用 <code>#{systemProperties.myProp}</code> 风格的 SpEL（Spring Expression
 * Language）表达式来注入值。或者，也可以使用 <code>${my.app.myProp}</code> 风格的属性占位符来注入
 * 值。
 *
 * <p>请注意，{@code @Value} 注解的实际处理是由
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 执行的，这
 * 反过来意味着你<em>不能</em>在
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * 类型中使用 {@code @Value}。请查阅 {@link AutowiredAnnotationBeanPostProcessor} 类的 javadoc（默
 * 认情况下，该类会检查此注解是否存在）。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Autowired
 * @see org.springframework.beans.factory.config.BeanExpressionResolver
 * @see org.springframework.beans.factory.support.AutowireCandidateResolver#getSuggestedValue
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

	/**
	 * 实际的值表达式，例如 <code>#{systemProperties.myProp}</code>，或属性占位符，例如 <code>
	 * ${my.app.myProp}</code>。
	 */
	String value();

}
