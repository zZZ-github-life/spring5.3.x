/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.test.context.TestContext;

/**
 * 为测试实例的依赖注入和初始化提供支持的 {@code TestExecutionListener}。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DependencyInjectionTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * {@link TestContext} 属性的属性名，用于指示是否应在 {@link #beforeTestMethod(TestContext) beforeTestMethod()} 中<em>重新注入</em>测试实例的依赖。请注意，无论如何都会在 {@link #prepareTestInstance(TestContext) prepareTestInstance()} 中注入依赖。
	 * <p>{@link TestContext} 的客户端（例如其他 {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}）因此可以选择设置此属性，以表明应在各个测试方法执行<em>之间</em>重新注入依赖。
	 * <p>允许的值包括 {@link Boolean#TRUE} 和 {@link Boolean#FALSE}。
	 */
	public static final String REINJECT_DEPENDENCIES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			DependencyInjectionTestExecutionListener.class, "reinjectDependencies");

	private static final Log logger = LogFactory.getLog(DependencyInjectionTestExecutionListener.class);


	/**
	 * 返回 {@code 2000}。
	 */
	@Override
	public final int getOrder() {
		return 2000;
	}

	/**
	 * 通过测试实例自身的{@link TestContext#getApplicationContext() 应用上下文}对所提供的 {@link TestContext 测试上下文}的{@link TestContext#getTestInstance() 测试实例}执行依赖注入（{@link AutowireCapableBeanFactory#autowireBeanProperties(Object, int, boolean) 自动装配}）和初始化（{@link AutowireCapableBeanFactory#initializeBean(Object, String) 初始化}）（不检查依赖）。
	 * <p>随后将从测试上下文中移除 {@link #REINJECT_DEPENDENCIES_ATTRIBUTE}，无论其值如何。
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Performing dependency injection for test context [" + testContext + "].");
		}
		injectDependencies(testContext);
	}

	/**
	 * 如果所提供的 {@link TestContext 测试上下文}中的 {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} 值为 {@link Boolean#TRUE}，则此方法的效果与 {@link #prepareTestInstance(TestContext) prepareTestInstance()} 相同；否则，此方法将不起作用。
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Reinjecting dependencies for test context [" + testContext + "].");
			}
			injectDependencies(testContext);
		}
	}

	/**
	 * 如 {@link #prepareTestInstance(TestContext) prepareTestInstance()} 中所述，为所提供的 {@link TestContext} 执行依赖注入和 Bean 初始化。
	 * <p>随后将从测试上下文中移除 {@link #REINJECT_DEPENDENCIES_ATTRIBUTE}，无论其值如何。
	 * @param testContext 应为其执行依赖注入的测试上下文（从不为 {@code null}）
	 * @throws Exception 允许任何异常传播
	 * @see #prepareTestInstance(TestContext)
	 * @see #beforeTestMethod(TestContext)
	 */
	protected void injectDependencies(TestContext testContext) throws Exception {
		Object bean = testContext.getTestInstance();
		Class<?> clazz = testContext.getTestClass();
		AutowireCapableBeanFactory beanFactory = testContext.getApplicationContext().getAutowireCapableBeanFactory();
		beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
		beanFactory.initializeBean(bean, clazz.getName() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX);
		testContext.removeAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE);
	}

}
