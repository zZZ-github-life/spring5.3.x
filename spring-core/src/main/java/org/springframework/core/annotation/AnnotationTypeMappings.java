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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 为单个源注解类型提供 {@link AnnotationTypeMapping} 信息。对所有元注解执行递归的广度优先遍历，最
 * 终提供一种快速映射根 {@link Annotation} 属性的方式。
 *
 * <p>支持基于约定的元注解合并，以及隐式和显式的 {@link AliasFor @AliasFor} 别名。还提供有关镜像属
 * 性的信息。
 *
 * <p>此类的设计目标是可被缓存，这样无论元注解实际被使用多少次，都只需搜索一次。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 * @see AnnotationTypeMapping
 */
final class AnnotationTypeMappings {

	private static final IntrospectionFailureLogger failureLogger = IntrospectionFailureLogger.DEBUG;

	private static final Map<AnnotationFilter, Cache> standardRepeatablesCache = new ConcurrentReferenceHashMap<>();

	private static final Map<AnnotationFilter, Cache> noRepeatablesCache = new ConcurrentReferenceHashMap<>();


	private final RepeatableContainers repeatableContainers;

	private final AnnotationFilter filter;

	private final List<AnnotationTypeMapping> mappings;


	private AnnotationTypeMappings(RepeatableContainers repeatableContainers,
			AnnotationFilter filter, Class<? extends Annotation> annotationType,
			Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		this.repeatableContainers = repeatableContainers;
		this.filter = filter;
		this.mappings = new ArrayList<>();
		addAllMappings(annotationType, visitedAnnotationTypes);
		this.mappings.forEach(AnnotationTypeMapping::afterAllMappingsSet);
	}


	private void addAllMappings(Class<? extends Annotation> annotationType,
			Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
		addIfPossible(queue, null, annotationType, null, visitedAnnotationTypes);
		while (!queue.isEmpty()) {
			AnnotationTypeMapping mapping = queue.removeFirst();
			this.mappings.add(mapping);
			addMetaAnnotationsToQueue(queue, mapping);
		}
	}

	private void addMetaAnnotationsToQueue(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source) {
		Annotation[] metaAnnotations = AnnotationsScanner.getDeclaredAnnotations(source.getAnnotationType(), false);
		for (Annotation metaAnnotation : metaAnnotations) {
			if (!isMappable(source, metaAnnotation)) {
				continue;
			}
			Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(metaAnnotation);
			if (repeatedAnnotations != null) {
				for (Annotation repeatedAnnotation : repeatedAnnotations) {
					if (!isMappable(source, repeatedAnnotation)) {
						continue;
					}
					addIfPossible(queue, source, repeatedAnnotation);
				}
			}
			else {
				addIfPossible(queue, source, metaAnnotation);
			}
		}
	}

	private void addIfPossible(Deque<AnnotationTypeMapping> queue, AnnotationTypeMapping source, Annotation ann) {
		addIfPossible(queue, source, ann.annotationType(), ann, new HashSet<>());
	}

	private void addIfPossible(Deque<AnnotationTypeMapping> queue, @Nullable AnnotationTypeMapping source,
			Class<? extends Annotation> annotationType, @Nullable Annotation ann,
			Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		try {
			queue.addLast(new AnnotationTypeMapping(source, annotationType, ann, visitedAnnotationTypes));
		}
		catch (Exception ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			if (failureLogger.isEnabled()) {
				failureLogger.log("Failed to introspect meta-annotation " + annotationType.getName(),
						(source != null ? source.getAnnotationType() : null), ex);
			}
		}
	}

	private boolean isMappable(AnnotationTypeMapping source, @Nullable Annotation metaAnnotation) {
		return (metaAnnotation != null && !this.filter.matches(metaAnnotation) &&
				!AnnotationFilter.PLAIN.matches(source.getAnnotationType()) &&
				!isAlreadyMapped(source, metaAnnotation));
	}

	private boolean isAlreadyMapped(AnnotationTypeMapping source, Annotation metaAnnotation) {
		Class<? extends Annotation> annotationType = metaAnnotation.annotationType();
		AnnotationTypeMapping mapping = source;
		while (mapping != null) {
			if (mapping.getAnnotationType() == annotationType) {
				return true;
			}
			mapping = mapping.getSource();
		}
		return false;
	}

	/**
	 * 获取所包含映射的总数。
	 * @return 映射的总数
	 */
	int size() {
		return this.mappings.size();
	}

