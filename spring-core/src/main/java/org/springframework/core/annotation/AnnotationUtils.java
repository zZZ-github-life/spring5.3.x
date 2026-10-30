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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理注解的通用工具方法，可处理元注解、桥接方法（编译器为泛型声明生成的方法）以及父方法（用于
 * 可选的<em>注解继承</em>）。
 *
 * <p>请注意，此类的大多数特性并非由 JDK 自身的自省设施提供。
 *
 * <p>作为运行时保留的应用注解（例如用于事务控制、授权或服务暴露）的一般规则，请始终使用此类中的查
 * 找方法（例如 {@link #findAnnotation(Method, Class)} 或 {@link #getAnnotation(Method, Class)}），
 * 而不要使用 JDK 中普通的注解查找方法。你仍然可以明确选择：仅在给定类层级上进行 <em>get</em> 查找
 * （{@link #getAnnotation(Method, Class)}），还是在给定方法的整个继承层次结构中进行 <em>find</em>
 * 查找（{@link #findAnnotation(Method, Class)}）。
 *
 * <h3>术语</h3>
 * 术语 <em>directly present</em>、<em>indirectly present</em> 和 <em>present</em> 的含义与
 * {@link AnnotatedElement}（Java 8 中）的类级 javadoc 中所定义的一致。
 *
 * <p>如果某个注解被声明为另一个<em>存在</em>于该元素上的注解的元注解，则该注解在该元素上是 <em>
 * meta-present</em>。如果注解 {@code A} 在另一个注解上<em>直接存在</em>或是 <em>meta-present</em>
 * ，则 {@code A} 在该另一个注解上是 <em>meta-present</em>。
 *
 * <h3>元注解支持</h3>
 * <p>此类中的大多数 {@code find*()} 方法以及部分 {@code get*()} 方法支持查找用作元注解的注解。详情
 * 请参阅此类中每个方法的 javadoc。若需要对<em>组合注解</em>中的<em>属性覆盖</em>提供细粒度元注解支
 * 持，请考虑改用 {@link AnnotatedElementUtils} 中更具体的方法。
 *
 * <h3>属性别名</h3>
 * <p>此类中所有返回注解、注解数组或 {@link AnnotationAttributes} 的公共方法都透明支持通过
 * {@link AliasFor @AliasFor} 配置的属性别名。详情请参阅各个 {@code synthesizeAnnotation*(..)} 方法
 * 。
 *
 * <h3>搜索范围</h3>
 * <p>此类中各方法使用的搜索算法在找到第一个指定类型的注解后就会停止搜索。因此，指定类型的其他注解
 * 将被静默忽略。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 * @since 2.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {

	/**
	 * 单元素注解的属性名。
	 */
	public static final String VALUE = MergedAnnotation.VALUE;

	private static final AnnotationFilter JAVA_LANG_ANNOTATION_FILTER =
			AnnotationFilter.packages("java.lang.annotation");

	private static final Map<Class<? extends Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache =
			new ConcurrentReferenceHashMap<>();


	/**
	 * 判断给定的类是否可能携带指定的某个注解（在类型、方法或字段级别）。
	 * @param clazz 要内省的类
	 * @param annotationTypes 可搜索的注解类型
	 * @return 如果已知该类在任何层级都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。此处返回 {@code true} 时，
	 * 调用方通常会执行完整的方法/字段内省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, Class)
	 * @see #isCandidateClass(Class, String)
	 */
	public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (isCandidateClass(clazz, annotationType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断给定的类是否可能携带指定的注解（在类型、方法或字段级别）。
	 * @param clazz 要内省的类
	 * @param annotationType 可搜索的注解类型
	 * @return 如果已知该类在任何层级都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。此处返回 {@code true} 时，
	 * 调用方通常会执行完整的方法/字段内省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, String)
	 */
	public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return isCandidateClass(clazz, annotationType.getName());
	}

	/**
	 * 判断给定的类是否可能携带指定的注解（在类型、方法或字段级别）。
	 * @param clazz 要内省的类
	 * @param annotationName 可搜索的注解类型的全限定名称
	 * @return 如果已知该类在任何层级都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。此处返回 {@code true} 时，
	 * 调用方通常会执行完整的方法/字段内省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, Class)
	 */
	public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
		if (annotationName.startsWith("java.")) {
			return true;
		}
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
			return false;
		}
		return true;
	}

	/**
	 * 从提供的注解中获取 {@code annotationType} 类型的单个 {@link Annotation}：即给定注解本身或其直接
	 * 元注解。
	 * <p>请注意，此方法仅支持单层元注解。若需支持任意层级的元注解，请改用某个 {@code find*()} 方法。
	 * @param annotation 要检查的 Annotation
	 * @param annotationType 要查找的注解类型，既包括本地声明也包括作为元注解
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 * @since 4.0
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (annotationType.isInstance(annotation)) {
			return synthesizeAnnotation((A) annotation, annotationType);
		}
		// Shortcut: no searchable annotations to be found on plain Java classes and core Spring types...
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotation)) {
			return null;
		}
		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(annotation, new Annotation[] {annotation}, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 获取 {@code annotationType} 类型的单个 {@link Annotation}，该
	 * 注解<em>存在</em>或 <em>meta-present</em> 于该 {@code AnnotatedElement} 上。
	 * <p>请注意，此方法仅支持单层元注解。若需支持任意层级的元注解，请改用
	 * {@link #findAnnotation(AnnotatedElement, Class)}。
	 * @param annotatedElement 要从中获取注解的 {@code AnnotatedElement}
	 * @param annotationType 要查找的注解类型，既包括本地声明也包括作为元注解
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 * @since 3.1
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotatedElement.getAnnotation(annotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
	}

	private static <A extends Annotation> boolean isSingleLevelPresent(MergedAnnotation<A> mergedAnnotation) {
		int distance = mergedAnnotation.getDistance();
		return (distance == 0 || distance == 1);
	}

	/**
	 * 从提供的 {@link Method} 获取 {@code annotationType} 类型的单个 {@link Annotation}，该注解<em>存
	 * 在</em>或 <em>meta-present</em> 于该方法上。
	 * <p>能够正确处理编译器生成的桥接 {@link Method Methods}。
	 * <p>请注意，此方法仅支持单层元注解。若需支持任意层级的元注解，请改用
	 * {@link #findAnnotation(Method, Class)}。
	 * @param method 要在其上查找注解的方法
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see #getAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
		return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
	}

	/**
	 * 获取<em>存在</em>于提供的 {@link AnnotatedElement} 上的所有 {@link Annotation Annotations}。
	 * <p>将<em>不会</em>搜索元注解。
	 * @param annotatedElement 要从中获取注解的 Method、Constructor 或 Field
	 * @return 找到的注解，空数组；如果无法解析
	 * （例如注解属性中的嵌套 Class 值
	 * 在运行时解析失败）则为 {@code null}
	 * @since 4.0.8
	 * @see AnnotatedElement#getAnnotations()
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	@Nullable
	public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
		try {
			return synthesizeAnnotationArray(annotatedElement.getAnnotations(), annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * 获取<em>存在</em>于提供的 {@link Method} 上的所有 {@link Annotation Annotations}。
	 * <p>能够正确处理编译器生成的桥接 {@link Method Methods}。
	 * <p>将<em>不会</em>搜索元注解。
	 * @param method 要从中获取注解的 Method
	 * @return 找到的注解，空数组；如果无法解析
	 * （例如注解属性中的嵌套 Class 值
	 * 在运行时解析失败）则为 {@code null}
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see AnnotatedElement#getAnnotations()
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	@Nullable
	public static Annotation[] getAnnotations(Method method) {
		try {
			return synthesizeAnnotationArray(BridgeMethodResolver.findBridgedMethod(method).getAnnotations(), method);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(method, ex);
			return null;
		}
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 获取 {@code annotationType} 的<em>可重复</em>
	 * {@linkplain Annotation annotations}，此类注解<em>存在</em>、<em>间接存在</em>或 <em>meta-present
	 * </em> 于该元素上。
	 * <p>此方法模仿 Java 8 的 {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
	 * 的功能，并支持自动检测通过 @{@link java.lang.annotation.Repeatable} 声明的<em>容器注解</em>（在
	 * Java 8 或更高版本上运行时），以及对元注解的额外支持。
	 * <p>同时处理单个注解以及嵌套在<em>容器注解</em>内的注解。
	 * <p>如果提供的元素是 {@link Method}，则能正确处理编译器生成的<em>桥接方法</em>。
	 * <p>如果注解不<em>存在</em>于提供的元素上，则会搜索元注解。
	 * @param annotatedElement 要在其上查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 找到的注解集合，或空集合（不能为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 获取 {@code annotationType} 的<em>可重复</em>
	 * {@linkplain Annotation annotations}，此类注解<em>存在</em>、<em>间接存在</em>或 <em>meta-present
	 * </em> 于该元素上。
	 * <p>此方法模仿 Java 8 的 {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
	 * 的功能，并额外支持元注解。
	 * <p>同时处理单个注解以及嵌套在<em>容器注解</em>内的注解。
	 * <p>如果提供的元素是 {@link Method}，则能正确处理编译器生成的<em>桥接方法</em>。
	 * <p>如果注解不<em>存在</em>于提供的元素上，则会搜索元注解。
	 * @param annotatedElement 要在其上查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 持有这些注解的容器类型；
	 * 如果不支持容器，或应在 Java 8 及以上版本中
	 * 通过 @{@link java.lang.annotation.Repeatable} 查找，
	 * 则可以为 {@code null}
	 * @return 找到的注解集合，或空集合（不能为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {

		RepeatableContainers repeatableContainers = (containerAnnotationType != null ?
				RepeatableContainers.of(annotationType, containerAnnotationType) :
				RepeatableContainers.standardRepeatables());

		return MergedAnnotations.from(annotatedElement, SearchStrategy.SUPERCLASS, repeatableContainers)
				.stream(annotationType)
				.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 获取 {@code annotationType} 的已声明<em>可重复</em>
	 * {@linkplain Annotation annotations}，此类注解<em>直接存在</em>、<em>间接存在</em>或 <em>
	 * meta-present</em> 于该元素上。
	 * <p>此方法模仿 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)} 的功能，并支持自
	 * 动检测通过 @{@link java.lang.annotation.Repeatable} 声明的<em>容器注解</em>（在 Java 8 或更高版
	 * 本上运行时），以及对元注解的额外支持。
	 * <p>同时处理单个注解以及嵌套在<em>容器注解</em>内的注解。
	 * <p>如果提供的元素是 {@link Method}，则能正确处理编译器生成的<em>桥接方法</em>。
	 * <p>如果注解不<em>存在</em>于提供的元素上，则会搜索元注解。
	 * @param annotatedElement 要在其上查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 找到的注解集合，或空集合（不能为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 获取 {@code annotationType} 的已声明<em>可重复</em>
	 * {@linkplain Annotation annotations}，此类注解<em>直接存在</em>、<em>间接存在</em>或 <em>
	 * meta-present</em> 于该元素上。
	 * <p>此方法模仿 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)} 的功能，并额外支
	 * 持元注解。
	 * <p>同时处理单个注解以及嵌套在<em>容器注解</em>内的注解。
	 * <p>如果提供的元素是 {@link Method}，则能正确处理编译器生成的<em>桥接方法</em>。
	 * <p>如果注解不<em>存在</em>于提供的元素上，则会搜索元注解。
	 * @param annotatedElement 要在其上查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 持有这些注解的容器类型；
	 * 如果不支持容器，或应在 Java 8 及以上版本中
	 * 通过 @{@link java.lang.annotation.Repeatable} 查找，
	 * 则可以为 {@code null}
	 * @return 找到的注解集合，或空集合（不能为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {

		RepeatableContainers repeatableContainers = containerAnnotationType != null ?
				RepeatableContainers.of(annotationType, containerAnnotationType) :
				RepeatableContainers.standardRepeatables();

		return MergedAnnotations.from(annotatedElement, SearchStrategy.DIRECT, repeatableContainers)
				.stream(annotationType)
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@link AnnotatedElement} 上查找 {@code annotationType} 类型的单个 {@link Annotation}。
	 * <p>如果注解不<em>直接存在</em>于提供的元素上，则会搜索元注解。
	 * <p><strong>警告</strong>：此方法以通用方式操作带注解的元素。换言之，此方法不会为类或方法执行专门
	 * 的搜索算法。如果需要 {@link #findAnnotation(Class, Class)} 或
	 * {@link #findAnnotation(Method, Class)} 的更具体语义，请改为调用其中之一。
	 * @param annotatedElement 要在其上查找注解的 {@code AnnotatedElement}
	 * @param annotationType 要查找的注解类型，既包括本地声明也包括作为元注解
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 * @since 4.2
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(
			AnnotatedElement annotatedElement, @Nullable Class<A> annotationType) {

		if (annotationType == null) {
			return null;
		}

		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotatedElement.getDeclaredAnnotation(annotationType);
		}

		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@link Method} 上查找 {@code annotationType} 类型的单个 {@link Annotation}，如果该注解
	 * 不<em>直接存在</em>于给定方法本身，则遍历其父方法（即来自父类和接口的方法）。
	 * <p>能够正确处理编译器生成的桥接 {@link Method Methods}。
	 * <p>如果注解不<em>直接存在</em>于该方法上，则会搜索元注解。
	 * <p>方法上的注解默认不会被继承，因此需要显式处理这种情况。
	 * @param method 要在其上查找注解的方法
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 * @see #getAnnotation(Method, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
		if (annotationType == null) {
			return null;
		}

		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
			return method.getDeclaredAnnotation(annotationType);
		}

		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@link Class} 上查找 {@code annotationType} 类型的单个 {@link Annotation}，如果该注解不
	 * <em>直接存在</em>于给定类本身，则遍历其接口、注解和父类。
	 * <p>此方法显式处理未声明为 {@link java.lang.annotation.Inherited inherited} 的类级注解，<em>以及
	 * 元注解和接口上的注解</em>。
	 * <p>算法运行方式如下：
	 * <ol>
	 * <li>在给定类上搜索该注解；如果找到则返回。
	 * <li>递归搜索给定类声明的所有注解。
	 * <li>递归搜索给定类声明的所有接口。
	 * <li>递归搜索给定类的父类层级。
	 * </ol>
	 * <p>注意：在此上下文中，<em>递归</em>一词意味着搜索过程会继续回到第 1 步，并将当前接口、注解或父
	 * 类作为要查找注解的类。
	 * @param clazz 要在其上查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，未找到则为 {@code null}
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, @Nullable Class<A> annotationType) {
		if (annotationType == null) {
			return null;
		}

		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
			A annotation = clazz.getDeclaredAnnotation(annotationType);
			if (annotation != null) {
				return annotation;
			}
			// For backwards compatibility, perform a superclass search with plain annotations
			// even if not marked as @Inherited: e.g. a findAnnotation search for @Deprecated
			Class<?> superclass = clazz.getSuperclass();
			if (superclass == null || superclass == Object.class) {
				return null;
			}
			return findAnnotation(superclass, annotationType);
		}

		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(clazz, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在指定的 {@code clazz}（包括该 {@code clazz} 本身）的继承层次结构中，查找第一个<em>直接存在</em>
	 * 指定 {@code annotationType} 注解的 {@link Class}。
	 * <p>如果提供的 {@code clazz} 是接口，则仅检查该接口本身；不会遍历接口的继承层次结构。
	 * <p>将<em>不会</em>搜索元注解。
	 * <p>标准 {@link Class} API 并未提供确定继承层次结构中究竟是哪个类声明了某个 {@link Annotation} 的
	 * 机制，因此需要显式处理这种情况。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要在其上检查该注解的类（可以为 {@code null}）
	 * @return 继承层次结构中第一个声明了指定
	 * {@code annotationType} 注解的 {@link Class}，
	 * 未找到则为 {@code null}
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	@Nullable
	public static Class<?> findAnnotationDeclaringClass(
			Class<? extends Annotation> annotationType, @Nullable Class<?> clazz) {

		if (clazz == null) {
			return null;
		}

		return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
				.get(annotationType, MergedAnnotation::isDirectlyPresent)
				.getSource();
	}

	/**
	 * 在指定的 {@code clazz}（包括该 {@code clazz} 本身）的继承层次结构中，查找第一个<em>直接存在</em>
	 * 指定的某个 {@code annotationTypes} 的 {@link Class}。
	 * <p>如果提供的 {@code clazz} 是接口，则仅检查该接口本身；不会遍历接口的继承层次结构。
	 * <p>将<em>不会</em>搜索元注解。
	 * <p>标准 {@link Class} API 并未提供确定继承层次结构中究竟是哪个类声明了若干候选
	 * {@linkplain Annotation annotations} 之一的机制，因此需要显式处理这种情况。
	 * @param annotationTypes 要查找的注解类型
	 * @param clazz 要在其上检查该注解的类（可以为 {@code null}）
	 * @return 继承层次结构中第一个声明了指定
	 * {@code annotationTypes} 中至少一个注解的 {@link Class}，
	 * 未找到则为 {@code null}
	 * @since 3.2.2
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	@Nullable
	public static Class<?> findAnnotationDeclaringClassForTypes(
			List<Class<? extends Annotation>> annotationTypes, @Nullable Class<?> clazz) {

		if (clazz == null) {
			return null;
		}

		return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
				.stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes).and(MergedAnnotation::isDirectlyPresent))
				.map(MergedAnnotation::getSource)
				.findFirst().orElse(null);
	}

	/**
	 * 判断指定的 {@code annotationType} 注解是否在提供的 {@code clazz} 上本地声明（即<em>直接存在</em>
	 * ）。
	 * <p>提供的 {@link Class} 可以表示任何类型。
	 * <p>将<em>不会</em>搜索元注解。
	 * <p>注意：此方法<strong>不</strong>判断该注解是否被
	 * {@linkplain java.lang.annotation.Inherited inherited}。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要在其上检查该注解的类
	 * @return 如果指定 {@code annotationType} 的注解
	 * <em>直接存在</em>则返回 {@code true}
	 * @see java.lang.Class#getDeclaredAnnotations()
	 * @see java.lang.Class#getDeclaredAnnotation(Class)
	 */
	public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent();
	}

	/**
	 * 判断指定的 {@code annotationType} 注解是否<em>存在</em>于提供的 {@code clazz} 上且是被
	 * {@linkplain java.lang.annotation.Inherited inherited} 的（即不是<em>直接存在</em>）。
	 * <p>将<em>不会</em>搜索元注解。
	 * <p>如果提供的 {@code clazz} 是接口，则仅检查该接口本身。按照 Java 中标准的元注解语义，不会遍历接
	 * 口的继承层次结构。有关注解继承的更多细节，请参阅 {@code @Inherited} 元注解的
	 * {@linkplain java.lang.annotation.Inherited javadoc}。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要在其上检查该注解的类
	 * @return 如果指定 {@code annotationType} 的注解
	 * <em>存在</em>且<em>被继承</em>则返回 {@code true}
	 * @see Class#isAnnotationPresent(Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MergedAnnotations.from(clazz, SearchStrategy.INHERITED_ANNOTATIONS)
				.stream(annotationType)
				.filter(MergedAnnotation::isDirectlyPresent)
				.findFirst().orElseGet(MergedAnnotation::missing)
				.getAggregateIndex() > 0;
	}

	/**
	 * 判断 {@code metaAnnotationType} 类型的注解是否 <em>meta-present</em> 于提供的
	 * {@code annotationType} 上。
	 * @param annotationType 要在其上搜索的注解类型
	 * @param metaAnnotationType 要搜索的元注解类型
	 * @return 如果存在这样的元注解则返回 {@code true}
	 * @since 4.2.1
	 * @deprecated as of 5.2 since it is superseded by the {@link MergedAnnotations} API
	 */
	@Deprecated
	public static boolean isAnnotationMetaPresent(Class<? extends Annotation> annotationType,
			@Nullable Class<? extends Annotation> metaAnnotationType) {

		if (metaAnnotationType == null) {
			return false;
		}
		// Shortcut: directly present on the element, with no merging needed?
		if (AnnotationFilter.PLAIN.matches(metaAnnotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotationType)) {
			return annotationType.isAnnotationPresent(metaAnnotationType);
		}
		// Exhaustive retrieval of merged annotations...
		return MergedAnnotations.from(annotationType, SearchStrategy.INHERITED_ANNOTATIONS,
				RepeatableContainers.none()).isPresent(metaAnnotationType);
	}

	/**
	 * 判断提供的 {@link Annotation} 是否定义在核心 JDK 的 {@code java.lang.annotation} 包中。
	 * @param annotation 要检查的注解
	 * @return 如果该注解位于 {@code java.lang.annotation} 包中则返回 {@code true}
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable Annotation annotation) {
		return (annotation != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotation));
	}

	/**
	 * 判断具有所提供名称的 {@link Annotation} 是否定义在核心 JDK 的 {@code java.lang.annotation} 包中
	 * 。
	 * @param annotationType 要检查的注解类型名称
	 * @return 如果该注解位于 {@code java.lang.annotation} 包中则返回 {@code true}
	 * @since 4.2
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable String annotationType) {
		return (annotationType != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotationType));
	}

	/**
	 * 检查给定注解的已声明属性，特别是涵盖了 Google App Engine 针对 {@code Class} 值延迟出现
	 * {@code TypeNotPresentExceptionProxy} 的情况（而不是早期 {@code Class.getAnnotations() failure}）。
	 * <p>此方法未失败表明 {@link #getAnnotationAttributes(Annotation)}（在稍后尝试时）也不会失败。
	 * @param annotation 要校验的注解
	 * @throws IllegalStateException 如果无法读取已声明的 {@code Class} 属性
	 * @since 4.3.15
	 * @see Class#getAnnotations()
	 * @see #getAnnotationAttributes(Annotation)
	 */
	public static void validateAnnotation(Annotation annotation) {
		AttributeMethods.forAnnotationType(annotation.annotationType()).validate(annotation);
	}

	/**
	 * 将给定注解的属性检索为 {@link Map}，并保留所有属性类型。
	 * <p>等同于调用 {@link #getAnnotationAttributes(Annotation, boolean, boolean)}，并将
	 * {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 参数设置为 {@code false}。
	 * <p>注意：此方法实际上返回的是 {@link AnnotationAttributes} 实例。但为了保持二进制兼容性，保留了
	 * {@code Map} 签名。
	 * @param annotation 要获取其属性的注解
	 * @return 注解属性的 Map，以属性名为键，
	 * 以对应的属性值为值（不能为 {@code null}）
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
		return getAnnotationAttributes(null, annotation);
	}

	/**
	 * 将给定注解的属性检索为 {@link Map}。
	 * <p>等同于调用 {@link #getAnnotationAttributes(Annotation, boolean, boolean)}，并将
	 * {@code nestedAnnotationsAsMap} 参数设置为 {@code false}。
	 * <p>注意：此方法实际上返回的是 {@link AnnotationAttributes} 实例。但为了保持二进制兼容性，保留了
	 * {@code Map} 签名。
	 * @param annotation 要获取其属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（以便
	 * 兼容 {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 Class 引用
	 * @return 注解属性的 Map，以属性名为键，
	 * 以对应的属性值为值（不能为 {@code null}）
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(
			Annotation annotation, boolean classValuesAsString) {

		return getAnnotationAttributes(annotation, classValuesAsString, false);
	}

	/**
	 * 将给定注解的属性检索为 {@link AnnotationAttributes} 映射。
	 * <p>此方法提供完全递归的注解读取能力，与基于反射的
	 * {@link org.springframework.core.type.StandardAnnotationMetadata} 相当。
	 * @param annotation 要获取其属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（以便
	 * 兼容 {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为
	 * {@link AnnotationAttributes} Map（以便兼容
	 * {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 {@code Annotation} 实例
	 * @return 注解属性（一种专用 Map），以属性名为键，
	 * 以对应的属性值为值（不能为 {@code null}）
	 * @since 3.1.1
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 将给定注解的属性检索为 {@link AnnotationAttributes} 映射。
	 * <p>等同于调用 {@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}，
	 * 并将 {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 参数设置为 {@code false}。
	 * @param annotatedElement 被所提供注解标注的元素；未知时可以为 {@code null}
	 * @param annotation 要获取其属性的注解
	 * @return 注解属性（一种专用 Map），以属性名为键，
	 * 以对应的属性值为值（不能为 {@code null}）
	 * @since 4.2
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			@Nullable AnnotatedElement annotatedElement, Annotation annotation) {

		return getAnnotationAttributes(annotatedElement, annotation, false, false);
	}

	/**
	 * 将给定注解的属性检索为 {@link AnnotationAttributes} 映射。
	 * <p>此方法提供完全递归的注解读取能力，与基于反射的
	 * {@link org.springframework.core.type.StandardAnnotationMetadata} 相当。
	 * @param annotatedElement 被所提供注解标注的元素；未知时可以为 {@code null}
	 * @param annotation 要获取其属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（以便
	 * 兼容 {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为
	 * {@link AnnotationAttributes} Map（以便兼容
	 * {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 {@code Annotation} 实例
	 * @return 注解属性（一种专用 Map），以属性名为键，
	 * 以对应的属性值为值（不能为 {@code null}）
	 * @since 4.2
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			@Nullable AnnotatedElement annotatedElement, Annotation annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
		return MergedAnnotation.from(annotatedElement, annotation)
				.withNonMergedAttributes()
				.asMap(mergedAnnotation ->
						new AnnotationAttributes(mergedAnnotation.getType(), true), adaptations);
	}

	/**
	 * 为给定属性注册注解声明的默认值（如果可用）。
	 * @param attributes 要处理的注解属性
	 * @since 4.3.2
	 */
	public static void registerDefaultValues(AnnotationAttributes attributes) {
		Class<? extends Annotation> annotationType = attributes.annotationType();
		if (annotationType != null && Modifier.isPublic(annotationType.getModifiers()) &&
				!AnnotationFilter.PLAIN.matches(annotationType)) {
			Map<String, DefaultValueHolder> defaultValues = getDefaultValues(annotationType);
			defaultValues.forEach(attributes::putIfAbsent);
		}
	}

	private static Map<String, DefaultValueHolder> getDefaultValues(
			Class<? extends Annotation> annotationType) {

		return defaultValuesCache.computeIfAbsent(annotationType,
				AnnotationUtils::computeDefaultValues);
	}

	private static Map<String, DefaultValueHolder> computeDefaultValues(
			Class<? extends Annotation> annotationType) {

		AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
		if (!methods.hasDefaultValueMethod()) {
			return Collections.emptyMap();
		}
		Map<String, DefaultValueHolder> result = CollectionUtils.newLinkedHashMap(methods.size());
		if (!methods.hasNestedAnnotation()) {
			// Use simpler method if there are no nested annotations
			for (int i = 0; i < methods.size(); i++) {
				Method method = methods.get(i);
				Object defaultValue = method.getDefaultValue();
				if (defaultValue != null) {
					result.put(method.getName(), new DefaultValueHolder(defaultValue));
				}
			}
		}
		else {
			// If we have nested annotations, we need them as nested maps
			AnnotationAttributes attributes = MergedAnnotation.of(annotationType)
					.asMap(annotation ->
							new AnnotationAttributes(annotation.getType(), true), Adapt.ANNOTATION_TO_MAP);
			for (Map.Entry<String, Object> element : attributes.entrySet()) {
				result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
			}
		}
		return result;
	}

	/**
	 * 对提供的 {@link AnnotationAttributes} 进行后处理，将嵌套注解保留为 {@code Annotation} 实例。
	 * <p>具体而言，此方法对使用 {@link AliasFor @AliasFor} 注解的注解属性强制执行<em>属性别名</em>语义
	 * ，并将默认值占位符替换为其原始默认值。
	 * @param annotatedElement 被某个注解或注解层次结构标注的元素，
	 * 所提供的属性即由该注解或层次结构创建；
	 * 未知时可以为 {@code null}
	 * @param attributes 要后处理的注解属性
	 * @param classValuesAsString 是否将 Class 引用转换为 String（以便
	 * 兼容 {@link org.springframework.core.type.AnnotationMetadata}），
	 * 还是将其保留为 Class 引用
	 * @since 4.3.2
	 * @see #getDefaultValue(Class, String)
	 */
	public static void postProcessAnnotationAttributes(@Nullable Object annotatedElement,
			@Nullable AnnotationAttributes attributes, boolean classValuesAsString) {

		if (attributes == null) {
			return;
		}
		if (!attributes.validated) {
			Class<? extends Annotation> annotationType = attributes.annotationType();
			if (annotationType == null) {
				return;
			}
			AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
			for (int i = 0; i < mapping.getMirrorSets().size(); i++) {
				MirrorSet mirrorSet = mapping.getMirrorSets().get(i);
				int resolved = mirrorSet.resolve(attributes.displayName, attributes,
						AnnotationUtils::getAttributeValueForMirrorResolution);
				if (resolved != -1) {
					Method attribute = mapping.getAttributes().get(resolved);
					Object value = attributes.get(attribute.getName());
					for (int j = 0; j < mirrorSet.size(); j++) {
						Method mirror = mirrorSet.get(j);
						if (mirror != attribute) {
							attributes.put(mirror.getName(),
									adaptValue(annotatedElement, value, classValuesAsString));
						}
					}
				}
			}
		}
		for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
			String attributeName = attributeEntry.getKey();
			Object value = attributeEntry.getValue();
			if (value instanceof DefaultValueHolder) {
				value = ((DefaultValueHolder) value).defaultValue;
				attributes.put(attributeName,
						adaptValue(annotatedElement, value, classValuesAsString));
			}
		}
	}

	private static Object getAttributeValueForMirrorResolution(Method attribute, Object attributes) {
		Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
		return (result instanceof DefaultValueHolder ? ((DefaultValueHolder) result).defaultValue : result);
	}

	@Nullable
	private static Object adaptValue(
			@Nullable Object annotatedElement, @Nullable Object value, boolean classValuesAsString) {

		if (classValuesAsString) {
			if (value instanceof Class) {
				return ((Class<?>) value).getName();
			}
			if (value instanceof Class[]) {
				Class<?>[] classes = (Class<?>[]) value;
				String[] names = new String[classes.length];
				for (int i = 0; i < classes.length; i++) {
					names[i] = classes[i].getName();
				}
				return names;
			}
		}
		if (value instanceof Annotation) {
			Annotation annotation = (Annotation) value;
			return MergedAnnotation.from(annotatedElement, annotation).synthesize();
		}
		if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			Annotation[] synthesized = (Annotation[]) Array.newInstance(
					annotations.getClass().getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				synthesized[i] = MergedAnnotation.from(annotatedElement, annotations[i]).synthesize();
			}
			return synthesized;
		}
		return value;
	}

	/**
	 * 在给定注解实例的情况下，检索单元素注解的 {@code value} 属性的<em>值</em>。
	 * @param annotation 要从中获取值的注解实例
	 * @return 属性值；未找到则为 {@code null}，但若因
	 * {@link AnnotationConfigurationException} 而无法获取属性值，
	 * 则会重新抛出该异常
	 * @see #getValue(Annotation, String)
	 */
	@Nullable
	public static Object getValue(Annotation annotation) {
		return getValue(annotation, VALUE);
	}

	/**
	 * 在给定注解实例的情况下，检索指定名称属性的<em>值</em>。
	 * @param annotation 要从中获取值的注解实例
	 * @param attributeName 要获取的属性值的名称
	 * @return 属性值；未找到则为 {@code null}，但若因
	 * {@link AnnotationConfigurationException} 而无法获取属性值，
	 * 则会重新抛出该异常
	 * @see #getValue(Annotation)
	 */
	@Nullable
	public static Object getValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		if (annotation == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			Method method = annotation.annotationType().getDeclaredMethod(attributeName);
			return invokeAnnotationMethod(method, annotation);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
		catch (Throwable ex) {
			rethrowAnnotationConfigurationException(ex);
			handleIntrospectionFailure(annotation.getClass(), ex);
			return null;
		}
	}

	/**
	 * 在提供的 {@link Annotation} 上调用提供的注解属性 {@link Method}。
	 * <p>将首先尝试通过注解的 {@link InvocationHandler} 调用该方法（如果注解实例是 JDK 动态代理）。如
	 * 果失败，则尝试通过反射调用该方法。
	 * @param method 要调用的方法
	 * @param annotation 要在其上调用该方法的注解
	 * @return 方法调用返回的值
	 * @since 5.3.24
	 */
	static Object invokeAnnotationMethod(Method method, Object annotation) {
		if (Proxy.isProxyClass(annotation.getClass())) {
			try {
				InvocationHandler handler = Proxy.getInvocationHandler(annotation);
				return handler.invoke(annotation, method, null);
			}
			catch (Throwable ex) {
				// ignore and fall back to reflection below
			}
		}
		return ReflectionUtils.invokeMethod(method, annotation);
	}

	/**
	 * 如果提供的 throwable 是 {@link AnnotationConfigurationException}，则将其转换为
	 * {@code AnnotationConfigurationException} 并抛出，使其传播给调用方。
	 * <p>否则，此方法不执行任何操作。
	 * @param ex 要检查的 Throwable
	 */
	static void rethrowAnnotationConfigurationException(Throwable ex) {
		if (ex instanceof AnnotationConfigurationException) {
			throw (AnnotationConfigurationException) ex;
		}
	}

	/**
	 * 处理提供的注解自省异常。
	 * <p>如果提供的异常是 {@link AnnotationConfigurationException}，则直接将其抛出，使其传播给调用方，
	 * 并且不会记录任何日志。
	 * <p>否则，此方法会先记录一次自省失败（特别是针对 {@link TypeNotPresentException}），然后继续执行
	 * ，假定注解属性中的嵌套 {@code Class} 值无法解析，从而实际上假装指定元素上没有任何注解。
	 * @param element 我们尝试对其内省注解的元素
	 * @param ex 我们遇到的异常
	 * @see #rethrowAnnotationConfigurationException
	 * @see IntrospectionFailureLogger
	 */
	static void handleIntrospectionFailure(@Nullable AnnotatedElement element, Throwable ex) {
		rethrowAnnotationConfigurationException(ex);
		IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
		boolean meta = false;
		if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
			// Meta-annotation or (default) value lookup on an annotation type
			logger = IntrospectionFailureLogger.DEBUG;
			meta = true;
		}
		if (logger.isEnabled()) {
			String message = meta ?
					"Failed to meta-introspect annotation " :
					"Failed to introspect annotations on ";
			logger.log(message + element + ": " + ex);
		}
	}

	/**
	 * 在给定注解实例的情况下，检索单元素注解的 {@code value} 属性的<em>默认值</em>。
	 * @param annotation 要从中获取默认值的注解实例
	 * @return 默认值，未找到则为 {@code null}
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(Annotation annotation) {
		return getDefaultValue(annotation, VALUE);
	}

	/**
	 * 在给定注解实例的情况下，检索指定名称属性的<em>默认值</em>。
	 * @param annotation 要从中获取默认值的注解实例
	 * @param attributeName 要获取的属性值的名称
	 * @return 指定名称属性的默认值，未找到则为 {@code null}
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		return (annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null);
	}

	/**
	 * 在给定 {@link Class annotation type} 的情况下，检索单元素注解的 {@code value} 属性的<em>默认值
	 * </em>。
	 * @param annotationType 应为其获取默认值的 <em>注解类型</em>
	 * @return 默认值，未找到则为 {@code null}
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
		return getDefaultValue(annotationType, VALUE);
	}

	/**
	 * 在给定 {@link Class annotation type} 的情况下，检索指定名称属性的<em>默认值</em>。
	 * @param annotationType 应为其获取默认值的 <em>注解类型</em>
	 * @param attributeName 要获取的属性值的名称。
	 * @return 指定名称属性的默认值，未找到则为 {@code null}
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(
			@Nullable Class<? extends Annotation> annotationType, @Nullable String attributeName) {

		if (annotationType == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		return MergedAnnotation.of(annotationType).getDefaultValue(attributeName).orElse(null);
	}

	/**
	 * 通过将提供的 {@code annotation} 包装在动态代理中，从该注解<em>合成</em>一个注解，该代理会为使用
	 * {@link AliasFor @AliasFor} 注解的注解属性透明地强制执行<em>属性别名</em>语义。
	 * @param annotation 要合成的注解
	 * @param annotatedElement 被所提供注解标注的元素；未知时可以为 {@code null}
	 * @return 如果所提供的注解<em>可合成</em>，则为合成后的注解；
	 * 如果所提供的注解为 {@code null}，则为 {@code null}；
	 * 否则为未经修改的所提供注解
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(
			A annotation, @Nullable AnnotatedElement annotatedElement) {

		if (isSynthesizedAnnotation(annotation) || AnnotationFilter.PLAIN.matches(annotation)) {
			return annotation;
		}
		return MergedAnnotation.from(annotatedElement, annotation).synthesize();
	}

	/**
	 * 根据注解的默认属性值<em>合成</em>一个注解。
	 * <p>此方法仅委托给 {@link #synthesizeAnnotation(Map, Class, AnnotatedElement)}，为源属性值提供空
	 * 映射，并为 {@link AnnotatedElement} 提供 {@code null}。
	 * @param annotationType 要合成的注解类型
	 * @return 合成后的注解
	 * @throws IllegalArgumentException 如果缺少必需的属性
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
		return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
	}

	/**
	 * 通过将提供的注解属性映射包装在动态代理中，从该映射<em>合成</em>一个注解，该代理实现指定
	 * {@code annotationType} 的注解，并为使用 {@link AliasFor @AliasFor} 注解的注解属性透明地强制执行
	 * <em>属性别名</em>语义。
	 * <p>提供的映射必须为 {@code annotationType} 中定义的每个未被别名化或没有默认值的属性包含一个键值
	 * 对。嵌套映射和嵌套的映射数组将分别被递归合成为嵌套注解或嵌套注解数组。
	 * <p>注意，{@link AnnotationAttributes} 是 {@link Map} 的一种特殊类型，非常适合作为此方法
	 * {@code attributes} 参数的值。
	 * @param attributes 要合成的注解属性 Map
	 * @param annotationType 要合成的注解类型
	 * @param annotatedElement 被与所提供属性对应的注解标注的元素；
	 * 未知时可以为 {@code null}
	 * @return 合成后的注解
	 * @throws IllegalArgumentException 如果缺少必需的属性，或某个属性的类型不正确
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
			Class<A> annotationType, @Nullable AnnotatedElement annotatedElement) {

		try {
			return MergedAnnotation.of(annotatedElement, annotationType, attributes).synthesize();
		}
		catch (NoSuchElementException | IllegalStateException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * 通过创建相同大小和类型的新数组，并用输入数组中各注解的
	 * {@linkplain #synthesizeAnnotation(Annotation, AnnotatedElement) synthesized} 版本填充它，从提供
	 * 的 {@code annotations} 数组<em>合成</em>一个注解数组。
	 * @param annotations 要合成的注解数组
	 * @param annotatedElement 被所提供的注解数组标注的元素；
	 * 未知时可以为 {@code null}
	 * @return 由合成后注解组成的新数组；如果所提供的
	 * 数组为 {@code null} 则为 {@code null}
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 */
	static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, AnnotatedElement annotatedElement) {
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotations;
		}
		Annotation[] synthesized = (Annotation[]) Array.newInstance(
				annotations.getClass().getComponentType(), annotations.length);
		for (int i = 0; i < annotations.length; i++) {
			synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
		}
		return synthesized;
	}

	/**
	 * 判断提供的 {@link Annotation} 是否由 Spring <em>合成</em>（即包装在动态代理中）并具有额外功能（
	 * 如属性别名处理）。
	 * @param annotation 要检查的注解
	 * @return 如果所提供的注解是合成注解则返回 {@code true}
	 * @since 5.3.23
	 */
	public static boolean isSynthesizedAnnotation(@Nullable Annotation annotation) {
		return (annotation instanceof SynthesizedAnnotation);
	}

	/**
	 * 清除内部注解元数据缓存。
	 * @since 4.3.15
	 */
	public static void clearCache() {
		AnnotationTypeMappings.clearCache();
		AnnotationsScanner.clearCache();
	}


	/**
	 * 用于包装默认值的内部持有者。
	 */
	private static class DefaultValueHolder {

		final Object defaultValue;

		public DefaultValueHolder(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String toString() {
			return "*" + this.defaultValue;
		}
	}

}
