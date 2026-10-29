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

package org.springframework.test.context.junit4.rules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.util.Assert;

/**
 * {@code SpringClassRule} 是一个自定义 JUnit {@link TestRule}，通过 {@link TestContextManager} 及相关支持类和注解，在标准 JUnit 测试中支持 <em>Spring TestContext Framework</em> 的<em>类级</em>功能。
 * <p>与 {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner SpringJUnit4ClassRunner} 相比，Spring 基于规则的 JUnit 支持的优势在于它独立于任何 {@link org.junit.runner.Runner Runner}，因此可以与现有的替代运行器（如 JUnit 的 {@code Parameterized} 或第三方运行器如 {@code MockitoJUnitRunner}）结合使用。
 * <p>然而，为了实现与 {@code SpringJUnit4ClassRunner} 相同的功能，必须将 {@code SpringClassRule} 与 {@link SpringMethodRule} 结合使用，因为 {@code SpringClassRule} 仅支持 {@code SpringJUnit4ClassRunner} 的类级功能。
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
 * <p>以下列表列出了 {@code SpringClassRule} 当前直接或间接支持的所有注解。<em>（请注意，各种 {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} 或 {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper} 实现可能支持其他注解。）</em>
 * <ul>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 * <p><strong>注意：</strong>从 Spring Framework 4.3 开始，此类要求 JUnit 4.12 或更高版本。
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #apply(Statement, Description)
 * @see SpringMethodRule
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
public class SpringClassRule implements TestRule {

	private static final Log logger = LogFactory.getLog(SpringClassRule.class);

	/**
	 * 以测试类为键的 {@code TestContextManagers} 缓存。
	 */
	private static final Map<Class<?>, TestContextManager> testContextManagerCache = new ConcurrentHashMap<>(64);


	/**
	 * 将 <em>Spring TestContext Framework</em> 的<em>类级</em>功能应用于所提供的 {@code base} 语句。
	 * <p>具体而言，此方法获取此规则及其关联的 {@link SpringMethodRule} 所使用的 {@link TestContextManager}，并在该 {@code TestContextManager} 上调用 {@link TestContextManager#beforeTestClass() beforeTestClass()} 和 {@link TestContextManager#afterTestClass() afterTestClass()} 方法。
	 * <p>此外，此方法会检查测试在当前执行环境中是否启用。这可防止带有不匹配的 {@code @IfProfileValue} 注解的类完全运行，甚至跳过 {@code TestExecutionListeners} 中 {@code beforeTestClass()} 方法的执行。
	 * @param base the base {@code Statement} that this rule should be applied to
	 * @param description a {@code Description} of the current test execution
	 * @return a statement that wraps the supplied {@code base} with class-level
	 * features of the Spring TestContext Framework
	 * @see #getTestContextManager
	 * @see #withBeforeTestClassCallbacks
	 * @see #withAfterTestClassCallbacks
	 * @see #withProfileValueCheck
	 * @see #withTestContextManagerCacheEviction
	 */
	@Override
	public Statement apply(Statement base, Description description) {
		Class<?> testClass = description.getTestClass();
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringClassRule to test class [" + testClass.getName() + "]");
		}
		TestContextManager testContextManager = getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestClassCallbacks(statement, testContextManager);
		statement = withAfterTestClassCallbacks(statement, testContextManager);
		statement = withProfileValueCheck(statement, testClass);
		statement = withTestContextManagerCacheEviction(statement, testClass);
		return statement;
	}

	/**
	 * 用 {@code RunBeforeTestClassCallbacks} 语句包装所提供的 {@link Statement}。
	 * @see RunBeforeTestClassCallbacks
	 */
	private Statement withBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		return new RunBeforeTestClassCallbacks(next, testContextManager);
	}

	/**
	 * 用 {@code RunAfterTestClassCallbacks} 语句包装所提供的 {@link Statement}。
	 * @see RunAfterTestClassCallbacks
	 */
	private Statement withAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		return new RunAfterTestClassCallbacks(next, testContextManager);
	}

	/**
	 * 用 {@code ProfileValueChecker} 语句包装所提供的 {@link Statement}。
	 * @see ProfileValueChecker
	 */
	private Statement withProfileValueCheck(Statement next, Class<?> testClass) {
		return new ProfileValueChecker(next, testClass, null);
	}

	/**
	 * 用 {@code TestContextManagerCacheEvictor} 语句包装所提供的 {@link Statement}。
	 * @see TestContextManagerCacheEvictor
	 */
	private Statement withTestContextManagerCacheEviction(Statement next, Class<?> testClass) {
		return new TestContextManagerCacheEvictor(next, testClass);
	}

	/**
	 * 获取与所提供的测试类关联的 {@link TestContextManager}。
	 * @param testClass the test class to be managed; never {@code null}
	 */
	static TestContextManager getTestContextManager(Class<?> testClass) {
		Assert.notNull(testClass, "Test Class must not be null");
		return testContextManagerCache.computeIfAbsent(testClass, TestContextManager::new);
	}


	private static class TestContextManagerCacheEvictor extends Statement {

		private final Statement next;

		private final Class<?> testClass;

		TestContextManagerCacheEvictor(Statement next, Class<?> testClass) {
			this.next = next;
			this.testClass = testClass;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.next.evaluate();
			}
			finally {
				testContextManagerCache.remove(this.testClass);
			}
		}
	}

}
