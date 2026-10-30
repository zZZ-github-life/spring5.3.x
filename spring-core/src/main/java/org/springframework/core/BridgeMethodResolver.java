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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * 用于将合成的 {@link Method#isBridge bridge Methods} 解析为其所桥接的 {@link Method}
 * 的辅助类。
 *
 * <p>给定一个合成的 {@link Method#isBridge bridge Method}，
 * 返回其正在桥接的 {@link Method}。当扩展一个方法带有参数化参数的参数化类型时，
 * 编译器可能会创建桥接方法。在运行时调用期间，
 * 桥接 {@link Method} 可能被调用和/或通过反射使用。
 * 在尝试定位 {@link Method Methods} 上的注解时，
 * 明智的做法是适当地检查桥接 {@link Method Methods}，并找到被桥接的 {@link Method}。
 *
 * <p>有关桥接方法使用的更多细节，
 * 请参见
 * <a href="https://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12.4.5">
 * 《Java 语言规范》</a>。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 */
public final class BridgeMethodResolver {

	private static final Map<Method, Method> cache = new ConcurrentReferenceHashMap<>();

	private BridgeMethodResolver() {
	}


	/**
	 * 查找提供的 {@link Method bridge Method} 对应的原始方法。
	 * <p>传入非桥接的 {@link Method} 实例来调用此方法是安全的。在这种情况下，
	 * 提供的 {@link Method} 实例会直接返回给调用者。
	 * 调用者<strong>无需</strong>在调用此方法之前检查是否存在桥接。
	 * @param bridgeMethod 要内省的方法
	 * @return 原始方法（桥接方法，或在找不到更具体的方法时传入的方法）
	 */
	public static Method findBridgedMethod(Method bridgeMethod) {
		if (!bridgeMethod.isBridge()) {
			return bridgeMethod;
		}
		Method bridgedMethod = cache.get(bridgeMethod);
		if (bridgedMethod == null) {
			// Gather all methods with matching name and parameter size.
			List<Method> candidateMethods = new ArrayList<>();
			MethodFilter filter = candidateMethod ->
					isBridgedCandidateFor(candidateMethod, bridgeMethod);
			ReflectionUtils.doWithMethods(bridgeMethod.getDeclaringClass(), candidateMethods::add, filter);
			if (!candidateMethods.isEmpty()) {
				bridgedMethod = candidateMethods.size() == 1 ?
						candidateMethods.get(0) :
						searchCandidates(candidateMethods, bridgeMethod);
			}
			if (bridgedMethod == null) {
				// A bridge method was passed in but we couldn't find the bridged method.
				// Let's proceed with the passed-in method and hope for the best...
				bridgedMethod = bridgeMethod;
			}
			cache.put(bridgeMethod, bridgedMethod);
		}
		return bridgedMethod;
	}

	/**
	 * 如果提供的 '{@code candidateMethod}' 可以被视为由提供的 {@link Method bridge Method}
	 * 所{@link Method#isBridge() bridged}的 {@link Method} 的有效候选，
	 * 则返回 {@code true}。此方法只执行开销很小的检查，可用于快速过滤出一组可能的匹配项。
	 */
	private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
		return (!candidateMethod.isBridge() && !candidateMethod.equals(bridgeMethod) &&
				candidateMethod.getName().equals(bridgeMethod.getName()) &&
				candidateMethod.getParameterCount() == bridgeMethod.getParameterCount());
	}

	/**
	 * 在给定的候选中搜索被桥接的方法。
	 * @param candidateMethods 候选 Method 的列表
	 * @param bridgeMethod 桥接方法
	 * @return 被桥接的方法，若未找到则为 {@code null}
	 */
	@Nullable
	private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
		if (candidateMethods.isEmpty()) {
			return null;
		}
		Method previousMethod = null;
		boolean sameSig = true;
		for (Method candidateMethod : candidateMethods) {
			if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
				return candidateMethod;
			}
			else if (previousMethod != null) {
				sameSig = sameSig &&
						Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
			}
			previousMethod = candidateMethod;
		}
		return (sameSig ? candidateMethods.get(0) : null);
	}

	/**
	 * 判断桥接 {@link Method} 是否为提供的候选 {@link Method} 的桥接方法。
	 */
	static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
		if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
			return true;
		}
		Method method = findGenericDeclaration(bridgeMethod);
		return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
	}

	/**
	 * 如果在针对 declaringType 解析所有类型之后，
	 * 提供的 {@link Method#getGenericParameterTypes() generic Method} 与具体 {@link Method} 的
	 * {@link Type} 签名相等，
	 * 则返回 {@code true}，否则返回 {@code false}。
	 */
	private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
		Type[] genericParameters = genericMethod.getGenericParameterTypes();
		if (genericParameters.length != candidateMethod.getParameterCount()) {
			return false;
		}
		Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
		for (int i = 0; i < candidateParameters.length; i++) {
			ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
			Class<?> candidateParameter = candidateParameters[i];
			if (candidateParameter.isArray()) {
				// An array type: compare the component type.
				if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().toClass())) {
					return false;
				}
			}
			// A non-array type: compare the type itself.
			if (!ClassUtils.resolvePrimitiveIfNecessary(candidateParameter).equals(ClassUtils.resolvePrimitiveIfNecessary(genericParameter.toClass()))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 搜索擦除后签名与提供的桥接方法相匹配的泛型 {@link Method} 声明。
	 * @throws IllegalStateException 如果找不到泛型声明
	 */
	@Nullable
	private static Method findGenericDeclaration(Method bridgeMethod) {
		// Search parent types for method that has same signature as bridge.
		Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
		while (superclass != null && Object.class != superclass) {
			Method method = searchForMatch(superclass, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			superclass = superclass.getSuperclass();
		}

		Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
		return searchInterfaces(interfaces, bridgeMethod);
	}

	@Nullable
	private static Method searchInterfaces(Class<?>[] interfaces, Method bridgeMethod) {
		for (Class<?> ifc : interfaces) {
			Method method = searchForMatch(ifc, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			else {
				method = searchInterfaces(ifc.getInterfaces(), bridgeMethod);
				if (method != null) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 如果提供的 {@link Class} 中有一个声明的 {@link Method} 的签名与提供的 {@link Method}
	 * 的签名匹配，
	 * 则返回该匹配的 {@link Method}，否则返回 {@code null}。
	 */
	@Nullable
	private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
		try {
			return type.getDeclaredMethod(bridgeMethod.getName(), bridgeMethod.getParameterTypes());
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 比较桥接方法与其所桥接方法的签名。如果参数类型和返回类型相同，
	 * 那么它就是 Java 6 中为修复 https://bugs.openjdk.org/browse/JDK-6342411.
	 * 而引入的“可见性”桥接方法。
	 * 另请参见 https://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html
	 * @return 签名是否如所述那样匹配
	 */
	public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
		if (bridgeMethod == bridgedMethod) {
			return true;
		}
		return (bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType()) &&
				bridgeMethod.getParameterCount() == bridgedMethod.getParameterCount() &&
				Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes()));
	}

}
