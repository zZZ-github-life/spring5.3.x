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

/**
 * 一个便捷注解，其本身标注了 {@link ControllerAdvice @ControllerAdvice}
 * 和 {@link ResponseBody @ResponseBody}。
 *
 * <p>标注了此注解的类型会被视为控制器通知类，其中的
 * {@link ExceptionHandler @ExceptionHandler} 方法默认采用
 * {@link ResponseBody @ResponseBody} 语义。
 *
 * <p><b>注意：</b>只有在配置了合适的 {@code HandlerMapping}-{@code HandlerAdapter}
 * 组合时才会处理 {@code @RestControllerAdvice}，例如
 * {@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter} 组合，
 * 它们是 MVC Java 配置和 MVC 命名空间中的默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.3
 * @see RestController
 * @see ControllerAdvice
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice {

	/**
	 * {@link #basePackages} 属性的别名。
	 * <p>允许更简洁的注解声明 &mdash;&mdash; 例如，
	 * {@code @RestControllerAdvice("org.my.pkg")} 等价于
	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")}。
	 * @see #basePackages
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] value() default {};

	/**
	 * 基础包数组。
	 * <p>属于这些基础包或其子包的控制器将被纳入 &mdash;&mdash; 例如，
	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")} 或
	 * {@code @RestControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}。
	 * <p>{@link #value} 是该属性的别名，只是为了让注解的使用更简洁。
	 * <p>也可以考虑使用 {@link #basePackageClasses} 作为基于字符串的包名的
	 * 类型安全替代方案。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] basePackages() default {};

	/**
	 * {@link #basePackages} 的类型安全替代方案，用于指定在哪些包中选择
	 * 需要由 {@code @RestControllerAdvice} 注解类通知的控制器。
	 * <p>可以考虑在每个包中创建一个特殊的空标记类或接口，仅用于被此属性引用。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] basePackageClasses() default {};

	/**
	 * 类数组。
	 * <p>可赋值给至少一个给定类型的控制器将由 {@code @RestControllerAdvice}
	 * 注解类进行通知。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解数组。
	 * <p>标注了至少一个所提供的注解类型的控制器将由 {@code @RestControllerAdvice}
	 * 注解类进行通知。
	 * <p>可以考虑创建自定义的组合注解，或使用预定义的注解，例如
	 * {@link RestController @RestController}。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<? extends Annotation>[] annotations() default {};

}
