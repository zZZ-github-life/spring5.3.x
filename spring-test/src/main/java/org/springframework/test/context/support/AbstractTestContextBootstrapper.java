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

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TestContextBootstrapper} 接口的抽象实现，提供引导器所需的大部分行为。
 *
 * <p>具体子类通常只需为以下方法提供实现：
 * <ul>
 * <li>{@link #getDefaultContextLoaderClass}
 * <li>{@link #processMergedContextConfiguration}
 * </ul>
 *
 * <p>要插入自定义 {@link org.springframework.test.context.cache.ContextCache ContextCache} 支持，请重写 {@link #getCacheAwareContextLoaderDelegate()}。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.1
 */
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {

	private final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private BootstrapContext bootstrapContext;


	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		Assert.state(this.bootstrapContext != null, "No BootstrapContext set");
		return this.bootstrapContext;
	}

	/**
	 * 使用与此引导器关联的 {@link BootstrapContext} 中的 {@linkplain Class 测试类}，并通过委托给 {@link #buildMergedContextConfiguration()} 和 {@link #getCacheAwareContextLoaderDelegate()} 来构建新的 {@link DefaultTestContext}。
	 * <p>具体子类可以选择重写此方法，以返回自定义的 {@link TestContext} 实现。
	 * @since 4.2
	 */
	@Override
	public TestContext buildTestContext() {
		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
				getCacheAwareContextLoaderDelegate());
	}

	@Override
	public final List<TestExecutionListener> getTestExecutionListeners() {
		Class<?> clazz = getBootstrapContext().getTestClass();
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<>();
		boolean usingDefaults = false;

		AnnotationDescriptor<TestExecutionListeners> descriptor =
				TestContextAnnotationUtils.findAnnotationDescriptor(clazz, annotationType);

		// Use defaults?
		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("@TestExecutionListeners is not present for class [%s]: using defaults.",
						clazz.getName()));
			}
			usingDefaults = true;
			classesList.addAll(getDefaultTestExecutionListenerClasses());
		}
		else {
			// Traverse the class hierarchy...
			while (descriptor != null) {
				Class<?> declaringClass = descriptor.getDeclaringClass();
				TestExecutionListeners testExecutionListeners = descriptor.getAnnotation();
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Retrieved @TestExecutionListeners [%s] for declaring class [%s].",
							testExecutionListeners, declaringClass.getName()));
				}

				boolean inheritListeners = testExecutionListeners.inheritListeners();
				AnnotationDescriptor<TestExecutionListeners> parentDescriptor = descriptor.next();

				// If there are no listeners to inherit, we might need to merge the
				// locally declared listeners with the defaults.
				if ((!inheritListeners || parentDescriptor == null) &&
						testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Merging default listeners with listeners configured via " +
								"@TestExecutionListeners for class [%s].", descriptor.getRootDeclaringClass().getName()));
					}
					usingDefaults = true;
					classesList.addAll(getDefaultTestExecutionListenerClasses());
				}

				classesList.addAll(0, Arrays.asList(testExecutionListeners.listeners()));

				descriptor = (inheritListeners ? parentDescriptor : null);
			}
		}

		Collection<Class<? extends TestExecutionListener>> classesToUse = classesList;
		// Remove possible duplicates if we loaded default listeners.
		if (usingDefaults) {
			classesToUse = new LinkedHashSet<>(classesList);
		}

		List<TestExecutionListener> listeners = instantiateListeners(classesToUse);
		// Sort by Ordered/@Order if we loaded default listeners.
		if (usingDefaults) {
			AnnotationAwareOrderComparator.sort(listeners);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Using TestExecutionListeners: " + listeners);
		}
		return listeners;
	}

	private List<TestExecutionListener> instantiateListeners(Collection<Class<? extends TestExecutionListener>> classes) {
		List<TestExecutionListener> listeners = new ArrayList<>(classes.size());
		for (Class<? extends TestExecutionListener> listenerClass : classes) {
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			}
			catch (BeanInstantiationException ex) {
				if (ex.getCause() instanceof NoClassDefFoundError) {
					// TestExecutionListener not applicable due to a missing dependency
					if (logger.isDebugEnabled()) {
						logger.debug(String.format(
								"Skipping candidate TestExecutionListener [%s] due to a missing dependency. " +
								"Specify custom listener classes or make the default listener classes " +
								"and their required dependencies available. Offending class: [%s]",
								listenerClass.getName(), ex.getCause().getMessage()));
					}
				}
				else {
					throw ex;
				}
			}
		}
		return listeners;
	}

	/**
	 * 获取此引导器的默认 {@link TestExecutionListener} 类。
	 * <p>此方法由 {@link #getTestExecutionListeners()} 调用，并委托给 {@link #getDefaultTestExecutionListenerClassNames()} 来检索类名。
	 * <p>如果无法加载某个特定类，将记录一条 {@code DEBUG} 消息，但不会重新抛出关联的异常。
	 */
	@SuppressWarnings("unchecked")
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<>();
		ClassLoader cl = getClass().getClassLoader();
		for (String className : getDefaultTestExecutionListenerClassNames()) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) ClassUtils.forName(className, cl));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className +
							"]. Specify custom listener classes or make the default listener classes available.", ex);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * 获取此引导器的默认 {@link TestExecutionListener} 类的名称。
	 * <p>默认实现会查找类路径上所有 {@code META-INF/spring.factories} 文件中配置的所有 {@code org.springframework.test.context.TestExecutionListener} 条目。
	 * <p>此方法由 {@link #getDefaultTestExecutionListenerClasses()} 调用。
	 * @return an <em>unmodifiable</em> list of names of default {@code TestExecutionListener}
	 * classes
	 * @see SpringFactoriesLoader#loadFactoryNames
	 */
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		List<String> classNames =
				SpringFactoriesLoader.loadFactoryNames(TestExecutionListener.class, getClass().getClassLoader());
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Loaded default TestExecutionListener class names from location [%s]: %s",
					SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION, classNames));
		}
		return Collections.unmodifiableList(classNames);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final MergedContextConfiguration buildMergedContextConfiguration() {
		Class<?> testClass = getBootstrapContext().getTestClass();
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

		if (TestContextAnnotationUtils.findAnnotationDescriptorForTypes(
				testClass, ContextConfiguration.class, ContextHierarchy.class) == null) {
			return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
		}

		if (TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ContextHierarchy.class) != null) {
			Map<String, List<ContextConfigurationAttributes>> hierarchyMap =
					ContextLoaderUtils.buildContextHierarchyMap(testClass);
			MergedContextConfiguration parentConfig = null;
			MergedContextConfiguration mergedConfig = null;

			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
				List<ContextConfigurationAttributes> reversedList = new ArrayList<>(list);
				Collections.reverse(reversedList);

				// Don't use the supplied testClass; instead ensure that we are
				// building the MCC for the actual test class that declared the
				// configuration for the current level in the context hierarchy.
				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

				mergedConfig = buildMergedContextConfiguration(
						declaringClass, reversedList, parentConfig, cacheAwareContextLoaderDelegate, true);
				parentConfig = mergedConfig;
			}

			// Return the last level in the context hierarchy
			Assert.state(mergedConfig != null, "No merged context configuration");
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass,
					ContextLoaderUtils.resolveContextConfigurationAttributes(testClass),
					null, cacheAwareContextLoaderDelegate, true);
		}
	}

	private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		List<ContextConfigurationAttributes> defaultConfigAttributesList =
				Collections.singletonList(new ContextConfigurationAttributes(testClass));

		ContextLoader contextLoader = resolveContextLoader(testClass, defaultConfigAttributesList);
		if (logger.isInfoEnabled()) {
			logger.info(String.format(
					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s], using %s",
					testClass.getName(), contextLoader.getClass().getSimpleName()));
		}
		return buildMergedContextConfiguration(testClass, defaultConfigAttributesList, null,
				cacheAwareContextLoaderDelegate, false);
	}

	/**
	 * 为所提供的 {@link Class testClass}、上下文配置属性和父上下文配置构建 {@link MergedContextConfiguration 合并上下文配置}。
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param configAttributesList the list of context configuration attributes for the
	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
	 * traversing up the class hierarchy and enclosing class hierarchy); never
	 * {@code null} or empty
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @param requireLocationsClassesOrInitializers whether locations, classes, or
	 * initializers are required; typically {@code true} but may be set to {@code false}
	 * if the configured loader supports empty configuration
	 * @return the merged context configuration
	 * @see #resolveContextLoader
	 * @see ContextLoaderUtils#resolveContextConfigurationAttributes
	 * @see SmartContextLoader#processContextConfiguration
	 * @see ContextLoader#processLocations
	 * @see ActiveProfilesUtils#resolveActiveProfiles
	 * @see ApplicationContextInitializerUtils#resolveInitializerClasses
	 * @see MergedContextConfiguration
	 */
	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, @Nullable MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			boolean requireLocationsClassesOrInitializers) {

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
		List<String> locations = new ArrayList<>();
		List<Class<?>> classes = new ArrayList<>();
		List<Class<?>> initializers = new ArrayList<>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
						configAttributes));
			}
			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(
						configAttributes.getDeclaringClass(), configAttributes.getLocations());
				locations.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}
			initializers.addAll(0, Arrays.asList(configAttributes.getInitializers()));
			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
				Collections.unmodifiableList(configAttributesList));

		Assert.state(!(requireLocationsClassesOrInitializers &&
				areAllEmpty(locations, classes, initializers, contextCustomizers)), () -> String.format(
				"%s was unable to detect defaults, and no ApplicationContextInitializers " +
				"or ContextCustomizers were declared for context configuration attributes %s",
				contextLoader.getClass().getSimpleName(), configAttributesList));

		MergedTestPropertySources mergedTestPropertySources =
				TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
				StringUtils.toStringArray(locations), ClassUtils.toClassArray(classes),
				ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList),
				ActiveProfilesUtils.resolveActiveProfiles(testClass),
				mergedTestPropertySources.getLocations(),
				mergedTestPropertySources.getProperties(),
				contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

		return processMergedContextConfiguration(mergedConfig);
	}

	private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		List<ContextCustomizerFactory> factories = getContextCustomizerFactories();
		Set<ContextCustomizer> customizers = new LinkedHashSet<>(factories.size());
		for (ContextCustomizerFactory factory : factories) {
			ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
			if (customizer != null) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	/**
	 * 获取此引导器的 {@link ContextCustomizerFactory} 实例。
	 * <p>默认实现使用 {@link SpringFactoriesLoader} 机制加载类路径上所有 {@code META-INF/spring.factories} 文件中配置的工厂。
	 * @since 4.3
	 * @see SpringFactoriesLoader#loadFactories
	 */
	protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
		return SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader());
	}

	/**
	 * 解析要用于所提供的 {@link ContextConfigurationAttributes} 列表的 {@link ContextLoader} {@linkplain Class 类}，然后实例化并返回该 {@code ContextLoader}。
	 * <p>如果用户未显式声明要使用哪个加载器，则 {@link #getDefaultContextLoaderClass} 返回的值将用作默认上下文加载器类。有关类解析过程的详细信息，请参见 {@link #resolveExplicitContextLoaderClass} 和 {@link #getDefaultContextLoaderClass}。
	 * @param testClass the test class for which the {@code ContextLoader} should be
	 * resolved; must not be {@code null}
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
	 * (never {@code null})
	 * @throws IllegalStateException if {@link #getDefaultContextLoaderClass(Class)}
	 * returns {@code null}
	 */
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(testClass, "Class must not be null");
		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
		if (contextLoaderClass == null) {
			contextLoaderClass = getDefaultContextLoaderClass(testClass);
		}
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
					contextLoaderClass.getName(), testClass.getName()));
		}
		return BeanUtils.instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * 解析要用于所提供的 {@link ContextConfigurationAttributes} 列表的 {@link ContextLoader} {@linkplain Class 类}。
	 * <p>从上下文配置属性层次结构中的第一层开始：
	 * <ol>
	 * <li>如果 {@link ContextConfigurationAttributes} 的 {@link ContextConfigurationAttributes#getContextLoaderClass() contextLoaderClass} 属性配置了显式类，则将返回该类。</li>
	 * <li>如果未在层次结构的当前层指定显式的 {@code ContextLoader} 类，则遍历到层次结构的下一层并返回步骤 #1。</li>
	 * </ol>
	 * @param configAttributesList the list of configuration attributes to process;
	 * must not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
	 * @return the {@code ContextLoader} class to use for the supplied configuration
	 * attributes, or {@code null} if no explicit loader is found
	 * @throws IllegalArgumentException if supplied configuration attributes are
	 * {@code null} or <em>empty</em>
	 */
	@Nullable
	protected Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Resolving ContextLoader for context configuration attributes %s",
						configAttributes));
			}
			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (ContextLoader.class != contextLoaderClass) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
							"Found explicit ContextLoader class [%s] for context configuration attributes %s",
							contextLoaderClass.getName(), configAttributes));
				}
				return contextLoaderClass;
			}
		}
		return null;
	}

	/**
	 * 获取用于与 {@code ContextCache} 进行透明交互的 {@link CacheAwareContextLoaderDelegate}。
	 * <p>默认实现简单地委托给 {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()}。
	 * <p>具体子类可以选择重写此方法，以返回带有自定义 {@link org.springframework.test.context.cache.ContextCache ContextCache} 支持的自定义 {@code CacheAwareContextLoaderDelegate} 实现。
	 * @return the context loader delegate (never {@code null})
	 */
	protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return getBootstrapContext().getCacheAwareContextLoaderDelegate();
	}

	/**
	 * 确定为所提供的测试类使用的默认 {@link ContextLoader} {@linkplain Class 类}。
	 * <p>仅当未通过 {@link ContextConfiguration#loader} 显式声明 {@code ContextLoader} 类时，才会使用此方法返回的类。
	 * @param testClass the test class for which to retrieve the default
	 * {@code ContextLoader} class
	 * @return the default {@code ContextLoader} class for the supplied test class
	 * (never {@code null})
	 */
	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

	/**
	 * 处理所提供的、新实例化的 {@link MergedContextConfiguration} 实例。
	 * <p>返回的 {@link MergedContextConfiguration} 实例可能是对原始实例的包装或替换。
	 * <p>默认实现只是原样返回所提供的实例。
	 * <p>具体子类可以选择根据所提供的实例中的属性返回 {@link MergedContextConfiguration} 的专用子类。
	 * @param mergedConfig the {@code MergedContextConfiguration} to process; never {@code null}
	 * @return a fully initialized {@code MergedContextConfiguration}; never {@code null}
	 */
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return mergedConfig;
	}


	private static boolean areAllEmpty(Collection<?>... collections) {
		return Arrays.stream(collections).allMatch(Collection::isEmpty);
	}

}
