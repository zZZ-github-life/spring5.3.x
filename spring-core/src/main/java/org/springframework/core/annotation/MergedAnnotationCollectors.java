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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 为 {@link MergedAnnotation} 实例提供各种归约操作的 {@link Collector} 实现。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class MergedAnnotationCollectors {

	private static final Characteristics[] NO_CHARACTERISTICS = {};

	private static final Characteristics[] IDENTITY_FINISH_CHARACTERISTICS = {Characteristics.IDENTITY_FINISH};


	private MergedAnnotationCollectors() {
	}


	/**
	 * 创建新的 {@link Collector}，将合并注解累积到包含 {@linkplain MergedAnnotation#synthesize() 合成}
	 * 版本的 {@link LinkedHashSet} 中。<p>此方法返回的收集器实际上等同于
	 * {@code Collectors.mapping(MergedAnnotation::synthesize, Collectors.toCollection(LinkedHashSet::new))}，
	 * 但避免了创建复合收集器。
	 * @param <A> the annotation type
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link Set}
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
		return Collector.of(LinkedHashSet::new, (set, annotation) -> set.add(annotation.synthesize()),
				MergedAnnotationCollectors::combiner);
	}

	/**
	 * 创建新的 {@link Collector}，将合并注解累积到包含 {@linkplain MergedAnnotation#synthesize() 合成}
	 * 版本的 {@link Annotation} 数组中。
	 * @param <A> the annotation type
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into an {@code Annotation[]}
	 * @see #toAnnotationArray(IntFunction)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Annotation[]> toAnnotationArray() {
		return toAnnotationArray(Annotation[]::new);
	}

	/**
	 * 创建新的 {@link Collector}，将合并注解累积到包含 {@linkplain MergedAnnotation#synthesize() 合成}
	 * 版本的 {@link Annotation} 数组中。
	 * @param <A> the annotation type
	 * @param <R> the resulting array type
	 * @param generator a function which produces a new array of the desired
	 * type and the provided length
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into an annotation array
	 * @see #toAnnotationArray
	 */
	public static <R extends Annotation, A extends R> Collector<MergedAnnotation<A>, ?, R[]> toAnnotationArray(
			IntFunction<R[]> generator) {

		return Collector.of(ArrayList::new, (list, annotation) -> list.add(annotation.synthesize()),
				MergedAnnotationCollectors::combiner, list -> list.toArray(generator.apply(list.size())));
	}

	/**
	 * 创建新的 {@link Collector}，将合并注解累积到 {@link MultiValueMap} 中，其条目由每个合并注解
	 * {@linkplain MergedAnnotation#asMap(Adapt...) 作为映射}
	 * {@linkplain MultiValueMap#add(Object, Object) 添加}。
	 * @param <A> the annotation type
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link LinkedMultiValueMap}
	 * @see #toMultiValueMap(Function, MergedAnnotation.Adapt...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Adapt... adaptations) {

		return toMultiValueMap(Function.identity(), adaptations);
	}

	/**
	 * 创建新的 {@link Collector}，将合并注解累积到 {@link MultiValueMap} 中，其条目由每个合并注解
	 * {@linkplain MergedAnnotation#asMap(Adapt...) 作为映射}
	 * {@linkplain MultiValueMap#add(Object, Object) 添加}。
	 * @param <A> the annotation type
	 * @param finisher the finisher function for the new {@link MultiValueMap}
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return a {@link Collector} which collects and synthesizes the
	 * annotations into a {@link LinkedMultiValueMap}
	 * @see #toMultiValueMap(MergedAnnotation.Adapt...)
	 */
	public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, MultiValueMap<String, Object>> toMultiValueMap(
			Function<MultiValueMap<String, Object>, MultiValueMap<String, Object>> finisher,
			Adapt... adaptations) {

		Characteristics[] characteristics = (isSameInstance(finisher, Function.identity()) ?
				IDENTITY_FINISH_CHARACTERISTICS : NO_CHARACTERISTICS);
		return Collector.of(LinkedMultiValueMap::new,
				(map, annotation) -> annotation.asMap(adaptations).forEach(map::add),
				MergedAnnotationCollectors::combiner, finisher, characteristics);
	}


	private static boolean isSameInstance(Object instance, Object candidate) {
		return instance == candidate;
	}

	/**
	 * 用于集合的 {@link Collector#combiner() Combiner}。<p>仅当 {@link java.util.stream.Stream} 以
	 * {@linkplain java.util.stream.Stream#parallel() 并行}方式处理时才会调用此方法。
	 */
	private static <E, C extends Collection<E>> C combiner(C collection, C additions) {
		collection.addAll(additions);
		return collection;
	}

	/**
	 * 用于多值映射的 {@link Collector#combiner() Combiner}。<p>仅当 {@link java.util.stream.Stream} 以
	 * {@linkplain java.util.stream.Stream#parallel() 并行}方式处理时才会调用此方法。
	 */
	private static <K, V> MultiValueMap<K, V> combiner(MultiValueMap<K, V> map, MultiValueMap<K, V> additions) {
		map.addAll(additions);
		return map;
	}

}
