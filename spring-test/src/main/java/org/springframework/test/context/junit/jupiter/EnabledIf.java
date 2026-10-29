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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @EnabledIf} 用于表示如果所提供的 {@link #expression} 求值为 {@code true}，则标注的测试类或测试方法处于<em>启用</em>状态，应当执行。
 * <p>当应用于类级别时，该类中的所有测试方法默认也会自动启用。
 * <p>有关基本示例，请参见 {@link #expression} 的 Javadoc。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。例如，可以按如下方式创建自定义 {@code @EnabledOnMac} 注解。
 * <pre style="code">
 * {@literal @}Target({ElementType.TYPE, ElementType.METHOD})
 * {@literal @}Retention(RetentionPolicy.RUNTIME)
 * {@literal @}EnabledIf(
 *     expression = "#{systemProperties['os.name'].toLowerCase().contains('mac')}",
 *     reason = "Enabled on Mac OS"
 * )
 * public {@literal @}interface EnabledOnMac {}
 * </pre>
 * <p>请注意，{@code @EnabledOnMac} 仅作为可能性示例。如果您恰好有这种用例，请使用 JUnit Jupiter 中内置的 {@link org.junit.jupiter.api.condition.EnabledOnOs @EnabledOnOs(MAC)} 支持。
 * <p>从 JUnit 5.7 开始，JUnit Jupiter 也提供了名为 {@link org.junit.jupiter.api.condition.EnabledIf @EnabledIf} 的条件注解。因此，如果希望使用 Spring 的 {@code @EnabledIf} 支持，请确保从正确的包中导入该注解类型。
 *
 * @author Sam Brannen
 * @since 5.0
 * @see SpringExtension
 * @see DisabledIf
 * @see org.junit.jupiter.api.Disabled
 * @see org.junit.jupiter.api.condition.EnabledIf
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfCondition.class)
public @interface EnabledIf {

	/**
	 * 作为 {@link #expression} 的别名；仅当未指定 {@link #reason} 和 {@link #loadContext} 时才打算使用。
	 *
	 * @see #expression
	 */
	@AliasFor("expression")
	String value() default "";

	/**
	 * 将对其求值以确定标注的测试类或测试方法是否<em>启用</em>的表达式。
	 * <p>如果表达式求值为 {@link Boolean#TRUE} 或等于 {@code "true"} 的 {@link String}（忽略大小写），则测试将被启用。
	 * <p>表达式可以是以下任意一种。
	 * <ul>
	 * <li>Spring 表达式语言（SpEL）表达式——例如：
	 * <pre style="code">@EnabledIf("#{systemProperties['os.name'].toLowerCase().contains('mac')}")</pre>
	 * <li>Spring {@link org.springframework.core.env.Environment Environment} 中可用属性的占位符——例如：
	 * <pre style="code">@EnabledIf("${smoke.tests.enabled}")</pre>
	 * <li>文本字面量——例如：
	 * <pre style="code">@EnabledIf("true")</pre>
	 * </ul>
	 * <p>但请注意，不是属性占位符动态解析结果的<em>文本字面量</em>没有任何实际价值，因为 {@code @EnabledIf("false")} 等价于 {@code @Disabled}，而 {@code @EnabledIf("true")} 在逻辑上没有意义。
	 *
	 * @see #reason
	 * @see #loadContext
	 * @see #value
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * 此测试被启用的原因。
	 *
	 * @see #expression
	 */
	String reason() default "";

	/**
	 * 是否应急切加载与当前测试关联的 {@code ApplicationContext} 以评估 {@link #expression}。
	 * <p>默认为 {@code false}，以避免不必要地急切加载测试应用程序上下文。如果表达式仅基于系统属性或环境变量，或者不与测试应用程序上下文中的 bean 交互，则无需过早加载上下文，因为如果测试最终被禁用，这样做会浪费时间。
	 *
	 * @see #expression
	 */
	boolean loadContext() default false;

}
