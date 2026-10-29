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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * 从 {@link MergedAnnotations} 集合中返回的单个合并注解。它呈现了注解的一个视图，其中的属性值可能
 * 已经由不同的源值“合并”而来。
 *
 * <p>可以使用各种 {@code get} 方法来访问属性值。例如，要访问 {@code int} 属性，将使用
 * {@link #getInt(String)} 方法。
 *
 * <p>请注意，访问属性值时<b>不会</b>进行转换。例如，如果底层属性是 {@code int}，则无法调用
 * {@link #getString(String)}。此规则唯一的例外是 {@code Class} 和 {@code Class[]} 值，它们可以分别
 * 作为 {@code String} 和 {@code String[]} 访问，以避免可能的类过早初始化。
 *
 * <p>如有必要，{@code MergedAnnotation} 可以被 {@linkplain #synthesize() 合成}回实际的
 * {@link java.lang.annotation.Annotation}。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2
 * @param <A> the annotation type
 * @see MergedAnnotations
 * @see MergedAnnotationPredicates
 */
public interface MergedAnnotation<A extends Annotation> {

	/**
	 * 单元素注解的属性名。
	 */
	String VALUE = "value";


	/**
	 * 获取实际注解类型的 {@code Class} 引用。
	 * @return the annotation type
	 */
	Class<A> getType();

	/**
	 * 判断注解是否存在于源上。会在所使用的 {@link SearchStrategy} 上下文中考虑
	 * {@linkplain #isDirectlyPresent() 直接存在}和 {@linkplain #isMetaPresent() 元存在}的注解。
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent();

	/**
	 * 判断注解是否直接存在于源上。<p>直接存在的注解是用户显式声明的注解，而不是
	 * {@linkplain #isMetaPresent() 元存在}或 {@link Inherited @Inherited} 的注解。
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent();

	/**
	 * 判断注解是否元存在于源上。<p>元存在的注解是指用户未显式声明、但在注解层次结构中的某处被用作元注
	 * 解的注解。
	 * @return {@code true} if the annotation is meta-present
	 */
	boolean isMetaPresent();

	/**
	 * 获取此注解作为元注解使用时的距离。<p>直接声明的注解距离为 {@code 0}，元注解距离为 {@code 1}，元
	 * 注解上的元注解距离为 {@code 2}，依此类推。{@linkplain #missing() 缺失}的注解将始终返回距离
	 * {@code -1}。
	 * @return the annotation distance or {@code -1} if the annotation is missing
	 */
	int getDistance();

	/**
	 * 获取包含此注解的聚合集合的索引。<p>可用于重新排列注解流，例如为超类或接口上声明的注解赋予更高的
	 * 优先级。{@linkplain #missing() 缺失}的注解将始终返回聚合索引 {@code -1}。
	 * @return the aggregate index (starting at {@code 0}) or {@code -1} if the
	 * annotation is missing
	 */
	int getAggregateIndex();

	/**
	 * 获取最终声明根注解的源，如果源未知则返回 {@code null}。<p>如果此合并注解是通过
	 * {@link MergedAnnotations#from(AnnotatedElement) from} 一个 {@link AnnotatedElement} 创建的，则此
	 * 源将是同一类型的元素。如果注解是在未使用反射的情况下加载的，则源可以是任何类型，但应具有合理的
	 * {@code toString()}。元注解将始终返回与 {@link #getRoot() 根}相同的源。
	 * @return the source, or {@code null}
	 */
	@Nullable
	Object getSource();

	/**
	 * 获取元注解的源，如果该注解不是 {@linkplain #isMetaPresent() 元存在}的，则返回 {@code null}。<p>
	 * 元源是用此注解进行元注解的那个注解。
	 * @return the meta-annotation source or {@code null}
	 * @see #getRoot()
	 */
	@Nullable
	MergedAnnotation<?> getMetaSource();

	/**
	 * 获取根注解，即直接在源上声明的、{@link #getDistance() 距离}为 {@code 0} 的注解。
	 * @return the root annotation
	 * @see #getMetaSource()
	 */
	MergedAnnotation<?> getRoot();

	/**
	 * 获取注解层次结构中从此注解到 {@link #getRoot() 根}的完整注解类型列表。<p>这提供了一种唯一标识合
	 * 并注解实例的有用方式。
	 * @return the meta types for the annotation
	 * @see MergedAnnotationPredicates#unique(Function)
	 * @see #getRoot()
	 * @see #getMetaSource()
	 */
	List<Class<? extends Annotation>> getMetaTypes();


	/**
	 * 判断指定属性名的值相较于注解声明是否为非默认值。
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is different from the default
	 * value
	 */
	boolean hasNonDefaultValue(String attributeName);

	/**
	 * 判断指定属性名的值相较于注解声明是否为默认值。
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute value is the same as the default
	 * value
	 */
	boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 byte 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a byte
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte getByte(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 byte 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a byte array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	byte[] getByteArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 boolean 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a boolean
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean getBoolean(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 boolean 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a boolean array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 char 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a char
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char getChar(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 char 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a char array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	char[] getCharArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 short 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a short
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short getShort(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 short 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a short array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	short[] getShortArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 int 属性值。
	 * @param attributeName the attribute name
	 * @return the value as an int
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int getInt(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 int 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as an int array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	int[] getIntArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 long 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a long
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long getLong(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 long 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a long array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	long[] getLongArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 double 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a double
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double getDouble(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 double 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a double array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	double[] getDoubleArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 float 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a float
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float getFloat(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 float 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a float array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	float[] getFloatArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符串属性值。
	 * @param attributeName the attribute name
	 * @return the value as a string
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String getString(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符串数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a string array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	String[] getStringArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 class 属性值。
	 * @param attributeName the attribute name
	 * @return the value as a class
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?> getClass(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的 class 数组属性值。
	 * @param attributeName the attribute name
	 * @return the value as a class array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的枚举属性值。
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as an enum
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的枚举数组属性值。
	 * @param attributeName the attribute name
	 * @param type the enum type
	 * @return the value as an enum array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的注解属性值。
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation}
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * 从注解中获取必需的注解数组属性值。
	 * @param attributeName the attribute name
	 * @param type the annotation type
	 * @return the value as a {@link MergedAnnotation} array
	 * @throws NoSuchElementException if there is no matching attribute
	 */
	<T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * 从注解中获取可选的属性值。
	 * @param attributeName the attribute name
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	Optional<Object> getValue(String attributeName);

	/**
	 * 从注解中获取可选的属性值。
	 * @param attributeName the attribute name
	 * @param type the attribute type. Must be compatible with the underlying
	 * attribute type or {@code Object.class}.
	 * @return an optional value or {@link Optional#empty()} if there is no
	 * matching attribute
	 */
	<T> Optional<T> getValue(String attributeName, Class<T> type);

	/**
	 * 获取注解中按注解声明所指定的默认属性值。
	 * @param attributeName the attribute name
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	Optional<Object> getDefaultValue(String attributeName);

	/**
	 * 获取注解中按注解声明所指定的默认属性值。
	 * @param attributeName the attribute name
	 * @param type the attribute type. Must be compatible with the underlying
	 * attribute type or {@code Object.class}.
	 * @return an optional of the default value or {@link Optional#empty()} if
	 * there is no matching attribute or no defined default
	 */
	<T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

	/**
	 * 创建注解的新视图，其中已移除所有具有默认值的属性。
	 * @return a filtered view of the annotation without any attributes that
	 * have a default value
	 * @see #filterAttributes(Predicate)
	 */
	MergedAnnotation<A> filterDefaultValues();

	/**
	 * 创建注解的新视图，其中仅包含与给定谓词匹配的属性。
	 * @param predicate a predicate used to filter attribute names
	 * @return a filtered view of the annotation
	 * @see #filterDefaultValues()
	 * @see MergedAnnotationPredicates
	 */
	MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

	/**
	 * 创建注解的新视图，以公开未合并的属性值。<p>此视图中的方法返回的属性值仅应用了别名镜像规则。指向
	 * {@link #getMetaSource() 元源}属性的别名将不会应用。
	 * @return a non-merged view of the annotation
	 */
	MergedAnnotation<A> withNonMergedAttributes();

	/**
	 * 从此合并注解创建一个新的可变 {@link AnnotationAttributes} 实例。<p>可以使用 {@link Adapt 适配}来
	 * 改变值的添加方式。
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return an immutable map containing the attributes and values
	 */
	AnnotationAttributes asAnnotationAttributes(Adapt... adaptations);

	/**
	 * 获取一个包含所有注解属性的不可变 {@link Map}。<p>可以使用 {@link Adapt 适配}来改变值的添加方式。
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return an immutable map containing the attributes and values
	 */
	Map<String, Object> asMap(Adapt... adaptations);

	/**
	 * 创建给定类型的新 {@link Map} 实例，其中包含所有注解属性。<p>可以使用 {@link Adapt 适配}来改变值
	 * 的添加方式。
	 * @param factory a map factory
	 * @param adaptations the adaptations that should be applied to the annotation values
	 * @return a map containing the attributes and values
	 */
	<T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations);

	/**
	 * 创建此合并注解的类型安全合成版本，可直接在代码中使用。<p>结果是使用 JDK
	 * {@link java.lang.reflect.Proxy Proxy} 合成的，因此在首次调用时可能产生计算开销。<p>如果此合并注
	 * 解是从注解属性映射或默认属性值创建的（{@linkplain #of(AnnotatedElement, Class, Map) from}），则
	 * 这些属性将始终被合成为注解实例。<p>如果此合并注解是从注解实例创建的
	 * （{@linkplain #from(Annotation) from}），那么当该注解不可<em>合成</em>时，将原样返回该注解。如果注
	 * 解尚未被合成且满足以下条件之一，则认为该注解是可合成的。
	 * <ul>
	 * <li>该注解声明了使用 {@link AliasFor @AliasFor} 标注的属性。</li>
	 * <li>该注解是组合注解，依赖元注解中基于约定的注解属性覆盖。</li>
	 * <li>该注解声明的属性本身是可合成的注解或注解数组。</li>
	 * </ul>
	 * @return a synthesized version of the annotation or the original annotation
	 * unmodified
	 * @throws NoSuchElementException on a missing annotation
	 */
	A synthesize() throws NoSuchElementException;

	/**
	 * 可选地基于条件谓词创建此注解的类型安全合成版本。<p>结果是使用 JDK
	 * {@link java.lang.reflect.Proxy Proxy} 合成的，因此在首次调用时可能产生计算开销。<p>有关何谓可合
	 * 成的说明，请参阅 {@link #synthesize()} 的文档。
	 * @param condition the test to determine if the annotation can be synthesized
	 * @return an optional containing the synthesized version of the annotation or
	 * an empty optional if the condition doesn't match
	 * @throws NoSuchElementException on a missing annotation
	 * @see MergedAnnotationPredicates
	 */
	Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;


	/**
	 * 创建表示缺失注解（即不存在的注解）的 {@link MergedAnnotation}。
	 * @return an instance representing a missing annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> missing() {
		return MissingMergedAnnotation.getInstance();
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotation} 实例。
	 * @param annotation the annotation to include
	 * @return a {@link MergedAnnotation} instance containing the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
		return from(null, annotation);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotation} 实例。
	 * @param source the source for the annotation. This source is used only for
	 * information and logging. It does not need to <em>actually</em> contain
	 * the specified annotations, and it will not be searched.
	 * @param annotation the annotation to include
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
		return TypeMappedAnnotation.from(source, annotation);
	}

	/**
	 * 创建指定注解类型的新 {@link MergedAnnotation} 实例。结果注解将不具有任何属性值，但仍可用于查询默
	 * 认值。
	 * @param annotationType the annotation type
	 * @return a {@link MergedAnnotation} instance for the annotation
	 */
	static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType) {
		return of(null, annotationType, null);
	}

	/**
	 * 创建指定注解类型的新 {@link MergedAnnotation} 实例，其属性值由映射提供。
	 * @param annotationType the annotation type
	 * @param attributes the annotation attributes or {@code null} if just default
	 * values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 * @see #of(AnnotatedElement, Class, Map)
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, annotationType, attributes);
	}

	/**
	 * 创建指定注解类型的新 {@link MergedAnnotation} 实例，其属性值由映射提供。
	 * @param source the source for the annotation. This source is used only for
	 * information and logging. It does not need to <em>actually</em> contain
	 * the specified annotations and it will not be searched.
	 * @param annotationType the annotation type
	 * @param attributes the annotation attributes or {@code null} if just default
	 * values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable AnnotatedElement source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, source, annotationType, attributes);
	}

	/**
	 * 创建指定注解类型的新 {@link MergedAnnotation} 实例，其属性值由映射提供。
	 * @param classLoader the class loader used to resolve class attributes
	 * @param source the source for the annotation. This source is used only for
	 * information and logging. It does not need to <em>actually</em> contain
	 * the specified annotations and it will not be searched.
	 * @param annotationType the annotation type
	 * @param attributes the annotation attributes or {@code null} if just default
	 * values should be used
	 * @return a {@link MergedAnnotation} instance for the annotation and attributes
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable ClassLoader classLoader, @Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return TypeMappedAnnotation.of(classLoader, source, annotationType, attributes);
	}


	/**
	 * 在创建 {@linkplain MergedAnnotation#asMap(Adapt...) Maps} 或
	 * {@link MergedAnnotation#asAnnotationAttributes(Adapt...) AnnotationAttributes} 时可以应用于属性
	 * 值的适配。
	 */
	enum Adapt {

		/**
		 * 将 class 或 class 数组属性适配为字符串。
		 */
		CLASS_TO_STRING,

		/**
		 * 将嵌套注解或注解数组适配为映射，而不是合成这些值。
		 */
		ANNOTATION_TO_MAP;

		protected final boolean isIn(Adapt... adaptations) {
			for (Adapt candidate : adaptations) {
				if (candidate == this) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 工厂方法，用于根据一组布尔标志创建 {@link Adapt} 数组。
		 * @param classToString if {@link Adapt#CLASS_TO_STRING} is included
		 * @param annotationsToMap if {@link Adapt#ANNOTATION_TO_MAP} is included
		 * @return a new {@link Adapt} array
		 */
		public static Adapt[] values(boolean classToString, boolean annotationsToMap) {
			EnumSet<Adapt> result = EnumSet.noneOf(Adapt.class);
			addIfTrue(result, Adapt.CLASS_TO_STRING, classToString);
			addIfTrue(result, Adapt.ANNOTATION_TO_MAP, annotationsToMap);
			return result.toArray(new Adapt[0]);
		}

		private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
			if (test) {
				result.add(value);
			}
		}
	}

}
