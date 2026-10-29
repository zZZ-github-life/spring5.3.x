/*
 * Copyright 2002-2016 the original author or authors.
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
 * 将方法参数绑定到请求属性的注解。
 *
 * <p>主要目的是从控制器方法中方便地访问请求属性，并提供可选/必需检查
 * 以及到目标方法参数类型的转换。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see RequestMapping
 * @see SessionAttribute
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的请求属性名称。
	 * <p>默认名称从方法参数名推断。
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 请求属性是否为必需。
	 * <p>默认为 {@code true}，如果属性缺失将抛出异常。
	 * 如果希望在属性不存在时得到 {@code null} 或 Java 8 的
	 * {@code java.util.Optional}，请将其改为 {@code false}。
	 */
	boolean required() default true;

}
