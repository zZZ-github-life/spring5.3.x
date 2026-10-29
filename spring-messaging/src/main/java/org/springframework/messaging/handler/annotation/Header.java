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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到消息头的注解。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Header {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求头名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 头是否为必需。
	 * <p>默认值为 {@code true}，当头缺失时会引发异常。
	 * 如果希望在头缺失时使用 {@code null} 值，请将其设置为 {@code false}。
	 * @see #defaultValue
	 */
	boolean required() default true;

	/**
	 * 用作回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
