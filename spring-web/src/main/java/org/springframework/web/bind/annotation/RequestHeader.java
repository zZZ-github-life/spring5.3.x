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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到 Web 请求头的注解。
 *
 * <p>支持用于 Spring MVC 和 Spring WebFlux 中的注解处理方法。
 *
 * <p>如果方法参数为 {@link java.util.Map Map&lt;String, String&gt;}、
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 或 {@link org.springframework.http.HttpHeaders HttpHeaders}，
 * 则该 Map 将填充所有请求头名称和值。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see RequestMapping
 * @see RequestParam
 * @see CookieValue
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeader {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的请求头名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 请求头是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少该请求头将抛出异常。
	 * 如果希望在请求中不存在该请求头时得到 {@code null} 值，
	 * 请将其改为 {@code false}。
	 * <p>另外，提供 {@link #defaultValue} 会隐式地将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 用作回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
