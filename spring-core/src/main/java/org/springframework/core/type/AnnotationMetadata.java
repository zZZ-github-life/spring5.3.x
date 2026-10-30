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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * 定义对特定类注解的抽象访问的接口，其形式不要求该类已被加载。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 */
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * 获取底层类上<em>存在</em>的所有注解类型的完全限定类名。
	 * @return 注解类型名称
	 */
	default Set<String> getAnnotationTypes() {
		return getAnnotations().stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.map(annotation -> annotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 获取底层类上给定注解类型中<em>存在</em>的所有元注解类型的完全限定类名。
	 * @param annotationName 要查找的元注解类型的
	 * 全限定类名
	 * @return 元注解类型名称；如果未找到则为空集合
	 */
	default Set<String> getMetaAnnotationTypes(String annotationName) {
		MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
		if (!annotation.isPresent()) {
			return Collections.emptySet();
		}
		return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 确定底层类上是否<em>存在</em>给定类型的注解。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @return 如果存在匹配的注解，则返回 {@code true}
	 */
	default boolean hasAnnotation(String annotationName) {
		return getAnnotations().isDirectlyPresent(annotationName);
	}

	/**
	 * 确定底层类是否具有本身被给定类型的元注解所标注的注解。
	 * @param metaAnnotationName 要查找的元注解类型的
	 * 全限定类名
	 * @return 如果存在匹配的元注解，则返回 {@code true}
	 */
	default boolean hasMetaAnnotation(String metaAnnotationName) {
		return getAnnotations().get(metaAnnotationName,
				MergedAnnotation::isMetaPresent).isPresent();
	}

	/**
	 * 确定底层类是否具有任何被给定注解类型标注（或元标注）的方法。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 */
	default boolean hasAnnotatedMethods(String annotationName) {
		return !getAnnotatedMethods(annotationName).isEmpty();
	}

	/**
	 * 检索所有被给定注解类型标注（或元标注）的方法的方法元数据。
	 * <p>对于任何返回的方法，{@link MethodMetadata#isAnnotated} 对给定注解类型都将返回 {@code true}。
	 * @param annotationName 要查找的注解类型的
	 * 全限定类名
	 * @return 匹配注解的方法的 {@link MethodMetadata} 集合。
	 * 如果没有方法匹配该注解类型，
	 * 返回值为空集合。
	 */
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);


	/**
	 * 使用标准反射为给定类创建新的 {@link AnnotationMetadata} 实例的工厂方法。
	 * @param type 要内省的类
	 * @return 一个新的 {@link AnnotationMetadata} 实例
	 * @since 5.2
	 */
	static AnnotationMetadata introspect(Class<?> type) {
		return StandardAnnotationMetadata.from(type);
	}

}
