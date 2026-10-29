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

package org.springframework.web.bind.annotation;

import java.beans.PropertyEditor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;

/**
 * 可用于将 "multipart/form-data" 请求的部件与方法参数关联的注解。
 *
 * <p>支持的方法参数类型包括：与 Spring 的 {@link MultipartResolver} 抽象结合使用的
 * {@link MultipartFile}、与 Servlet 3.0 multipart 请求结合使用的
 * {@code javax.servlet.http.Part}，或者对于其他任何方法参数，部件内容会通过
 * {@link HttpMessageConverter} 传递，并考虑请求部件的 'Content-Type' 头。
 * 这类似于 @{@link RequestBody} 基于非 multipart 常规请求的内容来解析参数的方式。
 *
 * <p>请注意，@{@link RequestParam} 注解也可用于将 "multipart/form-data" 请求的部件
 * 与方法参数关联，并支持相同的方法参数类型。主要区别在于，当方法参数不是 String
 * 或原始的 {@code MultipartFile} / {@code Part} 时，{@code @RequestParam} 依赖
 * 通过已注册的 {@link Converter} 或 {@link PropertyEditor} 进行类型转换，
 * 而 {@link RequestPart} 依赖 {@link HttpMessageConverter HttpMessageConverters}
 * 并考虑请求部件的 'Content-Type' 头。
 * {@link RequestParam} 通常用于名称-值形式的表单字段，
 * 而 {@link RequestPart} 通常用于包含更复杂内容（例如 JSON、XML）的部件。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.1
 * @see RequestParam
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPart {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的 {@code "multipart/form-data"} 请求中的部件名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 部件是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少该部件将抛出异常。
	 * 如果希望在请求中不存在该部件时得到 {@code null} 值，请将其改为 {@code false}。
	 */
	boolean required() default true;

}
