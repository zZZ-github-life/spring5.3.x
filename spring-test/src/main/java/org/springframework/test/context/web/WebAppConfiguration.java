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

package org.springframework.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebAppConfiguration} 是一个类级注解，用于声明为集成测试加载的 {@code ApplicationContext} 应为 {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}。
 * <p>测试类上存在 {@code @WebAppConfiguration} 表示应使用 Web 应用程序根路径的默认值为测试加载 {@code WebApplicationContext}。要覆盖默认值，请通过 {@link #value} 属性指定显式资源路径。
 * <p>请注意，{@code @WebAppConfiguration} 必须与 {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration} 结合使用，可以位于单个测试类中，也可以位于测试类层次结构中。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.web.context.WebApplicationContext
 * @see org.springframework.test.context.ContextConfiguration
 * @see ServletTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WebAppConfiguration {

	/**
	 * Web 应用程序根目录的资源路径。
	 * <p>不包含 Spring 资源前缀（例如 {@code classpath:}、{@code file:} 等）的路径将被解释为文件系统资源，并且路径不应以斜杠结尾。
	 * <p>默认为文件系统资源 {@code "src/main/webapp"}。请注意，对于遵循标准 Maven WAR 项目布局的项目，这是 Web 应用程序根目录的标准目录。
	 */
	String value() default "src/main/webapp";

}
