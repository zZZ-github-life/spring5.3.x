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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

/**
 * 基于反射的 {@link MethodResolver}，除非已指定显式的方法解析器，否则在
 * {@link StandardEvaluationContext} 中默认使用。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.0
 * @see StandardEvaluationContext#addMethodResolver(MethodResolver)
 */
public class ReflectiveMethodResolver implements MethodResolver {

	// Using distance will ensure a more accurate match is discovered,
	// more closely following the Java rules.
	private final boolean useDistance;

	@Nullable
	private Map<Class<?>, MethodFilter> filters;


	public ReflectiveMethodResolver() {
		this.useDistance = true;
	}

	/**
	 * 此构造器允许对 ReflectiveMethodResolver 进行配置，使其使用距离计算来检查两个接近的匹配中哪个更合
	 * 适（当存在多个匹配时）。使用距离计算旨在确保匹配结果更接近 Java 编译器在考虑装箱/拆箱以及方法候
	 * 选是否声明为处理所传入（参数）类型的超类型时的行为。
	 * @param useDistance {@code true} if distance computation should be used when
	 * calculating matches; {@code false} otherwise
	 */
	public ReflectiveMethodResolver(boolean useDistance) {
		this.useDistance = useDistance;
	}


	/**
	 * 为给定类型上的方法注册过滤器。
	 * @param type the type to filter on
	 * @param filter the corresponding method filter,
	 * or {@code null} to clear any filter for the given type
	 */
	public void registerMethodFilter(Class<?> type, @Nullable MethodFilter filter) {
		if (this.filters == null) {
			this.filters = new HashMap<>();
		}
		if (filter != null) {
			this.filters.put(type, filter);
		}
		else {
			this.filters.remove(type);
		}
	}

	/**
	 * 在类型上定位方法。可能发生三种匹配：
	 * <ol>
	 * <li>精确匹配：参数类型与方法类型匹配
	 * <li>非精确匹配：我们要查找的类型是方法上定义的类型的子类型
	 * <li>能够根据已注册的类型转换器将参数转换为方法期望的类型的匹配
	 * </ol>
	 */
	@Override
	@Nullable
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			ArrayList<Method> methods = new ArrayList<>(getMethods(type, targetObject));

			// If a filter is registered for this type, call it
			MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
			if (filter != null) {
				List<Method> filtered = filter.filter(methods);
				methods = (filtered instanceof ArrayList ? (ArrayList<Method>) filtered : new ArrayList<>(filtered));
			}

			// Sort methods into a sensible order
			if (methods.size() > 1) {
				methods.sort((m1, m2) -> {
					int m1pl = m1.getParameterCount();
					int m2pl = m2.getParameterCount();
					// vararg methods go last
					if (m1pl == m2pl) {
						if (!m1.isVarArgs() && m2.isVarArgs()) {
							return -1;
						}
						else if (m1.isVarArgs() && !m2.isVarArgs()) {
							return 1;
						}
						else {
							return 0;
						}
					}
					return Integer.compare(m1pl, m2pl);
				});
			}

			// Resolve any bridge methods
			for (int i = 0; i < methods.size(); i++) {
				methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
			}

			// Remove duplicate methods (possible due to resolved bridge methods)
			Set<Method> methodsToIterate = new LinkedHashSet<>(methods);

			Method closeMatch = null;
			int closeMatchDistance = Integer.MAX_VALUE;
			Method matchRequiringConversion = null;
			boolean multipleOptions = false;

			for (Method method : methodsToIterate) {
				if (method.getName().equals(name)) {
					int paramCount = method.getParameterCount();
					List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramCount);
					for (int i = 0; i < paramCount; i++) {
						paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
					}
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.size() >= (paramCount - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
					}
					else if (paramCount == argumentTypes.size()) {
						// Name and parameter number match, check the arguments
						matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.isExactMatch()) {
							return new ReflectiveMethodExecutor(method, type);
						}
						else if (matchInfo.isCloseMatch()) {
							if (this.useDistance) {
								int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
								if (closeMatch == null || matchDistance < closeMatchDistance) {
									// This is a better match...
									closeMatch = method;
									closeMatchDistance = matchDistance;
								}
							}
							else {
								// Take this as a close match if there isn't one already
								if (closeMatch == null) {
									closeMatch = method;
								}
							}
						}
						else if (matchInfo.isMatchRequiringConversion()) {
							if (matchRequiringConversion != null) {
								multipleOptions = true;
							}
							matchRequiringConversion = method;
						}
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch, type);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion, type);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}

	private Set<Method> getMethods(Class<?> type, Object targetObject) {
		if (targetObject instanceof Class) {
			Set<Method> result = new LinkedHashSet<>();
			// Add these so that static methods are invocable on the type: e.g. Float.valueOf(..)
			for (Method method : getMethods(type)) {
				if (Modifier.isStatic(method.getModifiers())) {
					result.add(method);
				}
			}
			// Also expose methods from java.lang.Class itself
			Collections.addAll(result, getMethods(Class.class));
			return result;
		}
		else if (Proxy.isProxyClass(type)) {
			Set<Method> result = new LinkedHashSet<>();
			// Expose interface methods (not proxy-declared overrides) for proper vararg introspection
			for (Class<?> ifc : type.getInterfaces()) {
				for (Method method : getMethods(ifc)) {
					if (isCandidateForInvocation(method, type)) {
						result.add(method);
					}
				}
			}
			// Ensure methods defined in java.lang.Object are exposed for JDK proxies.
			for (Method method : getMethods(Object.class)) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
		else {
			Set<Method> result = new LinkedHashSet<>();
			for (Method method : getMethods(type)) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
	}

	/**
	 * 返回此类型的方法集合。默认实现返回给定 {@code type} 的 {@link Class#getMethods()} 结果，但子类可
	 * 以重写以改变结果，例如指定在其他位置声明的静态方法。
	 * @param type the class for which to return the methods
	 * @since 3.1.1
	 */
	protected Method[] getMethods(Class<?> type) {
		return type.getMethods();
	}

	/**
	 * 判断给定的 {@code Method} 是否为在给定目标类实例上进行方法解析的候选方法。<p>默认实现将任何方法
	 * 都视为候选方法，即使是静态方法以及 {@link Object} 基类上非用户声明的方法。
	 * @param method the Method to evaluate
	 * @param targetClass the concrete target class that is being introspected
	 * @since 4.3.15
	 */
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		return true;
	}

}
