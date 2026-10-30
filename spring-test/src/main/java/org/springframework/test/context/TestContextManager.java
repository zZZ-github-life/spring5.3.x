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

package org.springframework.test.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestContextManager} 是 <em>Spring TestContext Framework</em>（Spring 测试上下文框架）的主要入口点。
 * <p>具体而言，{@code TestContextManager} 负责管理单个 {@link TestContext}，并在以下测试执行点向每个已注册的 {@link TestExecutionListener} 发送事件。
 * <ul>
 * <li>{@link #beforeTestClass() 测试类执行之前}：在某个测试框架的任意 <em>before class 回调</em>（例如 JUnit 4 的 {@link org.junit.BeforeClass @BeforeClass}）之前</li>
 * <li>{@link #prepareTestInstance 测试实例准备}：紧随测试类实例化之后</li>
 * <li>{@link #beforeTestMethod 测试准备之前}：在某个测试框架的任意 <em>before method 回调</em>（例如 JUnit 4 的 {@link org.junit.Before @Before}）之前</li>
 * <li>{@link #beforeTestExecution 测试执行之前}：紧邻 {@linkplain java.lang.reflect.Method 测试方法}执行之前，但在测试准备之后</li>
 * <li>{@link #afterTestExecution 测试执行之后}：紧邻 {@linkplain java.lang.reflect.Method 测试方法}执行之后，但在测试清理之前</li>
 * <li>{@link #afterTestMethod(Object, Method, Throwable) 测试清理之后}：在某个测试框架的任意 <em>after method 回调</em>（例如 JUnit 4 的 {@link org.junit.After @After}）之后</li>
 * <li>{@link #afterTestClass() 测试类执行之后}：在某个测试框架的任意 <em>after class 回调</em>（例如 JUnit 4 的 {@link org.junit.AfterClass @AfterClass}）之后</li>
 * </ul>
 * <p>对{@linkplain org.springframework.context.ApplicationContext 应用程序上下文}的加载与访问、测试实例的依赖注入、测试方法的{@linkplain org.springframework.transaction.annotation.Transactional 事务性}执行等支持，由 {@link SmartContextLoader ContextLoaders} 和 {@code TestExecutionListeners} 提供，它们分别通过 {@link ContextConfiguration @ContextConfiguration} 和 {@link TestExecutionListeners @TestExecutionListeners} 进行配置。
 * <p>{@code TestContext}、默认 {@code ContextLoader}、默认 {@code TestExecutionListeners} 及其协作者的引导工作由 {@link TestContextBootstrapper} 完成，并通过 {@link BootstrapWith @BootstrapWith} 进行配置。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @see TestContext
 * @see TestExecutionListener
 * @see TestExecutionListeners
 * @see ContextConfiguration
 * @see ContextHierarchy
 */
public class TestContextManager {

	private static final Log logger = LogFactory.getLog(TestContextManager.class);

	private final TestContext testContext;

	private final ThreadLocal<TestContext> testContextHolder = ThreadLocal.withInitial(
			// Implemented as an anonymous inner class instead of a lambda expression due to a bug
			// in Eclipse IDE: "The blank final field testContext may not have been initialized"
			new Supplier<TestContext>() {
				@Override
				public TestContext get() {
					return copyTestContext(TestContextManager.this.testContext);
				}
			});

	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<>();


	/**
	 * 为提供的{@linkplain Class 测试类}构造一个新的 {@code TestContextManager}。
	 * <p>使用为该测试类配置的 {@link TestContextBootstrapper} 委托给 {@link #TestContextManager(TestContextBootstrapper)}。如果测试类上直接或作为元注解存在 {@link BootstrapWith @BootstrapWith} 注解，则使用其 {@link BootstrapWith#value value} 作为引导器类型；否则使用 {@link org.springframework.test.context.support.DefaultTestContextBootstrapper DefaultTestContextBootstrapper}。
	 * @param testClass 要管理的测试类
	 * @see #TestContextManager(TestContextBootstrapper)
	 */
	public TestContextManager(Class<?> testClass) {
		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
	}

	/**
	 * 使用提供的 {@link TestContextBootstrapper} 构造一个新的 {@code TestContextManager}，并{@linkplain #registerTestExecutionListeners 注册}所需的 {@link TestExecutionListener TestExecutionListeners}。
	 * <p>由提供的 {@code TestContextBootstrapper} 负责构建 {@code TestContext} 并获取 {@code TestExecutionListeners}。
	 * @param testContextBootstrapper 要使用的引导器
	 * @see TestContextBootstrapper#buildTestContext
	 * @see TestContextBootstrapper#getTestExecutionListeners
	 * @see #registerTestExecutionListeners
	 */
	public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
		this.testContext = testContextBootstrapper.buildTestContext();
		registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
	}

	/**
	 * 获取此 {@code TestContextManager} 管理的 {@link TestContext}。
	 */
	public final TestContext getTestContext() {
		return this.testContextHolder.get();
	}

	/**
	 * 将提供的 {@link TestExecutionListener TestExecutionListeners} 列表追加到此 {@code TestContextManager} 使用的监听器列表末尾以完成注册。
	 * @see #registerTestExecutionListeners(TestExecutionListener...)
	 */
	public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
		registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[0]));
	}

	/**
	 * 将提供的 {@link TestExecutionListener TestExecutionListeners} 数组追加到此 {@code TestContextManager} 使用的监听器列表末尾以完成注册。
	 */
	public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
		for (TestExecutionListener listener : testExecutionListeners) {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering TestExecutionListener: " + listener);
			}
			this.testExecutionListeners.add(listener);
		}
	}

	/**
	 * 获取为此 {@code TestContextManager} 注册的当前 {@link TestExecutionListener TestExecutionListeners}。
	 * <p>允许进行修改，例如向列表开头添加监听器。不过，请确保在实际执行测试期间保持列表稳定。
	 */
	public final List<TestExecutionListener> getTestExecutionListeners() {
		return this.testExecutionListeners;
	}

	/**
	 * 按逆序获取为此 {@code TestContextManager} 注册的 {@link TestExecutionListener TestExecutionListeners} 的副本。
	 */
	private List<TestExecutionListener> getReversedTestExecutionListeners() {
		List<TestExecutionListener> listenersReversed = new ArrayList<>(getTestExecutionListeners());
		Collections.reverse(listenersReversed);
		return listenersReversed;
	}

	/**
	 * 用于在类中任何测试执行<em>之前</em>预处理测试类的钩子。应在任何特定于框架的<em>before class 方法</em>（例如使用 JUnit 4 的 {@link org.junit.BeforeClass @BeforeClass} 注解的方法）之前调用。
	 * <p>将尝试让每个已注册的 {@link TestExecutionListener} 有机会预处理测试类的执行。但是，如果某个监听器抛出异常，则其余已注册的监听器将<strong>不会</strong>被调用。
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @since 3.0
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestClass(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, "beforeTestClass", testExecutionListener, testClass);
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * 用于在执行任何单个测试方法之前准备测试实例的钩子，例如用于注入依赖。
	 * <p>此方法应在测试类实例化之后立即调用，或尽可能在实例化之后尽快调用（{@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule} 就是如此）。无论如何，此方法必须在任何特定于框架的生命周期回调之前调用。
	 * <p>受管理的 {@link TestContext} 将使用提供的 {@code testInstance} 进行更新。
	 * <p>将尝试让每个已注册的 {@link TestExecutionListener} 有机会准备测试实例。但是，如果某个监听器抛出异常，则其余已注册的监听器将<strong>不会</strong>被调用。
	 * @param testInstance 要准备的测试实例（永不为 {@code null}）
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @see #getTestExecutionListeners()
	 */
	public void prepareTestInstance(Object testInstance) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
		}
		getTestContext().updateState(testInstance, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.prepareTestInstance(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to prepare test instance [" + testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * 用于在底层测试框架的 <em>before</em> 生命周期回调之前预处理测试的钩子，例如设置测试夹具、启动事务等。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>before</em> 生命周期回调（例如使用 JUnit 4 的 {@link org.junit.Before @Before} 注解的方法）之前立即调用。由于历史原因，此方法被命名为 {@code beforeTestMethod}。自引入 {@link #beforeTestExecution} 以来，此方法更合适的名称也许是 {@code beforeTestSetUp} 或 {@code beforeEach}；然而，出于向后兼容性方面的顾虑，遗憾的是无法重命名此方法。
	 * <p>受管理的 {@link TestContext} 将使用提供的 {@code testInstance} 和 {@code testMethod} 进行更新。
	 * <p>将尝试让每个已注册的 {@link TestExecutionListener} 有机会执行其预处理。但是，如果某个监听器抛出异常，则其余已注册的监听器将<strong>不会</strong>被调用。
	 * @param testInstance 当前测试实例（永不为 {@code null}）
	 * @param testMethod 即将在该测试实例上执行的测试方法
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
		String callbackName = "beforeTestMethod";
		prepareForBeforeCallback(callbackName, testInstance, testMethod);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
			}
		}
	}

	/**
	 * 用于在提供的{@linkplain TestContext 测试上下文}中{@linkplain java.lang.reflect.Method 测试方法}执行<em>之前一刻</em>预处理测试的钩子，例如用于计时或日志记录。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>before</em> 生命周期回调（例如使用 JUnit 4 的 {@link org.junit.Before @Before} 注解的方法）之后调用。
	 * <p>受管理的 {@link TestContext} 将使用提供的 {@code testInstance} 和 {@code testMethod} 进行更新。
	 * <p>将尝试让每个已注册的 {@link TestExecutionListener} 有机会执行其预处理。但是，如果某个监听器抛出异常，则其余已注册的监听器将<strong>不会</strong>被调用。
	 * @param testInstance 当前测试实例（永不为 {@code null}）
	 * @param testMethod 即将在该测试实例上执行的测试方法
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestExecution(Object testInstance, Method testMethod) throws Exception {
		String callbackName = "beforeTestExecution";
		prepareForBeforeCallback(callbackName, testInstance, testMethod);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestExecution(getTestContext());
			}
			catch (Throwable ex) {
				handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
			}
		}
	}

	/**
	 * 用于在提供的{@linkplain TestContext 测试上下文}中{@linkplain java.lang.reflect.Method 测试方法}执行<em>之后一刻</em>后处理测试的钩子，例如用于计时或日志记录。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>after</em> 生命周期回调（例如使用 JUnit 4 的 {@link org.junit.After @After} 注解的方法）之前调用。
	 * <p>受管理的 {@link TestContext} 将使用提供的 {@code testInstance}、{@code testMethod} 和 {@code exception} 进行更新。
	 * <p>每个已注册的 {@link TestExecutionListener} 都将有机会执行其后处理。如果某个监听器抛出异常，其余已注册的监听器仍将被调用。在所有监听器执行完毕后，将重新抛出第一个捕获的异常，并将后续异常{@linkplain Throwable#addSuppressed 抑制}到第一个异常中。
	 * <p>请注意，已注册的监听器将按其注册顺序的相反顺序执行。
	 * @param testInstance 当前测试实例（永不为 {@code null}）
	 * @param testMethod 刚在该测试实例上执行的测试方法
	 * @param exception 在测试方法执行期间或由 TestExecutionListener
	 * 抛出的异常，如果未抛出则为 {@code null}
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestExecution(Object testInstance, Method testMethod, @Nullable Throwable exception)
			throws Exception {

		String callbackName = "afterTestExecution";
		prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
		Throwable afterTestExecutionException = null;

		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestExecution(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
				if (afterTestExecutionException == null) {
					afterTestExecutionException = ex;
				}
				else {
					afterTestExecutionException.addSuppressed(ex);
				}
			}
		}

		if (afterTestExecutionException != null) {
			ReflectionUtils.rethrowException(afterTestExecutionException);
		}
	}

	/**
	 * 用于在底层测试框架的 <em>after</em> 生命周期回调之后后处理测试的钩子，例如拆除测试夹具、结束事务等。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>after</em> 生命周期回调（例如使用 JUnit 4 的 {@link org.junit.After @After} 注解的方法）之后立即调用。由于历史原因，此方法被命名为 {@code afterTestMethod}。自引入 {@link #afterTestExecution} 以来，此方法更合适的名称也许是 {@code afterTestTearDown} 或 {@code afterEach}；然而，出于向后兼容性方面的顾虑，遗憾的是无法重命名此方法。
	 * <p>受管理的 {@link TestContext} 将使用提供的 {@code testInstance}、{@code testMethod} 和 {@code exception} 进行更新。
	 * <p>每个已注册的 {@link TestExecutionListener} 都将有机会执行其后处理。如果某个监听器抛出异常，其余已注册的监听器仍将被调用。在所有监听器执行完毕后，将重新抛出第一个捕获的异常，并将后续异常{@linkplain Throwable#addSuppressed 抑制}到第一个异常中。
	 * <p>请注意，已注册的监听器将按其注册顺序的相反顺序执行。
	 * @param testInstance 当前测试实例（永不为 {@code null}）
	 * @param testMethod 刚在该测试实例上执行的测试方法
	 * @param exception 在测试方法执行期间或由 TestExecutionListener
	 * 抛出的异常，如果未抛出则为 {@code null}
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @see #beforeTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestMethod(Object testInstance, Method testMethod, @Nullable Throwable exception)
			throws Exception {

		String callbackName = "afterTestMethod";
		prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
		Throwable afterTestMethodException = null;

		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
				if (afterTestMethodException == null) {
					afterTestMethodException = ex;
				}
				else {
					afterTestMethodException.addSuppressed(ex);
				}
			}
		}

		if (afterTestMethodException != null) {
			ReflectionUtils.rethrowException(afterTestMethodException);
		}
	}

	/**
	 * 用于在类中所有测试执行<em>之后</em>后处理测试类的钩子。应在任何特定于框架的 <em>after class 方法</em>（例如使用 JUnit 4 的 {@link org.junit.AfterClass @AfterClass} 注解的方法）之后调用。
	 * <p>每个已注册的 {@link TestExecutionListener} 都将有机会执行其后处理。如果某个监听器抛出异常，其余已注册的监听器仍将被调用。在所有监听器执行完毕后，将重新抛出第一个捕获的异常，并将后续异常{@linkplain Throwable#addSuppressed 抑制}到第一个异常中。
	 * <p>请注意，已注册的监听器将按其注册顺序的相反顺序执行。
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 * @since 3.0
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		Throwable afterTestClassException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestClass(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, "afterTestClass", testExecutionListener, testClass);
				if (afterTestClassException == null) {
					afterTestClassException = ex;
				}
				else {
					afterTestClassException.addSuppressed(ex);
				}
			}
		}

		this.testContextHolder.remove();

		if (afterTestClassException != null) {
			ReflectionUtils.rethrowException(afterTestClassException);
		}
	}

	private void prepareForBeforeCallback(String callbackName, Object testInstance, Method testMethod) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s(): instance [%s], method [%s]", callbackName, testInstance, testMethod));
		}
		getTestContext().updateState(testInstance, testMethod, null);
	}

	private void prepareForAfterCallback(String callbackName, Object testInstance, Method testMethod,
			@Nullable Throwable exception) {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s(): instance [%s], method [%s], exception [%s]",
					callbackName, testInstance, testMethod, exception));
		}
		getTestContext().updateState(testInstance, testMethod, exception);
	}

	private void handleBeforeException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
			Object testInstance, Method testMethod) throws Exception {

		logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
		ReflectionUtils.rethrowException(ex);
	}

	private void logException(
			Throwable ex, String callbackName, TestExecutionListener testExecutionListener, Class<?> testClass) {

		if (logger.isWarnEnabled()) {
			logger.warn(String.format("Caught exception while invoking '%s' callback on " +
					"TestExecutionListener [%s] for test class [%s]", callbackName, testExecutionListener,
					testClass), ex);
		}
	}

	private void logException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
			Object testInstance, Method testMethod) {

		if (logger.isWarnEnabled()) {
			logger.warn(String.format("Caught exception while invoking '%s' callback on " +
					"TestExecutionListener [%s] for test method [%s] and test instance [%s]",
					callbackName, testExecutionListener, testMethod, testInstance), ex);
		}
	}


	/**
	 * 尝试使用提供的 {@code TestContext} 的<em>拷贝构造函数</em>创建其副本。
	 */
	private static TestContext copyTestContext(TestContext testContext) {
		Constructor<? extends TestContext> constructor =
				ClassUtils.getConstructorIfAvailable(testContext.getClass(), testContext.getClass());

		if (constructor != null) {
			try {
				ReflectionUtils.makeAccessible(constructor);
				return constructor.newInstance(testContext);
			}
			catch (Exception ex) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Failed to invoke copy constructor for [%s]; " +
							"concurrent test execution is therefore likely not supported.",
							testContext), ex);
				}
			}
		}

		// Fallback to original instance
		return testContext;
	}

}
