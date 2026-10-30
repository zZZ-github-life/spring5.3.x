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

package org.springframework.test.context;

/**
 * {@code TestExecutionListener} 定义了<em>监听器</em> API，用于响应由注册该监听器的 {@link TestContextManager} 发布的测试执行事件。
 * <p>请注意，并非所有测试框架都支持此 API 中定义的全部生命周期回调。例如，在使用 {@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule} 时，JUnit 4 不支持 {@link #beforeTestExecution} 和 {@link #afterTestExecution}。
 * <p>此接口为所有方法提供了空的 {@code default} 实现。因此，具体实现可以选择只重写适合当前任务的方法。
 * <p>具体实现必须提供 {@code public} 无参构造函数，以便工具和配置机制能够透明地实例化监听器。
 * <p>实现可以选择通过 {@link org.springframework.core.Ordered Ordered} 接口或 {@link org.springframework.core.annotation.Order @Order} 注解声明其在默认监听器链中的排序位置。有关详情，请参见 {@link TestContextBootstrapper#getTestExecutionListeners()}。
 * <h3>注册 TestExecutionListener 实现</h3>
 * <p>可以通过 {@link TestExecutionListeners @TestExecutionListeners} 注解为测试类、其子类及其嵌套类显式注册 {@code TestExecutionListener}。显式注册适用于仅在有限测试场景中使用的自定义监听器。然而，如果自定义监听器需要在整个测试套件中使用，这种方式可能会变得繁琐。通过支持基于 {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader} 机制自动发现<em>默认</em> {@code TestExecutionListener} 实现，可以解决这一问题。具体而言，默认 {@code TestExecutionListener} 实现可以在 {@code META-INF/spring.factories} 属性文件中以 {@code org.springframework.test.context.TestExecutionListener} 键注册。
 * <p>Spring 提供了以下实现。它们均实现了 {@code Ordered}，并默认自动注册。
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener ServletTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener DirtiesContextBeforeModesTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.event.ApplicationEventsTestExecutionListener ApplicationEventsTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener DependencyInjectionTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener SqlScriptsTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.event.EventPublishingTestExecutionListener EventPublishingTestExecutionListener}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TestExecutionListeners @TestExecutionListeners
 * @see TestContextManager
 * @see org.springframework.test.context.support.AbstractTestExecutionListener
 */
public interface TestExecutionListener {

	/**
	 * 在类中所有测试执行<em>之前</em>预处理测试类。
	 * <p>此方法应在特定于框架的 <em>before class</em> 生命周期回调之前立即调用。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 测试的测试上下文；永不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @since 3.0
	 */
	default void beforeTestClass(TestContext testContext) throws Exception {
	}

	/**
	 * 准备所提供{@linkplain TestContext 测试上下文}的{@linkplain Object 测试实例}，例如用于注入依赖。
	 * <p>此方法应在测试类实例化之后立即调用，或尽可能在实例化之后尽快调用（{@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule} 就是如此）。无论如何，此方法必须在任何特定于框架的生命周期回调之前调用。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 测试的测试上下文；永不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 */
	default void prepareTestInstance(TestContext testContext) throws Exception {
	}

	/**
	 * 在底层测试框架的 <em>before</em> 生命周期回调执行<em>之前</em>预处理测试，例如通过设置测试夹具。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>before</em> 生命周期回调之前立即调用。由于历史原因，此方法被命名为 {@code beforeTestMethod}。自引入 {@link #beforeTestExecution} 以来，此方法更合适的名称也许是 {@code beforeTestSetUp} 或 {@code beforeEach}；然而，出于向后兼容性方面的顾虑，遗憾的是无法重命名此方法。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 将执行测试方法的测试上下文；
	 * 永不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 */
	default void beforeTestMethod(TestContext testContext) throws Exception {
	}

	/**
	 * 在所提供的{@linkplain TestContext 测试上下文}中{@linkplain java.lang.reflect.Method 测试方法}执行<em>之前一刻</em>预处理测试，例如用于计时或日志记录。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>before</em> 生命周期回调之后调用。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 将要执行该测试方法的测试上下文；从不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #afterTestExecution
	 */
	default void beforeTestExecution(TestContext testContext) throws Exception {
	}

	/**
	 * 在所提供的{@linkplain TestContext 测试上下文}中{@linkplain java.lang.reflect.Method 测试方法}执行<em>之后一刻</em>后处理测试，例如用于计时或日志记录。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>after</em> 生命周期回调之前调用。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 将要执行该测试方法的测试上下文；从不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 */
	default void afterTestExecution(TestContext testContext) throws Exception {
	}

	/**
	 * 在底层测试框架的 <em>after</em> 生命周期回调执行<em>之后</em>后处理测试，例如通过拆除测试夹具。
	 * <p>此方法<strong>必须</strong>在特定于框架的 <em>after</em> 生命周期回调之后立即调用。由于历史原因，此方法被命名为 {@code afterTestMethod}。自引入 {@link #afterTestExecution} 以来，此方法更合适的名称也许是 {@code afterTestTearDown} 或 {@code afterEach}；然而，出于向后兼容性方面的顾虑，遗憾的是无法重命名此方法。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 已执行该测试方法的测试上下文；从不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @see #beforeTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 */
	default void afterTestMethod(TestContext testContext) throws Exception {
	}

	/**
	 * 在类中所有测试执行<em>之后</em>后处理测试类。
	 * <p>此方法应在特定于框架的 <em>after class</em> 生命周期回调之后立即调用。
	 * <p>默认实现为<em>空</em>。具体类可根据需要重写。
	 * @param testContext 该测试的测试上下文；从不为 {@code null}
	 * @throws Exception 允许任何异常传播
	 * @since 3.0
	 */
	default void afterTestClass(TestContext testContext) throws Exception {
	}

}
