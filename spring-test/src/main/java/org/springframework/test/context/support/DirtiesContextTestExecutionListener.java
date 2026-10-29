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

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD;

/**
 * {@code TestExecutionListener}，为带有 {@link DirtiesContext @DirtiesContext} 注解的测试类和测试方法提供将测试关联的 {@code ApplicationContext} 标记为<em>脏</em>的支持。
 *
 * <p>此监听器支持{@linkplain DirtiesContext#methodMode 方法模式}设置为 {@link MethodMode#AFTER_METHOD AFTER_METHOD} 的测试方法，以及{@linkplain DirtiesContext#classMode() 类模式}设置为 {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD} 或 {@link ClassMode#AFTER_CLASS AFTER_CLASS} 的测试类。有关对 <em>BEFORE</em> 模式的支持，请参见 {@link DirtiesContextBeforeModesTestExecutionListener}。
 *
 * <p>在与默认值{@linkplain TestExecutionListeners#mergeMode 合并} {@code TestExecutionListeners} 时，此监听器将自动排在 {@link DependencyInjectionTestExecutionListener} 之后；否则，必须手动将此监听器配置为在 {@code DependencyInjectionTestExecutionListener} 之后执行。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see DirtiesContext
 * @see DirtiesContextBeforeModesTestExecutionListener
 */
public class DirtiesContextTestExecutionListener extends AbstractDirtiesContextTestExecutionListener {

	/**
	 * 返回 {@code 3000}。
	 */
	@Override
	public final int getOrder() {
		return 3000;
	}

	/**
	 * 如果所提供的 {@linkplain TestContext 测试上下文}的当前测试方法带有 {@code @DirtiesContext} 注解，且{@linkplain DirtiesContext#methodMode() 方法模式}设置为 {@link MethodMode#AFTER_METHOD AFTER_METHOD}，或者测试类带有 {@code @DirtiesContext} 注解且{@linkplain DirtiesContext#classMode() 类模式}设置为 {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}，则测试上下文的{@linkplain ApplicationContext 应用上下文}将被{@linkplain TestContext#markApplicationContextDirty 标记为脏}，并且测试上下文中的 {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE REINJECT_DEPENDENCIES_ATTRIBUTE} 将被设置为 {@code true}。
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, AFTER_METHOD, AFTER_EACH_TEST_METHOD);
	}

	/**
	 * 如果所提供的 {@linkplain TestContext 测试上下文}的测试类带有 {@code @DirtiesContext} 注解，且{@linkplain DirtiesContext#classMode() 类模式}设置为 {@link ClassMode#AFTER_CLASS AFTER_CLASS}，则测试上下文的{@linkplain ApplicationContext 应用上下文}将被{@linkplain TestContext#markApplicationContextDirty 标记为脏}，并且测试上下文中的 {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE REINJECT_DEPENDENCIES_ATTRIBUTE} 将被设置为 {@code true}。
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, AFTER_CLASS);
	}

}
