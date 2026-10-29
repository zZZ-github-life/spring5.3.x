/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.lang.Nullable;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestExecutionCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestExecutionCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringJUnit4ClassRunner} 是 JUnit {@link BlockJUnit4ClassRunner} 的自定义扩展，它通过 {@link TestContextManager} 及相关支持类和注解，将 <em>Spring TestContext Framework</em> 的功能提供给标准 JUnit 测试。
 * <p>要使用此类，只需使用 {@code @RunWith(SpringJUnit4ClassRunner.class)} 或 {@code @RunWith(SpringRunner.class)} 标注基于 JUnit 4 的测试类。
 * <p>以下列表列出了 {@code SpringJUnit4ClassRunner} 当前直接或间接支持的所有注解。<em>（请注意，各种 {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} 或 {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper} 实现可能支持其他注解。）</em>
 * <ul>
 * <li>{@link Test#expected() @Test(expected=...)}</li>
 * <li>{@link Test#timeout() @Test(timeout=...)}</li>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link Ignore @Ignore}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 * <p>如果希望将 Spring TestContext Framework 与此运行器以外的运行器一起使用，请使用 {@link SpringClassRule} 和 {@link SpringMethodRule}。
 * <p><strong>注意：</strong>从 Spring Framework 4.3 开始，此类要求 JUnit 4.12 或更高版本。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringRunner
 * @see TestContextManager
 * @see AbstractJUnit4SpringContextTests
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.junit4.rules.SpringClassRule
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 */
public class SpringJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(SpringJUnit4ClassRunner.class);

	private static final Method withRulesMethod;

	static {
		Assert.state(ClassUtils.isPresent("org.junit.internal.Throwables", SpringJUnit4ClassRunner.class.getClassLoader()),
				"SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");

		Method method = ReflectionUtils.findMethod(SpringJUnit4ClassRunner.class, "withRules",
				FrameworkMethod.class, Object.class, Statement.class);
		Assert.state(method != null, "SpringJUnit4ClassRunner requires JUnit 4.12 or higher");
		ReflectionUtils.makeAccessible(method);
		withRulesMethod = method;
	}


	private final TestContextManager testContextManager;


	private static void ensureSpringRulesAreNotPresent(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			Assert.state(!SpringClassRule.class.isAssignableFrom(field.getType()), () -> String.format(
					"Detected SpringClassRule field in test class [%s], " +
					"but SpringClassRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			Assert.state(!SpringMethodRule.class.isAssignableFrom(field.getType()), () -> String.format(
					"Detected SpringMethodRule field in test class [%s], " +
					"but SpringMethodRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
		}
	}

	/**
	 * 构造一个新的 {@code SpringJUnit4ClassRunner}，并初始化一个 {@link TestContextManager}，以为标准 JUnit 测试提供 Spring 测试功能。
	 * @param clazz the test class to be run
	 * @see #createTestContextManager(Class)
	 */
	public SpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "]");
		}
		ensureSpringRulesAreNotPresent(clazz);
		this.testContextManager = createTestContextManager(clazz);
	}

	/**
	 * 为所提供的测试类创建一个新的 {@link TestContextManager}。
	 * <p>可由子类重写。
	 * @param clazz the test class to be managed
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
	 * 获取与此运行器关联的 {@link TestContextManager}。
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * 如果测试在类级别通过 {@code @IfProfileValue} 被禁用，则返回适用于被忽略测试类的描述；否则委托给父实现。
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 */
	@Override
	public Description getDescription() {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			return Description.createSuiteDescription(getTestClass().getJavaClass());
		}
		return super.getDescription();
	}

	/**
	 * 检查测试在当前执行环境中是否启用。
	 * <p>这可防止带有不匹配的 {@code @IfProfileValue} 注解的类完全运行，甚至跳过 {@code TestExecutionListeners} 中 {@code prepareTestInstance()} 方法的执行。
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see org.springframework.test.annotation.IfProfileValue
	 * @see org.springframework.test.context.TestExecutionListener
	 */
	@Override
	public void run(RunNotifier notifier) {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			notifier.fireTestIgnored(getDescription());
			return;
		}
		super.run(notifier);
	}

	/**
	 * 用 {@code RunBeforeTestClassCallbacks} 语句包装父实现返回的 {@link Statement}，从而在保留默认 JUnit 功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunBeforeTestClassCallbacks
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
	}

	/**
	 * 用 {@code RunAfterTestClassCallbacks} 语句包装父实现返回的 {@link Statement}，从而在保留默认 JUnit 功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunAfterTestClassCallbacks
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement junitAfterClasses = super.withAfterClasses(statement);
		return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
	}

	/**
	 * 委托父实现创建测试实例，然后允许 {@link #getTestContextManager() TestContextManager} 在返回之前准备测试实例。
	 * @see TestContextManager#prepareTestInstance
	 */
	@Override
	protected Object createTest() throws Exception {
		Object testInstance = super.createTest();
		getTestContextManager().prepareTestInstance(testInstance);
		return testInstance;
	}

	/**
	 * 执行与 {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)} 相同的逻辑，不同之处在于通过 {@link #isTestMethodIgnored(FrameworkMethod)} 确定测试是否被<em>忽略</em>。
	 */
	@Override
	protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
		Description description = describeChild(frameworkMethod);
		if (isTestMethodIgnored(frameworkMethod)) {
			notifier.fireTestIgnored(description);
		}
		else {
			Statement statement;
			try {
				statement = methodBlock(frameworkMethod);
			}
			catch (Throwable ex) {
				statement = new Fail(ex);
			}
			runLeaf(statement, description, notifier);
		}
	}

	/**
	 * 增强默认 JUnit 行为，使整个执行链{@linkplain #withPotentialRepeat 可能重复执行}。
	 * <p>此外，超时支持已下移到执行链中，以便在计时执行中包含 {@link org.junit.Before @Before} 和 {@link org.junit.After @After} 方法的执行。请注意，这与默认 JUnit 行为不同：默认 JUnit 在主线程中执行 {@code @Before} 和 {@code @After} 方法，而在单独线程中执行实际测试方法。因此，最终效果是 {@code @Before} 和 {@code @After} 方法将与测试方法在同一线程中执行。因此，JUnit 指定的超时与 Spring 事务结合使用时可以正常工作。不过，JUnit 特有的超时与 Spring 特有的超时仍有所不同：前者在单独线程中执行，而后者只是像普通测试一样在主线程中执行。
	 * @see #methodInvoker(FrameworkMethod, Object)
	 * @see #withBeforeTestExecutionCallbacks(FrameworkMethod, Object, Statement)
	 * @see #withAfterTestExecutionCallbacks(FrameworkMethod, Object, Statement)
	 * @see #possiblyExpectingExceptions(FrameworkMethod, Object, Statement)
	 * @see #withBefores(FrameworkMethod, Object, Statement)
	 * @see #withAfters(FrameworkMethod, Object, Statement)
	 * @see #withRulesReflectively(FrameworkMethod, Object, Statement)
	 * @see #withPotentialRepeat(FrameworkMethod, Object, Statement)
	 * @see #withPotentialTimeout(FrameworkMethod, Object, Statement)
	 */
	@Override
	protected Statement methodBlock(FrameworkMethod frameworkMethod) {
		Object testInstance;
		try {
			testInstance = new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return createTest();
				}
			}.run();
		}
		catch (Throwable ex) {
			return new Fail(ex);
		}

		Statement statement = methodInvoker(frameworkMethod, testInstance);
		statement = withBeforeTestExecutionCallbacks(frameworkMethod, testInstance, statement);
		statement = withAfterTestExecutionCallbacks(frameworkMethod, testInstance, statement);
		statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
		statement = withBefores(frameworkMethod, testInstance, statement);
		statement = withAfters(frameworkMethod, testInstance, statement);
		statement = withRulesReflectively(frameworkMethod, testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		return statement;
	}

	/**
	 * 使用反射调用 JUnit 私有的 {@code withRules()} 方法。
	 */
	private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Object result = ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance, statement);
		Assert.state(result instanceof Statement, "withRules mismatch");
		return (Statement) result;
	}

	/**
	 * 如果所提供的{@linkplain FrameworkMethod 测试方法}存在 {@link Ignore @Ignore}，或者该测试方法通过 {@code @IfProfileValue} 被禁用，则返回 {@code true}。
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
	 */
	protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
		Method method = frameworkMethod.getMethod();
		return (method.isAnnotationPresent(Ignore.class) ||
				!ProfileValueUtils.isTestEnabledInThisEnvironment(method, getTestClass().getJavaClass()));
	}

	/**
	 * 执行与 {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)} 相同的逻辑，不同之处在于使用 {@link #getExpectedException(FrameworkMethod)} 获取<em>预期异常</em>。
	 */
	@Override
	protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
		return (expectedException != null ? new ExpectException(next, expectedException) : next);
	}

	/**
	 * 获取所提供的{@linkplain FrameworkMethod 测试方法}预期抛出的 {@code exception}。
	 * <p>支持 JUnit 的 {@link Test#expected() @Test(expected=...)} 注解。
	 * <p>可由子类重写。
	 * @return the expected exception, or {@code null} if none was specified
	 */
	@Nullable
	protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.expected() != Test.None.class ? test.expected() : null);
	}

	/**
	 * 执行与 {@link BlockJUnit4ClassRunner#withPotentialTimeout(FrameworkMethod, Object, Statement)} 相同的逻辑，但额外支持 Spring 的 {@code @Timed} 注解。
	 * <p>同时支持 Spring 的 {@link org.springframework.test.annotation.Timed @Timed} 和 JUnit 的 {@link Test#timeout() @Test(timeout=...)} 注解，但不能同时使用两者。
	 * @return either a {@link SpringFailOnTimeout}, a {@link FailOnTimeout},
	 * or the supplied {@link Statement} as appropriate
	 * @see #getSpringTimeout(FrameworkMethod)
	 * @see #getJUnitTimeout(FrameworkMethod)
	 */
	@Override
	// Retain the following warning suppression for deprecation (even if Eclipse
	// states it is unnecessary) since withPotentialTimeout(FrameworkMethod,Object,Statement)
	// in BlockJUnit4ClassRunner has been deprecated.
	@SuppressWarnings("deprecation")
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Statement statement = null;
		long springTimeout = getSpringTimeout(frameworkMethod);
		long junitTimeout = getJUnitTimeout(frameworkMethod);
		if (springTimeout > 0 && junitTimeout > 0) {
			String msg = String.format("Test method [%s] has been configured with Spring's @Timed(millis=%s) and " +
							"JUnit's @Test(timeout=%s) annotations, but only one declaration of a 'timeout' is " +
							"permitted per test method.", frameworkMethod.getMethod(), springTimeout, junitTimeout);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (springTimeout > 0) {
			statement = new SpringFailOnTimeout(next, springTimeout);
		}
		else if (junitTimeout > 0) {
			statement = FailOnTimeout.builder().withTimeout(junitTimeout, TimeUnit.MILLISECONDS).build(next);
		}
		else {
			statement = next;
		}

		return statement;
	}

	/**
	 * 从所提供的{@linkplain FrameworkMethod 测试方法}上的 {@link Test @Test} 注解中获取配置的 JUnit {@code timeout}。
	 * @return the timeout, or {@code 0} if none was specified
	 */
	protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test == null ? 0 : Math.max(0, test.timeout()));
	}

	/**
	 * 从所提供的{@linkplain FrameworkMethod 测试方法}上的 {@link org.springframework.test.annotation.Timed @Timed} 注解中获取配置的 Spring 特有 {@code timeout}。
	 * @return the timeout, or {@code 0} if none was specified
	 * @see TestAnnotationUtils#getTimeout(Method)
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		return TestAnnotationUtils.getTimeout(frameworkMethod.getMethod());
	}

	/**
	 * 用 {@code RunBeforeTestExecutionCallbacks} 语句包装所提供的 {@link Statement}，从而在保留默认功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunBeforeTestExecutionCallbacks
	 */
	protected Statement withBeforeTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new RunBeforeTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * 用 {@code RunAfterTestExecutionCallbacks} 语句包装所提供的 {@link Statement}，从而在保留默认功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunAfterTestExecutionCallbacks
	 */
	protected Statement withAfterTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new RunAfterTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * 用 {@code RunBeforeTestMethodCallbacks} 语句包装父实现返回的 {@link Statement}，从而在保留默认功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunBeforeTestMethodCallbacks
	 */
	@Override
	protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
		return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * 用 {@code RunAfterTestMethodCallbacks} 语句包装父实现返回的 {@link Statement}，从而在保留默认功能的同时增加对 Spring TestContext Framework 的支持。
	 * @see RunAfterTestMethodCallbacks
	 */
	@Override
	protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
		return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * 用 {@code SpringRepeat} 语句包装所提供的 {@link Statement}。
	 * <p>支持 Spring 的 {@link org.springframework.test.annotation.Repeat @Repeat} 注解。
	 * @see TestAnnotationUtils#getRepeatCount(Method)
	 * @see SpringRepeat
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		return new SpringRepeat(next, frameworkMethod.getMethod());
	}

}
