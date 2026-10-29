/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.ui.Model;

/**
 * 将方法参数或方法返回值绑定到具名模型属性并暴露给 Web 视图的注解。
 * 支持用于带有 {@link RequestMapping @RequestMapping} 方法的控制器类。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，应仔细考虑数据绑定的设计与使用对安全性的影响。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和 <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>{@code @ModelAttribute} 可用于通过为 {@link RequestMapping @RequestMapping}
 * 方法的相应参数添加注解，以特定属性名称将命令对象暴露给 Web 视图。
 *
 * <p>{@code @ModelAttribute} 也可用于通过为带有
 * {@link RequestMapping @RequestMapping} 方法的控制器类中的访问器方法添加注解，
 * 将参考数据暴露给 Web 视图。此类访问器方法允许具有
 * {@link RequestMapping @RequestMapping} 方法支持的任何参数，
 * 并返回要暴露的模型属性值。
 *
 * <p>但请注意，当请求处理导致 {@code Exception} 时，参考数据和所有其他模型内容
 * 对 Web 视图不可用，因为异常可能在任何时刻抛出，从而使模型内容不可靠。
 * 因此，{@link ExceptionHandler @ExceptionHandler} 方法不提供对
 * {@link Model} 参数的访问。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5
 * @see ControllerAdvice
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的模型属性名称。
	 * <p>默认模型属性名称根据声明的属性类型（即方法参数类型或方法返回类型）
	 * 的非限定类名推断：例如类 "mypackage.OrderAddress" 对应 "orderAddress"，
	 * 或者 "List&lt;mypackage.OrderAddress&gt;" 对应 "orderAddressList"。
	 * @since 4.3
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 允许直接在 {@code @ModelAttribute} 方法参数上或 {@code @ModelAttribute}
	 * 方法返回的属性上禁用数据绑定，两者都会阻止对该属性进行数据绑定。
	 * <p>默认设置为 {@code true}，此时会应用数据绑定。
	 * 将其设置为 {@code false} 可禁用数据绑定。
	 * @since 4.3
	 */
	boolean binding() default true;

}
