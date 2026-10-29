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
 * 一种标注 'lookup' 方法的注解，这些方法由容器重写，以便将调用重定向回
 * {@link org.springframework.beans.factory.BeanFactory} 执行 {@code getBean} 调用。这实质上是 XML
 * {@code lookup-method} 属性的基于注解的版本，会产生相同的运行时安排。
 *
 * <p>目标 Bean 的解析可以基于返回类型（{@code getBean(Class)}），也可以基于建议的 Bean 名称
 * （{@code getBean(String)}）；两种情况下都会将方法的参数传递给 {@code getBean} 调用，以将其作为目标
 * 工厂方法参数或构造器参数应用。
 *
 * <p>此类查找方法可以带有默认（存根）实现，这些实现会被容器直接替换；也可以声明为抽象方法，由容器
 * 在运行时填充。两种情况下，容器都会通过 CGLIB 生成该方法所在类的运行时子类，这就是此类查找方法只
 * 能作用于容器通过常规构造器实例化的 Bean 的原因：也就是说，对于从工厂方法返回的 Bean，如果我们无
 * 法为其动态提供子类，查找方法就无法被替换。
 *
 * <p><b>典型 Spring 配置场景的建议：</b>当某些场景可能需要具体类时，请考虑为查找方法提供存根实现。
 * 并且请记住，查找方法不会作用于配置类中从 {@code @Bean} 方法返回的 Bean；此时只能改用
 * {@code @Inject Provider<TargetBean>} 之类的方式。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

	/**
	 * 该注解属性可以建议要查找的目标 Bean 名称。如果未指定，则将根据被注解方法的返回类型声明来解析目标
	 * Bean。
	 */
	String value() default "";

}
