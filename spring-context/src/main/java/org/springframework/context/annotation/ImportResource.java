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

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.annotation.AliasFor;

/**
 * 指示要导入的一个或多个包含 Bean 定义的资源。
 *
 * <p>与 {@link Import @Import} 类似，此注解提供与 Spring XML 中 {@code <import/>} 元素相似的功能。
 * 它通常用于设计由 {@link AnnotationConfigApplicationContext} 引导的
 * {@link Configuration @Configuration} 类，但仍需要命名空间等某些 XML 功能的场景。
 *
 * <p>默认情况下，{@link #value} 属性的参数如果以 {@code ".groovy"} 结尾，将使用
 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}
 * 处理；否则将使用
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader} 解
 * 析 Spring {@code <beans/>} XML 文件。可选地，可以声明 {@link #reader} 属性，以允许用户选择自定义
 * 的 {@link BeanDefinitionReader} 实现。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Import
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

	/**
	 * {@link #locations} 的别名。
	 * @see #locations
	 * @see #reader
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 要从中导入的资源位置。
	 * <p>支持 {@code classpath:}、{@code file:} 等资源加载前缀。
	 * <p>有关资源处理方式的详细信息，请查阅 {@link #reader} 的 Javadoc。
	 * @since 4.2
	 * @see #value
	 * @see #reader
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 处理通过 {@link #value} 属性指定的资源时要使用的 {@link BeanDefinitionReader} 实现。
	 * <p>默认情况下，读取器会适配所指定的资源路径：{@code ".groovy"} 文件将使用
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}
	 * 处理；而所有其他资源将使用
	 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader} 处
	 * 理。
	 * @see #value
	 */
	Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
