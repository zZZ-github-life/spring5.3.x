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

package org.springframework.test.context.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.event.AfterTestClassEvent;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EventListener @EventListener} 注解，用于消费由 {@link org.springframework.test.context.event.EventPublishingTestExecutionListener EventPublishingTestExecutionListener} 发布的 {@link AfterTestClassEvent}。
 * <p>此注解可用于 Spring 测试 {@link org.springframework.context.ApplicationContext ApplicationContext} 中符合 {@code @EventListener} 的方法——例如，{@link org.springframework.context.annotation.Configuration @Configuration} 类中的方法。标注此注解的方法将作为 {@link org.springframework.test.context.TestExecutionListener#afterTestClass} 生命周期的一部分被调用。
 * <p>可以选择通过 SpEL 表达式使事件处理{@linkplain #value 带条件}——例如 {@code @AfterTestClass("event.testContext.testClass.name matches '.+IntegrationTests'")}。
 * <p>必须注册 {@code EventPublishingTestExecutionListener} 才能使此注解生效——例如，通过 {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners}。但请注意，{@code EventPublishingTestExecutionListener} 默认已注册。
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 * @see AfterTestClassEvent
 */
@Retention(RUNTIME)
@Target({ METHOD, ANNOTATION_TYPE })
@Documented
@EventListener(AfterTestClassEvent.class)
public @interface AfterTestClass {

	/**
	 * {@link EventListener#condition} 的别名。
	 */
	@AliasFor(annotation = EventListener.class, attribute = "condition")
	String value() default "";

}
