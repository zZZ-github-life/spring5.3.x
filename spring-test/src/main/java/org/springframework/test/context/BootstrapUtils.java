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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@code BootstrapUtils} 是一组用于协助引导
 * <em>Spring TestContext Framework</em> 的工具方法。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
abstract class BootstrapUtils {

	private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
			"org.springframework.test.context.support.DefaultBootstrapContext";

	private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
			"org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.web.WebTestContextBootstrapper";

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
			"org.springframework.test.context.web.WebAppConfiguration";

	private static final Class<? extends Annotation> webAppConfigurationClass = loadWebAppConfigurationClass();

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	/**
	 * 为指定的 {@linkplain Class 测试类}创建 {@code BootstrapContext}。
	 * <p>使用反射创建一个 {@link org.springframework.test.context.support.DefaultBootstrapContext}，
	 * 它使用默认的 {@link CacheAwareContextLoaderDelegate} —— 该委托通过
	 * {@link CacheAwareContextLoaderDelegate#DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME}
	 * 系统属性配置，如果该系统属性未定义，则回退到
	 * {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}。
	 * @param testClass the test class for which the bootstrap context should be created
	 * @return a new {@code BootstrapContext}; never {@code null}
	 */
	@SuppressWarnings("unchecked")
	static BootstrapContext createBootstrapContext(Class<?> testClass) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
		Class<? extends BootstrapContext> clazz = null;
		try {
			clazz = (Class<? extends BootstrapContext>) ClassUtils.forName(
					DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME, BootstrapUtils.class.getClassLoader());
			Constructor<? extends BootstrapContext> constructor = clazz.getConstructor(
					Class.class, CacheAwareContextLoaderDelegate.class);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating BootstrapContext using constructor [%s]", constructor));
			}
			return BeanUtils.instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load BootstrapContext [" + clazz + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
		String className = SpringProperties.getProperty(
				CacheAwareContextLoaderDelegate.DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME);
		className = (StringUtils.hasText(className) ? className.trim() :
				DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME);
		try {
			Class<? extends CacheAwareContextLoaderDelegate> clazz =
					(Class<? extends CacheAwareContextLoaderDelegate>) ClassUtils.forName(
						className, BootstrapUtils.class.getClassLoader());
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]", className));
			}
			return BeanUtils.instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not create CacheAwareContextLoaderDelegate [" + className + "]", ex);
		}
	}

	/**
	 * 解析提供的 {@link BootstrapContext} 中测试类的 {@link TestContextBootstrapper} 类型，
	 * 实例化它，并向其提供对
	 * {@link BootstrapContext} 的引用。
	 * <p>如果测试类上存在 {@link BootstrapWith @BootstrapWith} 注解
	 * （直接存在或作为元注解存在），则将其
	 * {@link BootstrapWith#value value} 用作引导器类型。
	 * 否则，将根据是否存在
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}，
	 * 使用
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} 或
	 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
	 * WebTestContextBootstrapper}。
	 * @param bootstrapContext the bootstrap context to use
	 * @return a fully configured {@code TestContextBootstrapper}
	 */
	static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		Class<?> testClass = bootstrapContext.getTestClass();

		Class<?> clazz = null;
		try {
			clazz = resolveExplicitTestContextBootstrapper(testClass);
			if (clazz == null) {
				clazz = resolveDefaultTestContextBootstrapper(testClass);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
						testClass.getName(), clazz.getName()));
			}
			TestContextBootstrapper testContextBootstrapper =
					BeanUtils.instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);
			return testContextBootstrapper;
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz +
					"]. Specify @BootstrapWith's 'value' attribute or make the default bootstrapper class available.",
					ex);
		}
	}

	@Nullable
	private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
		Set<BootstrapWith> annotations = new LinkedHashSet<>();
		AnnotationDescriptor<BootstrapWith> descriptor =
				TestContextAnnotationUtils.findAnnotationDescriptor(testClass, BootstrapWith.class);
		while (descriptor != null) {
			annotations.addAll(descriptor.findAllLocalMergedAnnotations());
			descriptor = descriptor.next();
		}

		if (annotations.isEmpty()) {
			return null;
		}
		if (annotations.size() == 1) {
			return annotations.iterator().next().value();
		}

		// Allow directly-present annotation to override annotations that are meta-present.
		BootstrapWith bootstrapWith = testClass.getDeclaredAnnotation(BootstrapWith.class);
		if (bootstrapWith != null) {
			return bootstrapWith.value();
		}

		throw new IllegalStateException(String.format(
				"Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
				testClass.getName(), annotations));
	}

	private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
		boolean webApp = TestContextAnnotationUtils.hasAnnotation(testClass, webAppConfigurationClass);
		String bootstrapperClassName = (webApp ? DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME :
				DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME);
		return ClassUtils.forName(bootstrapperClassName, BootstrapUtils.class.getClassLoader());
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> loadWebAppConfigurationClass() {
		try {
			return (Class<? extends Annotation>) ClassUtils.forName(WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME,
				BootstrapUtils.class.getClassLoader());
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new IllegalStateException(
				"Failed to load class for @" + WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, ex);
		}
	}

}
