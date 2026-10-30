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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 提供用于支持框架中各种命名及其他约定的方法。主要供框架内部使用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
public final class Conventions {

	/**
	 * 使用数组时添加到名称上的后缀。
	 */
	private static final String PLURAL_SUFFIX = "List";


	private Conventions() {
	}


	/**
	 * 根据提供的 {@code Object} 的具体类型，确定其常规变量名。所采用的约定是：
	 * 按照 JavaBeans 属性命名规则，返回 {@code Class} 首字母小写的短名称。
	 * <p>例如：<br>
	 * {@code com.myapp.Product} 变为 {@code "product"}<br>
	 * {@code com.myapp.MyProduct} 变为 {@code "myProduct"}<br>
	 * {@code com.myapp.UKProduct} 变为 {@code "UKProduct"}<br>
	 * <p>对于数组，使用数组组件类型的复数形式。对于 {@code Collection}，
	 * 会尝试“向前查看”以确定组件类型，并返回其复数形式。
	 * @param value 要为其生成变量名的值
	 * @return 生成的变量名
	 */
	public static String getVariableName(Object value) {
		Assert.notNull(value, "Value must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (value.getClass().isArray()) {
			valueClass = value.getClass().getComponentType();
			pluralize = true;
		}
		else if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an empty Collection");
			}
			Object valueToCheck = peekAhead(collection);
			valueClass = getClassForValue(valueToCheck);
			pluralize = true;
		}
		else {
			valueClass = getClassForValue(value);
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * 确定给定参数的常规变量名，并考虑泛型集合类型（如果有）。
	 * <p>从 5.0 开始，此方法支持响应式类型：<br>
	 * {@code Mono<com.myapp.Product>} 变为 {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} 变为 {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} 变为 {@code "myProductObservable"}<br>
	 * @param parameter 方法或构造方法参数
	 * @return 生成的变量名
	 */
	public static String getVariableNameForParameter(MethodParameter parameter) {
		Assert.notNull(parameter, "MethodParameter must not be null");
		Class<?> valueClass;
		boolean pluralize = false;
		String reactiveSuffix = "";

		if (parameter.getParameterType().isArray()) {
			valueClass = parameter.getParameterType().getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
			valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
			if (valueClass == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for non-typed Collection parameter type");
			}
			pluralize = true;
		}
		else {
			valueClass = parameter.getParameterType();
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				reactiveSuffix = ClassUtils.getShortName(valueClass);
				valueClass = parameter.nested().getNestedParameterType();
			}
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name + reactiveSuffix);
	}

	/**
	 * 确定给定方法返回类型的常规变量名，并考虑泛型集合类型（如果有）。
	 * @param method 要为其生成变量名的方法
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * 确定给定方法返回类型的常规变量名，并考虑泛型集合类型（如果有）；
	 * 如果方法声明不够具体（例如返回类型为 {@code Object} 或未指定类型的集合），
	 * 则回退使用给定的实际返回值。
	 * @param method 要为其生成变量名的方法
	 * @param value 返回值（不可用时可以为 {@code null}）
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method, @Nullable Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * 确定给定方法返回类型的常规变量名，并考虑泛型集合类型（如果有）；
	 * 如果方法声明不够具体（例如返回类型为 {@code Object} 或未指定类型的集合），
	 * 则回退使用给定的返回值。
	 * <p>从 5.0 开始，此方法支持响应式类型：<br>
	 * {@code Mono<com.myapp.Product>} 变为 {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} 变为 {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} 变为 {@code "myProductObservable"}<br>
	 * @param method 要为其生成变量名的方法
	 * @param resolvedType 方法解析后的返回类型
	 * @param value 返回值（不可用时可以为 {@code null}）
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, @Nullable Object value) {
		Assert.notNull(method, "Method must not be null");

		if (Object.class == resolvedType) {
			if (value == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an Object return type with null value");
			}
			return getVariableName(value);
		}

		Class<?> valueClass;
		boolean pluralize = false;
		String reactiveSuffix = "";

		if (resolvedType.isArray()) {
			valueClass = resolvedType.getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(resolvedType)) {
			valueClass = ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
			if (valueClass == null) {
				if (!(value instanceof Collection)) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and a non-Collection value");
				}
				Collection<?> collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and an empty Collection value");
				}
				Object valueToCheck = peekAhead(collection);
				valueClass = getClassForValue(valueToCheck);
			}
			pluralize = true;
		}
		else {
			valueClass = resolvedType;
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				reactiveSuffix = ClassUtils.getShortName(valueClass);
				valueClass = ResolvableType.forMethodReturnType(method).getGeneric().toClass();
			}
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name + reactiveSuffix);
	}

	/**
	 * 将属性名称格式的 {@code String}（例如小写、
	 * 用连字符分隔单词）转换为属性名格式（驼峰式）。
	 * 例如 {@code transaction-manager} 变为 {@code "transactionManager"}。
	 */
	public static String attributeNameToPropertyName(String attributeName) {
		Assert.notNull(attributeName, "'attributeName' must not be null");
		if (!attributeName.contains("-")) {
			return attributeName;
		}
		char[] result = new char[attributeName.length() -1]; // not completely accurate but good guess
		int currPos = 0;
		boolean upperCaseNext = false;
		for (int i = 0; i < attributeName.length(); i++ ) {
			char c = attributeName.charAt(i);
			if (c == '-') {
				upperCaseNext = true;
			}
			else if (upperCaseNext) {
				result[currPos++] = Character.toUpperCase(c);
				upperCaseNext = false;
			}
			else {
				result[currPos++] = c;
			}
		}
		return new String(result, 0, currPos);
	}

	/**
	 * 返回由给定外围 {@link Class} 限定的属性名。例如，
	 * 属性名 '{@code foo}' 由 {@link Class} '{@code com.myapp.SomeClass}' 限定后为
	 * '{@code com.myapp.SomeClass.foo}'。
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + '.' + attributeName;
	}


	/**
	 * 确定用于为包含给定值的变量命名的类。
	 * <p>将返回给定值的类，但在遇到 JDK 代理时除外；此时它确定该代理所实现的“主要”接口。
	 * @param value 要检查的值
	 * @return 用于为变量命名的类
	 */
	private static Class<?> getClassForValue(Object value) {
		Class<?> valueClass = value.getClass();
		if (Proxy.isProxyClass(valueClass)) {
			Class<?>[] ifcs = valueClass.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (!ClassUtils.isJavaLanguageInterface(ifc)) {
					return ifc;
				}
			}
		}
		else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
			// '$' in the class name but no inner class -
			// assuming it's a special subclass (e.g. by OpenJPA)
			valueClass = valueClass.getSuperclass();
		}
		return valueClass;
	}

	/**
	 * 将给定名称转为复数形式。
	 */
	private static String pluralize(String name) {
		return name + PLURAL_SUFFIX;
	}

	/**
	 * 获取 {@code Collection} 中某个元素的 {@code Class}。具体获取哪个元素的 {@code Class}，
	 * 取决于 {@code Collection} 的具体实现。
	 */
	private static <E> E peekAhead(Collection<E> collection) {
		Iterator<E> it = collection.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - no element found");
		}
		E value = it.next();
		if (value == null) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - only null element found");
		}
		return value;
	}

}
