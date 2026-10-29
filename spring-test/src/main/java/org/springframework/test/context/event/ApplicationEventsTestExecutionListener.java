/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.Serializable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener}，为 {@link ApplicationEvents} 提供支持。
 * <p>此监听器在测试执行生命周期的各个点管理当前线程的 {@code ApplicationEvents} 注册，并通过测试类中的 {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 字段将当前的 {@code ApplicationEvents} 实例提供给测试使用。
 * <p>如果测试类未标注或元标注 {@link RecordApplicationEvents @RecordApplicationEvents}，则此监听器实际上不做任何事。
 *
 * @author Sam Brannen
 * @since 5.3.3
 * @see ApplicationEvents
 * @see ApplicationEventsHolder
 */
public class ApplicationEventsTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * 用于 {@link TestContext} 属性的属性名，指示给定测试上下文的测试类是否标注了 {@link RecordApplicationEvents @RecordApplicationEvents}。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 */
	private static final String RECORD_APPLICATION_EVENTS = Conventions.getQualifiedAttributeName(
			ApplicationEventsTestExecutionListener.class, "recordApplicationEvents");

	private static final Object applicationEventsMonitor = new Object();


	/**
	 * 返回 {@code 1800}。
	 */
	@Override
	public final int getOrder() {
		return 1800;
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		if (recordApplicationEvents(testContext)) {
			registerListenerAndResolvableDependencyIfNecessary(testContext.getApplicationContext());
			ApplicationEventsHolder.registerApplicationEvents();
		}
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (recordApplicationEvents(testContext)) {
			// Register a new ApplicationEvents instance for the current thread
			// in case the test instance is shared -- for example, in TestNG or
			// JUnit Jupiter with @TestInstance(PER_CLASS) semantics.
			ApplicationEventsHolder.registerApplicationEventsIfNecessary();
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (recordApplicationEvents(testContext)) {
			ApplicationEventsHolder.unregisterApplicationEvents();
		}
	}

	private boolean recordApplicationEvents(TestContext testContext) {
		return testContext.computeAttribute(RECORD_APPLICATION_EVENTS, name ->
				TestContextAnnotationUtils.hasAnnotation(testContext.getTestClass(), RecordApplicationEvents.class));
	}

	private void registerListenerAndResolvableDependencyIfNecessary(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext,
				"The ApplicationContext for the test must be an AbstractApplicationContext");
		AbstractApplicationContext aac = (AbstractApplicationContext) applicationContext;
		// Synchronize to avoid race condition in parallel test execution
		synchronized(applicationEventsMonitor) {
			boolean notAlreadyRegistered = aac.getApplicationListeners().stream()
					.map(Object::getClass)
					.noneMatch(ApplicationEventsApplicationListener.class::equals);
			if (notAlreadyRegistered) {
				// Register a new ApplicationEventsApplicationListener.
				aac.addApplicationListener(new ApplicationEventsApplicationListener());

				// Register ApplicationEvents as a resolvable dependency for @Autowired support in test classes.
				ConfigurableListableBeanFactory beanFactory = aac.getBeanFactory();
				beanFactory.registerResolvableDependency(ApplicationEvents.class, new ApplicationEventsObjectFactory());
			}
		}
	}

	/**
	 * 按需公开当前 {@link ApplicationEvents} 对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class ApplicationEventsObjectFactory implements ObjectFactory<ApplicationEvents>, Serializable {

		@Override
		public ApplicationEvents getObject() {
			return ApplicationEventsHolder.getRequiredApplicationEvents();
		}

		@Override
		public String toString() {
			return "Current ApplicationEvents";
		}
	}

}
