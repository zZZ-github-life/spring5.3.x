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
import org.springframework.http.HttpStatus;

/**
 * 用应返回的状态 {@link #code} 和 {@link #reason} 标记方法或异常类。
 *
 * <p>当调用处理方法时，该状态码会被应用到 HTTP 响应上，并覆盖通过其他方式
 * （例如 {@code ResponseEntity} 或 {@code "redirect:"}）设置的状态信息。
 *
 * <p><strong>警告</strong>：在异常类上使用此注解，或设置此注解的 {@code reason}
 * 属性时，将使用 {@code HttpServletResponse.sendError} 方法。
 *
 * <p>使用 {@code HttpServletResponse.sendError} 时，响应被视为已完整，
 * 不应再向其写入任何内容。此外，Servlet 容器通常会写入一个 HTML 错误页面，
 * 因此 {@code reason} 的用法不适合 REST API。对于这类场景，最好使用
 * {@link org.springframework.http.ResponseEntity} 作为返回类型，并完全避免使用
 * {@code @ResponseStatus}。
 *
 * <p>请注意，控制器类也可以标注 {@code @ResponseStatus}，该注解随后会被该类
 * 及其子类中的所有 {@code @RequestMapping} 和 {@code @ExceptionHandler} 方法继承，
 * 除非被方法上的局部 {@code @ResponseStatus} 声明覆盖。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

	/**
	 * {@link #code} 的别名。
	 */
	@AliasFor("code")
	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的状态<em>码</em>。
	 * <p>默认值为 {@link HttpStatus#INTERNAL_SERVER_ERROR}，通常应将其改为
	 * 更合适的值。
	 * @since 4.2
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	@AliasFor("value")
	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的<em>原因</em>。
	 * <p>默认为空字符串，此时将被忽略。将原因设置为非空值即可将其用于响应。
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
	 */
	String reason() default "";

}
