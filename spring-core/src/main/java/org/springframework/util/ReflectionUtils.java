/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 用于操作反射 API 并处理反射异常的简单工具类。
 *
 * <p>仅供内部使用。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Costin Leau
 * @author Sam Brannen
 * @author Chris Beams
 * @since 1.2.2
 */
public abstract class ReflectionUtils {

	/**
	 * 预构建的 {@link MethodFilter}，匹配所有非桥接、非合成的方法，且这些方法未在
	 * {@code java.lang.Object} 上声明。
	 * @since 3.0.5
	 */
	public static final MethodFilter USER_DECLARED_METHODS =
			(method -> !method.isBridge() && !method.isSynthetic() && (method.getDeclaringClass() != Object.class));

	/**
	 * 预构建的 FieldFilter，匹配所有非静态、非 final 字段。
	 */
	public static final FieldFilter COPYABLE_FIELDS =
			(field -> !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())));


	/**
	 * CGLIB 重命名方法的命名前缀。
	 * @see #isCglibRenamedMethod
	 */
	private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

	private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];


	/**
	 * {@link Class#getDeclaredMethods()} 以及来自基于 Java 8 的接口的等效默认方法的缓存，便于快速迭代。
	 *
	 */
	private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);

	/**
	 * {@link Class#getDeclaredFields()} 的缓存，便于快速迭代。
	 */
	private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);


	// Exception handling

	/**
	 * 处理给定的反射异常。
	 * <p>仅当目标方法不应抛出受检异常，或在访问方法或字段时发生错误时才应调用。
	 * <p>如果 InvocationTargetException 具有此类根因，则抛出底层的 RuntimeException 或 Error。否则抛出
	 * 带有适当消息的 IllegalStateException 或 UndeclaredThrowableException。
	 * @param ex the reflection exception to handle
	 */
	public static void handleReflectionException(Exception ex) {
		if (ex instanceof NoSuchMethodException) {
			throw new IllegalStateException("Method not found: " + ex.getMessage());
		}
		if (ex instanceof IllegalAccessException) {
			throw new IllegalStateException("Could not access method or field: " + ex.getMessage());
		}
		if (ex instanceof InvocationTargetException) {
			handleInvocationTargetException((InvocationTargetException) ex);
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 处理给定的调用目标异常。仅当目标方法不应抛出受检异常时才应调用。
	 * <p>如果存在此类根因，则抛出底层的 RuntimeException 或 Error。否则抛出
	 * UndeclaredThrowableException。
	 * @param ex the invocation target exception to handle
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	/**
	 * 重新抛出给定的 {@link Throwable exception}，它大概是 {@link InvocationTargetException} 的 <em>
	 * target exception</em>。仅当目标方法不应抛出受检异常时才应调用。
	 * <p>如果合适，将底层异常强制转换为 {@link RuntimeException} 或 {@link Error} 后重新抛出；否则抛出
	 * {@link UndeclaredThrowableException}。
	 * @param ex the exception to rethrow
	 * @throws RuntimeException the rethrown exception
	 */
	public static void rethrowRuntimeException(Throwable ex) {
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 重新抛出给定的 {@link Throwable exception}，它大概是 {@link InvocationTargetException} 的 <em>
	 * target exception</em>。仅当目标方法不应抛出受检异常时才应调用。
	 * <p>如果合适，将底层异常强制转换为 {@link Exception} 或 {@link Error} 后重新抛出；否则抛出
	 * {@link UndeclaredThrowableException}。
	 * @param ex the exception to rethrow
	 * @throws Exception the rethrown exception (in case of a checked exception)
	 */
	public static void rethrowException(Throwable ex) throws Exception {
		if (ex instanceof Exception) {
			throw (Exception) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}


	// Constructor handling

	/**
	 * 为给定类和参数获取可访问的构造函数。
	 * @param clazz the clazz to check
	 * @param parameterTypes the parameter types of the desired constructor
	 * @return the constructor reference
	 * @throws NoSuchMethodException if no such constructor exists
	 * @since 5.0
	 */
	public static <T> Constructor<T> accessibleConstructor(Class<T> clazz, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
		makeAccessible(ctor);
		return ctor;
	}

	/**
	 * 使给定的构造函数可访问，必要时显式将其设置为可访问。仅在确实需要时才调用
	 * {@code setAccessible(true)} 方法，以避免与 JVM SecurityManager（若启用）产生不必要的冲突。
	 * @param ctor the constructor to make accessible
	 * @see java.lang.reflect.Constructor#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Constructor<?> ctor) {
		if ((!Modifier.isPublic(ctor.getModifiers()) ||
				!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
			ctor.setAccessible(true);
		}
	}


	// Method handling

	/**
	 * 尝试在所提供的类上查找具有所提供名称且无参数的 {@link Method}。会搜索直至 {@code Object} 的所有
	 * 超类。
	 * <p>如果找不到 {@link Method}，则返回 {@code null}。
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @return the Method object, or {@code null} if none found
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
	}

	/**
	 * 尝试在所提供的类上查找具有所提供名称和参数类型的 {@link Method}。会搜索直至 {@code Object} 的所
	 * 有超类。
	 * <p>如果找不到 {@link Method}，则返回 {@code null}。
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be {@code null} to indicate any signature)
	 * @return the Method object, or {@code null} if none found
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String name, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(name, "Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() :
					getDeclaredMethods(searchType, false));
			for (Method method : methods) {
				if (name.equals(method.getName()) && (paramTypes == null || hasSameParams(method, paramTypes))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
		return (paramTypes.length == method.getParameterCount() &&
				Arrays.equals(paramTypes, method.getParameterTypes()));
	}

	/**
	 * 使用无参数在所提供的目标对象上调用指定的 {@link Method}。调用静态 {@link Method} 时目标对象可以
	 * 为 {@code null}。
	 * <p>抛出的异常通过调用 {@link #handleReflectionException} 进行处理。
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @return the invocation result, if any
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */
	@Nullable
	public static Object invokeMethod(Method method, @Nullable Object target) {
		return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
	}

	/**
	 * 使用所提供的参数在所提供的目标对象上调用指定的 {@link Method}。调用静态 {@link Method} 时目标对
	 * 象可以为 {@code null}。
	 * <p>抛出的异常通过调用 {@link #handleReflectionException} 进行处理。
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @param args the invocation arguments (may be {@code null})
	 * @return the invocation result, if any
	 */
	@Nullable
	public static Object invokeMethod(Method method, @Nullable Object target, @Nullable Object... args) {
		try {
			return method.invoke(target, args);
		}
		catch (Exception ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 判断给定方法是否显式声明了给定异常或其某个超类，这意味着该类型的异常可以在反射调用中按原样传播。
	 * @param method the declaring method
	 * @param exceptionType the exception to throw
	 * @return {@code true} if the exception can be thrown as-is;
	 * {@code false} if it needs to be wrapped
	 */
	public static boolean declaresException(Method method, Class<?> exceptionType) {
		Assert.notNull(method, "Method must not be null");
		Class<?>[] declaredExceptions = method.getExceptionTypes();
		for (Class<?> declaredException : declaredExceptions) {
			if (declaredException.isAssignableFrom(exceptionType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对给定类的所有匹配方法执行给定的回调操作，这些方法是在该类本地声明的或其等效项（例如给定类所实现
	 * 的基于 Java 8 的接口上的默认方法）。
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
	 * @since 4.2
	 * @see #doWithMethods
	 */
	public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 对给定类及其超类的所有匹配方法执行给定的回调操作。
	 * <p>出现在子类和超类上的同名方法将出现两次，除非被 {@link MethodFilter} 排除。
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
	 * @see #doWithMethods(Class, MethodCallback, MethodFilter)
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
		doWithMethods(clazz, mc, null);
	}

	/**
	 * 对给定类及其超类（或给定接口及其父接口）的所有匹配方法执行给定的回调操作。
	 * <p>出现在子类和超类上的同名方法将出现两次，除非被指定的 {@link MethodFilter} 排除。
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @param mf the filter that determines the methods to apply the callback to
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc, @Nullable MethodFilter mf) {
		if (mf == USER_DECLARED_METHODS && clazz == Object.class) {
			// nothing to introspect
			return;
		}
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			if (mf != null && !mf.matches(method)) {
				continue;
			}
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
		// Keep backing up the inheritance hierarchy.
		if (clazz.getSuperclass() != null && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
			doWithMethods(clazz.getSuperclass(), mc, mf);
		}
		else if (clazz.isInterface()) {
			for (Class<?> superIfc : clazz.getInterfaces()) {
				doWithMethods(superIfc, mc, mf);
			}
		}
	}

	/**
	 * 获取叶子类及所有超类上声明的全部方法。叶子类的方法优先包含。
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
	 */
	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, methods::add);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * 获取叶子类及所有超类上声明的唯一方法集合。叶子类的方法优先包含，并且在遍历超类层次结构时，任何签
	 * 名与已包含方法匹配的方法都会被过滤掉。
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
		return getUniqueDeclaredMethods(leafClass, null);
	}

	/**
	 * 获取叶子类及所有超类上声明的唯一方法集合。叶子类的方法优先包含，并且在遍历超类层次结构时，任何签
	 * 名与已包含方法匹配的方法都会被过滤掉。
	 * @param leafClass the class to introspect
	 * @param mf the filter that determines the methods to take into account
	 * @throws IllegalStateException if introspection fails
	 * @since 5.2
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass, @Nullable MethodFilter mf) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, method -> {
			boolean knownSignature = false;
			Method methodBeingOverriddenWithCovariantReturnType = null;
			for (Method existingMethod : methods) {
				if (method.getName().equals(existingMethod.getName()) &&
						method.getParameterCount() == existingMethod.getParameterCount() &&
						Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
					// Is this a covariant return type situation?
					if (existingMethod.getReturnType() != method.getReturnType() &&
							existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
						methodBeingOverriddenWithCovariantReturnType = existingMethod;
					}
					else {
						knownSignature = true;
					}
					break;
				}
			}
			if (methodBeingOverriddenWithCovariantReturnType != null) {
				methods.remove(methodBeingOverriddenWithCovariantReturnType);
			}
			if (!knownSignature && !isCglibRenamedMethod(method)) {
				methods.add(method);
			}
		}, mf);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * {@link Class#getDeclaredMethods()} 的变体，使用本地缓存以避免 JVM 的 SecurityManager 检查和创建
	 * 新的 Method 实例。此外，它还包括本地实现的接口中的 Java 8 默认方法，因为这些方法实际上应被视为已
	 * 声明的方法。
	 * @param clazz the class to introspect
	 * @return the cached array of methods
	 * @throws IllegalStateException if introspection fails
	 * @since 5.2
	 * @see Class#getDeclaredMethods()
	 */
	public static Method[] getDeclaredMethods(Class<?> clazz) {
		return getDeclaredMethods(clazz, true);
	}

	private static Method[] getDeclaredMethods(Class<?> clazz, boolean defensive) {
		Assert.notNull(clazz, "Class must not be null");
		Method[] result = declaredMethodsCache.get(clazz);
		if (result == null) {
			try {
				Method[] declaredMethods = clazz.getDeclaredMethods();
				List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
				if (defaultMethods != null) {
					result = new Method[declaredMethods.length + defaultMethods.size()];
					System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
					int index = declaredMethods.length;
					for (Method defaultMethod : defaultMethods) {
						result[index] = defaultMethod;
						index++;
					}
				}
				else {
					result = declaredMethods;
				}
				declaredMethodsCache.put(clazz, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
						"] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return (result.length == 0 || !defensive) ? result : result.clone();
	}

	@Nullable
	private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
		List<Method> result = null;
		for (Class<?> ifc : clazz.getInterfaces()) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
					if (result == null) {
						result = new ArrayList<>();
					}
					result.add(ifcMethod);
				}
			}
		}
		return result;
	}

	/**
	 * 判断给定方法是否为 "equals" 方法。
	 * @see java.lang.Object#equals(Object)
	 */
	public static boolean isEqualsMethod(@Nullable Method method) {
		if (method == null) {
			return false;
		}
		if (method.getParameterCount() != 1) {
			return false;
		}
		if (!method.getName().equals("equals")) {
			return false;
		}
		return method.getParameterTypes()[0] == Object.class;
	}

	/**
	 * 判断给定方法是否为 "hashCode" 方法。
	 * @see java.lang.Object#hashCode()
	 */
	public static boolean isHashCodeMethod(@Nullable Method method) {
		return method != null && method.getParameterCount() == 0 && method.getName().equals("hashCode");
	}

	/**
	 * 判断给定方法是否为 "toString" 方法。
	 * @see java.lang.Object#toString()
	 */
	public static boolean isToStringMethod(@Nullable Method method) {
		return (method != null && method.getParameterCount() == 0 && method.getName().equals("toString"));
	}

	/**
	 * 判断给定方法是否最初由 {@link java.lang.Object} 声明。
	 */
	public static boolean isObjectMethod(@Nullable Method method) {
		return (method != null && (method.getDeclaringClass() == Object.class ||
				isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method)));
	}

	/**
	 * 判断给定方法是否为 CGLIB 的“重命名”方法，遵循 "CGLIB$methodName$0" 模式。
	 * @param renamedMethod the method to check
	 */
	public static boolean isCglibRenamedMethod(Method renamedMethod) {
		String name = renamedMethod.getName();
		if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
			int i = name.length() - 1;
			while (i >= 0 && Character.isDigit(name.charAt(i))) {
				i--;
			}
			return (i > CGLIB_RENAMED_METHOD_PREFIX.length() && (i < name.length() - 1) && name.charAt(i) == '$');
		}
		return false;
	}

	/**
	 * 使给定的方法可访问，必要时显式将其设置为可访问。仅在确实需要时才调用 {@code setAccessible(true)}
	 * 方法，以避免与 JVM SecurityManager（若启用）产生不必要的冲突。
	 * @param method the method to make accessible
	 * @see java.lang.reflect.Method#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
			method.setAccessible(true);
		}
	}


	// Field handling

	/**
	 * 尝试在所提供的 {@link Class} 上查找具有所提供 {@code name} 的 {@link Field field}。会搜索直至
	 * {@link Object} 的所有超类。
	 * @param clazz the class to introspect
	 * @param name the name of the field
	 * @return the corresponding Field object, or {@code null} if not found
	 */
	@Nullable
	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	/**
	 * 尝试在所提供的 {@link Class} 上查找具有所提供 {@code name} 和/或 {@link Class type} 的
	 * {@link Field field}。会搜索直至 {@link Object} 的所有超类。
	 * @param clazz the class to introspect
	 * @param name the name of the field (may be {@code null} if type is specified)
	 * @param type the type of the field (may be {@code null} if name is specified)
	 * @return the corresponding Field object, or {@code null} if not found
	 */
	@Nullable
	public static Field findField(Class<?> clazz, @Nullable String name, @Nullable Class<?> type) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) &&
						(type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	/**
	 * 将所提供 {@linkplain Field field object} 在指定 {@linkplain Object target object} 上表示的字段设
	 * 置为指定的 {@code value}。
	 * <p>按照 {@link Field#set(Object, Object)} 语义，如果底层字段为基本类型，新值会自动拆箱。
	 * <p>此方法不支持设置 {@code static final} 字段。
	 * <p>抛出的异常通过调用 {@link #handleReflectionException(Exception)} 进行处理。
	 * @param field the field to set
	 * @param target the target object on which to set the field
	 * (or {@code null} for a static field)
	 * @param value the value to set (may be {@code null})
	 */
	public static void setField(Field field, @Nullable Object target, @Nullable Object value) {
		try {
			field.set(target, value);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
	}

	/**
	 * 获取所提供 {@link Field field object} 在指定 {@link Object target object} 上表示的字段。按照
	 * {@link Field#get(Object)} 语义，如果底层字段为基本类型，返回的值会自动装箱。
	 * <p>抛出的异常通过调用 {@link #handleReflectionException(Exception)} 进行处理。
	 * @param field the field to get
	 * @param target the target object from which to get the field
	 * (or {@code null} for a static field)
	 * @return the field's current value
	 */
	@Nullable
	public static Object getField(Field field, @Nullable Object target) {
		try {
			return field.get(target);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 对给定类中所有本地声明的字段调用给定的回调。
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
	 * @since 4.2
	 * @see #doWithFields
	 */
	public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
		for (Field field : getDeclaredFields(clazz)) {
			try {
				fc.doWith(field);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 对目标类中的所有字段调用给定的回调，沿类层次结构向上获取所有声明的字段。
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	/**
	 * 对目标类中的所有字段调用给定的回调，沿类层次结构向上获取所有声明的字段。
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @param ff the filter that determines the fields to apply the callback to
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc, @Nullable FieldFilter ff) {
		// Keep backing up the inheritance hierarchy.
		Class<?> targetClass = clazz;
		do {
			Field[] fields = getDeclaredFields(targetClass);
			for (Field field : fields) {
				if (ff != null && !ff.matches(field)) {
					continue;
				}
				try {
					fc.doWith(field);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	/**
	 * 此变体从本地缓存中获取 {@link Class#getDeclaredFields()}，以避免 JVM 的 SecurityManager 检查和防
	 * 御性数组复制。
	 * @param clazz the class to introspect
	 * @return the cached array of fields
	 * @throws IllegalStateException if introspection fails
	 * @see Class#getDeclaredFields()
	 */
	private static Field[] getDeclaredFields(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Field[] result = declaredFieldsCache.get(clazz);
		if (result == null) {
			try {
				result = clazz.getDeclaredFields();
				declaredFieldsCache.put(clazz, (result.length == 0 ? EMPTY_FIELD_ARRAY : result));
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
						"] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return result;
	}

	/**
	 * 给定源对象和目标对象（二者必须为同一个类或目标为源类的子类），复制所有字段，包括继承的字段。设计
	 * 用于具有 public 无参构造函数的对象。
	 * @throws IllegalStateException if introspection fails
	 */
	public static void shallowCopyFieldState(final Object src, final Object dest) {
		Assert.notNull(src, "Source for field copy cannot be null");
		Assert.notNull(dest, "Destination for field copy cannot be null");
		if (!src.getClass().isAssignableFrom(dest.getClass())) {
			throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() +
					"] must be same or subclass as source class [" + src.getClass().getName() + "]");
		}
		doWithFields(src.getClass(), field -> {
			makeAccessible(field);
			Object srcValue = field.get(src);
			field.set(dest, srcValue);
		}, COPYABLE_FIELDS);
	}

	/**
	 * 判断给定字段是否为 "public static final" 常量。
	 * @param field the field to check
	 */
	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * 使给定的字段可访问，必要时显式将其设置为可访问。仅在确实需要时才调用 {@code setAccessible(true)}
	 * 方法，以避免与 JVM SecurityManager（若启用）产生不必要的冲突。
	 * @param field the field to make accessible
	 * @see java.lang.reflect.Field#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) ||
				!Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
				Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
			field.setAccessible(true);
		}
	}


	// Cache handling

	/**
	 * 清除内部的方法/字段缓存。
	 * @since 4.2.4
	 */
	public static void clearCache() {
		declaredMethodsCache.clear();
		declaredFieldsCache.clear();
	}


	/**
	 * 要对每个方法执行的操作。
	 */
	@FunctionalInterface
	public interface MethodCallback {

		/**
		 * 使用给定方法执行一个操作。
		 * @param method the method to operate on
		 */
		void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 可选的回调，用于过滤要由方法回调操作的方法。
	 */
	@FunctionalInterface
	public interface MethodFilter {

		/**
		 * 判断给定方法是否匹配。
		 * @param method the method to check
		 */
		boolean matches(Method method);

		/**
		 * 基于此过滤器 <em>和</em> 所提供的过滤器创建一个组合过滤器。
		 * <p>如果此过滤器不匹配，则不会应用下一个过滤器。
		 * @param next the next {@code MethodFilter}
		 * @return a composite {@code MethodFilter}
		 * @throws IllegalArgumentException if the MethodFilter argument is {@code null}
		 * @since 5.3.2
		 */
		default MethodFilter and(MethodFilter next) {
			Assert.notNull(next, "Next MethodFilter must not be null");
			return method -> matches(method) && next.matches(method);
		}
	}


	/**
	 * 在层次结构中的每个字段上调用的回调接口。
	 */
	@FunctionalInterface
	public interface FieldCallback {

		/**
		 * 使用给定字段执行一个操作。
		 * @param field the field to operate on
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 可选的回调，用于过滤要由字段回调操作的字段。
	 */
	@FunctionalInterface
	public interface FieldFilter {

		/**
		 * 判断给定字段是否匹配。
		 * @param field the field to check
		 */
		boolean matches(Field field);

		/**
		 * 基于此过滤器 <em>和</em> 所提供的过滤器创建一个组合过滤器。
		 * <p>如果此过滤器不匹配，则不会应用下一个过滤器。
		 * @param next the next {@code FieldFilter}
		 * @return a composite {@code FieldFilter}
		 * @throws IllegalArgumentException if the FieldFilter argument is {@code null}
		 * @since 5.3.2
		 */
		default FieldFilter and(FieldFilter next) {
			Assert.notNull(next, "Next FieldFilter must not be null");
			return field -> matches(field) && next.matches(field);
		}
	}

}
