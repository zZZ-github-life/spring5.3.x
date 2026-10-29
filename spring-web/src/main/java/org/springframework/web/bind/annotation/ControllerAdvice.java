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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * {@link Component @Component} 的特化，用于声明
 * {@link ExceptionHandler @ExceptionHandler}、{@link InitBinder @InitBinder} 或
 * {@link ModelAttribute @ModelAttribute} 方法，使其在多个 {@code @Controller}
 * 类之间共享。
 *
 * <p>标注了 {@code @ControllerAdvice} 的类可以显式声明为 Spring bean，也可以通过
 * 类路径扫描自动检测。所有这些 bean 都会基于
 * {@link org.springframework.core.Ordered Ordered} 语义或
 * {@link org.springframework.core.annotation.Order @Order} /
 * {@link javax.annotation.Priority @Priority} 声明进行排序，其中 {@code Ordered}
 * 语义优先于 {@code @Order} / {@code @Priority} 声明。
 * {@code @ControllerAdvice} bean 随后会按该顺序在运行时应用。
 * 但请注意，实现了 {@link org.springframework.core.PriorityOrdered PriorityOrdered}
 * 的 {@code @ControllerAdvice} bean <em>不会</em>优先于实现了 {@code Ordered}
 * 的 {@code @ControllerAdvice} bean。此外，对于有作用域的 {@code @ControllerAdvice}
 * bean（例如配置为请求作用域或会话作用域的 bean），{@code Ordered} 不会被遵循。
 * 对于异常处理，将在第一个具有匹配异常处理方法的通知中选取
 * {@code @ExceptionHandler}。对于模型属性和数据绑定初始化，
 * {@code @ModelAttribute} 和 {@code @InitBinder} 方法将遵循
 * {@code @ControllerAdvice} 的顺序。
 *
 * <p>注意：对于 {@code @ExceptionHandler} 方法，在特定通知 bean 的处理方法中，
 * 根异常匹配会优先于仅匹配当前异常的原因。然而，较高优先级通知上的原因匹配
 * 仍然优先于较低优先级通知 bean 上的任何匹配（无论是根级别还是原因级别）。
 * 因此，请将主要的根异常映射声明在具有相应顺序的已排序通知 bean 上。
 *
 * <p>默认情况下，{@code @ControllerAdvice} 中的方法会全局应用于所有控制器。
 * 使用诸如 {@link #annotations}、{@link #basePackageClasses} 和
 * {@link #basePackages}（或其别名 {@link #value}）之类的选择器来定义
 * 更窄的目标控制器子集。如果声明了多个选择器，则应用布尔 {@code OR} 逻辑，
 * 即选中的控制器应至少匹配一个选择器。请注意，选择器检查在运行时执行，
 * 因此添加大量选择器可能会对性能产生负面影响并增加复杂性。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.stereotype.Controller
 * @see RestControllerAdvice
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

	/**
	 * {@link #basePackages} 属性的别名。
	 * <p>允许更简洁的注解声明 &mdash;&mdash; 例如，
	 * {@code @ControllerAdvice("org.my.pkg")} 等价于
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")}。
	 * @since 4.0
	 * @see #basePackages
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 基础包数组。
	 * <p>属于这些基础包或其子包的控制器将被纳入 &mdash;&mdash; 例如，
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")} 或
	 * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}。
	 * <p>{@link #value} 是该属性的别名，只是为了让注解的使用更简洁。
	 * <p>也可以考虑使用 {@link #basePackageClasses} 作为基于字符串的包名的
	 * 类型安全替代方案。
	 * @since 4.0
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * {@link #basePackages} 的类型安全替代方案，用于指定在哪些包中选择
	 * 需要由 {@code @ControllerAdvice} 注解类通知的控制器。
	 * <p>可以考虑在每个包中创建一个特殊的空标记类或接口，仅用于被此属性引用。
	 * @since 4.0
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * 类数组。
	 * <p>可赋值给至少一个给定类型的控制器将由 {@code @ControllerAdvice}
	 * 注解类进行通知。
	 * @since 4.0
	 */
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解类型数组。
	 * <p>标注了至少一个所提供的注解类型的控制器将由 {@code @ControllerAdvice}
	 * 注解类进行通知。
	 * <p>可以考虑创建自定义的组合注解，或使用预定义的注解，例如
	 * {@link RestController @RestController}。
	 * @since 4.0
	 */
	Class<? extends Annotation>[] annotations() default {};

}
