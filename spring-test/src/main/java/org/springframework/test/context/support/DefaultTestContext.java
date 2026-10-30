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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link TestContext} 接口的默认实现。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 4.0
 */
public class DefaultTestContext implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>(4);

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	@Nullable
	private volatile Object testInstance;

	@Nullable
	private volatile Method testMethod;

	@Nullable
	private volatile Throwable testException;


	/**
	 * <em>复制构造器</em>，用于根据所提供的上下文的<em>属性</em>和不可变状态创建新的 {@code DefaultTestContext}。
	 * <p><em>不可变状态</em>包括传递给{@linkplain #DefaultTestContext(Class, MergedContextConfiguration, CacheAwareContextLoaderDelegate) 标准构造器}的所有参数。
	 * @throws NullPointerException 如果所提供的 {@code DefaultTestContext} 为 {@code null}
	 */
	public DefaultTestContext(DefaultTestContext testContext) {
		this(testContext.testClass, testContext.mergedContextConfiguration,
			testContext.cacheAwareContextLoaderDelegate);
		this.attributes.putAll(testContext.attributes);
	}

	/**
	 * 根据所提供的参数构造新的 {@code DefaultTestContext}。
	 * @param testClass 此测试上下文的测试类
	 * @param mergedContextConfiguration 此测试上下文的合并应用上下文配置
	 * @param cacheAwareContextLoaderDelegate 用于为此测试上下文加载和关闭应用上下文的委托
	 */
	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedContextConfiguration,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		Assert.notNull(testClass, "Test Class must not be null");
		Assert.notNull(mergedContextConfiguration, "MergedContextConfiguration must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * 确定此测试上下文的{@linkplain ApplicationContext 应用上下文}是否存在于上下文缓存中。
	 * @return 如果应用上下文已加载并存储在上下文缓存中，则为 {@code true}
	 * @since 5.2
	 * @see #getApplicationContext()
	 * @see CacheAwareContextLoaderDelegate#isContextLoaded
	 */
	@Override
	public boolean hasApplicationContext() {
		return this.cacheAwareContextLoaderDelegate.isContextLoaded(this.mergedContextConfiguration);
	}

	/**
	 * 获取此测试上下文的{@linkplain ApplicationContext 应用上下文}。
	 * <p>默认实现委托给构造此 {@code TestContext} 时提供的 {@link CacheAwareContextLoaderDelegate}。
	 * @throws IllegalStateException 如果上下文加载器委托返回的上下文不是<em>活动的</em>（即已关闭）
	 * @see CacheAwareContextLoaderDelegate#loadContext
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		if (context instanceof ConfigurableApplicationContext) {
			@SuppressWarnings("resource")
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			Assert.state(cac.isActive(), () ->
					"The ApplicationContext loaded for [" + this.mergedContextConfiguration +
					"] is not active. This may be due to one of the following reasons: " +
					"1) the context was closed programmatically by user code; " +
					"2) the context was closed during parallel test execution either " +
					"according to @DirtiesContext semantics or due to automatic eviction " +
					"from the ContextCache due to a maximum cache size policy.");
		}
		return context;
	}

	/**
	 * 将与此测试上下文关联的{@linkplain ApplicationContext 应用上下文}标记为<em>脏</em>（即，通过将其从上下文缓存中移除并关闭它）。
	 * <p>默认实现委托给构造此 {@code TestContext} 时提供的 {@link CacheAwareContextLoaderDelegate}。
	 * @see CacheAwareContextLoaderDelegate#closeContext
	 */
	@Override
	public void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode) {
		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedContextConfiguration, hierarchyMode);
	}

	@Override
	public final Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public final Object getTestInstance() {
		Object testInstance = this.testInstance;
		Assert.state(testInstance != null, "No test instance");
		return testInstance;
	}

	@Override
	public final Method getTestMethod() {
		Method testMethod = this.testMethod;
		Assert.state(testMethod != null, "No test method");
		return testMethod;
	}

	@Override
	@Nullable
	public final Throwable getTestException() {
		return this.testException;
	}

	@Override
	public void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.attributes) {
			if (value != null) {
				this.attributes.put(name, value);
			}
			else {
				this.attributes.remove(name);
			}
		}
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = this.attributes.computeIfAbsent(name, computeFunction);
		Assert.state(value != null,
				() -> String.format("Compute function must not return null for attribute named '%s'", name));
		return (T) value;
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.remove(name);
	}

	@Override
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.containsKey(name);
	}

	@Override
	public String[] attributeNames() {
		synchronized (this.attributes) {
			return StringUtils.toStringArray(this.attributes.keySet());
		}
	}


	/**
	 * 提供此测试上下文状态的字符串表示。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("testInstance", this.testInstance)
				.append("testMethod", this.testMethod)
				.append("testException", this.testException)
				.append("mergedContextConfiguration", this.mergedContextConfiguration)
				.append("attributes", this.attributes)
				.toString();
	}

}
