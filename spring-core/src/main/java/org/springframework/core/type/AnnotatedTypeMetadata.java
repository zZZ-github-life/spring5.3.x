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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 定义对特定类型（{@link AnnotationMetadata class} 或 {@link MethodMetadata method}）注解的访问，
 * 其形式不一定需要类加载。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationMetadata
 * @see MethodMetadata
 */
public interface AnnotatedTypeMetadata {

	/**
	 * 基于底层元素的直接注解返回注解详细信息。
	 * @return 基于直接注解的合并注解
	 * @since 5.2
	 */
	MergedAnnotations getAnnotations();

	/**
	 * 确定底层元素是否定义了给定类型的注解或元注解。
	 * <p>如果此方法返回 {@code true}，则 {@link #getAnnotationAttributes} 将返回非 null 的 Map。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @return 是否定义了匹配的注解
	 */
	default boolean isAnnotated(String annotationName) {
		return getAnnotations().isPresent(annotationName);
	}

	/**
	 * 检索给定类型的注解的属性（如果有，即在底层元素上定义为直接注解或元注解），同时考虑组合注解上的属
	 * 性覆盖。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @return 属性 Map，以属性名（例如 "value"）为键，
	 * 以定义的属性值为值。如果没有定义匹配的注解，
	 * 此返回值将为 {@code null}。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	/**
	 * 检索给定类型的注解的属性（如果有，即在底层元素上定义为直接注解或元注解），同时考虑组合注解上的属
	 * 性覆盖。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @param classValuesAsString 是否将类引用转换为 String 类名，
	 * 以便作为返回 Map 中的值暴露，
	 * 而不是可能必须先加载的 Class 引用
	 * @return 属性 Map，以属性名（例如 "value"）为键，
	 * 以定义的属性值为值。如果没有定义匹配的注解，
	 * 此返回值将为 {@code null}。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {

		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
				null, MergedAnnotationSelectors.firstDirectlyDeclared());
		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
	}

	/**
	 * 检索给定类型所有注解的所有属性（如果有，即在底层元素上定义为直接注解或元注解）。请注意，此变体
	 * <i>不</i>考虑属性覆盖。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @return 属性 MultiMap，以属性名（例如 "value"）为键，
	 * 以定义的属性值列表为值。如果没有定义匹配的注解，
	 * 此返回值将为 {@code null}。
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	/**
	 * 检索给定类型所有注解的所有属性（如果有，即在底层元素上定义为直接注解或元注解）。请注意，此变体
	 * <i>不</i>考虑属性覆盖。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @param classValuesAsString 是否将类引用转换为 String
	 * @return 属性 MultiMap，以属性名（例如 "value"）为键，
	 * 以定义的属性值列表为值。如果没有定义匹配的注解，
	 * 此返回值将为 {@code null}。
	 * @see #getAllAnnotationAttributes(String)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(
			String annotationName, boolean classValuesAsString) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return getAnnotations().stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(map ->
						map.isEmpty() ? null : map, adaptations));
	}

}
