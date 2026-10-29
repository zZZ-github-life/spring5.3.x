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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.annotation.Testable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.support.PropertyProvider;
import org.springframework.test.context.support.TestConstructorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * {@code SpringExtension} 将 <em>Spring TestContext Framework</em> 集成到 JUnit 5 的 <em>Jupiter</em> 编程模型中。
 * <p>要使用此扩展，只需使用 {@code @ExtendWith(SpringExtension.class)}、{@code @SpringJUnitConfig} 或 {@code @SpringJUnitWebConfig} 标注基于 JUnit Jupiter 的测试类。
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.junit.jupiter.EnabledIf
 * @see org.springframework.test.context.junit.jupiter.DisabledIf
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
 * @see org.springframework.test.context.TestContextManager
 */
public class SpringExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor,
		BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
		ParameterResolver {

	/**
	 * 存储 {@code TestContextManagers} 的 {@link Namespace}，以测试类为键。
	 */
	private static final Namespace TEST_CONTEXT_MANAGER_NAMESPACE = Namespace.create(SpringExtension.class);

	/**
	 * 存储 {@code @Autowired} 验证错误消息的 {@link Namespace}，以测试类为键。
	 */
	private static final Namespace AUTOWIRED_VALIDATION_NAMESPACE =
			Namespace.create(SpringExtension.class.getName() + "#autowired.validation");

	private static final String NO_AUTOWIRED_VIOLATIONS_DETECTED = "NO AUTOWIRED VIOLATIONS DETECTED";

	// Note that @Test, @TestFactory, @TestTemplate, @RepeatedTest, and @ParameterizedTest
	// are all meta-annotated with @Testable.
	private static final List<Class<? extends Annotation>> JUPITER_ANNOTATION_TYPES =
			Arrays.asList(BeforeAll.class, AfterAll.class, BeforeEach.class, AfterEach.class, Testable.class);

	private static final MethodFilter autowiredTestOrLifecycleMethodFilter =
			ReflectionUtils.USER_DECLARED_METHODS
					.and(method -> !Modifier.isPrivate(method.getModifiers()))
					.and(SpringExtension::isAutowiredTestOrLifecycleMethod);


	/**
	 * 委托给 {@link TestContextManager#beforeTestClass}。
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		getTestContextManager(context).beforeTestClass();
	}

	/**
	 * 委托给 {@link TestContextManager#afterTestClass}。
	 */
	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		try {
			getTestContextManager(context).afterTestClass();
		}
		finally {
			getStore(context).remove(context.getRequiredTestClass());
		}
	}

	/**
	 * 委托给 {@link TestContextManager#prepareTestInstance}。
	 * <p>从 Spring Framework 5.3.2 开始，此方法还会验证测试方法和测试生命周期方法未标注 {@link Autowired @Autowired}。
	 */
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		validateAutowiredConfig(context);
		getTestContextManager(context).prepareTestInstance(testInstance);
	}

	/**
	 * 验证所提供测试类中的测试方法和测试生命周期方法未标注 {@link Autowired @Autowired}。
	 * @since 5.3.2
	 */
	private void validateAutowiredConfig(ExtensionContext context) {
		// We save the result in the ExtensionContext.Store so that we don't
		// re-validate all methods for the same test class multiple times.
		Store store = context.getStore(AUTOWIRED_VALIDATION_NAMESPACE);

		String errorMessage = store.getOrComputeIfAbsent(context.getRequiredTestClass(), testClass -> {
				Method[] methodsWithErrors =
						ReflectionUtils.getUniqueDeclaredMethods(testClass, autowiredTestOrLifecycleMethodFilter);
				return (methodsWithErrors.length == 0 ? NO_AUTOWIRED_VIOLATIONS_DETECTED :
						String.format(
								"Test methods and test lifecycle methods must not be annotated with @Autowired. " +
								"You should instead annotate individual method parameters with @Autowired, " +
								"@Qualifier, or @Value. Offending methods in test class %s: %s",
								testClass.getName(), Arrays.toString(methodsWithErrors)));
			}, String.class);

		if (errorMessage != NO_AUTOWIRED_VIOLATIONS_DETECTED) {
			throw new IllegalStateException(errorMessage);
		}
	}

	/**
	 * 委托给 {@link TestContextManager#beforeTestMethod}。
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
	}

	/**
	 * 委托给 {@link TestContextManager#beforeTestExecution}。
	 */
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		getTestContextManager(context).beforeTestExecution(testInstance, testMethod);
	}

	/**
	 * 委托给 {@link TestContextManager#afterTestExecution}。
	 */
	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		getTestContextManager(context).afterTestExecution(testInstance, testMethod, testException);
	}

	/**
	 * 委托给 {@link TestContextManager#afterTestMethod}。
	 */
	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Object testInstance = context.getRequiredTestInstance();
		Method testMethod = context.getRequiredTestMethod();
		Throwable testException = context.getExecutionException().orElse(null);
		getTestContextManager(context).afterTestMethod(testInstance, testMethod, testException);
	}

	/**
	 * 确定是否应从测试的 {@link ApplicationContext} 自动注入所提供的 {@link ParameterContext} 中 {@link Parameter} 的值。
	 * <p>如果以下条件之一为 {@code true}，则认为参数可自动注入。
	 * <ol>
	 * <li>{@linkplain ParameterContext#getDeclaringExecutable() 声明可执行对象}是 {@link Constructor}，并且 {@link TestConstructorUtils#isAutowirableConstructor(Constructor, Class, PropertyProvider)} 返回 {@code true}。请注意，{@code isAutowirableConstructor()} 将使用一个回退 {@link PropertyProvider} 调用，该提供程序将其查找委托给 {@link ExtensionContext#getConfigurationParameter(String)}。</li>
	 * <li>参数类型为 {@link ApplicationContext} 或其子类型。</li>
	 * <li>参数类型为 {@link ApplicationEvents} 或其子类型。</li>
	 * <li>{@link ParameterResolutionDelegate#isAutowirable} 返回 {@code true}。</li>
	 * </ol>
	 * <p><strong>警告</strong>：如果测试类的 {@code Constructor} 标注了 {@code @Autowired} 或可自动注入（请参见 {@link TestConstructor}），Spring 将负责解析构造函数中的所有参数。因此，其他已注册的 {@link ParameterResolver} 将无法解析参数。
	 * @see #resolveParameter
	 * @see TestConstructorUtils#isAutowirableConstructor(Constructor, Class)
	 * @see ParameterResolutionDelegate#isAutowirable
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		Executable executable = parameter.getDeclaringExecutable();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		PropertyProvider junitPropertyProvider = propertyName ->
				extensionContext.getConfigurationParameter(propertyName).orElse(null);
		return (TestConstructorUtils.isAutowirableConstructor(executable, testClass, junitPropertyProvider) ||
				ApplicationContext.class.isAssignableFrom(parameter.getType()) ||
				supportsApplicationEvents(parameterContext) ||
				ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
	}

	private boolean supportsApplicationEvents(ParameterContext parameterContext) {
		if (ApplicationEvents.class.isAssignableFrom(parameterContext.getParameter().getType())) {
			Assert.isTrue(parameterContext.getDeclaringExecutable() instanceof Method,
					"ApplicationEvents can only be injected into test and lifecycle methods");
			return true;
		}
		return false;
	}

	/**
	 * 通过从测试的 {@link ApplicationContext} 获取相应依赖，解析所提供的 {@link ParameterContext} 中 {@link Parameter} 的值。
	 * <p>委托给 {@link ParameterResolutionDelegate#resolveDependency}。
	 * @see #supportsParameter
	 * @see ParameterResolutionDelegate#resolveDependency
	 */
	@Override
	@Nullable
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Parameter parameter = parameterContext.getParameter();
		int index = parameterContext.getIndex();
		Class<?> testClass = extensionContext.getRequiredTestClass();
		ApplicationContext applicationContext = getApplicationContext(extensionContext);
		return ParameterResolutionDelegate.resolveDependency(parameter, index, testClass,
				applicationContext.getAutowireCapableBeanFactory());
	}


	/**
	 * 获取与所提供的 {@code ExtensionContext} 关联的 {@link ApplicationContext}。
	 * @param context the current {@code ExtensionContext} (never {@code null})
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the application context
	 * @see org.springframework.test.context.TestContext#getApplicationContext()
	 */
	public static ApplicationContext getApplicationContext(ExtensionContext context) {
		return getTestContextManager(context).getTestContext().getApplicationContext();
	}

	/**
	 * 获取与所提供的 {@code ExtensionContext} 关联的 {@link TestContextManager}。
	 * @return the {@code TestContextManager} (never {@code null})
	 */
	static TestContextManager getTestContextManager(ExtensionContext context) {
		Assert.notNull(context, "ExtensionContext must not be null");
		Class<?> testClass = context.getRequiredTestClass();
		Store store = getStore(context);
		return store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
	}

	private static Store getStore(ExtensionContext context) {
		return context.getRoot().getStore(TEST_CONTEXT_MANAGER_NAMESPACE);
	}

	private static boolean isAutowiredTestOrLifecycleMethod(Method method) {
		MergedAnnotations mergedAnnotations =
				MergedAnnotations.from(method, SearchStrategy.DIRECT, RepeatableContainers.none());
		if (!mergedAnnotations.isPresent(Autowired.class)) {
			return false;
		}
		for (Class<? extends Annotation> annotationType : JUPITER_ANNOTATION_TYPES) {
			if (mergedAnnotations.isPresent(annotationType)) {
				return true;
			}
		}
		return false;
	}

}