	/**
	 * 获取此实例中的单个映射。
	 * <p>索引 {@code 0} 始终返回根映射；更高的索引将返回元注解映射。
	 * @param index 要返回的索引
	 * @return 该 {@link AnnotationTypeMapping}
	 * @throws IndexOutOfBoundsException 如果索引超出范围
	 * （{@code index < 0 || index >= size()}）
	 */
	AnnotationTypeMapping get(int index) {
		return this.mappings.get(index);
	}


	/**
	 * 为指定的注解类型创建 {@link AnnotationTypeMappings}。
	 * @param annotationType 源注解类型
	 * @return 该注解类型的类型映射
	 */
	static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType) {
		return forAnnotationType(annotationType, new HashSet<>());
	}

	/**
	 * 为指定的注解类型创建 {@link AnnotationTypeMappings}。
	 * @param annotationType 源注解类型
	 * @param visitedAnnotationTypes 我们已经访问过的注解集合；用于避免
	 * 某些 JVM 语言（如 Kotlin）所支持的递归注解
	 * 引发的无限递归
	 * @return 该注解类型的类型映射
	 */
	static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
			Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		return forAnnotationType(annotationType, RepeatableContainers.standardRepeatables(),
				AnnotationFilter.PLAIN, visitedAnnotationTypes);
	}

	/**
	 * 为指定的注解类型创建 {@link AnnotationTypeMappings}。
	 * @param annotationType 源注解类型
	 * @param repeatableContainers 元注解可能使用的可重复容器
	 * @param annotationFilter 用于限定考虑哪些注解的注解过滤器
	 * @return 该注解类型的类型映射
	 */
	static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		return forAnnotationType(annotationType, repeatableContainers, annotationFilter, new HashSet<>());
	}

	/**
	 * 为指定的注解类型创建 {@link AnnotationTypeMappings}。
	 * @param annotationType 源注解类型
	 * @param repeatableContainers 元注解可能使用的可重复容器
	 * @param annotationFilter 用于限定考虑哪些注解的注解过滤器
	 * @param visitedAnnotationTypes 我们已经访问过的注解集合；用于避免
	 * 某些 JVM 语言（如 Kotlin）所支持的递归注解
	 * 引发的无限递归
	 * @return 该注解类型的类型映射
	 */
	private static AnnotationTypeMappings forAnnotationType(Class<? extends Annotation> annotationType,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter,
			Set<Class<? extends Annotation>> visitedAnnotationTypes) {

		if (repeatableContainers == RepeatableContainers.standardRepeatables()) {
			return standardRepeatablesCache.computeIfAbsent(annotationFilter,
					key -> new Cache(repeatableContainers, key)).get(annotationType, visitedAnnotationTypes);
		}
		if (repeatableContainers == RepeatableContainers.none()) {
			return noRepeatablesCache.computeIfAbsent(annotationFilter,
					key -> new Cache(repeatableContainers, key)).get(annotationType, visitedAnnotationTypes);
		}
		return new AnnotationTypeMappings(repeatableContainers, annotationFilter, annotationType, visitedAnnotationTypes);
	}

	static void clearCache() {
		standardRepeatablesCache.clear();
		noRepeatablesCache.clear();
	}


	/**
	 * 针对每个 {@link AnnotationFilter} 创建的缓存。
	 */
	private static class Cache {

		private final RepeatableContainers repeatableContainers;

		private final AnnotationFilter filter;

		private final Map<Class<? extends Annotation>, AnnotationTypeMappings> mappings;

		/**
		 * 使用指定的过滤器创建缓存实例。
		 * @param filter 注解过滤器
		 */
		Cache(RepeatableContainers repeatableContainers, AnnotationFilter filter) {
			this.repeatableContainers = repeatableContainers;
			this.filter = filter;
			this.mappings = new ConcurrentReferenceHashMap<>();
		}

		/**
		 * 获取或创建指定注解类型的 {@link AnnotationTypeMappings}。
		 * @param annotationType 注解类型
		 * @param visitedAnnotationTypes 我们已经访问过的注解集合；用于避免
		 * 某些 JVM 语言（如 Kotlin）所支持的递归注解
		 * 引发的无限递归
		 * @return 一个新的或已有的 {@link AnnotationTypeMappings} 实例
		 */
		AnnotationTypeMappings get(Class<? extends Annotation> annotationType,
				Set<Class<? extends Annotation>> visitedAnnotationTypes) {

			return this.mappings.computeIfAbsent(annotationType, key -> createMappings(key, visitedAnnotationTypes));
		}

		private AnnotationTypeMappings createMappings(Class<? extends Annotation> annotationType,
				Set<Class<? extends Annotation>> visitedAnnotationTypes) {

			return new AnnotationTypeMappings(this.repeatableContainers, this.filter, annotationType, visitedAnnotationTypes);
		}
	}

}
