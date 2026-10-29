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

package org.springframework.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AliasFor;

/**
 * 用于将方法标记为应用事件监听器的注解。
 *
 * <p>如果带注解的方法支持单一事件类型，该方法可以声明一个反映要监听的事件类型的参数。如果带注解的
 * 方法支持多个事件类型，此注解可以使用 {@code classes} 属性引用一个或多个受支持的事件类型。更多细
 * 节请参见 {@link #classes} 的 javadoc。
 *
 * <p>事件可以是 {@link ApplicationEvent} 实例，也可以是任意对象。
 *
 * <p>{@code @EventListener} 注解的处理通过内部的 {@link EventListenerMethodProcessor} Bean 完成，
 * 在使用 Java 配置时会自动注册该 Bean，或者在使用 XML 配置时通过
 * {@code <context:annotation-config/>} 或 {@code <context:component-scan/>} 元素手动注册。
 *
 * <p>带注解的方法可以具有非 {@code void} 的返回类型。在这种情况下，方法调用的结果会作为新事件发送。
 * 如果返回类型是数组或集合，则每个元素都作为单独的新事件发送。
 *
 * <p>此注解可以用作<em>元注解</em>，以创建自定义的<em>组合注解</em>。
 *
 * <h3>异常处理</h3><p>虽然事件监听器可以声明抛出任意异常类型，但从事件监听器抛出的任何受检异常都会
 * 被包装在 {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException} 中，
 * 因为事件发布器只能处理运行时异常。
 *
 * <h3>异步监听器</h3><p>如果你希望某个监听器异步处理事件，可以使用 Spring 的
 * {@link org.springframework.scheduling.annotation.Async @Async} 支持，但在使用异步事件时应注意以
 * 下限制。
 *
 * <ul>
 * <li>如果异步事件监听器抛出异常，该异常不会传播给调用方。详情请参见 {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler AsyncUncaughtExceptionHandler}。</li>
 * <li>异步事件监听器方法不能通过返回值发布后续事件。如果你需要在处理结果中发布另一个事件，请注入 {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher} 来手动发布事件。</li>
 * </ul>
 *
 * <h3>监听器排序</h3><p>还可以定义某个事件的监听器的调用顺序。为此，请在此事件监听器注解旁添加
 * Spring 通用的 {@link org.springframework.core.annotation.Order @Order} 注解。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 * @see EventListenerMethodProcessor
 * @see org.springframework.transaction.event.TransactionalEventListener
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

	/**
	 * {@link #classes} 的别名。
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * 此监听器处理的事件类。<p>如果此属性指定了单个值，则带注解的方法可以选择性地接受一个参数。但是，
	 * 如果此属性指定了多个值，则带注解的方法<em>不得</em>声明任何参数。
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * 用于使事件处理带条件的 Spring 表达式语言（SpEL）表达式。<p>如果表达式求值为布尔值 {@code true}
	 * 或以下字符串之一，则会处理该事件：{@code "true"}、{@code "on"}、{@code "yes"} 或 {@code "1"}。
	 * <p>默认表达式为 {@code ""}，表示始终处理该事件。<p>SpEL 表达式将针对一个专用上下文求值，该上下文
	 * 提供以下元数据：
	 * <ul>
	 * <li>{@code #root.event} 或 {@code event}，用于引用 {@link ApplicationEvent}</li>
	 * <li>{@code #root.args} 或 {@code args}，用于引用方法参数数组</li>
	 * <li>方法参数可以通过索引访问。例如，第一个参数可以通过 {@code #root.args[0]}、{@code args[0]}、{@code #a0} 或 {@code #p0} 访问。</li>
	 * <li>如果编译后的字节码中提供了参数名，也可以通过参数名（前面加井号）访问方法参数。</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * 监听器的可选标识符，默认值为声明方法的完全限定签名（例如 "mypackage.MyClass.myMethod()"）。
	 * @since 5.3.5
	 * @see SmartApplicationListener#getListenerId()
	 * @see ApplicationEventMulticaster#removeApplicationListeners(Predicate)
	 */
	String id() default "";

}
