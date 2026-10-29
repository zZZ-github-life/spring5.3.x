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

package org.springframework.test.context.event;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener}，将测试执行事件发布到当前执行测试所使用的 {@link org.springframework.context.ApplicationContext ApplicationContext}。
 * <h3>支持的事件</h3>
 * <ul>
 * <li>{@link BeforeTestClassEvent}</li>
 * <li>{@link PrepareTestInstanceEvent}</li>
 * <li>{@link BeforeTestMethodEvent}</li>
 * <li>{@link BeforeTestExecutionEvent}</li>
 * <li>{@link AfterTestExecutionEvent}</li>
 * <li>{@link AfterTestMethodEvent}</li>
 * <li>{@link AfterTestClassEvent}</li>
 * </ul>
 * <p>这些事件可出于各种原因被消费，例如重置 <em>mock</em> bean 或跟踪测试执行。与实现自定义 {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} 相比，消费测试事件的一个优势是，测试事件可以由测试 {@code ApplicationContext} 中注册的任何 Spring bean 消费，并且此类 bean 可以直接受益于依赖注入和 {@code ApplicationContext} 的其他功能。相比之下，{@code TestExecutionListener} 并不是 {@code ApplicationContext} 中的 bean。
 * <p>请注意，{@code EventPublishingTestExecutionListener} 默认已注册；但是，只有在 {@code ApplicationContext} {@linkplain TestContext#hasApplicationContext() 已经加载}时，它才会发布事件。这可避免不必要地或过早地加载 {@code ApplicationContext}。因此，{@code BeforeTestClassEvent} 要等到 {@code ApplicationContext} 被另一个 {@code TestExecutionListener} 加载之后才会发布。例如，在注册了默认 {@code TestExecutionListeners} 集合的情况下，对于使用特定测试 {@code ApplicationContext} 的第一个测试类，不会发布 {@code BeforeTestClassEvent}；但对于同一测试套件中随后使用相同测试 {@code ApplicationContext} 的任何测试类，都会发布 {@code BeforeTestClassEvent}，因为当后续测试类运行时上下文已经加载（只要该上下文未通过 {@link org.springframework.test.annotation.DirtiesContext @DirtiesContext} 或最大大小驱逐策略从 {@link org.springframework.test.context.cache.ContextCache ContextCache} 中移除）。如果希望确保为每个测试类都发布 {@code BeforeTestClassEvent}，需要注册一个在 {@link org.springframework.test.context.TestExecutionListener#beforeTestClass beforeTestClass} 回调中加载 {@code ApplicationContext} 的 {@code TestExecutionListener}，并且该 {@code TestExecutionListener} 必须注册在 {@code EventPublishingTestExecutionListener} 之前。类似地，如果在给定测试类的最后一个测试方法之后使用 {@code @DirtiesContext} 将 {@code ApplicationContext} 从上下文缓存中移除，则不会为该测试类发布 {@code AfterTestClassEvent}。
 * <h3>异常处理</h3>
 * <p>默认情况下，如果测试事件监听器在消费测试事件时抛出异常，该异常将传播到所使用的底层测试框架。例如，如果消费 {@code BeforeTestMethodEvent} 导致异常，则相应的测试方法将因该异常而失败。相比之下，如果异步测试事件监听器抛出异常，该异常不会传播到底层测试框架。有关异步异常处理的更多详情，请查阅 {@link org.springframework.context.event.EventListener @EventListener} 的类级 Javadoc。
 * <h3>异步监听器</h3>
 * <p>如果希望某个测试事件监听器异步处理事件，可以使用 Spring 的 {@link org.springframework.scheduling.annotation.Async @Async} 支持。有关更多详情，请查阅 {@link org.springframework.context.event.EventListener @EventListener} 的类级 Javadoc。
 *
 * @author Sam Brannen
 * @author Frank Scheffler
 * @since 5.2
 * @see org.springframework.test.context.event.annotation.BeforeTestClass @BeforeTestClass
 * @see org.springframework.test.context.event.annotation.PrepareTestInstance @PrepareTestInstance
 * @see org.springframework.test.context.event.annotation.BeforeTestMethod @BeforeTestMethod
 * @see org.springframework.test.context.event.annotation.BeforeTestExecution @BeforeTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestExecution @AfterTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestMethod @AfterTestMethod
 * @see org.springframework.test.context.event.annotation.AfterTestClass @AfterTestClass
 */
public class EventPublishingTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * 返回 {@code 10000}。
	 */
	@Override
	public final int getOrder() {
		return 10_000;
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link BeforeTestClassEvent}。
	 */
	@Override
	public void beforeTestClass(TestContext testContext) {
		testContext.publishEvent(BeforeTestClassEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link PrepareTestInstanceEvent}。
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) {
		testContext.publishEvent(PrepareTestInstanceEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link BeforeTestMethodEvent}。
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		testContext.publishEvent(BeforeTestMethodEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link BeforeTestExecutionEvent}。
	 */
	@Override
	public void beforeTestExecution(TestContext testContext) {
		testContext.publishEvent(BeforeTestExecutionEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link AfterTestExecutionEvent}。
	 */
	@Override
	public void afterTestExecution(TestContext testContext) {
		testContext.publishEvent(AfterTestExecutionEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link AfterTestMethodEvent}。
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		testContext.publishEvent(AfterTestMethodEvent::new);
	}

	/**
	 * 向所提供的 {@link TestContext} 的 {@code ApplicationContext} 发布 {@link AfterTestClassEvent}。
	 */
	@Override
	public void afterTestClass(TestContext testContext) {
		testContext.publishEvent(AfterTestClassEvent::new);
	}

}
