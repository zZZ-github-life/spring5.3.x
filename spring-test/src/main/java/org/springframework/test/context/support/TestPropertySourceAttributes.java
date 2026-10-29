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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.log.LogMessage;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code TestPropertySourceAttributes} 封装通过 {@link TestPropertySource @TestPropertySource} 注解声明的属性。
 *
 * <p>除了封装已声明的属性外，{@code TestPropertySourceAttributes} 还强制执行配置规则。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see TestPropertySource
 * @see MergedTestPropertySources
 */
class TestPropertySourceAttributes {

	private static final Log logger = LogFactory.getLog(TestPropertySourceAttributes.class);


	private final Class<?> declaringClass;

	private final MergedAnnotation<?> rootAnnotation;

	private final List<String> locations = new ArrayList<>();

	private final boolean inheritLocations;

	private final List<String> properties = new ArrayList<>();

	private final boolean inheritProperties;


	TestPropertySourceAttributes(MergedAnnotation<TestPropertySource> annotation) {
		this.declaringClass = declaringClass(annotation);
		this.rootAnnotation = annotation.getRoot();
		this.inheritLocations = annotation.getBoolean("inheritLocations");
		this.inheritProperties = annotation.getBoolean("inheritProperties");
		addPropertiesAndLocationsFrom(annotation);
	}

	/**
	 * 将此 {@code TestPropertySourceAttributes} 实例与所提供的 {@code TestPropertySourceAttributes} 合并，并断言两组测试属性源属性在 {@link TestPropertySource#inheritLocations} 和 {@link TestPropertySource#inheritProperties} 标志上具有相同的值，且两个底层注解声明在同一个类上。
	 * @since 5.2
	 */
	void mergeWith(TestPropertySourceAttributes attributes) {
		Assert.state(attributes.declaringClass == this.declaringClass,
				() -> "Detected @TestPropertySource declarations within an aggregate index "
						+ "with different sources: " + this.declaringClass.getName() + " and "
						+ attributes.declaringClass.getName());
		logger.trace(LogMessage.format("Retrieved %s for declaring class [%s].",
				attributes, this.declaringClass.getName()));
		assertSameBooleanAttribute(this.inheritLocations, attributes.inheritLocations,
				"inheritLocations", attributes);
		assertSameBooleanAttribute(this.inheritProperties, attributes.inheritProperties,
				"inheritProperties", attributes);
		mergePropertiesAndLocationsFrom(attributes);
	}

	private void assertSameBooleanAttribute(boolean expected, boolean actual,
			String attributeName, TestPropertySourceAttributes that) {

		Assert.isTrue(expected == actual, () -> String.format(
				"@%s on %s and @%s on %s must declare the same value for '%s' as other " +
				"directly present or meta-present @TestPropertySource annotations",
			this.rootAnnotation.getType().getSimpleName(), this.declaringClass.getSimpleName(),
			that.rootAnnotation.getType().getSimpleName(), that.declaringClass.getSimpleName(),
			attributeName));
	}

	private void addPropertiesAndLocationsFrom(MergedAnnotation<TestPropertySource> mergedAnnotation) {
		String[] locations = mergedAnnotation.getStringArray("locations");
		String[] properties = mergedAnnotation.getStringArray("properties");
		addPropertiesAndLocations(locations, properties, declaringClass(mergedAnnotation), false);
	}

	private void mergePropertiesAndLocationsFrom(TestPropertySourceAttributes attributes) {
		addPropertiesAndLocations(attributes.getLocations(), attributes.getProperties(),
				attributes.getDeclaringClass(), true);
	}

	private void addPropertiesAndLocations(String[] locations, String[] properties,
			Class<?> declaringClass, boolean prepend) {

		if (ObjectUtils.isEmpty(locations) && ObjectUtils.isEmpty(properties)) {
			addAll(prepend, this.locations, detectDefaultPropertiesFile(declaringClass));
		}
		else {
			addAll(prepend, this.locations, locations);
			addAll(prepend, this.properties, properties);
		}
	}

