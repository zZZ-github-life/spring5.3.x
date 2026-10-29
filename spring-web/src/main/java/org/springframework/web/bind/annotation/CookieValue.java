/*
 * Copyright 2002-2021 the original author or authors.
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
 * 指示方法参数绑定到某个 HTTP Cookie 的注解。
 *
 * <p>方法参数可以声明为 {@link javax.servlet.http.Cookie} 类型，
 * 也可以声明为 Cookie 值类型（String、int 等）。
 *
 * <p>请注意，在 spring-webmvc 5.3.x 及更早版本中，Cookie 值会进行 URL 解码。
 * 这将在 6.0 中改变，但在此之前，应用也可以声明
 * {@link javax.servlet.http.Cookie} 类型的参数以访问原始值。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see RequestMapping
 * @see RequestParam
 * @see RequestHeader
 * @see org.springframework.web.bind.annotation.RequestMapping
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的 Cookie 名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * Cookie 是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少该 Cookie 将抛出异常。
	 * 如果希望在请求中不存在该 Cookie 时得到 {@code null} 值，
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
