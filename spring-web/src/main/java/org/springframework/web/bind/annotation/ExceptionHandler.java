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

/**
 * 用于在特定处理器类和/或处理器方法中处理异常的注解。
 *
 * <p>使用此注解标注的处理器方法允许使用非常灵活的方法签名。它们可以按任意顺序
 * 具有以下类型的参数：
 * <ul>
 * <li>异常参数：声明为通用的 Exception 或更具体的异常。如果注解本身没有通过
 * {@link #value()} 缩小异常类型范围，此参数也充当映射提示。
 * 可以引用正在传播的顶层异常，也可以引用包装异常中的嵌套原因。
 * 自 5.3 起，任何层级的原因都会被暴露，而此前只考虑直接原因。
 * <li>请求和/或响应对象（通常来自 Servlet API）。可以选择任何特定的
 * 请求/响应类型，例如 {@link javax.servlet.ServletRequest} /
 * {@link javax.servlet.http.HttpServletRequest}。
 * <li>会话对象：通常是 {@link javax.servlet.http.HttpSession}。
 * 此类型的参数将强制要求存在相应的会话。因此，此类参数永远不会是
 * {@code null}。<i>请注意，会话访问可能不是线程安全的，特别是在 Servlet 环境中：
 * 如果允许多个请求并发访问会话，请考虑将
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
 * "synchronizeOnSession"} 标志设置为 "true"。</i>
 * <li>{@link org.springframework.web.context.request.WebRequest} 或
 * {@link org.springframework.web.context.request.NativeWebRequest}。
 * 允许进行通用的请求参数访问以及请求/会话属性访问，而无需绑定到原生 Servlet API。
 * <li>{@link java.util.Locale} 表示当前请求的区域设置
 * （由最具体的可用区域解析器确定，即在 Servlet 环境中为配置的
 * {@link org.springframework.web.servlet.LocaleResolver}）。
 * <li>{@link java.io.InputStream} / {@link java.io.Reader} 用于访问请求内容。
 * 这将是 Servlet API 所暴露的原始 InputStream/Reader。
 * <li>{@link java.io.OutputStream} / {@link java.io.Writer} 用于生成响应内容。
 * 这将是 Servlet API 所暴露的原始 OutputStream/Writer。
 * <li>{@link org.springframework.ui.Model}，作为从处理器方法返回模型 Map 的替代方式。
 * 请注意，所提供的模型不会预填充常规模型属性，因此始终为空，
 * 这样便于为异常特定的视图准备模型。
 * </ul>
 *
 * <p>处理器方法支持以下返回类型：
 * <ul>
 * <li>{@code ModelAndView} 对象（来自 Servlet MVC）。
 * <li>{@link org.springframework.ui.Model} 对象，视图名称通过
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定。
 * <li>{@link java.util.Map} 对象用于暴露模型，视图名称通过
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定。
 * <li>{@link org.springframework.web.servlet.View} 对象。
 * <li>{@link String} 值，被解释为视图名称。
 * <li>{@link ResponseBody @ResponseBody} 注解的方法（仅限 Servlet），
 * 用于设置响应内容。返回值将使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}
 * 转换为响应流。
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} 或
 * {@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;} 对象
 * （仅限 Servlet），用于设置响应头和内容。ResponseEntity 的正文将使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}
 * 转换并写入响应流。
 * <li>{@code void}，如果方法自行处理响应（通过直接写入响应内容，为此声明
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * 类型的参数），或者如果视图名称应通过
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定
 * （不在处理器方法签名中声明响应参数）。
 * </ul>
 *
 * <p>可以将 {@code ExceptionHandler} 注解与 {@link ResponseStatus @ResponseStatus}
 * 结合使用，以指定特定的 HTTP 错误状态。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see ControllerAdvice
 * @see org.springframework.web.context.request.WebRequest
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {

	/**
	 * 注解方法所处理的异常。如果为空，则默认使用方法参数列表中列出的
	 * 任何异常。
	 */
	Class<? extends Throwable>[] value() default {};

}
