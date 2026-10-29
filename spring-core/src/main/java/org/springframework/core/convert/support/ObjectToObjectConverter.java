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

package org.springframework.core.convert.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * 通用转换器，通过委托源对象上的方法、{@code targetType} 上的静态工厂方法或构造函数，
 * 按照约定将源对象转换为 {@code targetType}。
 *
 * <h3>转换算法</h3>
 * <ol>
 * <li>如果存在这样的方法，则调用源对象上返回类型可赋值给 {@code targetType} 的非静态
 * {@code to[targetType.simpleName]()} 方法。例如，{@code org.example.Bar Foo#toBar()} 是
 * 遵循此约定的方法。
 * <li>否则，如果存在这样的方法，则调用 {@code targetType} 上的 <em>静态</em> {@code valueOf(sourceType)}
 * 或 Java 8 风格的 <em>静态</em> {@code of(sourceType)} 或 {@code from(sourceType)} 方法，
 * 其返回类型与 {@code targetType} <em>相关</em>。例如，返回 {@code Foo}、{@code SuperFooType}
 * 或 {@code SubFooType} 的静态 {@code Foo.of(sourceType)} 方法是遵循此约定的方法。
 * {@link java.time.ZoneId#of(String)} 是此类静态工厂方法的具体示例，它返回 {@code ZoneId} 的子类型。
 * <li>否则，如果存在这样的构造函数，则调用 {@code targetType} 上接受单个 {@code sourceType}
 * 参数的构造函数。
 * <li>否则抛出 {@link ConversionFailedException} 或 {@link IllegalStateException}。
 * </ol>
 *
 * <p><strong>警告</strong>：此转换器<em>不</em>支持通过 {@link Object#toString()} 或
 * {@link String#valueOf(Object)} 方法将 {@code sourceType} 转换为 {@code java.lang.String}。
 * 如需 {@code toString()} 支持，请改用 {@link FallbackObjectToStringConverter}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see FallbackObjectToStringConverter
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

	// Cache for the latest to-method, static factory method, or factory constructor
	// resolved on a given Class
	private static final Map<Class<?>, Executable> conversionExecutableCache =
			new ConcurrentReferenceHashMap<>(32);


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (sourceType.getType() != targetType.getType() &&
				hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		Executable executable = getValidatedExecutable(targetClass, sourceClass);

		try {
			if (executable instanceof Method) {
				Method method = (Method) executable;
				ReflectionUtils.makeAccessible(method);
				if (!Modifier.isStatic(method.getModifiers())) {
					return method.invoke(source);
				}
				else {
					return method.invoke(null, source);
				}
			}
			else if (executable instanceof Constructor) {
				Constructor<?> ctor = (Constructor<?>) executable;
				ReflectionUtils.makeAccessible(ctor);
				return ctor.newInstance(source);
			}
		}
		catch (InvocationTargetException ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}

		// If sourceClass is Number and targetClass is Integer, the following message should expand to:
		// No toInteger() method exists on java.lang.Number, and no static valueOf/of/from(java.lang.Number)
		// method or Integer(java.lang.Number) constructor exists on java.lang.Integer.
		throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, " +
				"and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
				sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
	}


	static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return (getValidatedExecutable(targetClass, sourceClass) != null);
	}

	@Nullable
	private static Executable getValidatedExecutable(Class<?> targetClass, Class<?> sourceClass) {
		Executable executable = conversionExecutableCache.get(targetClass);
		if (isApplicable(executable, sourceClass)) {
			return executable;
		}

		executable = determineToMethod(targetClass, sourceClass);
		if (executable == null) {
			executable = determineFactoryMethod(targetClass, sourceClass);
			if (executable == null) {
				executable = determineFactoryConstructor(targetClass, sourceClass);
				if (executable == null) {
					return null;
				}
			}
		}

		conversionExecutableCache.put(targetClass, executable);
		return executable;
	}

	private static boolean isApplicable(Executable executable, Class<?> sourceClass) {
		if (executable instanceof Method) {
			Method method = (Method) executable;
			return (!Modifier.isStatic(method.getModifiers()) ?
					ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
					method.getParameterTypes()[0] == sourceClass);
		}
		else if (executable instanceof Constructor) {
			Constructor<?> ctor = (Constructor<?>) executable;
			return (ctor.getParameterTypes()[0] == sourceClass);
		}
		else {
			return false;
		}
	}

	@Nullable
	private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass || String.class == sourceClass) {
			// Do not accept a toString() method or any to methods on String itself
			return null;
		}

		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
		return (method != null && !Modifier.isStatic(method.getModifiers()) &&
				ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
	}

	@Nullable
	private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass) {
			// Do not accept the String.valueOf(Object) method
			return null;
		}

		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}

		return (method != null && areRelatedTypes(targetClass, method.getReturnType()) ? method : null);
	}

	/**
	 * 判断两个类型是否存在于同一类型层次结构中（即类型 1 可赋值给类型 2，或反之）。
	 * @since 5.3.21
	 * @see ClassUtils#isAssignable(Class, Class)
	 */
	private static boolean areRelatedTypes(Class<?> type1, Class<?> type2) {
		return (ClassUtils.isAssignable(type1, type2) || ClassUtils.isAssignable(type2, type1));
	}

	@Nullable
	private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
	}

}
