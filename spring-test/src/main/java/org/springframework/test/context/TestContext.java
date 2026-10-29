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

package org.springframework.test.context;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code TestContext} 封装测试执行所处的上下文，
 * 与所使用的实际测试框架无关。
 *
 * <p>从 Spring Framework 5.0 开始，强烈建议具体实现
 * 提供<em>复制构造函数</em>，以便将 {@code TestContext} 的不可变状态
 * 和属性用作模板，为并行测试执行创建其他
 * 上下文。复制构造函数必须接受一个
 * 类型为具体实现的参数。任何不提供复制构造函数的实现
 * 都可能在并发执行测试的环境中
 * 失败。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TestContextManager
 * @see TestExecutionListener
 */
// Suppression required due to bug in javac in Java 8: presence of default method in a Serializable interface
@SuppressWarnings("serial")
public interface TestContext extends AttributeAccessor, Serializable {

	/**
	 * 确定此测试上下文的 {@linkplain ApplicationContext 应用程序上下文}
	 * 是否已知可用。
	 * <p>如果此方法返回 {@code true}，则后续调用
	 * {@link #getApplicationContext()} 应能成功。
	 * <p>此方法的默认实现始终返回 {@code false}。
	 * 因此，强烈建议自定义 {@code TestContext} 实现
	 * 覆写此方法并提供更有意义的实现。请注意，
	 * Spring 中标准的 {@code TestContext} 实现已适当地
	 * 覆写了此方法。
	 * @return {@code true} if the application context has already been loaded
	 * @since 5.2
	 * @see #getApplicationContext()
	 */
	default boolean hasApplicationContext() {
		return false;
	}

	/**
	 * 获取此测试上下文的
	 * {@linkplain ApplicationContext 应用程序上下文}，可能已缓存。
	 * <p>此方法的实现负责在相应上下文尚未加载时
	 * 加载应用程序上下文，并可能缓存该上下文。
	 * @return the application context (never {@code null})
	 * @throws IllegalStateException if an error occurs while retrieving the
	 * application context
	 * @see #hasApplicationContext()
	 */
	ApplicationContext getApplicationContext();

	/**
	 * 将由给定 {@code eventFactory} 创建的 {@link ApplicationEvent} 发布到此
	 * 测试上下文的 {@linkplain ApplicationContext 应用程序上下文}。
	 * <p>仅当此测试上下文的应用程序
	 * 上下文{@linkplain #hasApplicationContext() 可用}时，才会发布 {@code ApplicationEvent}。
	 * @param eventFactory factory for lazy creation of the {@code ApplicationEvent}
	 * @since 5.2
	 * @see #hasApplicationContext()
	 * @see #getApplicationContext()
	 */
	default void publishEvent(Function<TestContext, ? extends ApplicationEvent> eventFactory) {
		if (hasApplicationContext()) {
			getApplicationContext().publishEvent(eventFactory.apply(this));
		}
	}

	/**
	 * 获取此测试上下文的 {@linkplain Class 测试类}。
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * 获取此测试上下文的当前 {@linkplain Object 测试实例}。
	 * <p>注意：这是一个可变属性。
	 * @return the current test instance (never {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	Object getTestInstance();

	/**
	 * 获取此测试上下文的当前 {@linkplain Method 测试方法}。
	 * <p>注意：这是一个可变属性。
	 * @return the current test method (never {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	Method getTestMethod();

	/**
	 * 获取在
	 * {@linkplain #getTestMethod() 测试方法}执行期间抛出的 {@linkplain Throwable 异常}。
	 * <p>注意：这是一个可变属性。
	 * @return the exception that was thrown, or {@code null} if no exception was thrown
	 * @see #updateState(Object, Method, Throwable)
	 */
	@Nullable
	Throwable getTestException();

	/**
	 * 调用此方法以表明与此测试上下文关联的 {@linkplain ApplicationContext 应用程序
	 * 上下文}是<em>脏的</em>，应从上下文缓存中
	 * 移除。
	 * <p>如果测试修改了上下文（例如
	 * 修改单例 Bean 的状态、修改嵌入式数据库的状态等），请执行此操作。
	 * @param hierarchyMode the context cache clearing mode to be applied if the
	 * context is part of a hierarchy (may be {@code null})
	 */
	void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode);

	/**
	 * 更新此测试上下文以反映当前执行测试的状态。
	 * <p><strong>警告</strong>：此方法只应由
	 * {@link TestContextManager} 调用。
	 * <p>注意：对此方法的并发调用可能不是线程安全的，
	 * 具体取决于底层实现。
	 * @param testInstance the current test instance (may be {@code null})
	 * @param testMethod the current test method (may be {@code null})
	 * @param testException the exception that was thrown in the test method,
	 * or {@code null} if no exception was thrown
	 */
	void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException);

}
