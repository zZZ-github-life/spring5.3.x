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

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理 {@link TestPropertySource @TestPropertySource} 以及将测试 {@link PropertySource PropertySources} 添加到 {@code Environment} 的工具方法。
 *
 * <p>主要供框架内部使用。
 *
 * @author Sam Brannen
 * @author Anatoliy Korovin
 * @author Phillip Webb
 * @since 4.1
 * @see TestPropertySource
 */
public abstract class TestPropertySourceUtils {

	/**
	 * 由<em>内联属性</em>创建的 {@link MapPropertySource} 的名称。
	 * @since 4.1.5
	 * @see #addInlinedPropertiesToEnvironment
	 */
	public static final String INLINED_PROPERTIES_PROPERTY_SOURCE_NAME = "Inlined Test Properties";

	private static final Log logger = LogFactory.getLog(TestPropertySourceUtils.class);


	static MergedTestPropertySources buildMergedTestPropertySources(Class<?> testClass) {
		List<TestPropertySourceAttributes> attributesList = new ArrayList<>();

		TestPropertySourceAttributes previousAttributes = null;
		// Iterate over all aggregate levels, where each level is represented by
		// a list of merged annotations found at that level (e.g., on a test
		// class in the class hierarchy).
		for (List<MergedAnnotation<TestPropertySource>> aggregatedAnnotations :
				findRepeatableAnnotations(testClass, TestPropertySource.class)) {

			// Convert all the merged annotations for the current aggregate
			// level to a list of TestPropertySourceAttributes.
			List<TestPropertySourceAttributes> aggregatedAttributesList =
					aggregatedAnnotations.stream().map(TestPropertySourceAttributes::new).collect(Collectors.toList());
			// Merge all TestPropertySourceAttributes instances for the current
			// aggregate level into a single TestPropertySourceAttributes instance.
			TestPropertySourceAttributes mergedAttributes = mergeTestPropertySourceAttributes(aggregatedAttributesList);
			if (mergedAttributes != null) {
				if (!duplicationDetected(mergedAttributes, previousAttributes)) {
					attributesList.add(mergedAttributes);
				}
				previousAttributes = mergedAttributes;
			}
		}

		if (attributesList.isEmpty()) {
			return MergedTestPropertySources.empty();
		}
		return new MergedTestPropertySources(mergeLocations(attributesList), mergeProperties(attributesList));
	}

	@Nullable
	private static TestPropertySourceAttributes mergeTestPropertySourceAttributes(
			List<TestPropertySourceAttributes> aggregatedAttributesList) {

		TestPropertySourceAttributes mergedAttributes = null;
		TestPropertySourceAttributes previousAttributes = null;
		for (TestPropertySourceAttributes currentAttributes : aggregatedAttributesList) {
			if (mergedAttributes == null) {
				mergedAttributes = currentAttributes;
			}
			else if (!duplicationDetected(currentAttributes, previousAttributes)) {
				mergedAttributes.mergeWith(currentAttributes);
			}
			previousAttributes = currentAttributes;
		}

		return mergedAttributes;
	}

	private static boolean duplicationDetected(TestPropertySourceAttributes currentAttributes,
			@Nullable TestPropertySourceAttributes previousAttributes) {

		boolean duplicationDetected =
				(currentAttributes.equals(previousAttributes) && !currentAttributes.isEmpty());

		if (duplicationDetected && logger.isDebugEnabled()) {
			logger.debug(String.format("Ignoring duplicate %s declaration on %s since it is also declared on %s",
					currentAttributes, currentAttributes.getDeclaringClass().getName(),
					previousAttributes.getDeclaringClass().getName()));
		}

		return duplicationDetected;
	}

