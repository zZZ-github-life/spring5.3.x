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
import org.springframework.messaging.converter.MessageConverter;

/**
 * 将方法参数绑定到消息载荷的注解。也可以用于将载荷与方法调用关联。
 * 载荷可以通过 {@link MessageConverter} 从具有特定 MIME 类型的序列化形式
 * 转换为与目标方法参数匹配的 Object。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload {

	/**
	 * {@link #expression} 的别名。
	 */
	@AliasFor("expression")
	String value() default "";

	/**
	 * 以载荷对象为根上下文求值的 SpEL 表达式。
	 * <p>此属性是否受支持取决于正在处理的消息的载荷是否为非原始 Object，
	 * 或者是否处于序列化形式并需要消息转换。
	 * <p>处理基于 WebSocket 的 STOMP 消息时不支持此属性。
	 * @since 4.2
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * 载荷内容是否为必需。
	 * <p>默认值为 {@code true}，如果没有载荷则会引发异常。
	 * 将其切换为 {@code false}，以便在没有载荷时传入 {@code null}。
	 */
	boolean required() default true;

}
