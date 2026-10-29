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

package org.springframework.core.convert;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 关于要转换的源类型或目标类型的上下文描述符。
 * <p>能够表示数组和泛型集合类型。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementAdapter annotatedElement;


	/**
	 * 根据 {@link MethodParameter} 创建一个新的类型描述符。
	 * <p>当源转换点或目标转换点是构造器参数、方法参数或方法返回值时，请使用此构造器。
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * 根据 {@link Field} 创建一个新的类型描述符。
	 * <p>当源转换点或目标转换点是字段时，请使用此构造器。
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * 根据 {@link Property} 创建一个新的类型描述符。
	 * <p>当源转换点或目标转换点是 Java 类上的属性时，请使用此构造器。
	 * @param property the property
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
	}

	/**
	 * 根据 {@link ResolvableType} 创建一个新的类型描述符。
	 * <p>此构造器供内部使用，也可由支持具有扩展类型系统的非 Java 语言的子类使用。自 5.1.4 起它变为
	 * public，在此之前为 protected。
	 * @param resolvableType the resolvable type
	 * @param type the backing type (or {@code null} if it should get resolved)
	 * @param annotations the type annotations
	 * @since 4.0
	 */
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElement = new AnnotatedElementAdapter(annotations);
	}


	/**
	 * {@link #getType()} 的变体，它通过返回基本类型的对象包装类型来处理基本类型。
	 * <p>这对于希望规范化为基于对象的类型而不直接使用基本类型的转换服务实现很有用。
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * 此 TypeDescriptor 所描述的底层类、方法参数、字段或属性的类型。
	 * <p>按原样返回基本类型。如果需要将基本类型解析为对应的 Object 类型，请参见此操作的变体
	 * {@link #getObjectType()}。
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * 返回底层的 {@link ResolvableType}。
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回描述符的底层来源。根据 {@link TypeDescriptor} 的构造方式，将返回 {@link Field}、
	 * {@link MethodParameter} 或 {@link Type}。此方法主要用于提供对替代 JVM 语言可能提供的额外类型信息
	 * 或元数据的访问。
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}

	/**
	 * 通过将此 {@link TypeDescriptor} 的类型设置为所提供值的类来收窄它。
	 * <p>如果值为 {@code null}，则不执行收窄，并原样返回此 TypeDescriptor。
	 * <p>设计为由绑定框架在读取属性、字段或方法返回值时调用。允许此类框架收窄基于声明的属性、字段或方
	 * 法返回值类型构建的 TypeDescriptor。例如，声明为 {@code java.lang.Object} 的字段，如果被设置为
	 * {@code java.util.HashMap} 值，则会被收窄为 {@code java.util.HashMap}。收窄后的 TypeDescriptor 随
	 * 后可用于将 HashMap 转换为其他类型。收窄后的副本会保留注解和嵌套类型上下文。
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
	 * class of the provided value)
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * 将此 {@link TypeDescriptor} 转换为父类或已实现的接口，并保留注解和嵌套类型上下文。
	 * @param superType the supertype to cast to (can be {@code null})
	 * @return a new TypeDescriptor for the up-cast type
	 * @throws IllegalArgumentException if this type is not assignable to the super-type
	 * @since 3.2
	 */
	@Nullable
	public TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * 返回此类型的名称：完全限定的类名。
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * 此类型是否为基本类型？
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * 返回与此类型描述符关联的注解（如果有）。
	 * @return the annotations, or an empty array if none
	 */
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * 确定此类型描述符是否具有指定的注解。
	 * <p>自 Spring Framework 4.2 起，此方法支持任意层级的元注解。
	 * @param annotationType the annotation type
	 * @return {@code true} if the annotation is present
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * 获取此类型描述符上指定 {@code annotationType} 的注解。
	 * <p>自 Spring Framework 4.2 起，此方法支持任意层级的元注解。
	 * @param annotationType the annotation type
	 * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
	 */
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
	}

	/**
	 * 如果此类型描述符的对象可以赋值给给定类型描述符所描述的位置，则返回 true。
	 * <p>例如，{@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))} 返回
	 * {@code true}，因为 String 值可以赋值给 CharSequence 变量。另一方面，
	 * {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))} 返回 {@code false}，因为虽
	 * 然所有 Integer 都是 Number，但并非所有 Number 都是 Integer。
	 * <p>对于数组、集合和映射，如果已声明，则会检查元素类型以及键/值类型。例如，{@code List<String>}
	 * 字段值可赋值给 {@code Collection<CharSequence>} 字段，但 {@code List<Number>} 不可赋值给
	 * {@code List<Integer>}。
	 * @return {@code true} if this type is assignable to the type represented by the provided
	 * type descriptor
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
				isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		}
		else {
			return true;
		}
	}

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
			@Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

	/**
	 * 此类型是否为 {@link Collection} 类型？
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * 此类型是否为数组类型？
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * 如果此类型是数组，则返回数组的组件类型。如果此类型是 {@code Stream}，则返回流的组件类型。如果此
	 * 类型是 {@link Collection} 且已参数化，则返回该 Collection 的元素类型。如果该 Collection 未参数化，
	 * 则返回 {@code null}，表示未声明元素类型。
	 * @return the array component type or Collection element type, or {@code null} if this type is not
	 * an array type or a {@code java.util.Collection} or if its element type is not parameterized
	 * @see #elementTypeDescriptor(Object)
	 */
	@Nullable
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Collection} 或数组，则根据提供的集合或数组元素创建元素 TypeDescriptor。
	 * <p>将 {@link #getElementTypeDescriptor() elementType} 属性收窄为所提供的集合或数组元素的类。例如，
	 * 如果此描述符描述的是 {@code java.util.List<java.lang.Number>}，而元素参数是
	 * {@code java.lang.Integer}，则返回的 TypeDescriptor 为 {@code java.lang.Integer}。如果此描述符描
	 * 述的是 {@code java.util.List<?>}，而元素参数是 {@code java.lang.Integer}，则返回的
	 * TypeDescriptor 同样为 {@code java.lang.Integer}。
	 * <p>返回的收窄 TypeDescriptor 中会保留注解和嵌套类型上下文。
	 * @param element the collection or array element
	 * @return an element type descriptor, narrowed to the type of the provided element
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * 此类型是否为 {@link Map} 类型？
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * 如果此类型是 {@link Map} 且其键类型已参数化，则返回映射的键类型。如果该 Map 的键类型未参数化，则
	 * 返回 {@code null}，表示未声明键类型。
	 * @return the Map key type, or {@code null} if this type is a Map
	 * but its key type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Map}，则根据提供的映射键创建 mapKey {@link TypeDescriptor}。
	 * <p>将 {@link #getMapKeyTypeDescriptor() mapKeyType} 属性收窄为所提供映射键的类。例如，如果此描述
	 * 符描述的是 {@code java.util.Map<java.lang.Number, java.lang.String>}，而键参数是
	 * {@code java.lang.Integer}，则返回的 TypeDescriptor 为 {@code java.lang.Integer}。如果此描述符描
	 * 述的是 {@code java.util.Map<?, ?>}，而键参数是 {@code java.lang.Integer}，则返回的
	 * TypeDescriptor 同样为 {@code java.lang.Integer}。
	 * <p>返回的收窄 TypeDescriptor 中会保留注解和嵌套类型上下文。
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * 如果此类型是 {@link Map} 且其值类型已参数化，则返回映射的值类型。
	 * <p>如果该 Map 的值类型未参数化，则返回 {@code null}，表示未声明值类型。
	 * @return the Map value type, or {@code null} if this type is a Map
	 * but its value type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * 如果此类型是 {@link Map}，则根据提供的映射值创建 mapValue {@link TypeDescriptor}。
	 * <p>将 {@link #getMapValueTypeDescriptor() mapValueType} 属性收窄为所提供映射值的类。例如，如果此
	 * 描述符描述的是 {@code java.util.Map<java.lang.String, java.lang.Number>}，而值参数是
	 * {@code java.lang.Integer}，则返回的 TypeDescriptor 为 {@code java.lang.Integer}。如果此描述符描
	 * 述的是 {@code java.util.Map<?, ?>}，而值参数是 {@code java.lang.Integer}，则返回的
	 * TypeDescriptor 同样为 {@code java.lang.Integer}。
	 * <p>返回的收窄 TypeDescriptor 中会保留注解和嵌套类型上下文。
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	@Nullable
	private TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor otherDesc = (TypeDescriptor) other;
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
		}
		else if (isMap()) {
			return (ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor()) &&
					ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor()));
		}
		else {
			return true;
		}
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(ann.annotationType().getName()).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}


	/**
	 * 为对象创建一个新的类型描述符。
	 * <p>使用此工厂方法先内省源对象，然后再让转换系统将其转换为其他某个类型。
	 * <p>如果提供的对象为 {@code null}，则返回 {@code null}，否则调用 {@link #valueOf(Class)} 根据对象
	 * 的类构建 TypeDescriptor。
	 * @param source the source object
	 * @return the type descriptor
	 */
	@Nullable
	public static TypeDescriptor forObject(@Nullable Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * 根据给定类型创建一个新的类型描述符。
	 * <p>当没有方法参数或字段之类的类型位置可用于提供额外的转换上下文时，可使用此方法指示转换系统将对
	 * 象转换为特定的目标类型。
	 * <p>通常更推荐使用 {@link #forObject(Object)} 从源对象构造类型描述符，因为它会处理 {@code null}
	 * 对象的情况。
	 * @param type the class (may be {@code null} to indicate {@code Object.class})
	 * @return the corresponding type descriptor
	 */
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		TypeDescriptor desc = commonTypesCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * 根据 {@link java.util.Collection} 类型创建一个新的类型描述符。
	 * <p>用于转换为有类型的 Collection。
	 * <p>例如，通过转换为用此方法构建的 targetType，可以将 {@code List<String>} 转换为
	 * {@code List<EmailAddress>}。构造此类 {@code TypeDescriptor} 的方法调用大致如下：
	 * {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementTypeDescriptor a descriptor for the collection's element type,
	 * used to convert collection elements
	 * @return the collection type descriptor
	 */
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * 根据 {@link java.util.Map} 类型创建一个新的类型描述符。
	 * <p>用于转换为有类型的 Map。
	 * <p>例如，通过转换为用此方法构建的 targetType，可以将 {@code Map<String, String>} 转换为
	 * {@code Map<Id, EmailAddress>}。构造此类 TypeDescriptor 的方法调用大致如下：
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 * @param mapType the map type, which must implement {@link Map}
	 * @param keyTypeDescriptor a descriptor for the map's key type, used to convert map keys
	 * @param valueTypeDescriptor the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
			@Nullable TypeDescriptor valueTypeDescriptor) {

		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * 创建一个表示指定类型数组的新类型描述符。
	 * <p>例如，要创建 {@code Map<String,String>[]}，请使用：
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 * @param elementTypeDescriptor the {@link TypeDescriptor} of the array element or {@code null}
	 * @return an array {@link TypeDescriptor} or {@code null} if {@code elementTypeDescriptor} is {@code null}
	 * @since 3.2.1
	 */
	@Nullable
	public static TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * 为在方法参数中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果 methodParameter 是 {@code List<String>} 且嵌套层级为 1，则嵌套类型描述符将是
	 * String.class。
	 * <p>如果 methodParameter 是 {@code List<List<String>>} 且嵌套层级为 2，则嵌套类型描述符同样将是
	 * String.class。
	 * <p>如果 methodParameter 是 {@code Map<Integer, String>} 且嵌套层级为 1，则嵌套类型描述符将是从映
	 * 射值派生的 String。
	 * <p>如果 methodParameter 是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，则嵌套类型描述符将
	 * 是从映射值派生的 String。
	 * <p>如果由于未声明而无法获取嵌套类型，则返回 {@code null}。例如，如果方法参数是 {@code List<?>}，
	 * 则返回的嵌套类型描述符为 {@code null}。
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the method parameter
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the nesting level of the input
	 * {@link MethodParameter} argument is not 1, or if the types up to the
	 * specified nesting level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return nested(new TypeDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * 为在字段中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果该字段是 {@code List<String>} 且嵌套层级为 1，则嵌套类型描述符将是
	 * {@code String.class}。
	 * <p>如果该字段是 {@code List<List<String>>} 且嵌套层级为 2，则嵌套类型描述符同样将是
	 * {@code String.class}。
	 * <p>如果该字段是 {@code Map<Integer, String>} 且嵌套层级为 1，则嵌套类型描述符将是从映射值派生的
	 * String。
	 * <p>如果该字段是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，则嵌套类型描述符将是从映射值
	 * 派生的 String。
	 * <p>如果由于未声明而无法获取嵌套类型，则返回 {@code null}。例如，如果该字段是 {@code List<?>}，则
	 * 返回的嵌套类型描述符为 {@code null}。
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the field
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * 为在属性中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果该属性是 {@code List<String>} 且嵌套层级为 1，则嵌套类型描述符将是
	 * {@code String.class}。
	 * <p>如果该属性是 {@code List<List<String>>} 且嵌套层级为 2，则嵌套类型描述符同样将是
	 * {@code String.class}。
	 * <p>如果该属性是 {@code Map<Integer, String>} 且嵌套层级为 1，则嵌套类型描述符将是从映射值派生的
	 * String。
	 * <p>如果该属性是 {@code List<Map<Integer, String>>} 且嵌套层级为 2，则嵌套类型描述符将是从映射值
	 * 派生的 String。
	 * <p>如果由于未声明而无法获取嵌套类型，则返回 {@code null}。例如，如果该属性是 {@code List<?>}，则
	 * 返回的嵌套类型描述符为 {@code null}。
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new TypeDescriptor(property), nestingLevel);
	}

	@Nullable
	private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
		ResolvableType nested = typeDescriptor.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
			}
			else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(typeDescriptor, nested);
	}

	@Nullable
	private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, source.getAnnotations());
	}


	/**
	 * 适配器类，用于将 {@code TypeDescriptor} 的注解暴露为 {@link AnnotatedElement}，特别是提供给
	 * {@link AnnotatedElementUtils}。
	 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
	 */
	private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		@Nullable
		private final Annotation[] annotations;

		public AnnotatedElementAdapter(@Nullable Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return ObjectUtils.isEmpty(this.annotations);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter &&
					Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return TypeDescriptor.this.toString();
		}
	}

}
