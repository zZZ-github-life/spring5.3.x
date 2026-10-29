/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans;

import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 JavaBeans 的静态便捷方法：实例化 Bean、检查 Bean 属性类型、复制 Bean 属性等。
 *
 * <p>主要供框架内部使用，但在某种程度上对应用程序类也有用。如需更全面的 Bean 工具，可考虑
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>、
 * <a href="https://github.com/ExpediaGroup/bull">BULL - Bean Utils Light Library</a> 或类似的第三
 * 方框架。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

	private static final ParameterNameDiscoverer parameterNameDiscoverer =
			new DefaultParameterNameDiscoverer();

	private static final Set<Class<?>> unknownEditorTypes =
			Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

	private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

	static {
		Map<Class<?>, Object> values = new HashMap<>();
		values.put(boolean.class, false);
		values.put(byte.class, (byte) 0);
		values.put(short.class, (short) 0);
		values.put(int.class, 0);
		values.put(long.class, 0L);
		values.put(float.class, 0F);
		values.put(double.class, 0D);
		values.put(char.class, '\0');
		DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
	}


	/**
	 * 使用无参构造方法实例化类的便捷方法。
	 * @param clazz class to instantiate
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Class#newInstance()
	 * @deprecated as of Spring 5.0, following the deprecation of
	 * {@link Class#newInstance()} in JDK 9
	 */
	@Deprecated
	public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
		}
	}

	/**
	 * 使用类的“主”构造方法（对于 Kotlin 类，可能声明了默认参数）或其默认构造方法（对于常规 Java 类，期
	 * 望标准的无参设置）来实例化类。<p>请注意，如果给定的构造方法不可访问（即非 public），此方法会尝试
	 * 将其设置为可访问。
	 * @param clazz the class to instantiate
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated.
	 * The cause may notably indicate a {@link NoSuchMethodException} if no
	 * primary/default constructor was found, a {@link NoClassDefFoundError}
	 * or other {@link LinkageError} in case of an unresolvable class definition
	 * (e.g. due to a missing dependency at runtime), or an exception thrown
	 * from the constructor invocation itself.
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return instantiateClass(clazz.getDeclaredConstructor());
		}
		catch (NoSuchMethodException ex) {
			Constructor<T> ctor = findPrimaryConstructor(clazz);
			if (ctor != null) {
				return instantiateClass(ctor);
			}
			throw new BeanInstantiationException(clazz, "No default constructor found", ex);
		}
		catch (LinkageError err) {
			throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
		}
	}

	/**
	 * 使用无参构造方法实例化类，并将新实例作为指定的可赋值类型返回。<p>在待实例化类的类型（clazz）不可
	 * 用、但所需类型（assignableTo）已知的情况下很有用。<p>请注意，如果给定的构造方法不可访问（即非
	 * public），此方法会尝试将其设置为可访问。
	 * @param clazz class to instantiate
	 * @param assignableTo type that clazz must be assignableTo
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
		Assert.isAssignable(assignableTo, clazz);
		return (T) instantiateClass(clazz);
	}

	/**
	 * 使用给定构造方法实例化类的便捷方法。<p>请注意，如果给定的构造方法不可访问（即非 public），此方法
	 * 会尝试将其设置为可访问，并支持带可选参数和默认值的 Kotlin 类。
	 * @param ctor the constructor to instantiate
	 * @param args the constructor arguments to apply (use {@code null} for an unspecified
	 * parameter, Kotlin optional parameters and Java primitive types are supported)
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
		Assert.notNull(ctor, "Constructor must not be null");
		try {
			ReflectionUtils.makeAccessible(ctor);
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
				return KotlinDelegate.instantiateClass(ctor, args);
			}
			else {
				Class<?>[] parameterTypes = ctor.getParameterTypes();
				Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
				Object[] argsWithDefaultValues = new Object[args.length];
				for (int i = 0 ; i < args.length; i++) {
					if (args[i] == null) {
						Class<?> parameterType = parameterTypes[i];
						argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
					}
					else {
						argsWithDefaultValues[i] = args[i];
					}
				}
				return ctor.newInstance(argsWithDefaultValues);
			}
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
		}
		catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
		}
		catch (InvocationTargetException ex) {
			throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
		}
	}

	/**
	 * 为提供的类返回一个可解析的构造方法：要么是带参数的主构造方法或单个 public 构造方法，要么是带参数
	 * 的单个非 public 构造方法，要么就是默认构造方法。调用方必须准备好为返回构造方法的参数（如果有）解
	 * 析实参。
	 * @param clazz the class to check
	 * @throws IllegalStateException in case of no unique constructor found at all
	 * @since 5.3
	 * @see #findPrimaryConstructor
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> getResolvableConstructor(Class<T> clazz) {
		Constructor<T> ctor = findPrimaryConstructor(clazz);
		if (ctor != null) {
			return ctor;
		}

		Constructor<?>[] ctors = clazz.getConstructors();
		if (ctors.length == 1) {
			// A single public constructor
			return (Constructor<T>) ctors[0];
		}
		else if (ctors.length == 0) {
			// No public constructors -> check non-public
			ctors = clazz.getDeclaredConstructors();
			if (ctors.length == 1) {
				// A single non-public constructor, e.g. from a non-public record type
				return (Constructor<T>) ctors[0];
			}
		}

		// Several constructors -> let's try to take the default constructor
		try {
			return clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException ex) {
			// Giving up...
		}

		// No unique constructor at all
		throw new IllegalStateException("No primary or single unique constructor found for " + clazz);
	}

	/**
	 * 返回所提供类的主构造方法。对于 Kotlin 类，返回与 Kotlin 主构造方法相对应的 Java 构造方法（按
	 * Kotlin 规范定义）。否则，尤其是对于非 Kotlin 类，仅返回 {@code null}。
	 * @param clazz the class to check
	 * @since 5.0
	 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
	 */
	@Nullable
	public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
			return KotlinDelegate.findPrimaryConstructor(clazz);
		}
		return null;
	}

	/**
	 * 查找具有给定方法名和给定参数类型、声明在给定类或其某个超类上的方法。优先选择 public 方法，但也会
	 * 返回 protected、包级访问或 private 方法。<p>先检查 {@code Class.getMethod}，再回退到
	 * {@code findDeclaredMethod}。这样即使在 Java 安全设置受限的环境中也能顺利找到 public 方法。
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getMethod
	 * @see #findDeclaredMethod
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}

	/**
	 * 查找具有给定方法名和给定参数类型、声明在给定类或其某个超类上的方法。将返回 public、protected、包
	 * 级访问或 private 方法。<p>检查 {@code Class.getDeclaredMethod}，并向上级联到所有超类。
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getDeclaredMethod
	 */
	@Nullable
	public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}

	/**
	 * 查找具有给定方法名和最少参数（最好是无参数）、声明在给定类或其某个超类上的方法。优先选择 public
	 * 方法，但也会返回 protected、包级访问或 private 方法。<p>先检查 {@code Class.getMethods}，再回退
	 * 到 {@code findDeclaredMethodWithMinimalParameters}。这样即使在 Java 安全设置受限的环境中也能顺利
	 * 找到 public 方法。
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getMethods
	 * @see #findDeclaredMethodWithMinimalParameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
		if (targetMethod == null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
		return targetMethod;
	}

	/**
	 * 查找具有给定方法名和最少参数（最好是无参数）、声明在给定类或其某个超类上的方法。将返回 public、
	 * protected、包级访问或 private 方法。<p>检查 {@code Class.getDeclaredMethods}，并向上级联到所有超
	 * 类。
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getDeclaredMethods
	 */
	@Nullable
	public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
		if (targetMethod == null && clazz.getSuperclass() != null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
		}
		return targetMethod;
	}

	/**
	 * 在给定的方法列表中查找具有给定方法名和最少参数（最好是无参数）的方法。
	 * @param methods the methods to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = null;
		int numMethodsFoundWithCurrentMinimumArgs = 0;
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				int numParams = method.getParameterCount();
				if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
					targetMethod = method;
					numMethodsFoundWithCurrentMinimumArgs = 1;
				}
				else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
					if (targetMethod.isBridge()) {
						// Prefer regular method over bridge...
						targetMethod = method;
					}
					else {
						// Additional candidate with same length
						numMethodsFoundWithCurrentMinimumArgs++;
					}
				}
			}
		}
		if (numMethodsFoundWithCurrentMinimumArgs > 1) {
			throw new IllegalArgumentException("Cannot resolve method '" + methodName +
					"' to a unique method. Attempted to resolve to overloaded method with " +
					"the least number of parameters but there were " +
					numMethodsFoundWithCurrentMinimumArgs + " candidates.");
		}
		return targetMethod;
	}

	/**
	 * 解析形式为 {@code methodName[([arg_list])]} 的方法签名，其中 {@code arg_list} 是可选的、以逗号分
	 * 隔的完全限定类型名列表，并尝试根据所提供的 {@code Class} 解析该签名。<p>不提供参数列表
	 * （{@code methodName}）时，将返回名称匹配且参数数量最少的方法。提供参数类型列表时，只返回名称和参数
	 * 类型均匹配的方法。<p>请注意，{@code methodName} 和 {@code methodName()} 的解析方式<strong>并不
	 * </strong>相同。签名 {@code methodName} 表示名为 {@code methodName} 且参数数量最少的方法，而
	 * {@code methodName()} 表示名为 {@code methodName} 且参数恰好为 0 个的方法。<p>如果找不到方法，则
	 * 返回 {@code null}。
	 * @param signature the method signature as String representation
	 * @param clazz the class to resolve the method signature against
	 * @return the resolved Method
	 * @see #findMethod
	 * @see #findMethodWithMinimalParameters
	 */
	@Nullable
	public static Method resolveSignature(String signature, Class<?> clazz) {
		Assert.hasText(signature, "'signature' must not be empty");
		Assert.notNull(clazz, "Class must not be null");
		int startParen = signature.indexOf('(');
		int endParen = signature.indexOf(')');
		if (startParen > -1 && endParen == -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected closing ')' for args list");
		}
		else if (startParen == -1 && endParen > -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected opening '(' for args list");
		}
		else if (startParen == -1) {
			return findMethodWithMinimalParameters(clazz, signature);
		}
		else {
			String methodName = signature.substring(0, startParen);
			String[] parameterTypeNames =
					StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				String parameterTypeName = parameterTypeNames[i].trim();
				try {
					parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
				}
				catch (Throwable ex) {
					throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
							parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
				}
			}
			return findMethod(clazz, methodName, parameterTypes);
		}
	}


	/**
	 * 获取给定类的 JavaBeans {@code PropertyDescriptor}。
	 * @param clazz the Class to retrieve the PropertyDescriptors for
	 * @return an array of {@code PropertyDescriptors} for the given class
	 * @throws BeansException if PropertyDescriptor look fails
	 */
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptors();
	}

	/**
	 * 获取给定属性的 JavaBeans {@code PropertyDescriptors}。
	 * @param clazz the Class to retrieve the PropertyDescriptor for
	 * @param propertyName the name of the property
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptor(propertyName);
	}

	/**
	 * 查找给定方法的 JavaBeans {@code PropertyDescriptor}，该方法可以是该 Bean 属性的读方法或写方法。
	 * @param method the method to find a corresponding PropertyDescriptor for,
	 * introspecting its declaring class
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		return findPropertyForMethod(method, method.getDeclaringClass());
	}

	/**
	 * 查找给定方法的 JavaBeans {@code PropertyDescriptor}，该方法可以是该 Bean 属性的读方法或写方法。
	 * @param method the method to find a corresponding PropertyDescriptor for
	 * @param clazz the (most specific) class to introspect for descriptors
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 * @since 3.2.13
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
		Assert.notNull(method, "Method must not be null");
		PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
		for (PropertyDescriptor pd : pds) {
			if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * 按 'Editor' 后缀约定查找 JavaBeans PropertyEditor（例如 "mypackage.MyDomainClass" &rarr;
	 * "mypackage.MyDomainClassEditor"）。<p>与 {@link java.beans.PropertyEditorManager} 实现的标准
	 * JavaBeans 约定兼容，但与该管理器为基本类型注册的默认编辑器相隔离。
	 * @param targetType the type to find an editor for
	 * @return the corresponding editor, or {@code null} if none found
	 */
	@Nullable
	public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
		if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
			return null;
		}

		ClassLoader cl = targetType.getClassLoader();
		if (cl == null) {
			try {
				cl = ClassLoader.getSystemClassLoader();
				if (cl == null) {
					return null;
				}
			}
			catch (Throwable ex) {
				// e.g. AccessControlException on Google App Engine
				return null;
			}
		}

		String targetTypeName = targetType.getName();
		String editorName = targetTypeName + "Editor";
		try {
			Class<?> editorClass = cl.loadClass(editorName);
			if (editorClass != null) {
				if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
					unknownEditorTypes.add(targetType);
					return null;
				}
				return (PropertyEditor) instantiateClass(editorClass);
			}
			// Misbehaving ClassLoader returned null instead of ClassNotFoundException
			// - fall back to unknown editor type registration below
		}
		catch (ClassNotFoundException ex) {
			// Ignore - fall back to unknown editor type registration below
		}
		unknownEditorTypes.add(targetType);
		return null;
	}

	/**
	 * 如果可能，根据给定的类/接口确定给定属性的 Bean 属性类型。
	 * @param propertyName the name of the bean property
	 * @param beanClasses the classes to check against
	 * @return the property type, or {@code Object.class} as fallback
	 */
	public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
		if (beanClasses != null) {
			for (Class<?> beanClass : beanClasses) {
				PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
				if (pd != null) {
					return pd.getPropertyType();
				}
			}
		}
		return Object.class;
	}

	/**
	 * 为指定属性的写方法获取一个新的 MethodParameter 对象。
	 * @param pd the PropertyDescriptor for the property
	 * @return a corresponding MethodParameter object
	 */
	public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor) {
			return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
		}
		else {
			Method writeMethod = pd.getWriteMethod();
			Assert.state(writeMethod != null, "No write method available");
			return new MethodParameter(writeMethod, 0);
		}
	}

	/**
	 * 确定给定构造方法所需的参数名，同时考虑 JavaBeans 的 {@link ConstructorProperties} 注解以及
	 * Spring 的 {@link DefaultParameterNameDiscoverer}。
	 * @param ctor the constructor to find parameter names for
	 * @return the parameter names (matching the constructor's parameter count)
	 * @throws IllegalStateException if the parameter names are not resolvable
	 * @since 5.3
	 * @see ConstructorProperties
	 * @see DefaultParameterNameDiscoverer
	 */
	public static String[] getParameterNames(Constructor<?> ctor) {
		ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
		String[] paramNames = (cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor));
		Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
		Assert.state(paramNames.length == ctor.getParameterCount(),
				() -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
		return paramNames;
	}

	/**
	 * 检查给定类型是否表示“简单”属性：简单值类型或简单值类型的数组。<p>有关<em>简单值类型</em>的定义，
	 * 请参见 {@link #isSimpleValueType(Class)}。<p>用于确定要针对“简单”依赖检查进行校验的属性。
	 * @param type the type to check
	 * @return whether the given type represents a "simple" property
	 * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
	 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
	 * @see #isSimpleValueType(Class)
	 */
	public static boolean isSimpleProperty(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
	}

	/**
	 * 检查给定类型是否表示“简单”值类型：基本类型或基本类型包装类、枚举、String 或其他 CharSequence、
	 * Number、Date、Temporal、URI、URL、Locale 或 Class。<p>{@code Void} 和 {@code void} 不被视为简单
	 * 值类型。
	 * @param type the type to check
	 * @return whether the given type represents a "simple" value type
	 * @see #isSimpleProperty(Class)
	 */
	public static boolean isSimpleValueType(Class<?> type) {
		return (Void.class != type && void.class != type &&
				(ClassUtils.isPrimitiveOrWrapper(type) ||
				Enum.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type) ||
				Date.class.isAssignableFrom(type) ||
				Temporal.class.isAssignableFrom(type) ||
				URI.class == type ||
				URL.class == type ||
				Locale.class == type ||
				Class.class == type));
	}


	/**
	 * 将给定源 Bean 的属性值复制到目标 Bean 中。<p>注意：源类和目标类不必相同，也不必相互派生，只要属
	 * 性匹配即可。源 Bean 公开而目标 Bean 不公开的任何 Bean 属性都将被静默忽略。<p>这只是一个便捷方法。
	 * 对于更复杂的传输需求，可考虑使用完整的 {@link BeanWrapper}。<p>从 Spring Framework 5.3 开始，
	 * 此方法在匹配源对象和目标对象中的属性时会遵循泛型类型信息。<p>下表提供了一组非穷尽的示例，说明可
	 * 以复制的源和目标属性类型，以及不能复制的源和目标属性类型。
	 * <table border="1">
	 * <tr><th>源属性类型</th><th>目标属性类型</th><th>是否支持复制</th></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Integer}</td><td>是</td></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Number}</td><td>是</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Integer>}</td><td>是</td></tr>
	 * <tr><td>{@code List<?>}</td><td>{@code List<?>}</td><td>是</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<?>}</td><td>是</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<? extends Number>}</td><td>是</td></tr>
	 * <tr><td>{@code String}</td><td>{@code Integer}</td><td>否</td></tr>
	 * <tr><td>{@code Number}</td><td>{@code Integer}</td><td>否</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Long>}</td><td>否</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Number>}</td><td>否</td></tr>
	 * </table>
	 * @param source the source bean
	 * @param target the target bean
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		copyProperties(source, target, null, (String[]) null);
	}

	/**
	 * 将给定源 Bean 的属性值复制到给定目标 Bean 中，仅设置给定“可编辑”类（或接口）中定义的属性。<p>注
	 * 意：源类和目标类不必相同，也不必相互派生，只要属性匹配即可。源 Bean 公开而目标 Bean 不公开的任何
	 * Bean 属性都将被静默忽略。<p>这只是一个便捷方法。对于更复杂的传输需求，可考虑使用完整的
	 * {@link BeanWrapper}。<p>从 Spring Framework 5.3 开始，此方法在匹配源对象和目标对象中的属性时会遵
	 * 循泛型类型信息。有关详细信息，请参见 {@link #copyProperties(Object, Object)} 的文档。
	 * @param source the source bean
	 * @param target the target bean
	 * @param editable the class (or interface) to restrict property setting to
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		copyProperties(source, target, editable, (String[]) null);
	}

	/**
	 * 将给定源 Bean 的属性值复制到给定目标 Bean 中，忽略给定的 "ignoreProperties"。<p>注意：源类和目标
	 * 类不必相同，也不必相互派生，只要属性匹配即可。源 Bean 公开而目标 Bean 不公开的任何 Bean 属性都将
	 * 被静默忽略。<p>这只是一个便捷方法。对于更复杂的传输需求，可考虑使用完整的 {@link BeanWrapper}。
	 * <p>从 Spring Framework 5.3 开始，此方法在匹配源对象和目标对象中的属性时会遵循泛型类型信息。有关
	 * 详细信息，请参见 {@link #copyProperties(Object, Object)} 的文档。
	 * @param source the source bean
	 * @param target the target bean
	 * @param ignoreProperties array of property names to ignore
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		copyProperties(source, target, null, ignoreProperties);
	}

	/**
	 * 将给定源 Bean 的属性值复制到给定目标 Bean 中。<p>注意：源类和目标类不必相同，也不必相互派生，只
	 * 要属性匹配即可。源 Bean 公开而目标 Bean 不公开的任何 Bean 属性都将被静默忽略。<p>从 Spring
	 * Framework 5.3 开始，此方法在匹配源对象和目标对象中的属性时会遵循泛型类型信息。有关详细信息，请参
	 * 见 {@link #copyProperties(Object, Object)} 的文档。
	 * @param source the source bean
	 * @param target the target bean
	 * @param editable the class (or interface) to restrict property setting to
	 * @param ignoreProperties array of property names to ignore
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	private static void copyProperties(Object source, Object target, @Nullable Class<?> editable,
			@Nullable String... ignoreProperties) throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		Class<?> actualEditable = target.getClass();
		if (editable != null) {
			if (!editable.isInstance(target)) {
				throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
						"] not assignable to Editable class [" + editable.getName() + "]");
			}
			actualEditable = editable;
		}
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

		for (PropertyDescriptor targetPd : targetPds) {
			Method writeMethod = targetPd.getWriteMethod();
			if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
				PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
				if (sourcePd != null) {
					Method readMethod = sourcePd.getReadMethod();
					if (readMethod != null) {
						ResolvableType sourceResolvableType = ResolvableType.forMethodReturnType(readMethod);
						ResolvableType targetResolvableType = ResolvableType.forMethodParameter(writeMethod, 0);

						// Ignore generic types in assignable check if either ResolvableType has unresolvable generics.
						boolean isAssignable =
								(sourceResolvableType.hasUnresolvableGenerics() || targetResolvableType.hasUnresolvableGenerics() ?
										ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType()) :
										targetResolvableType.isAssignableFrom(sourceResolvableType));

						if (isAssignable) {
							try {
								if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
									readMethod.setAccessible(true);
								}
								Object value = readMethod.invoke(source);
								if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
									writeMethod.setAccessible(true);
								}
								writeMethod.invoke(target, value);
							}
							catch (Throwable ex) {
								throw new FatalBeanException(
										"Could not copy property '" + targetPd.getName() + "' from source to target", ex);
							}
						}
					}
				}
			}
		}
	}


	/**
	 * 内部类，用于避免运行时对 Kotlin 的硬依赖。
	 */
	private static class KotlinDelegate {

		/**
		 * 获取与 Kotlin 主构造方法相对应的 Java 构造方法（如果有）。
		 * @param clazz the {@link Class} of the Kotlin class
		 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">
		 * https://kotlinlang.org/docs/reference/classes.html#constructors</a>
		 */
		@Nullable
		public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
			try {
				KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
				if (primaryCtor == null) {
					return null;
				}
				Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
				if (constructor == null) {
					throw new IllegalStateException(
							"Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
				}
				return constructor;
			}
			catch (UnsupportedOperationException ex) {
				return null;
			}
		}

		/**
		 * 使用提供的构造方法实例化 Kotlin 类。
		 * @param ctor the constructor of the Kotlin class to instantiate
		 * @param args the constructor arguments to apply
		 * (use {@code null} for unspecified parameter if needed)
		 */
		public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
				throws IllegalAccessException, InvocationTargetException, InstantiationException {

			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
			if (kotlinConstructor == null) {
				return ctor.newInstance(args);
			}

			if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))) {
				KCallablesJvm.setAccessible(kotlinConstructor, true);
			}

			List<KParameter> parameters = kotlinConstructor.getParameters();
			Map<KParameter, Object> argParameters = CollectionUtils.newHashMap(parameters.size());
			Assert.isTrue(args.length <= parameters.size(),
					"Number of provided arguments should be less of equals than number of constructor parameters");
			for (int i = 0 ; i < args.length ; i++) {
				if (!(parameters.get(i).isOptional() && args[i] == null)) {
					argParameters.put(parameters.get(i), args[i]);
				}
			}
			return kotlinConstructor.callBy(argParameters);
		}

	}

}
