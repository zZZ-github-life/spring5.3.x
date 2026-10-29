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

package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 包含各种有助于实现支持自动装配的 Bean 工厂的方法的工具类。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 1.1.2
 * @see AbstractAutowireCapableBeanFactory
 */
abstract class AutowireUtils {

	public static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
		int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
		return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
	};


	/**
	 * 对给定的构造函数排序，优先选择公共构造函数以及参数数量最多的“贪婪”构造函数。结果将包含公共构造函
	 * 数在前（参数数量递减），然后是非公共构造函数（同样参数数量递减）。
	 * @param constructors the constructor array to sort
	 */
	public static void sortConstructors(Constructor<?>[] constructors) {
		Arrays.sort(constructors, EXECUTABLE_COMPARATOR);
	}

	/**
	 * 对给定的工厂方法排序，优先选择公共方法以及参数数量最多的“贪婪”方法。结果将包含公共方法在前（参数
	 * 数量递减），然后是非公共方法（同样参数数量递减）。
	 * @param factoryMethods the factory method array to sort
	 */
	public static void sortFactoryMethods(Method[] factoryMethods) {
		Arrays.sort(factoryMethods, EXECUTABLE_COMPARATOR);
	}

	/**
	 * 确定给定的 Bean 属性是否被排除在依赖检查之外。<p>此实现排除由 CGLIB 定义的属性。
	 * @param pd the PropertyDescriptor of the bean property
	 * @return whether the bean property is excluded
	 */
	public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		Method wm = pd.getWriteMethod();
		if (wm == null) {
			return false;
		}
		if (!wm.getDeclaringClass().getName().contains("$$")) {
			// Not a CGLIB method so it's OK.
			return false;
		}
		// It was declared by CGLIB, but we might still want to autowire it
		// if it was actually declared by the superclass.
		Class<?> superclass = wm.getDeclaringClass().getSuperclass();
		return !ClassUtils.hasMethod(superclass, wm);
	}

	/**
	 * 返回给定 Bean 属性的 setter 方法是否定义在任何给定接口中。
	 * @param pd the PropertyDescriptor of the bean property
	 * @param interfaces the Set of interfaces (Class objects)
	 * @return whether the setter method is defined by an interface
	 */
	public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
		Method setter = pd.getWriteMethod();
		if (setter != null) {
			Class<?> targetClass = setter.getDeclaringClass();
			for (Class<?> ifc : interfaces) {
				if (ifc.isAssignableFrom(targetClass) && ClassUtils.hasMethod(ifc, setter)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 根据给定的必需类型解析给定的自动装配值，例如将 {@link ObjectFactory} 值解析为其实际对象结果。
	 * @param autowiringValue the value to resolve
	 * @param requiredType the type to assign the result to
	 * @return the resolved value
	 */
	public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
		if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
			ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
			if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
				autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
						new Class<?>[] {requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
			}
			else {
				return factory.getObject();
			}
		}
		return autowiringValue;
	}

	/**
	 * 确定给定<em>泛型工厂方法</em>的泛型返回类型的目标类型，其中形式类型变量声明在该方法自身上。<p>例
	 * 如，给定具有如下签名的工厂方法，如果对 {@code createProxy()} 的反射方法以及包含
	 * {@code MyService.class} 的 {@code Object[]} 数组调用 {@code resolveReturnTypeForFactoryMethod()}，
	 * 则 {@code resolveReturnTypeForFactoryMethod()} 将推断目标返回类型为 {@code MyService}。
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>可能的返回值</h4>
	 * <ul>
	 * <li>目标返回类型（如果可以推断）</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型}（如果给定的 {@code method} 未声明任何 {@linkplain Method#getTypeParameters() 形式类型变量}）</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型}（如果无法推断目标返回类型，例如由于类型擦除）</li>
	 * <li>{@code null}（如果给定参数数组的长度小于给定方法的 {@linkplain Method#getGenericParameterTypes() 形式参数列表}长度）</li>
	 * </ul>
	 * @param method the method to introspect (never {@code null})
	 * @param args the arguments that will be supplied to the method when it is
	 * invoked (never {@code null})
	 * @param classLoader the ClassLoader to resolve class names against,
	 * if necessary (never {@code null})
	 * @return the resolved target return type or the standard method return type
	 * @since 3.2.5
	 */
	public static Class<?> resolveReturnTypeForFactoryMethod(
			Method method, Object[] args, @Nullable ClassLoader classLoader) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");

		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		Type genericReturnType = method.getGenericReturnType();
		Type[] methodParameterTypes = method.getGenericParameterTypes();
		Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");

		// Ensure that the type variable (e.g., T) is declared directly on the method
		// itself (e.g., via <T>), not on the enclosing class or interface.
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			if (currentTypeVariable.equals(genericReturnType)) {
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodParameterTypes.length; i++) {
				Type methodParameterType = methodParameterTypes[i];
				Object arg = args[i];
				if (methodParameterType.equals(genericReturnType)) {
					if (arg instanceof TypedStringValue) {
						TypedStringValue typedValue = ((TypedStringValue) arg);
						if (typedValue.hasTargetType()) {
							return typedValue.getTargetType();
						}
						try {
							Class<?> resolvedType = typedValue.resolveTargetType(classLoader);
							if (resolvedType != null) {
								return resolvedType;
							}
						}
						catch (ClassNotFoundException ex) {
							throw new IllegalStateException("Failed to resolve value type [" +
									typedValue.getTargetTypeName() + "] for factory method argument", ex);
						}
					}
					else if (arg != null && !(arg instanceof BeanMetadataElement)) {
						// Only consider argument type if it is a simple value...
						return arg.getClass();
					}
					return method.getReturnType();
				}
				else if (methodParameterType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							if (arg instanceof Class) {
								return (Class<?>) arg;
							}
							else {
								String className = null;
								if (arg instanceof String) {
									className = (String) arg;
								}
								else if (arg instanceof TypedStringValue) {
									TypedStringValue typedValue = ((TypedStringValue) arg);
									String targetTypeName = typedValue.getTargetTypeName();
									if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
										className = typedValue.getValue();
									}
								}
								if (className != null) {
									try {
										return ClassUtils.forName(className, classLoader);
									}
									catch (ClassNotFoundException ex) {
										throw new IllegalStateException("Could not resolve class name [" + arg +
												"] for factory method argument", ex);
									}
								}
								// Consider adding logic to determine the class of the typeArg, if possible.
								// For now, just fall back...
								return method.getReturnType();
							}
						}
					}
				}
			}
		}

		// Fall back...
		return method.getReturnType();
	}


	/**
	 * 用于延迟访问当前目标对象的反射 {@link InvocationHandler}。
	 */
	@SuppressWarnings("serial")
	private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

		private final ObjectFactory<?> objectFactory;

		ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of proxy.
					return System.identityHashCode(proxy);
				case "toString":
					return this.objectFactory.toString();
			}
			try {
				return method.invoke(this.objectFactory.getObject(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
