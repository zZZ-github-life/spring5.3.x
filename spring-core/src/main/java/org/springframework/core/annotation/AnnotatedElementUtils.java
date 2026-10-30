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
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 用于在 {@link AnnotatedElement AnnotatedElements} 上查找注解、元注解和可重复注解的通用工具方法。
 *
 * <p>{@code AnnotatedElementUtils} 定义了 Spring 元注解编程模型的公共 API，并支持<em>注解属性覆盖
 * </em>。如果不需要注解属性覆盖支持，请考虑改用 {@link AnnotationUtils}。
 *
 * <p>请注意，此类提供的功能并非由 JDK 的内省设施本身提供。
 *
 * <h3>注解属性覆盖</h3>
 * <p>对<em>组合注解</em>中带<em>属性覆盖</em>的元注解的支持，由
 * {@code getMergedAnnotationAttributes()}、{@code getMergedAnnotation()}、
 * {@code getAllMergedAnnotations()}、{@code getMergedRepeatableAnnotations()}、
 * {@code findMergedAnnotationAttributes()}、{@code findMergedAnnotation()}、
 * {@code findAllMergedAnnotations()} 和 {@code findMergedRepeatableAnnotations()} 等方法提供。
 *
 * <h3>find 与 get 语义</h3>
 * <p>此类中的方法所使用的搜索算法遵循 <em>find</em> 或 <em>get</em> 语义。有关每个方法使用哪种搜索
 * 算法的详细信息，请参见各个方法的 javadoc。
 *
 * <p><strong>Get 语义</strong>仅限于搜索<em>存在</em>于 {@code AnnotatedElement} 上的注解（即在本
 * 地声明或 {@linkplain java.lang.annotation.Inherited 继承}的注解），或者在
 * {@code AnnotatedElement} <em>之上</em>的注解层级中声明的注解。
 *
 * <p><strong>Find 语义</strong>要详尽得多，它提供 <em>get 语义</em>并额外支持以下内容：
 *
 * <ul>
 * <li>如果带注解的元素是类，则搜索接口
 * <li>如果带注解的元素是类，则搜索父类
 * <li>如果带注解的元素是方法，则解析桥接方法
 * <li>如果带注解的元素是方法，则搜索接口中的方法
 * <li>如果带注解的元素是方法，则搜索父类中的方法
 * </ul>
 *
 * <h3>对 {@code @Inherited} 的支持</h3>
 * <p>遵循 <em>get 语义</em>的方法将遵守 Java 的 {@link java.lang.annotation.Inherited @Inherited}
 * 注解的约定，只是本地声明的注解（包括自定义组合注解）将优先于继承的注解。相反，遵循 <em>find 语义
 * </em>的方法将完全忽略 {@code @Inherited} 的存在，因为 <em>find</em> 搜索算法会手动遍历类型和方法
 * 层级，从而隐式支持注解继承，无需 {@code @Inherited}。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotationUtils
 * @see BridgeMethodResolver
 */
public abstract class AnnotatedElementUtils {

