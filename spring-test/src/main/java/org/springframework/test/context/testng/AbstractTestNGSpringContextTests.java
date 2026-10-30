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

package org.springframework.test.context.testng;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * 抽象基测试类，在 <strong>TestNG</strong> 环境中将 <em>Spring TestContext Framework</em> 与显式的 {@link ApplicationContext} 测试支持集成在一起。
 * <p>具体子类通常应声明类级 {@link ContextConfiguration @ContextConfiguration} 注解，以配置 {@linkplain ApplicationContext 应用程序上下文}的{@linkplain ContextConfiguration#locations() 资源位置}或{@linkplain ContextConfiguration#classes() 组件类}。<em>如果测试不需要加载应用程序上下文，可以选择省略 {@link ContextConfiguration @ContextConfiguration} 声明，并手动配置适当的 {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}。</em>具体子类还必须具有隐式或显式委托给 {@code super();} 的构造函数。
 * <p>此类显式注册以下 {@code TestExecutionListener} 实现。如果希望切换到<em>默认</em>监听器集合，请参阅 {@link TestExecutionListeners @TestExecutionListeners} 的类级 Javadoc 以了解详情。
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.ApplicationEventsTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * <li>{@link org.springframework.test.context.event.EventPublishingTestExecutionListener}
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see ServletTestExecutionListener
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see ApplicationEventsTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see EventPublishingTestExecutionListener
 * @see AbstractTransactionalTestNGSpringContextTests
 * @see org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
 */
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
	ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
	DirtiesContextTestExecutionListener.class, EventPublishingTestExecutionListener.class })
public abstract class AbstractTestNGSpringContextTests implements IHookable, ApplicationContextAware {

	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 通过 {@link #setApplicationContext(ApplicationContext)} 注入此测试实例的 {@link ApplicationContext}。
	 */
	@Nullable
	protected ApplicationContext applicationContext;

	private final TestContextManager testContextManager;

	@Nullable
	private Throwable testException;


	/**
	 * 构造一个新的 {@code AbstractTestNGSpringContextTests} 实例，并为当前测试类初始化内部的 {@link TestContextManager}。
	 */
	public AbstractTestNGSpringContextTests() {
		this.testContextManager = new TestContextManager(getClass());
	}

	/**
	 * 设置此测试实例要使用的 {@link ApplicationContext}，通过 {@link ApplicationContextAware} 语义提供。
	 * @param applicationContext 该测试运行所在的 ApplicationContext
	 */
	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * 委托配置的 {@link TestContextManager} 调用{@linkplain TestContextManager#beforeTestClass() 'before test class'}回调。
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 */
	@BeforeClass(alwaysRun = true)
	protected void springTestContextBeforeTestClass() throws Exception {
		this.testContextManager.beforeTestClass();
	}

	/**
	 * 委托配置的 {@link TestContextManager} 在执行任何单个测试之前{@linkplain TestContextManager#prepareTestInstance(Object) 准备}此测试实例，例如用于注入依赖等。
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 */
	@BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
	protected void springTestContextPrepareTestInstance() throws Exception {
		this.testContextManager.prepareTestInstance(this);
	}

	/**
	 * 委托配置的 {@link TestContextManager} 在实际执行测试之前{@linkplain TestContextManager#beforeTestMethod(Object,Method) 预处理}测试方法。
	 * @param testMethod 即将执行的测试方法
	 * @throws Exception 允许所有异常传播
	 */
	@BeforeMethod(alwaysRun = true)
	protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
		this.testContextManager.beforeTestMethod(this, testMethod);
	}

	/**
	 * 委托给所提供的 {@code callback} 中的{@linkplain IHookCallBack#runTestMethod(ITestResult) 测试方法}以执行实际测试，然后跟踪测试执行期间抛出的异常（如果有）。
	 * @see org.testng.IHookable#run(IHookCallBack, ITestResult)
	 */
	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		Method testMethod = testResult.getMethod().getConstructorOrMethod().getMethod();
		boolean beforeCallbacksExecuted = false;

		try {
			this.testContextManager.beforeTestExecution(this, testMethod);
			beforeCallbacksExecuted = true;
		}
		catch (Throwable ex) {
			this.testException = ex;
		}

		if (beforeCallbacksExecuted) {
			callBack.runTestMethod(testResult);
			this.testException = getTestResultException(testResult);
		}

		try {
			this.testContextManager.afterTestExecution(this, testMethod, this.testException);
		}
		catch (Throwable ex) {
			if (this.testException == null) {
				this.testException = ex;
			}
		}

		if (this.testException != null) {
			throwAsUncheckedException(this.testException);
		}
	}

	/**
	 * 委托配置的 {@link TestContextManager} 在实际测试执行后{@linkplain TestContextManager#afterTestMethod(Object, Method, Throwable) 后处理}测试方法。
	 * @param testMethod 刚刚在测试实例上执行的测试方法
	 * @throws Exception 允许所有异常传播
	 */
	@AfterMethod(alwaysRun = true)
	protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
		try {
			this.testContextManager.afterTestMethod(this, testMethod, this.testException);
		}
		finally {
			this.testException = null;
		}
	}

	/**
	 * 委托配置的 {@link TestContextManager} 调用{@linkplain TestContextManager#afterTestClass() 'after test class'}回调。
	 * @throws Exception 如果已注册的 TestExecutionListener 抛出异常
	 */
	@AfterClass(alwaysRun = true)
	protected void springTestContextAfterTestClass() throws Exception {
		this.testContextManager.afterTestClass();
	}


	private Throwable getTestResultException(ITestResult testResult) {
		Throwable testResultException = testResult.getThrowable();
		if (testResultException instanceof InvocationTargetException) {
			testResultException = ((InvocationTargetException) testResultException).getCause();
		}
		return testResultException;
	}

	private RuntimeException throwAsUncheckedException(Throwable t) {
		throwAs(t);
		// Appeasing the compiler: the following line will never be executed.
		throw new IllegalStateException(t);
	}

	@SuppressWarnings("unchecked")
	private <T extends Throwable> void throwAs(Throwable t) throws T {
		throw (T) t;
	}

}
