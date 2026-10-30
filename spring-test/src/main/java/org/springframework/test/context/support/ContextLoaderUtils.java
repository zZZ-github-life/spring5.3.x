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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import org.springframework.test.context.TestContextAnnotationUtils.UntypedAnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.isAnnotationDeclaredLocally;
import static org.springframework.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;
import static org.springframework.test.context.TestContextAnnotationUtils.findAnnotationDescriptorForTypes;

/**
 * 用于从 {@link ContextConfiguration @ContextConfiguration} 和 {@link ContextHierarchy @ContextHierarchy} 注解中解析 {@link ContextConfigurationAttributes} 以配合 {@link SmartContextLoader SmartContextLoaders} 使用的工具方法。
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see ContextConfiguration
 * @see ContextHierarchy
 */
abstract class ContextLoaderUtils {

	static final String GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX = "ContextHierarchyLevel#";

	private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);


	/**
	 * 为所提供的 {@linkplain Class 测试类}及其父类和外围类解析{@linkplain ContextConfigurationAttributes 上下文配置属性}的列表的列表，同时考虑通过 {@link ContextHierarchy @ContextHierarchy} 和 {@link ContextConfiguration @ContextConfiguration} 声明的上下文层次结构。
	 * <p>外层列表表示上下文配置属性的自顶向下顺序，其中列表中的每个元素表示在类层次结构或外围类层次结构中的给定测试类上声明的上下文配置。每个嵌套列表包含通过该特定类上的单个 {@code @ContextConfiguration} 实例声明的上下文配置属性，或通过该特定类上的单个 {@code @ContextHierarchy} 实例中声明的多个 {@code @ContextConfiguration} 实例声明的上下文配置属性。此外，每个嵌套列表保持 {@code @ContextConfiguration} 实例的声明顺序。
	 * <p>请注意，将<strong>不</strong>考虑 {@link ContextConfiguration @ContextConfiguration} 的 {@link ContextConfiguration#inheritLocations inheritLocations} 和 {@link ContextConfiguration#inheritInitializers() inheritInitializers} 标志。如果需要遵循这些标志，则必须在遍历此方法返回的嵌套列表时手动处理。
	 * @param testClass 要为其解析上下文层次结构属性的类（不能为 {@code null}）
	 * @return 指定类的配置属性列表的列表；从不为 {@code null}
	 * @throws IllegalArgumentException 如果所提供的类为 {@code null}；
	 * 或者如果所提供的类上<em>没有</em> {@code @ContextConfiguration}
	 * 也没有 {@code @ContextHierarchy}
	 * @throws IllegalStateException 如果类层次结构中的某个测试类或组合注解
	 * 将 {@code @ContextConfiguration} 和 {@code @ContextHierarchy}
	 * 同时声明为顶层注解
	 * @since 3.2.2
	 * @see #buildContextHierarchyMap(Class)
	 * @see #resolveContextConfigurationAttributes(Class)
	 */
	@SuppressWarnings("unchecked")
	static List<List<ContextConfigurationAttributes>> resolveContextHierarchyAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
		Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = new ArrayList<>();

		UntypedAnnotationDescriptor desc =
				findAnnotationDescriptorForTypes(testClass, contextConfigType, contextHierarchyType);
		Assert.notNull(desc, () -> String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] or [%s] and test class [%s]",
					contextConfigType.getName(), contextHierarchyType.getName(), testClass.getName()));

		while (desc != null) {
			Class<?> rootDeclaringClass = desc.getRootDeclaringClass();
			Class<?> declaringClass = desc.getDeclaringClass();

			boolean contextConfigDeclaredLocally = isAnnotationDeclaredLocally(contextConfigType, declaringClass);
			boolean contextHierarchyDeclaredLocally = isAnnotationDeclaredLocally(contextHierarchyType, declaringClass);

			if (contextConfigDeclaredLocally && contextHierarchyDeclaredLocally) {
				String msg = String.format("Class [%s] has been configured with both @ContextConfiguration " +
						"and @ContextHierarchy. Only one of these annotations may be declared on a test class " +
						"or composed annotation.", declaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			List<ContextConfigurationAttributes> configAttributesList = new ArrayList<>();

			if (contextConfigDeclaredLocally) {
				ContextConfiguration contextConfiguration = (ContextConfiguration) desc.getAnnotation();
				convertContextConfigToConfigAttributesAndAddToList(
						contextConfiguration, rootDeclaringClass, configAttributesList);
			}
			else if (contextHierarchyDeclaredLocally) {
				ContextHierarchy contextHierarchy = getAnnotation(declaringClass, contextHierarchyType);
				if (contextHierarchy != null) {
					for (ContextConfiguration contextConfiguration : contextHierarchy.value()) {
						convertContextConfigToConfigAttributesAndAddToList(
								contextConfiguration, rootDeclaringClass, configAttributesList);
					}
				}
			}
			else {
				// This should theoretically never happen...
				String msg = String.format("Test class [%s] has been configured with neither @ContextConfiguration " +
						"nor @ContextHierarchy as a class-level annotation.", rootDeclaringClass.getName());
				logger.error(msg);
				throw new IllegalStateException(msg);
			}

			hierarchyAttributes.add(0, configAttributesList);

			desc = desc.next();
		}

		return hierarchyAttributes;
	}

	/**
	 * 为所提供的 {@linkplain Class 测试类}及其父类和外围类构建<em>上下文层次结构映射</em>，同时考虑通过 {@link ContextHierarchy @ContextHierarchy} 和 {@link ContextConfiguration @ContextConfiguration} 声明的上下文层次结构。
	 * <p>映射中的每个值表示上下文层次结构中给定层级的{@linkplain ContextConfigurationAttributes 上下文配置属性}的合并列表（可能跨测试类层次结构和外围类层次结构），以该上下文层次结构层级的 {@link ContextConfiguration#name() 名称}为键。
	 * <p>如果上下文层次结构中的给定层级没有显式名称（即通过 {@link ContextConfiguration#name} 配置），则将通过在该层级的{@link #GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX}后追加数字层级来为该层次结构层级生成名称。
	 * @param testClass 要为其解析上下文层次结构映射的类（不能为 {@code null}）
	 * @return 上下文层次结构的上下文配置属性映射，以上下文层次结构级别名称为键；
	 * 从不为 {@code null}
	 * @throws IllegalArgumentException 如果 {@code @ContextHierarchy} 中每一级的上下文配置属性列表
	 * 在整体层次结构中未定义唯一的上下文配置
	 * @since 3.2.2
	 * @see #resolveContextHierarchyAttributes(Class)
	 */
	static Map<String, List<ContextConfigurationAttributes>> buildContextHierarchyMap(Class<?> testClass) {
		Map<String, List<ContextConfigurationAttributes>> map = new LinkedHashMap<>();
		int hierarchyLevel = 1;

		for (List<ContextConfigurationAttributes> configAttributesList : resolveContextHierarchyAttributes(testClass)) {
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				String name = configAttributes.getName();

				// Assign a generated name?
				if (!StringUtils.hasText(name)) {
					name = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + hierarchyLevel;
				}

				// Encountered a new context hierarchy level?
				if (!map.containsKey(name)) {
					hierarchyLevel++;
					map.put(name, new ArrayList<>());
				}

				map.get(name).add(configAttributes);
			}
		}

		// Check for uniqueness
		Set<List<ContextConfigurationAttributes>> set = new HashSet<>(map.values());
		if (set.size() != map.size()) {
			String msg = String.format("The @ContextConfiguration elements configured via @ContextHierarchy in " +
					"test class [%s] and its superclasses must define unique contexts per hierarchy level.",
					testClass.getName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}

		return map;
	}

	/**
	 * 为所提供的 {@linkplain Class 测试类}及其父类和外围类解析{@linkplain ContextConfigurationAttributes 上下文配置属性}列表。
	 * <p>请注意，将<strong>不</strong>考虑 {@link ContextConfiguration @ContextConfiguration} 的 {@link ContextConfiguration#inheritLocations inheritLocations} 和 {@link ContextConfiguration#inheritInitializers() inheritInitializers} 标志。如果需要遵循这些标志，则必须在遍历此方法返回的列表时手动处理。
	 * @param testClass 要为其解析配置属性的类（不能为 {@code null}）
	 * @return 指定类的配置属性列表，按<em>自下而上</em>顺序排列
	 * （即，如同向上遍历类层次结构和外围类层次结构）；
	 * 从不为 {@code null}
	 * @throws IllegalArgumentException 如果所提供的类为 {@code null}，
	 * 或者如果所提供的类上<em>不存在</em> {@code @ContextConfiguration}
	 */
	static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");

		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(testClass, annotationType);
		Assert.notNull(descriptor, () -> String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					annotationType.getName(), testClass.getName()));

		List<ContextConfigurationAttributes> attributesList = new ArrayList<>();
		ContextConfiguration previousAnnotation = null;
		Class<?> previousDeclaringClass = null;
		while (descriptor != null) {
			ContextConfiguration currentAnnotation = descriptor.getAnnotation();
			// Don't ignore duplicate @ContextConfiguration declaration without resources,
			// because the ContextLoader will likely detect default resources specific to the
			// annotated class.
			if (currentAnnotation.equals(previousAnnotation) && hasResources(currentAnnotation)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Ignoring duplicate %s declaration on [%s], "
							+ "since it is also declared on [%s].", currentAnnotation,
							previousDeclaringClass.getName(), descriptor.getRootDeclaringClass().getName()));
				}
			}
			else {
				convertContextConfigToConfigAttributesAndAddToList(currentAnnotation,
						descriptor.getRootDeclaringClass(), attributesList);
			}
			previousAnnotation = currentAnnotation;
			previousDeclaringClass = descriptor.getRootDeclaringClass();
			descriptor = descriptor.next();
		}
		return attributesList;
	}

	private static boolean hasResources(ContextConfiguration contextConfiguration) {
		return (contextConfiguration.locations().length > 0 || contextConfiguration.classes().length > 0);
	}

	/**
	 * 便捷方法，用于从所提供的 {@link ContextConfiguration} 注解和声明类创建 {@link ContextConfigurationAttributes} 实例，然后将该属性添加到所提供的列表中。
	 *
	 */
	private static void convertContextConfigToConfigAttributesAndAddToList(ContextConfiguration contextConfiguration,
			Class<?> declaringClass, List<ContextConfigurationAttributes> attributesList) {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
					contextConfiguration, declaringClass.getName()));
		}
		ContextConfigurationAttributes attributes =
				new ContextConfigurationAttributes(declaringClass, contextConfiguration);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved context configuration attributes: " + attributes);
		}
		attributesList.add(attributes);
	}

}
