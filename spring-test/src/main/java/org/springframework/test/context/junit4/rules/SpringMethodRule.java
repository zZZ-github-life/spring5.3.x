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

package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;

/**
 * {@code SpringMethodRule} 是一个自定义 JUnit 4 {@link MethodRule}，通过 {@link TestContextManager} 及相关支持类和注解，在标准 JUnit 测试中支持 <em>Spring TestContext Framework</em> 的实例级和方法级功能。
 * <p>与 {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner SpringJUnit4ClassRunner} 相比，Spring 基于规则的 JUnit 支持的优势在于它独立于任何 {@link org.junit.runner.Runner Runner}，因此可以与现有的替代运行器（如 JUnit 的 {@code Parameterized} 或第三方运行器如 {@code MockitoJUnitRunner}）结合使用。
 * <p>然而，为了实现与 {@code SpringJUnit4ClassRunner} 相同的功能，必须将 {@code SpringMethodRule} 与 {@link SpringClassRule} 结合使用，因为 {@code SpringMethodRule} 仅支持 {@code SpringJUnit4ClassRunner} 的实例级和方法级功能。
 * <h3>示例用法</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 * &#064;ClassRule
 * public static final SpringClassRule springClassRule = new SpringClassRule();
 *
 * &#064;Rule
 * public final SpringMethodRule springMethodRule = new SpringMethodRule();
 *
 * // ...
 * }</code></pre>
 * <p>以下列表列出了 {@code SpringMethodRule} 当前直接或间接支持的所有注解。<em>（请注意，各种 {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} 或 {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper} 实现可能支持其他注解。）</em>
 * <ul>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 * <p><strong>注意：</strong>从 Spring Framework 4.3 开始，此类要求 JUnit 4.12 或更高版本。
 * <p><strong>警告：</strong>由于 JUnit 规则的局限性，{@code SpringMethodRule} 在 {@code @Before} 生命周期方法之前{@linkplain org.springframework.test.context.TestExecutionListener#prepareTestInstance 准备测试实例}，而不是在测试类实例化之后立即准备。此外，{@code SpringMethodRule} <strong>不</strong>支持 {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} API 的 {@code beforeTestExecution()} 和 {@code afterTestExecution()} 回调。
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #apply(Statement, FrameworkMethod, Object)
 * @see SpringClassRule
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
public class SpringMethodRule implements MethodRule {

	private static final Log logger = LogFactory.getLog(SpringMethodRule.class);


	/**
	 * 将 <em>Spring TestContext Framework</em> 的<em>实例级</em>和<em>方法级</em>功能应用于所提供的 {@code base} 语句。
	 * <p>具体而言，此方法在 {@code TestContextManager} 上调用 {@link TestContextManager#prepareTestInstance prepareTestInstance()}、{@link TestContextManager#beforeTestMethod beforeTestMethod()} 和 {@link TestContextManager#afterTestMethod afterTestMethod()} 方法，并可能应用 Spring 超时和重复。
	 * <p>此外，此方法会检查测试在当前执行环境中是否启用。这可防止带有不匹配的 {@code @IfProfileValue} 注解的方法完全运行，甚至跳过 {@code TestExecutionListeners} 中 {@code prepareTestInstance()} 方法的执行。
	 * @param base the base {@code Statement} that this rule should be applied to
	 * @param frameworkMethod the method which is about to be invoked on the test instance
	 * @param testInstance the current test instance
	 * @return a statement that wraps the supplied {@code base} with instance-level
	 * and method-level features of the Spring TestContext Framework
	 * @see #withBeforeTestMethodCallbacks
	 * @see #withAfterTestMethodCallbacks
	 * @see #withPotentialRepeat
	 * @see #withPotentialTimeout
	 * @see #withTestInstancePreparation
	 * @see #withProfileValueCheck
	 */
	@Override
	public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object testInstance) {
		Method testMethod = frameworkMethod.getMethod();
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringMethodRule to test method [" + testMethod + "]");
		}
		Class<?> testClass = testInstance.getClass();
		TestContextManager testContextManager = SpringClassRule.getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
		statement = withAfterTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
		statement = withTestInstancePreparation(statement, testInstance, testContextManager);
		statement = withPotentialRepeat(statement, testMethod, testInstance);
		statement = withPotentialTimeout(statement, testMethod, testInstance);
		statement = withProfileValueCheck(statement, testMethod, testInstance);
		return statement;
	}

	/**
	 * 用 {@code RunBeforeTestMethodCallbacks} 语句包装所提供的 {@link Statement}。
	 * @see RunBeforeTestMethodCallbacks
	 */
	private Statement withBeforeTestMethodCallbacks(Statement next, Method testMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunBeforeTestMethodCallbacks(
				next, testInstance, testMethod, testContextManager);
	}

	/**
	 * 用 {@code RunAfterTestMethodCallbacks} 语句包装所提供的 {@link Statement}。
	 * @see RunAfterTestMethodCallbacks
	 */
	private Statement withAfterTestMethodCallbacks(Statement next, Method testMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunAfterTestMethodCallbacks(
				next, testInstance, testMethod, testContextManager);
	}

	/**
	 * 用 {@code RunPrepareTestInstanceCallbacks} 语句包装所提供的 {@link Statement}。
	 * @see RunPrepareTestInstanceCallbacks
	 */
	private Statement withTestInstancePreparation(
			Statement next, Object testInstance, TestContextManager testContextManager) {

		return new RunPrepareTestInstanceCallbacks(next, testInstance, testContextManager);
	}

	/**
	 * 用 {@code SpringRepeat} 语句包装所提供的 {@link Statement}。
	 * <p>支持 Spring 的 {@link org.springframework.test.annotation.Repeat @Repeat} 注解。
	 * @see SpringRepeat
	 */
	private Statement withPotentialRepeat(Statement next, Method testMethod, Object testInstance) {
		return new SpringRepeat(next, testMethod);
	}

	/**
	 * 用 {@code SpringFailOnTimeout} 语句包装所提供的 {@link Statement}。
	 * <p>支持 Spring 的 {@link org.springframework.test.annotation.Timed @Timed} 注解。
	 * @see SpringFailOnTimeout
	 */
	private Statement withPotentialTimeout(Statement next, Method testMethod, Object testInstance) {
		return new SpringFailOnTimeout(next, testMethod);
	}

	/**
	 * 用 {@code ProfileValueChecker} 语句包装所提供的 {@link Statement}。
	 * @see ProfileValueChecker
	 */
	private Statement withProfileValueCheck(Statement next, Method testMethod, Object testInstance) {
		return new ProfileValueChecker(next, testInstance.getClass(), testMethod);
	}

}