	private static String[] mergeLocations(List<TestPropertySourceAttributes> attributesList) {
		List<String> locations = new ArrayList<>();
		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations for TestPropertySource attributes %s", attrs));
			}
			String[] locationsArray = TestContextResourceUtils.convertToClasspathResourcePaths(
					attrs.getDeclaringClass(), true, attrs.getLocations());
			locations.addAll(0, Arrays.asList(locationsArray));
			if (!attrs.isInheritLocations()) {
				break;
			}
		}
		return StringUtils.toStringArray(locations);
	}

	private static String[] mergeProperties(List<TestPropertySourceAttributes> attributesList) {
		List<String> properties = new ArrayList<>();
		for (TestPropertySourceAttributes attrs : attributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing inlined properties for TestPropertySource attributes %s", attrs));
			}
			String[] attrProps = attrs.getProperties();
			properties.addAll(0, Arrays.asList(attrProps));
			if (!attrs.isInheritProperties()) {
				break;
			}
		}
		return StringUtils.toStringArray(properties);
	}

	/**
	 * 将给定资源 {@code locations} 中的 {@link Properties} 文件添加到所提供 {@code context} 的 {@link Environment} 中。
	 * <p>此方法简单地委托给 {@link #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)}。
	 * @param context 应更新其环境的应用程序上下文；从不为 {@code null}
	 * @param locations 要添加到环境的 {@code Properties} 文件的资源位置；
	 * 可以为空，但从不为 {@code null}
	 * @throws IllegalStateException 如果处理属性文件时发生错误
	 * @since 4.1.5
	 * @see ResourcePropertySource
	 * @see TestPropertySource#locations
	 * @see #addPropertiesFilesToEnvironment(ConfigurableEnvironment, ResourceLoader, String...)
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableApplicationContext context, String... locations) {
		Assert.notNull(context, "'context' must not be null");
		Assert.notNull(locations, "'locations' must not be null");
		addPropertiesFilesToEnvironment(context.getEnvironment(), context, locations);
	}

	/**
	 * 将给定资源 {@code locations} 中的 {@link Properties} 文件添加到所提供的 {@link ConfigurableEnvironment environment} 中。
	 * <p>资源位置中的属性占位符（即 <code>${...}</code>）将根据 {@code Environment} 进行{@linkplain Environment#resolveRequiredPlaceholders(String) 解析}。
	 * <p>每个属性文件将被转换为 {@link ResourcePropertySource}，并以最高优先级添加到环境的 {@link PropertySources} 中。
	 * @param environment 要更新的环境；从不为 {@code null}
	 * @param resourceLoader 用于加载每个资源的 {@code ResourceLoader}；从不为 {@code null}
	 * @param locations 要添加到环境的 {@code Properties} 文件的资源位置；
	 * 可以为空，但从不为 {@code null}
	 * @throws IllegalStateException 如果处理属性文件时发生错误
	 * @since 4.3
	 * @see ResourcePropertySource
	 * @see TestPropertySource#locations
	 * @see #addPropertiesFilesToEnvironment(ConfigurableApplicationContext, String...)
	 */
	public static void addPropertiesFilesToEnvironment(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String... locations) {

		Assert.notNull(environment, "'environment' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		Assert.notNull(locations, "'locations' must not be null");
		try {
			for (String location : locations) {
				String resolvedLocation = environment.resolveRequiredPlaceholders(location);
				Resource resource = resourceLoader.getResource(resolvedLocation);
				environment.getPropertySources().addFirst(new ResourcePropertySource(resource));
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to add PropertySource to Environment", ex);
		}
	}

	/**
	 * 将给定的<em>内联属性</em>添加到所提供 {@code context} 的 {@link Environment} 中。
	 * <p>此方法简单地委托给 {@link #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])}。
	 * @param context 应更新其环境的应用程序上下文；从不为 {@code null}
	 * @param inlinedProperties 要添加到环境的内联属性；可以为空，但从不为 {@code null}
	 * @since 4.1.5
	 * @see TestPropertySource#properties
	 * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context, String... inlinedProperties) {
		Assert.notNull(context, "'context' must not be null");
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		addInlinedPropertiesToEnvironment(context.getEnvironment(), inlinedProperties);
	}

	/**
	 * 将给定的<em>内联属性</em>（以<em>键值</em>对形式）添加到所提供的 {@link ConfigurableEnvironment environment} 中。
	 * <p>所有键值对将作为单个具有最高优先级的 {@link MapPropertySource} 添加到 {@code Environment} 中。
	 * <p>有关<em>内联属性</em>解析的详细信息，请查阅 {@link #convertInlinedPropertiesToMap} 的 Javadoc。
	 * @param environment 要更新的环境；从不为 {@code null}
	 * @param inlinedProperties 要添加到环境的内联属性；可以为空，但从不为 {@code null}
	 * @since 4.1.5
	 * @see MapPropertySource
	 * @see #INLINED_PROPERTIES_PROPERTY_SOURCE_NAME
	 * @see TestPropertySource#properties
	 * @see #convertInlinedPropertiesToMap
	 */
	public static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String... inlinedProperties) {
		Assert.notNull(environment, "'environment' must not be null");
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		if (!ObjectUtils.isEmpty(inlinedProperties)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Adding inlined properties to environment: " +
						ObjectUtils.nullSafeToString(inlinedProperties));
			}
			MapPropertySource ps = (MapPropertySource)
					environment.getPropertySources().get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
			if (ps == null) {
				ps = new MapPropertySource(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME, new LinkedHashMap<>());
				environment.getPropertySources().addFirst(ps);
			}
			ps.getSource().putAll(convertInlinedPropertiesToMap(inlinedProperties));
		}
	}

	/**
	 * 将所提供的<em>内联属性</em>（以<em>键值</em>对形式）转换为以属性名为键的映射，并在返回的映射中保留属性名的顺序。
	 * <p>键值对的解析通过将所有对转换为内存中的<em>虚拟</em>属性文件，并委托给 {@link Properties#load(java.io.Reader)} 解析每个虚拟文件来实现。
	 * <p>有关<em>内联属性</em>的完整讨论，请查阅 {@link TestPropertySource#properties} 的 Javadoc。
	 * @param inlinedProperties 要转换的内联属性；可以为空，但从不为 {@code null}
	 * @return 包含转换后属性的新有序映射
	 * @throws IllegalStateException 如果无法解析给定的键值对，
	 * 或者给定的内联属性包含多个键值对
	 * @since 4.1.5
	 * @see #addInlinedPropertiesToEnvironment(ConfigurableEnvironment, String[])
	 */
	public static Map<String, Object> convertInlinedPropertiesToMap(String... inlinedProperties) {
		Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
		Map<String, Object> map = new LinkedHashMap<>();
		Properties props = new Properties();

		for (String pair : inlinedProperties) {
			if (!StringUtils.hasText(pair)) {
				continue;
			}
			try {
				props.load(new StringReader(pair));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to load test environment property from [" + pair + "]", ex);
			}
			Assert.state(props.size() == 1, () -> "Failed to load exactly one test environment property from [" + pair + "]");
			for (String name : props.stringPropertyNames()) {
				map.put(name, props.getProperty(name));
			}
			props.clear();
		}

		return map;
	}

	private static <T extends Annotation> List<List<MergedAnnotation<T>>> findRepeatableAnnotations(
			Class<?> clazz, Class<T> annotationType) {

		List<List<MergedAnnotation<T>>> listOfLists = new ArrayList<>();
		findRepeatableAnnotations(clazz, annotationType, listOfLists, new int[] {0});
		return listOfLists;
	}

	private static <T extends Annotation> void findRepeatableAnnotations(
			Class<?> clazz, Class<T> annotationType, List<List<MergedAnnotation<T>>> listOfLists, int[] aggregateIndex) {

		// Ensure we have a list for the current aggregate index.
		if (listOfLists.size() < aggregateIndex[0] + 1) {
			listOfLists.add(new ArrayList<>());
		}

		MergedAnnotations.from(clazz, SearchStrategy.DIRECT)
			.stream(annotationType)
			.sorted(highMetaDistancesFirst())
			.forEach(annotation -> listOfLists.get(aggregateIndex[0]).add(0, annotation));

		aggregateIndex[0]++;

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			findRepeatableAnnotations(ifc, annotationType, listOfLists, aggregateIndex);
		}

		// Declared on a superclass?
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null & superclass != Object.class) {
			findRepeatableAnnotations(superclass, annotationType, listOfLists, aggregateIndex);
		}

		// Declared on an enclosing class of an inner class?
		if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			findRepeatableAnnotations(clazz.getEnclosingClass(), annotationType, listOfLists, aggregateIndex);
		}
	}

	private static <A extends Annotation> Comparator<MergedAnnotation<A>> highMetaDistancesFirst() {
		return Comparator.<MergedAnnotation<A>> comparingInt(MergedAnnotation::getDistance).reversed();
	}

}