	/**
	 * 将所提供的所有元素添加到给定列表中，并遵循 {@code prepend} 标志。
	 * <p>如果 {@code prepend} 标志为 {@code false}，则元素将追加到列表中。
	 * @param prepend whether the elements should be prepended to the list
	 * @param list the list to which to add the elements
	 * @param elements the elements to add to the list
	 */
	private void addAll(boolean prepend, List<String> list, String... elements) {
		list.addAll((prepend ? 0 : list.size()), Arrays.asList(elements));
	}

	private String detectDefaultPropertiesFile(Class<?> testClass) {
		String resourcePath = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".properties";
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);
		if (!classPathResource.exists()) {
			String msg = String.format(
					"Could not detect default properties file for test class [%s]: " +
							"%s does not exist. Either declare the 'locations' or 'properties' attributes " +
							"of @TestPropertySource or make the default properties file available.",
					testClass.getName(), classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Detected default properties file \"%s\" for test class [%s]",
					prefixedResourcePath, testClass.getName()));
		}
		return prefixedResourcePath;
	}

	/**
	 * 获取声明 {@code @TestPropertySource} 的{@linkplain Class 类}。
	 * @return the declaring class; never {@code null}
	 */
	Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * 获取通过 {@code @TestPropertySource} 声明的资源位置。
	 * <p>注意：返回的值可能表示<em>检测到的默认值</em>或合并后的位置，它们可能与通过单个 {@code @TestPropertySource} 注解声明的原始值不匹配。
	 * @return the resource locations; potentially <em>empty</em>
	 * @see TestPropertySource#value
	 * @see TestPropertySource#locations
	 */
	String[] getLocations() {
		return StringUtils.toStringArray(this.locations);
	}

	/**
	 * 获取通过 {@code @TestPropertySource} 声明的 {@code inheritLocations} 标志。
	 * @return the {@code inheritLocations} flag
	 * @see TestPropertySource#inheritLocations
	 */
	boolean isInheritLocations() {
		return this.inheritLocations;
	}

	/**
	 * 获取通过 {@code @TestPropertySource} 声明的内联属性。
	 * <p>注意：返回的值可能表示合并后的属性，它们可能与通过单个 {@code @TestPropertySource} 注解声明的原始值不匹配。
	 * @return the inlined properties; potentially <em>empty</em>
	 * @see TestPropertySource#properties
	 */
	String[] getProperties() {
		return StringUtils.toStringArray(this.properties);
	}

	/**
	 * 获取通过 {@code @TestPropertySource} 声明的 {@code inheritProperties} 标志。
	 * @return the {@code inheritProperties} flag
	 * @see TestPropertySource#inheritProperties
	 */
	boolean isInheritProperties() {
		return this.inheritProperties;
	}

	boolean isEmpty() {
		return (this.locations.isEmpty() && this.properties.isEmpty());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		TestPropertySourceAttributes that = (TestPropertySourceAttributes) other;
		if (!this.locations.equals(that.locations)) {
			return false;
		}
		if (!this.properties.equals(that.properties)) {
			return false;
		}
		if (this.inheritLocations != that.inheritLocations) {
			return false;
		}
		if (this.inheritProperties != that.inheritProperties) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = this.locations.hashCode();
		result = 31 * result + this.properties.hashCode();
		result = 31 * result + (this.inheritLocations ? 1231 : 1237);
		result = 31 * result + (this.inheritProperties ? 1231 : 1237);
		return result;
	}

	/**
	 * 提供 {@code @TestPropertySource} 属性和声明类的字符串表示。
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("declaringClass", this.declaringClass.getName())
				.append("locations", this.locations)
				.append("inheritLocations", this.inheritLocations)
				.append("properties", this.properties)
				.append("inheritProperties", this.inheritProperties)
				.toString();
	}

	private static Class<?> declaringClass(MergedAnnotation<?> mergedAnnotation) {
		Object source = mergedAnnotation.getSource();
		Assert.state(source instanceof Class, "No source class available");
		return (Class<?>) source;
	}

}
