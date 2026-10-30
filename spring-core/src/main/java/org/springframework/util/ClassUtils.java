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

package org.springframework.util;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * 杂项 {@code java.lang.Class} 工具方法。主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 1.1
 * @see TypeUtils
 * @see ReflectionUtils
 */
public abstract class ClassUtils {

	/** 数组类名的后缀：{@code "[]"}。 */
	public static final String ARRAY_SUFFIX = "[]";

	/** 内部数组类名的前缀：{@code "["}。 */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/** 内部非基本类型数组类名的前缀：{@code "[L"}。 */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/** 可复用的空 Class 数组常量。 */
	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

	/** 包分隔符字符：{@code '.'}。 */
	private static final char PACKAGE_SEPARATOR = '.';

	/** 路径分隔符字符：{@code '/'}。 */
	private static final char PATH_SEPARATOR = '/';

	/** 嵌套类分隔符字符：{@code '$'}。 */
	private static final char NESTED_CLASS_SEPARATOR = '$';

	/** CGLIB 类分隔符：{@code "$$"}。 */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/** ".class" 文件后缀。 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * Map，键为基本类型包装器类型，值为对应的基本类型，例如：Integer.class -> int.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

	/**
	 * Map，键为基本类型，值为对应的包装器类型，例如：int.class -> Integer.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

	/**
	 * Map，键为基本类型名称，值为对应的基本类型，例如："int" -> "int.class"。
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * Map，键为常见的 Java 语言类名，值为对应的 Class。主要用于远程调用的高效反序列化。
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * 在查找“primary”用户级接口时应被忽略的常见 Java 语言接口。
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;

	/**
	 * 用于声明类所实现接口上等价方法的缓存。
	 */
	private static final Map<Method, Method> interfaceMethodCache = new ConcurrentReferenceHashMap<>(256);


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);

		// Map entry iteration is less expensive to initialize than forEach with lambdas
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
	}


	/**
	 * 将给定的常见类注册到 ClassUtils 缓存中。
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * 返回要使用的默认 ClassLoader：如果可用，通常是线程上下文 ClassLoader；否则回退到加载 ClassUtils
	 * 类的 ClassLoader。
	 * <p>如果你打算在明确希望使用非 null 的 ClassLoader 引用的场景中使用线程上下文 ClassLoader，请调用
	 * 此方法：例如用于类路径资源加载（但不一定适用于 {@code Class.forName}，它也接受 {@code null} 的
	 * ClassLoader 引用）。
	 * @return 默认 ClassLoader（仅当系统 ClassLoader 也无法访问时才为 {@code null}）
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	@Nullable
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
				}
			}
		}
		return cl;
	}

	/**
	 * 如有必要，用环境的 Bean ClassLoader 覆盖线程上下文 ClassLoader，即当 Bean ClassLoader 已经与线程
	 * 上下文 ClassLoader 不等价时。
	 * @param classLoaderToUse 用于线程上下文的实际 ClassLoader
	 * @return 原始的线程上下文 ClassLoader，如果未被覆盖则为 {@code null}
	 */
	@Nullable
	public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	/**
	 * 对 {@code Class.forName()} 的替代实现，它还能为基本类型（如 "int"）和数组类名（如 "String[]"）返
	 * 回 Class 实例。此外，它还能解析 Java 源码风格的嵌套类名（如 "java.lang.Thread.State" 而不是
	 * "java.lang.Thread$State"）。
	 * @param name Class 的名称
	 * @param classLoader 要使用的类加载器（可以为 {@code null}，表示使用默认类加载器）
	 * @return 所提供名称对应的类实例
	 * @throws ClassNotFoundException 如果未找到该类
	 * @throws LinkageError 如果类文件无法加载
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "Name must not be null");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" style arrays
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" style arrays
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return Class.forName(name, false, clToUse);
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String nestedClassName =
						name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return Class.forName(nestedClassName, false, clToUse);
				}
				catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}

	/**
	 * 将给定的类名解析为 Class 实例。支持基本类型（如 "int"）和数组类名（如 "String[]"）。
	 * <p>这在效果上等同于参数相同的 {@code forName} 方法，唯一区别在于类加载失败时抛出的异常。
	 * @param className Class 的名称
	 * @param classLoader 要使用的类加载器（可以为 {@code null}，表示使用默认类加载器）
	 * @return 所提供名称对应的类实例
	 * @throws IllegalArgumentException 如果类名无法解析（即无法找到该类或无法加载其类文件）
	 * @throws IllegalStateException 如果对应类可解析，但该类的继承层次中存在可读性不匹配
	 * （通常是 Jigsaw 模块定义中缺少依赖声明，即此处要加载的类
	 * 所实现的超类或接口）
	 * @see #forName(String, ClassLoader)
	 */
	public static Class<?> resolveClassName(String className, @Nullable ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
	}

	/**
	 * 判断由给定名称标识的 {@link Class} 是否存在且可被加载。如果该类或其某个依赖不存在或无法加载，将
	 * 返回 {@code false}。
	 * @param className 要检查的类名
	 * @param classLoader 要使用的类加载器（可以为 {@code null}，表示使用默认类加载器）
	 * @return 指定类是否存在（包括其所有超类和接口）
	 * @throws IllegalStateException 如果对应类可解析，但该类的继承层次中存在可读性不匹配
	 * （通常是 Jigsaw 模块定义中缺少依赖声明，即此处要检查的类
	 * 所实现的超类或接口）
	 */
	public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		}
		catch (Throwable ex) {
			// Typically ClassNotFoundException or NoClassDefFoundError...
			return false;
		}
	}

	/**
	 * 检查给定类在给定 ClassLoader 中是否可见。
	 * @param clazz 要检查的类（通常是接口）
	 * @param classLoader 要检查的 ClassLoader
	 * （可以为 {@code null}，此时该方法始终返回 {@code true}）
	 */
	public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Visible if same Class can be loaded from given ClassLoader
		return isLoadable(clazz, classLoader);
	}

	/**
	 * 检查给定类在给定上下文中是否可以安全缓存，即它是否由给定 ClassLoader 或其父 ClassLoader 加载。
	 * @param clazz 要分析的类
	 * @param classLoader 可能缓存元数据的 ClassLoader
	 * （可以为 {@code null}，表示系统类加载器）
	 */
	public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Fallback for ClassLoaders without parent/child relationship:
		// safe if same Class can be loaded from given ClassLoader
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	/**
	 * 检查给定类在给定 ClassLoader 中是否可加载。
	 * @param clazz 要检查的类（通常是接口）
	 * @param classLoader 要检查的 ClassLoader
	 * @since 5.0.6
	 */
	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: different class with same name found
		}
		catch (ClassNotFoundException ex) {
			// No corresponding class found at all
			return false;
		}
	}

	/**
	 * 如适用，按 JVM 对基本类型的命名规则，将给定类名解析为基本类型类。
	 * <p>还支持 JVM 用于基本类型数组的内部类名。<i>不</i>支持基本类型数组的 "[]" 后缀写法；该写法仅由
	 * {@link #forName(String, ClassLoader)} 支持。
	 * @param name 可能是基本类型的类的名称
	 * @return 基本类型类，如果名称不表示基本类型或基本类型数组类则为 {@code null}
	 */
	@Nullable
	public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name != null && name.length() <= 7) {
			// Could be a primitive - likely.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * 检查给定类是否表示基本类型包装器，即 Boolean、Byte、Character、Short、Integer、Long、Float、
	 * Double 或 Void。
	 * @param clazz 要检查的类
	 * @return 给定类是否为基本类型包装类
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * 检查给定类是否表示基本类型（即 boolean、byte、char、short、int、long、float 或 double）、
	 * {@code void}，或这些类型的包装器（即 Boolean、Byte、Character、Short、Integer、Long、Float、
	 * Double 或 Void）。
	 * @param clazz 要检查的类
	 * @return 如果给定类表示基本类型、void 或包装类，则返回 {@code true}
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * 检查给定类是否表示基本类型数组，即 boolean、byte、char、short、int、long、float 或 double 的数组。
	 *
	 * @param clazz 要检查的类
	 * @return 给定类是否为基本类型数组类
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * 检查给定类是否表示基本类型包装器数组，即 Boolean、Byte、Character、Short、Integer、Long、Float
	 * 或 Double 的数组。
	 * @param clazz 要检查的类
	 * @return 给定类是否为基本类型包装类数组
	 */
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * 如果给定类是基本类型类，则解析它并改为返回对应的基本类型包装器类型。
	 * @param clazz 要检查的类
	 * @return 原始类，或原始基本类型对应的包装类
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * 检查在假定通过反射赋值的情况下，右侧类型是否可赋值给左侧类型。将基本类型包装器类视为可赋值给对应
	 * 的基本类型。
	 * @param lhsType 目标类型
	 * @param rhsType 应赋值给目标类型的值类型
	 * @return 如果目标类型可从值类型赋值
	 * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
	}

	/**
	 * 在假定通过反射赋值的情况下，判断给定类型是否可从给定值赋值。将基本类型包装器类视为可赋值给对应的
	 * 基本类型。
	 * @param type 目标类型
	 * @param value 应赋值给该类型的值
	 * @return 如果该类型可从值赋值
	 */
	public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * 将以 "/" 为基础的资源路径转换为以 "." 为基础的全限定类名。
	 * @param resourcePath 指向某个类的资源路径
	 * @return 对应的全限定类名
	 */
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * 将以 "." 为基础的全限定类名转换为以 "/" 为基础的资源路径。
	 * @param className 全限定类名
	 * @return 对应的资源路径，指向该类
	 */
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 返回适合与 {@code ClassLoader.getResource} 一起使用的路径（在返回值前加上斜杠（'/'）后，也适合与
	 * {@code Class.getResource} 一起使用）。其构建方式是：取指定类文件的包，将所有点（'.'）转换为斜杠
	 * （'/'），必要时添加末尾斜杠，然后在此基础上拼接指定的资源名。
	 * <br/>因此，该函数可用于构建适合加载与类文件处于同一包中的资源文件的路径，不过
	 * {@link org.springframework.core.io.ClassPathResource} 通常更为方便。
	 * @param clazz 将以其包作为基准的 Class
	 * @param resourceName 要追加的资源名称。开头的斜杠可选。
	 * @return 构建出的资源路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * 给定一个输入类对象，返回由该类包名构成路径名的字符串，即所有点（'.'）都被替换为斜杠（'/'）。既不
	 * 会添加前导斜杠，也不会添加末尾斜杠。结果可以与一个斜杠和资源名拼接，直接传给
	 * {@code ClassLoader.getResource()}。若要改为传给 {@code Class.getResource}，还需在返回值前加上前
	 * 导斜杠。
	 * @param clazz 输入的类。{@code null} 值或默认（空）包将导致返回空字符串。
	 * @return 表示包名的路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 构建一个由给定数组中各类/接口名称组成的 String。
	 * <p>大体上类似于 {@code AbstractCollection.toString()}，但会去掉每个类名前的 "class "/"interface
	 * " 前缀。
	 * @param classes Class 对象数组
	 * @return 形如 “[com.foo.Bar, com.foo.Baz]” 的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * 构建一个由给定集合中各类/接口名称组成的 String。
	 * <p>大体上类似于 {@code AbstractCollection.toString()}，但会去掉每个类名前的 "class "/"interface
	 * " 前缀。
	 * @param classes Class 对象的 Collection（可以为 {@code null}）
	 * @return 形如 “[com.foo.Bar, com.foo.Baz]” 的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(@Nullable Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
		for (Class<?> clazz : classes) {
			stringJoiner.add(clazz.getName());
		}
		return stringJoiner.toString();
	}

	/**
	 * 将给定的 {@code Collection} 复制到 {@code Class} 数组中。
	 * <p>该 {@code Collection} 只能包含 {@code Class} 元素。
	 * @param collection 要复制的 {@code Collection}
	 * @return 该 {@code Class} 数组
	 * @since 3.1
	 * @see StringUtils#toStringArray
	 */
	public static Class<?>[] toClassArray(@Nullable Collection<Class<?>> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
	}

	/**
	 * 以数组形式返回给定实例所实现的所有接口，包括由超类实现的接口。
	 * @param instance 要分析其接口的实例
	 * @return 给定实例所实现的所有接口，以数组形式返回
	 */
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * 以数组形式返回给定类所实现的所有接口，包括由超类实现的接口。
	 * <p>如果该类本身是接口，则将它作为唯一的接口返回。
	 * @param clazz 要分析其接口的类
	 * @return 给定对象所实现的所有接口，以数组形式返回
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * 以数组形式返回给定类所实现的所有接口，包括由超类实现的接口。
	 * <p>如果该类本身是接口，则将它作为唯一的接口返回。
	 * @param clazz 要分析其接口的类
	 * @param classLoader 接口需要对其可见的 ClassLoader
	 * （在允许所有已声明接口时可以为 {@code null}）
	 * @return 给定对象所实现的所有接口，以数组形式返回
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * 以 Set 形式返回给定实例所实现的所有接口，包括由超类实现的接口。
	 * @param instance 要分析其接口的实例
	 * @return 给定实例所实现的所有接口，以 Set 形式返回
	 */
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * 以 Set 形式返回给定类所实现的所有接口，包括由超类实现的接口。
	 * <p>如果该类本身是接口，则将它作为唯一的接口返回。
	 * @param clazz 要分析其接口的类
	 * @return 给定对象所实现的所有接口，以 Set 形式返回
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * 以 Set 形式返回给定类所实现的所有接口，包括由超类实现的接口。
	 * <p>如果该类本身是接口，则将它作为唯一的接口返回。
	 * @param clazz 要分析其接口的类
	 * @param classLoader 接口需要对其可见的 ClassLoader
	 * （在允许所有已声明接口时可以为 {@code null}）
	 * @return 给定对象所实现的所有接口，以 Set 形式返回
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (isVisible(ifc, classLoader)) {
					interfaces.add(ifc);
				}
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * 为给定接口创建组合接口 Class，在单个 Class 中实现给定的这些接口。
	 * <p>该实现会为给定接口构建一个 JDK 代理类。
	 * @param interfaces 要合并的接口
	 * @param classLoader 用于创建复合 Class 的 ClassLoader
	 * @return 合并后的接口（以 Class 形式）
	 * @throws IllegalArgumentException 如果指定的接口暴露了冲突的方法签名
	 * （或违反了类似的约束）
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interface array must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * 确定给定类的共同祖先（如果有）。
	 * @param clazz1 要内省的类
	 * @param clazz2 要内省的另一个类
	 * @return 公共祖先（即公共超类、一个接口继承另一个接口），如果未找到则为 {@code null}。
	 * 如果给定类中有任一为 {@code null}，则返回另一个类。
	 * @since 3.2.6
	 */
	@Nullable
	public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * 判断给定接口是否为常见的 Java 语言接口：{@link Serializable}、{@link Externalizable}、
	 * {@link Closeable}、{@link AutoCloseable}、{@link Cloneable}、{@link Comparable}——在查找“primary”
	 * 用户级接口时，这些接口都可以被忽略。共同特征：没有服务级操作，没有 Bean 属性方法，没有默认方法。
	 * @param ifc 要检查的接口
	 * @since 5.0.3
	 */
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * 判断所提供的类是否为<em>内部类</em>，即外围类的非静态成员。
	 * @return 如果所提供的类是内部类，则返回 {@code true}
	 * @since 5.0.5
	 * @see Class#isMemberClass()
	 */
	public static boolean isInnerClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
	}

	/**
	 * 判断所提供的 {@link Class} 是否为 JVM 为 lambda 表达式或方法引用生成的实现类。
	 * <p>此方法基于在现代主流 JVM 上有效的检查，尽力做出判断。
	 * @param clazz 要检查的类
	 * @return 如果该类是 lambda 实现类，则返回 {@code true}
	 * @since 5.3.19
	 */
	public static boolean isLambdaClass(Class<?> clazz) {
		return (clazz.isSynthetic() && (clazz.getSuperclass() == Object.class) &&
				(clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda"));
	}

	/**
	 * 检查给定对象是否为 CGLIB 代理。
	 * @param object 要检查的对象
	 * @see #isCglibProxyClass(Class)
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	@Deprecated
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * 检查指定类是否为 CGLIB 生成的类。
	 * @param clazz 要检查的类
	 * @see #isCglibProxyClassName(String)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	@Deprecated
	public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * 检查指定类名是否为 CGLIB 生成的类。
	 * @param className 要检查的类名
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	@Deprecated
	public static boolean isCglibProxyClassName(@Nullable String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 返回给定实例的用户定义类：通常就是该实例的类，但如果是 CGLIB 生成的子类，则返回原始类。
	 * @param instance 要检查的实例
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * 返回给定类的用户定义类：通常就是给定的类，但如果是 CGLIB 生成的子类，则返回原始类。
	 * @param clazz 要检查的类
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * 返回给定对象类型的描述性名称：通常就是类名，但数组为组件类型类名 + "[]"，而 JDK 代理还会追加所实
	 * 现接口的列表。
	 * @param value 要内省的值
	 * @return 类的限定名
	 */
	@Nullable
	public static String getDescriptiveType(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			String prefix = clazz.getName() + " implementing ";
			StringJoiner result = new StringJoiner(",", prefix, "");
			for (Class<?> ifc : clazz.getInterfaces()) {
				result.add(ifc.getName());
			}
			return result.toString();
		}
		else {
			return clazz.getTypeName();
		}
	}

	/**
	 * 检查给定类是否与用户指定的类型名匹配。
	 * @param clazz 要检查的类
	 * @param typeName 要匹配的类型名称
	 */
	public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
	}

	/**
	 * 获取不含限定包名的类名。
	 * @param className 要获取短名称的 className
	 * @return 类名，不含包名
	 * @throws IllegalArgumentException 如果 className 为空
	 */
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * 获取不含限定包名的类名。
	 * @param clazz 要获取短名称的类
	 * @return 类名，不含包名
	 */
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * 以首字母小写的 JavaBeans 属性格式返回 Java 类的短字符串名称。如果是嵌套类，会去掉外层类名。
	 * @param clazz 类
	 * @return 以标准 JavaBeans 属性格式表示的短名称
	 * @see java.beans.Introspector#decapitalize(String)
	 */
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	/**
	 * 确定类文件的名称，相对于其所在包：例如 "String.class"。
	 * @param clazz 类
	 * @return “.class” 文件的文件名
	 */
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * 确定给定类的包名，例如对于 {@code java.lang.String} 类为 "java.lang"。
	 * @param clazz 类
	 * @return 包名，如果类定义在默认包中则为空字符串
	 */
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * 确定给定全限定类名的包名，例如对于 {@code java.lang.String} 类名为 "java.lang"。
	 * @param fqClassName 全限定类名
	 * @return 包名，如果类定义在默认包中则为空字符串
	 */
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * 返回给定类的限定名：通常就是类名，但数组为组件类型类名 + "[]"。
	 * @param clazz 类
	 * @return 类的限定名
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * 返回给定方法的限定名，由全限定的接口/类名 + "." + 方法名组成。
	 * @param method 方法
	 * @return 方法的限定名
	 */
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * 返回给定方法的限定名，由全限定的接口/类名 + "." + 方法名组成。
	 * @param method 方法
	 * @param clazz 调用该方法时所在的类
	 * （可以为 {@code null}，表示该方法的声明类）
	 * @return 方法的限定名
	 * @since 4.3.4
	 */
	public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * 判断给定类是否具有给定签名的 public 构造方法。
	 * <p>本质上是将 {@code NoSuchMethodException} 转换为 "false"。
	 * @param clazz 要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 该类是否有对应的构造方法
	 * @see Class#getConstructor
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有给定签名的 public 构造方法，若有则返回它（否则返回 {@code null}）。
	 * <p>本质上是将 {@code NoSuchMethodException} 转换为 {@code null}。
	 * @param clazz 要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 构造方法，如果未找到则为 {@code null}
	 * @see Class#getConstructor
	 */
	@Nullable
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 判断给定类是否具有给定签名的 public 方法。
	 * @param clazz 要分析的类
	 * @param method 要查找的方法
	 * @return 该类是否有对应的方法
	 * @since 5.2.3
	 */
	public static boolean hasMethod(Class<?> clazz, Method method) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(method, "Method must not be null");
		if (clazz == method.getDeclaringClass()) {
			return true;
		}
		String methodName = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		return getMethodOrNull(clazz, methodName, paramTypes) != null;
	}

	/**
	 * 判断给定类是否具有给定签名的 public 方法。
	 * <p>本质上是将 {@code NoSuchMethodException} 转换为 "false"。
	 * @param clazz 要分析的类
	 * @param methodName 方法的名称
	 * @param paramTypes 方法的参数类型
	 * @return 该类是否有对应的方法
	 * @see Class#getMethod
	 */
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有给定签名的 public 方法，若有则返回它（否则抛出 {@code IllegalStateException}）。
	 *
	 * <p>在指定了签名的情况下，仅当存在唯一候选（即具有指定名称的单个 public 方法）时才返回该方法。
	 * <p>本质上是将 {@code NoSuchMethodException} 转换为 {@code IllegalStateException}。
	 * @param clazz 要分析的类
	 * @param methodName 方法的名称
	 * @param paramTypes 方法的参数类型
	 * （可以为 {@code null}，表示任意签名）
	 * @return 该方法（永不为 {@code null}）
	 * @throws IllegalStateException 如果未找到该方法
	 * @see Class#getMethod
	 */
	public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * 判断给定类是否具有给定签名的 public 方法，若有则返回它（否则返回 {@code null}）。
	 * <p>在指定了签名的情况下，仅当存在唯一候选（即具有指定名称的单个 public 方法）时才返回该方法。
	 * <p>本质上是将 {@code NoSuchMethodException} 转换为 {@code null}。
	 * @param clazz 要分析的类
	 * @param methodName 方法的名称
	 * @param paramTypes 方法的参数类型
	 * （可以为 {@code null}，表示任意签名）
	 * @return 该方法，如果未找到则为 {@code null}
	 * @see Class#getMethod
	 */
	@Nullable
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			return getMethodOrNull(clazz, methodName, paramTypes);
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * 返回给定类和/或其超类中具有给定名称（参数类型不限）的方法数量。包括非 public 方法。
	 * @param clazz 要检查的类
	 * @param methodName 方法的名称
	 * @return 具有给定名称的方法数量
	 */
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * 给定类或其某个超类是否至少有一个或多个具有所提供名称（参数类型不限）的方法？包括非 public 方法。
	 * @param clazz 要检查的类
	 * @param methodName 方法的名称
	 * @return 是否至少存在一个具有给定名称的方法
	 */
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * 给定一个方法（可能来自接口）以及当前反射调用中使用的目标类，如果存在对应的目标方法，则找出它。例
	 * 如，该方法可能是 {@code IFoo.bar()}，目标类可能是 {@code DefaultFoo}。在这种情况下，方法可能是
	 * {@code DefaultFoo.bar()}。这样可以找到该方法上的属性。
	 * <p><b>NOTE:</b> 与 {@link org.springframework.aop.support.AopUtils#getMostSpecificMethod} 不同，
	 * 此方法<i>不会</i>自动解析桥接方法。如果需要解析桥接方法（例如要从原始方法定义中获取元数据），请
	 * 调用 {@link org.springframework.core.BridgeMethodResolver#findBridgedMethod}。
	 * <p><b>NOTE:</b> 自 Spring 3.1.1 起，如果 Java 安全设置不允许反射访问（例如调用
	 * {@code Class#getDeclaredMethods} 等），此实现将回退为返回最初提供的方法。
	 * @param method 要调用的方法，可能来自某个接口
	 * @param targetClass 当前调用的目标类
	 * （可以为 {@code null}，甚至可能未实现该方法）
	 * @return 具体的目标方法，如果 {@code targetClass} 未实现该方法，
	 * 则为原始方法
	 * @see #getInterfaceMethodIfPossible(Method, Class)
	 */
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				// Security settings are disallowing reflective access; fall back to 'method' below.
			}
		}
		return method;
	}

	/**
	 * 如果可能，为给定方法句柄确定对应的接口方法。
	 * <p>这对于在 Jigsaw 上获得可进行反射调用且不会产生非法访问警告的 public 导出类型尤为有用。
	 * @param method 要调用的方法，可能来自某个实现类
	 * @return 对应的接口方法，如果未找到则为原始方法
	 * @since 5.1
	 * @deprecated in favor of {@link #getInterfaceMethodIfPossible(Method, Class)}
	 */
	@Deprecated
	public static Method getInterfaceMethodIfPossible(Method method) {
		return getInterfaceMethodIfPossible(method, null);
	}

	/**
	 * 如果可能，为给定方法句柄确定对应的接口方法。
	 * <p>这对于在 Jigsaw 上获得可进行反射调用且不会产生非法访问警告的 public 导出类型尤为有用。
	 * @param method 要调用的方法，可能来自某个实现类
	 * @param targetClass 要检查其已声明接口的目标类
	 * @return 对应的接口方法，如果未找到则为原始方法
	 * @since 5.3.16
	 * @see #getMostSpecificMethod
	 */
	public static Method getInterfaceMethodIfPossible(Method method, @Nullable Class<?> targetClass) {
		if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return method;
		}
		// Try cached version of method in its declaring class
		Method result = interfaceMethodCache.computeIfAbsent(method,
				key -> findInterfaceMethodIfPossible(key, key.getDeclaringClass(), Object.class));
		if (result == method && targetClass != null) {
			// No interface method found yet -> try given target class (possibly a subclass of the
			// declaring class, late-binding a base class method to a subclass-declared interface:
			// see e.g. HashMap.HashIterator.hasNext)
			result = findInterfaceMethodIfPossible(method, targetClass, method.getDeclaringClass());
		}
		return result;
	}

	private static Method findInterfaceMethodIfPossible(Method method, Class<?> startClass, Class<?> endClass) {
		Class<?> current = startClass;
		while (current != null && current != endClass) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				try {
					return ifc.getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException ex) {
					// ignore
				}
			}
			current = current.getSuperclass();
		}
		return method;
	}

	/**
	 * 判断给定方法是由用户声明的，还是至少指向用户声明的方法。
	 * <p>既检查 {@link Method#isSynthetic()}（用于实现方法），也检查 {@code GroovyObject} 接口（用于接
	 * 口方法；在实现类上，{@code GroovyObject} 方法的实现无论如何都会被标记为 synthetic）。注意，桥接
	 * 方法（{@link Method#isBridge()}）尽管是 synthetic，仍被视为用户级方法，因为它们最终指向用户声明
	 * 的泛型方法。
	 * @param method 要检查的方法
	 * @return 如果该方法可被视为用户声明的，则返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * 判断给定方法在给定目标类中是否可覆写。
	 * @param method 要检查的方法
	 * @param targetClass 要对照检查的目标类
	 */
	private static boolean isOverridable(Method method, @Nullable Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return (targetClass == null ||
				getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
	}

	/**
	 * 返回类的 public static 方法。
	 * @param clazz 定义该方法的类
	 * @param methodName 静态方法的名称
	 * @param args 该方法的参数类型
	 * @return 该静态方法，如果未找到静态方法则为 {@code null}
	 * @throws IllegalArgumentException 如果方法名为空白或 clazz 为 null
	 */
	@Nullable
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}


	@Nullable
	private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
		Set<Method> candidates = new HashSet<>(1);
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (methodName.equals(method.getName())) {
				candidates.add(method);
			}
		}
		return candidates;
	}

}
