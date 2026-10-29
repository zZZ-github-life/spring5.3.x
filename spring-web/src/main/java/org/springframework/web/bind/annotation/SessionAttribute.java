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
 * 用于将方法参数绑定到会话属性的注解。
 *
 * <p>其主要目的是提供对现有永久会话属性（例如用户认证对象）的便捷访问，
 * 支持可选/必需检查，并转换为目标方法参数类型。
 *
 * <p>对于需要添加或移除会话属性的用例，考虑将
 * {@code org.springframework.web.context.request.WebRequest} 或
 * {@code javax.servlet.http.HttpSession} 注入到控制器方法中。
 *
 * <p>如果需要在控制器工作流中将会话中的模型属性临时存储，考虑改用
 * {@link SessionAttributes}。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see RequestMapping
 * @see SessionAttributes
 * @see RequestAttribute
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SessionAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的会话属性的名称。
	 * <p>默认名称由方法参数名称推断得出。
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 会话属性是否为必需。
	 * <p>默认为 {@code true}，如果会话中缺少该属性或不存在会话，将抛出异常。
	 * 如果你希望在该属性不存在时返回 {@code null} 或 Java 8
	 * {@code java.util.Optional}，请将此值切换为 {@code false}。
	 */
	boolean required() default true;

}