	/**
	 * 为给定注解构建一个适配的 {@link AnnotatedElement}，通常用于 {@link AnnotatedElementUtils} 上的其
	 * 他方法。
	 * @param annotations 要通过 {@code AnnotatedElement} 暴露的注解
	 * @since 4.3
	 */
	public static AnnotatedElement forAnnotations(Annotation... annotations) {
		return new AnnotatedElementForAnnotations(annotations);
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 上该注解（指定 {@code annotationType} 的注解）中所有<em>存
	 * 在</em>的元注解类型的完全限定类名。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationType 要在其上查找元注解的注解类型
	 * @return 该注解上存在的所有元注解的名称，如果未找到则为空 Set
	 * @since 4.2
	 * @see #getMetaAnnotationTypes(AnnotatedElement, String)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element,
			Class<? extends Annotation> annotationType) {

		return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 上该注解（指定 {@code annotationName} 的注解）中所有<em>存
	 * 在</em>的元注解类型的完全限定类名。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要在其上查找元注解的注解类型的完全限定类名
	 * @return 该注解上存在的所有元注解的名称，如果未找到则为空 Set
	 * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		for (Annotation annotation : element.getAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationName)) {
				return getMetaAnnotationTypes(element, annotation);
			}
		}
		return Collections.emptySet();
	}

	private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, @Nullable Annotation annotation) {
		if (annotation == null) {
			return Collections.emptySet();
		}
		return getAnnotations(annotation.annotationType()).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 判断所提供的 {@link AnnotatedElement} 是否标注了一个<em>组合注解</em>，该注解以指定
	 * {@code annotationType} 的注解作为元注解。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的元注解类型
	 * @return 如果存在匹配的元注解则返回 {@code true}
	 * @since 4.2.3
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return getAnnotations(element).stream(annotationType).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * 判断所提供的 {@link AnnotatedElement} 是否标注了一个<em>组合注解</em>，该注解以指定
	 * {@code annotationName} 的注解作为元注解。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的元注解类型的完全限定类名
	 * @return 如果存在匹配的元注解则返回 {@code true}
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).stream(annotationName).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * 判断指定 {@code annotationType} 的注解是否<em>存在</em>于所提供的 {@link AnnotatedElement} 上，
	 * 或位于指定元素<em>之上</em>的注解层级中。
	 * <p>如果此方法返回 {@code true}，则 {@link #getMergedAnnotationAttributes} 将返回非 null 值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 如果存在匹配的注解则返回 {@code true}
	 * @since 4.2.3
	 * @see #hasAnnotation(AnnotatedElement, Class)
	 */
	public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return getAnnotations(element).isPresent(annotationType);
	}

	/**
	 * 判断指定 {@code annotationName} 的注解是否<em>存在</em>于所提供的 {@link AnnotatedElement} 上，
	 * 或在指定元素<em>之上</em>的注解层级中。
	 * <p>如果此方法返回 {@code true}，则 {@link #getMergedAnnotationAttributes} 将返回非 null 值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 如果存在匹配的注解则返回 {@code true}
	 */
	public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).isPresent(annotationName);
	}

	/**
	 * 获取指定 {@code annotationType} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，并将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法委托给 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)}。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(
			AnnotatedElement element, Class<? extends Annotation> annotationType) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, false, false);
	}

	/**
	 * 获取指定 {@code annotationName} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，并将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法委托给
	 * {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)}，并为
	 * {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 传入 {@code false}。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName) {

		return getMergedAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 获取指定 {@code annotationName} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，并将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并。
	 * <p>注解层级中较低层的属性会覆盖较上层中同名的属性，并且在单个注解内以及注解层级内，都完全支持
	 * {@link AliasFor @AliasFor} 语义。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法所使用的搜索算法在找到指定
	 * {@code annotationName} 的第一个注解后将停止搜索注解层级。因此，指定 {@code annotationName} 的其
	 * 他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是将 Class 引用转换为 String，还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是将嵌套的 Annotation 实例转换为 {@code AnnotationAttributes} Map，
	 * 还是将其保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 获取指定 {@code annotationType} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并，并将结果合成为指定
	 * {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后的、合成的 {@code Annotation}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 获取指定 {@code annotationType} 的<strong>所有</strong>注解，这些注解位于所提供的
	 * {@code element} <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的
	 * <em>匹配</em>属性合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并的、合成的 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getAllMergedAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getAnnotations(element).stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 获取指定 {@code annotationTypes} 的<strong>所有</strong>注解，这些注解位于所提供的
	 * {@code element} <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的
	 * <em>匹配</em>属性合并，并将结果合成为相应 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationTypes 要查找的注解类型
	 * @return 找到的所有合并的、合成的 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @since 5.1
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> getAllMergedAnnotations(AnnotatedElement element,
			Set<Class<? extends Annotation>> annotationTypes) {

		return getAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 获取指定 {@code annotationType} 的所有<em>可重复注解</em>，这些注解位于所提供的 {@code element}
	 * <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的<em>匹配</em>属性
	 * 合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>存放可重复注解的容器类型将通过 {@link java.lang.annotation.Repeatable} 查找。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并的可重复 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType} 为 {@code null}，或无法解析容器类型
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 获取指定 {@code annotationType} 的所有<em>可重复注解</em>，这些注解位于所提供的 {@code element}
	 * <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的<em>匹配</em>属性
	 * 合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * <p><strong>警告</strong>：如果所提供的 {@code containerType} 不为 {@code null}，则搜索将仅限于支
	 * 持容器为所提供 {@code containerType} 的可重复注解。这可防止搜索到在其他类型的可重复注解上声明为
	 * 元注解的可重复注解。如果需要支持此类用例，请优先使用
	 * {@link #getMergedRepeatableAnnotations(AnnotatedElement, Class)}，而不要使用此方法；或者也可以直
	 * 接使用 {@link MergedAnnotations} API，并结合通过
	 * {@linkplain RepeatableContainers#and(Class, Class) composed} 组合的 {@link RepeatableContainers}
	 * 来支持多种可重复注解类型。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @param containerType 存放注解的容器类型；如果应通过
	 * {@link java.lang.annotation.Repeatable} 查找容器类型，则可以为 {@code null}
	 * @return 找到的所有合并的可重复 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType} 为 {@code null}，或无法解析容器类型
	 * @throws AnnotationConfigurationException 如果提供的 {@code containerType} 不是提供的 {@code annotationType} 的有效容器注解
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType,
			@Nullable Class<? extends Annotation> containerType) {

		return getRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 之上注解层级中指定 {@code annotationName} 的<strong>所有
	 * </strong>注解的注解属性，并将结果存储在 {@link MultiValueMap} 中。
	 * <p>注意：与 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} 不同，此方法<em>不
	 * </em>支持属性覆盖。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 以属性名为键的 {@link MultiValueMap}，包含找到的所有注解的注解属性，
	 * 如果未找到则为 {@code null}
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(
			AnnotatedElement element, String annotationName) {

		return getAllAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 之上注解层级中指定 {@code annotationName} 的<strong>所有
	 * </strong>注解的注解属性，并将结果存储在 {@link MultiValueMap} 中。
	 * <p>注意：与 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} 不同，此方法<em>不
	 * </em>支持属性覆盖。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>get 语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是将 Class 引用转换为 String，还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是将嵌套的 Annotation 实例转换为 {@code AnnotationAttributes} Map，
	 * 还是将其保留为 Annotation 实例
	 * @return 以属性名为键的 {@link MultiValueMap}，包含找到的所有注解的注解属性，
	 * 如果未找到则为 {@code null}
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
		return getAnnotations(element).stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(AnnotatedElementUtils::nullIfEmpty, adaptations));
	}

	/**
	 * 判断指定 {@code annotationType} 的注解是否<em>可用</em>于所提供的 {@link AnnotatedElement} 上，
	 * 或位于指定元素<em>之上</em>的注解层级中。
	 * <p>如果此方法返回 {@code true}，则 {@link #findMergedAnnotationAttributes} 将返回非 null 值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 如果存在匹配的注解则返回 {@code true}
	 * @since 4.3
	 * @see #isAnnotated(AnnotatedElement, Class)
	 */
	public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return findAnnotations(element).isPresent(annotationType);
	}

	/**
	 * 查找指定 {@code annotationType} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，并将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并。
	 * <p>注解层级中较低层的属性会覆盖较上层中同名的属性，并且在单个注解内以及注解层级内，都完全支持
	 * {@link AliasFor @AliasFor} 语义。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法所使用的搜索算法在找到指定
	 * {@code annotationType} 的第一个注解后将停止搜索注解层级。因此，指定 {@code annotationType} 的其
	 * 他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param classValuesAsString 是将 Class 引用转换为 String，还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是将嵌套的 Annotation 实例转换为 {@code AnnotationAttributes} Map，
	 * 还是将其保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 查找指定 {@code annotationName} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，并将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并。
	 * <p>注解层级中较低层的属性会覆盖较上层中同名的属性，并且在单个注解内以及注解层级内，都完全支持
	 * {@link AliasFor @AliasFor} 语义。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法所使用的搜索算法在找到指定
	 * {@code annotationName} 的第一个注解后将停止搜索注解层级。因此，指定 {@code annotationName} 的其
	 * 他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是将 Class 引用转换为 String，还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是将嵌套的 Annotation 实例转换为 {@code AnnotationAttributes} Map，
	 * 还是将其保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 查找指定 {@code annotationType} 的第一个注解，该注解位于所提供的 {@code element} <em>之上</em>的
	 * 注解层级中，将该注解的属性与注解层级较低层中注解的<em>匹配</em>属性合并，并将结果合成为指定
	 * {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后的、合成的 {@code Annotation}，如果未找到则为 {@code null}
	 * @since 4.2
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 查找指定 {@code annotationType} 的<strong>所有</strong>注解，这些注解位于所提供的
	 * {@code element} <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的
	 * <em>匹配</em>属性合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并的、合成的 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
		return findAnnotations(element).stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 查找指定 {@code annotationTypes} 的<strong>所有</strong>注解，这些注解位于所提供的
	 * {@code element} <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的
	 * <em>匹配</em>属性合并，并将结果合成为相应 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationTypes 要查找的注解类型
	 * @return 找到的所有合并的、合成的 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @since 5.1
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> findAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
		return findAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 查找指定 {@code annotationType} 的所有<em>可重复注解</em>，这些注解位于所提供的 {@code element}
	 * <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的<em>匹配</em>属性
	 * 合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>存放可重复注解的容器类型将通过 {@link java.lang.annotation.Repeatable} 查找。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并的可重复 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType} 为 {@code null}，或无法解析容器类型
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		return findMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 查找指定 {@code annotationType} 的所有<em>可重复注解</em>，这些注解位于所提供的 {@code element}
	 * <em>之上</em>的注解层级中；对于找到的每个注解，将其属性与注解层级较低层中注解的<em>匹配</em>属性
	 * 合并，并将结果合成为指定 {@code annotationType} 的注解。
	 * <p>在单个注解内以及注解层级内，都完全支持 {@link AliasFor @AliasFor} 语义。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils class-level javadoc}中所述的 <em>find 语义</em>。
	 *
	 * <p><strong>警告</strong>：如果所提供的 {@code containerType} 不为 {@code null}，则搜索将仅限于支
	 * 持容器为所提供 {@code containerType} 的可重复注解。这可防止搜索到在其他类型的可重复注解上声明为
	 * 元注解的可重复注解。如果需要支持此类用例，请优先使用
	 * {@link #findMergedRepeatableAnnotations(AnnotatedElement, Class)}，而不要使用此方法；或者也可以
	 * 直接使用 {@link MergedAnnotations} API，并结合通过
	 * {@linkplain RepeatableContainers#and(Class, Class) composed} 组合的 {@link RepeatableContainers}
	 * 来支持多种可重复注解类型。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @param containerType 存放注解的容器类型；如果应通过
	 * {@link java.lang.annotation.Repeatable} 查找容器类型，则可以为 {@code null}
	 * @return 找到的所有合并的可重复 {@code Annotations} 的 Set，如果未找到则为空 Set
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType} 为 {@code null}，或无法解析容器类型
	 * @throws AnnotationConfigurationException 如果提供的 {@code containerType} 不是提供的 {@code annotationType} 的有效容器注解
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerType) {

		return findRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private static MergedAnnotations getAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
	}

	private static MergedAnnotations getRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers;
		if (containerType == null) {
			// Invoke RepeatableContainers.of() in order to adhere to the contract of
			// getMergedRepeatableAnnotations() which states that an IllegalArgumentException
			// will be thrown if the container cannot be resolved.
			//
			// In any case, we use standardRepeatables() in order to support repeatable
			// annotations on other types of repeatable annotations (i.e., nested repeatable
			// annotation types).
			//
			// See https://github.com/spring-projects/spring-framework/issues/20279
			RepeatableContainers.of(annotationType, null);
			repeatableContainers = RepeatableContainers.standardRepeatables();
		}
		else {
			repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		}
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, repeatableContainers);
	}

	private static MergedAnnotations findAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
	}

	private static MergedAnnotations findRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers;
		if (containerType == null) {
			// Invoke RepeatableContainers.of() in order to adhere to the contract of
			// findMergedRepeatableAnnotations() which states that an IllegalArgumentException
			// will be thrown if the container cannot be resolved.
			//
			// In any case, we use standardRepeatables() in order to support repeatable
			// annotations on other types of repeatable annotations (i.e., nested repeatable
			// annotation types).
			//
			// See https://github.com/spring-projects/spring-framework/issues/20279
			RepeatableContainers.of(annotationType, null);
			repeatableContainers = RepeatableContainers.standardRepeatables();
		}
		else {
			repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		}
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, repeatableContainers);
	}

	@Nullable
	private static MultiValueMap<String, Object> nullIfEmpty(MultiValueMap<String, Object> map) {
		return (map.isEmpty() ? null : map);
	}

	private static <A extends Annotation> Comparator<MergedAnnotation<A>> highAggregateIndexesFirst() {
		return Comparator.<MergedAnnotation<A>> comparingInt(MergedAnnotation::getAggregateIndex).reversed();
	}

	@Nullable
	private static AnnotationAttributes getAnnotationAttributes(MergedAnnotation<?> annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, nestedAnnotationsAsMap));
	}


	/**
	 * 持有特定注解的适配 {@link AnnotatedElement}。
	 */
	private static class AnnotatedElementForAnnotations implements AnnotatedElement {

		private final Annotation[] annotations;

		AnnotatedElementForAnnotations(Annotation... annotations) {
			this.annotations = annotations;
		}

		@Override
		@SuppressWarnings("unchecked")
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : this.annotations) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return this.annotations.clone();
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return this.annotations.clone();
		}

	}

}
