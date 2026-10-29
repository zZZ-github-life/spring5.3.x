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
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 提供对合并注解集合的访问，这些注解通常从诸如 {@link Class} 或 {@link Method} 之类的源获取。
 *
 * <p>每个合并注解表示一个视图，其中的属性值可能由不同的源值“合并”而来，通常是：
 *
 * <ul>
 * <li>注解中一个或多个属性上的显式和隐式 {@link AliasFor @AliasFor} 声明</li>
 * <li>针对元注解的显式 {@link AliasFor @AliasFor} 声明</li>
 * <li>针对元注解的基于约定的属性别名</li>
 * <li>来自元注解的声明</li>
 * </ul>
 *
 * <p>例如，{@code @PostMapping} 注解可以定义如下：
 *
 * <pre class="code">
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;RequestMapping(method = RequestMethod.POST)
 * public &#064;interface PostMapping {
 *
 *     &#064;AliasFor(attribute = "path")
 *     String[] value() default {};
 *
 *     &#064;AliasFor(attribute = "value")
 *     String[] path() default {};
 * }
 * </pre>
 *
 * <p>如果某个方法标注了 {@code @PostMapping("/home")}，则它将同时包含 {@code @PostMapping} 和元注
 * 解 {@code @RequestMapping} 的合并注解。{@code @RequestMapping} 注解的合并视图将包含以下属性：
 *
 * <p><table border="1">
 * <tr>
 * <th>名称</th>
 * <th>值</th>
 * <th>来源</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>在 {@code @PostMapping} 中声明</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>显式 {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>RequestMethod.POST</td>
 * <td>在元注解中声明</td>
 * </tr>
 * </table>
 *
 * <p>{@code MergedAnnotations} 可以从任何 Java {@link AnnotatedElement} 通过
 * {@linkplain #from(AnnotatedElement) from} 获取。它们也可用于不使用反射的源（例如直接解析字节码的
 * 源）。
 *
 * <p>可以使用不同的 {@linkplain SearchStrategy 搜索策略}来定位包含待聚合注解的相关源元素。例如，
 * {@link SearchStrategy#TYPE_HIERARCHY} 将同时搜索超类和已实现的接口。
 *
 * <p>从 {@code MergedAnnotations} 实例中，您可以{@linkplain #get(String) 获取}单个注解，也可以
 * {@linkplain #stream() 流式获取所有注解}，或仅获取与{@linkplain #stream(String) 特定类型}匹配的注
 * 解。您还可以快速判断某个注解是否{@linkplain #isPresent(String) 存在}。
 *
 * <p>以下是一些典型示例：
 *
 * <pre class="code">
 * // is an annotation present or meta-present?
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // get the merged "value" attribute of ExampleAnnotation (either directly or
 * // meta-present)
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // get all meta-annotations but no directly present annotations
 * mergedAnnotations.stream().filter(MergedAnnotation::isMetaPresent);
 *
 * // get all ExampleAnnotation declarations (including any meta-annotations) and
 * // print the merged "value" attributes
 * mergedAnnotations.stream(ExampleAnnotation.class)
 *     .map(mergedAnnotation -&gt; mergedAnnotation.getString("value"))
 *     .forEach(System.out::println);
 * </pre>
 *
 * <p><b>注意：{@code MergedAnnotations} API 及其底层模型是为 Spring 通用组件模型中的可组合注解而设
 * 计的，重点在于属性别名和元注解关系。</b>此 API 不支持检索普通的 Java 注解；对于简单的注解检索用
 * 途，请使用标准 Java 反射或 Spring 的 {@link AnnotationUtils}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 * @see MergedAnnotation
 * @see MergedAnnotationCollectors
 * @see MergedAnnotationPredicates
 * @see MergedAnnotationSelectors
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

	/**
	 * 判断指定的注解类型是直接存在还是元存在。<p>等同于调用 {@code get(annotationType).isPresent()}。
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is present
	 */
	<A extends Annotation> boolean isPresent(Class<A> annotationType);

	/**
	 * 判断指定的注解类型是直接存在还是元存在。<p>等同于调用 {@code get(annotationType).isPresent()}。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to check
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent(String annotationType);

	/**
	 * 判断指定的注解类型是否直接存在。<p>等同于调用 {@code get(annotationType).isDirectlyPresent()}。
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is directly present
	 */
	<A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

	/**
	 * 判断指定的注解类型是否直接存在。<p>等同于调用 {@code get(annotationType).isDirectlyPresent()}。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to check
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent(String annotationType);

	/**
	 * 获取指定类型的{@linkplain MergedAnnotationSelectors#nearest() 最近}匹配注解或元注解，如果不存在
	 * 则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

	/**
	 * 获取指定类型的{@linkplain MergedAnnotationSelectors#nearest() 最近}匹配注解或元注解，如果不存在
	 * 则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the annotation type to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的匹配注解或元注解，如果不存在则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the annotation type to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @param selector a selector used to choose the most appropriate annotation
	 * within an aggregate, or {@code null} to select the
	 * {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 获取指定类型的{@linkplain MergedAnnotationSelectors#nearest() 最近}匹配注解或元注解，如果不存在
	 * 则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * 获取指定类型的{@linkplain MergedAnnotationSelectors#nearest() 最近}匹配注解或元注解，如果不存在
	 * 则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的匹配注解或元注解，如果不存在则返回 {@link MergedAnnotation#missing()}。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to get
	 * @param predicate a predicate that must match, or {@code null} if only
	 * type matching is required
	 * @param selector a selector used to choose the most appropriate annotation
	 * within an aggregate, or {@code null} to select the
	 * {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 流式获取所有与指定类型匹配的注解和元注解。<p>结果流遵循与 {@link #stream()} 相同的排序规则。
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * 流式获取所有与指定类型匹配的注解和元注解。<p>结果流遵循与 {@link #stream()} 相同的排序规则。
	 * @param annotationType the fully qualified class name of the annotation type
	 * to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * 流式获取此集合中包含的所有注解和元注解。<p>结果流首先按
	 * {@linkplain MergedAnnotation#getAggregateIndex() 聚合索引}排序，然后按注解距离排序（最接近的注解
	 * 在前）。这种排序意味着，在大多数用例中，最合适的注解会最先出现在流中。
	 * @return a stream of annotations
	 */
	Stream<MergedAnnotation<Annotation>> stream();


	/**
	 * 创建新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解。<p>结果实例将不包含
	 * 任何继承的注解。如果还想包含这些注解，应使用带有适当 {@link SearchStrategy} 的
	 * {@link #from(AnnotatedElement, SearchStrategy)}。
	 * @param element the source element
	 * @return a {@code MergedAnnotations} instance containing the element's
	 * annotations
	 */
	static MergedAnnotations from(AnnotatedElement element) {
		return from(element, SearchStrategy.DIRECT);
	}

	/**
	 * 创建新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，以及（取决于
	 * {@link SearchStrategy}）相关的继承元素。
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @return a {@code MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
		return from(element, searchStrategy, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 创建新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，以及（取决于
	 * {@link SearchStrategy}）相关的继承元素。
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 * the element annotations or the meta-annotations
	 * @return a {@code MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers) {

		return from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 创建新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，以及（取决于
	 * {@link SearchStrategy}）相关的继承元素。
	 * @param element the source element
	 * @param searchStrategy the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 * the element annotations or the meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered
	 * @return a {@code MergedAnnotations} instance containing the merged
	 * annotations for the supplied element
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param annotations the annotations to include
	 * @return a {@code MergedAnnotations} instance containing the annotations
	 * @see #from(Object, Annotation...)
	 */
	static MergedAnnotations from(Annotation... annotations) {
		return from(annotations, annotations);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source the source for the annotations. This source is used only
	 * for information and logging. It does not need to <em>actually</em>
	 * contain the specified annotations, and it will not be searched.
	 * @param annotations the annotations to include
	 * @return a {@code MergedAnnotations} instance containing the annotations
	 * @see #from(Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(Object source, Annotation... annotations) {
		return from(source, annotations, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source the source for the annotations. This source is used only
	 * for information and logging. It does not need to <em>actually</em>
	 * contain the specified annotations, and it will not be searched.
	 * @param annotations the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 * meta-annotations
	 * @return a {@code MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers) {
		return from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source the source for the annotations. This source is used only
	 * for information and logging. It does not need to <em>actually</em>
	 * contain the specified annotations, and it will not be searched.
	 * @param annotations the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 * meta-annotations
	 * @param annotationFilter an annotation filter used to restrict the
	 * annotations considered
	 * @return a {@code MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
	}

	/**
	 * 从指定的直接存在注解集合创建新的 {@link MergedAnnotations} 实例。此方法允许从未必使用反射加载的
	 * 注解创建 {@code MergedAnnotations} 实例。所提供的注解必须都是
	 * {@link MergedAnnotation#isDirectlyPresent() 直接存在}的，并且其
	 * {@link MergedAnnotation#getAggregateIndex() 聚合索引}必须为 {@code 0}。<p>结果
	 * {@code MergedAnnotations} 实例将同时包含指定的注解以及任何可通过反射读取的元注解。
	 * @param annotations the annotations to include
	 * @return a {@code MergedAnnotations} instance containing the annotations
	 * @see MergedAnnotation#of(ClassLoader, Object, Class, java.util.Map)
	 */
	static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
		return MergedAnnotationsCollection.of(annotations);
	}


	/**
	 * 由 {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)} 及其变体方法支持的搜索策略。
	 *
	 * <p>每种策略都会创建一组不同的聚合，这些聚合将被组合以创建最终的 {@link MergedAnnotations}。
	 */
	enum SearchStrategy {

		/**
		 * 仅查找直接声明的注解，不考虑 {@link Inherited @Inherited} 注解，也不搜索超类或已实现的接口。
		 */
		DIRECT,

		/**
		 * 查找所有直接声明的注解以及任何 {@link Inherited @Inherited} 超类注解。<p>此策略仅在用于
		 * {@link Class} 类型时才真正有用，因为对于所有其他 {@linkplain AnnotatedElement 被注解元素}，
		 * {@link Inherited @Inherited} 注解会被忽略。<p>此策略不搜索已实现的接口。
		 */
		INHERITED_ANNOTATIONS,

		/**
		 * 查找所有直接声明的注解和超类注解。<p>此策略与 {@link #INHERITED_ANNOTATIONS} 类似，只是注解不需
		 * 要用 {@link Inherited @Inherited} 进行元注解。<p>此策略不搜索已实现的接口。
		 */
		SUPERCLASS,

		/**
		 * 对整个类型层次结构执行完整搜索，包括超类和已实现的接口。<p>超类注解不需要用
		 * {@link Inherited @Inherited} 进行元注解。
		 */
		TYPE_HIERARCHY,

		/**
		 * 对源<em>以及</em>任何外围类执行整个类型层次结构的完整搜索。<p>此策略与 {@link #TYPE_HIERARCHY}
		 * 类似，只是还会搜索 {@linkplain Class#getEnclosingClass() 外围类}。<p>超类和外围类的注解不需要用
		 * {@link Inherited @Inherited} 进行元注解。<p>当搜索 {@link Method} 源时，此策略与
		 * {@link #TYPE_HIERARCHY} 相同。<p><strong>警告：</strong>此策略会递归搜索任何源类型的外围类上的注
		 * 解，无论该源类型是<em>内部类</em>、{@code static} 嵌套类还是嵌套接口。因此，它可能找到比您预期更
		 * 多的注解。
		 */
		TYPE_HIERARCHY_AND_ENCLOSING_CLASSES

	}

}
