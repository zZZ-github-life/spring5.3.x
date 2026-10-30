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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 用于针对类型变量解析泛型类型的辅助类。
 *
 * <p>主要供框架内部使用，即使方法参数类型以泛型方式声明，也能解析它们。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 2.5.2
 */
public final class GenericTypeResolver {

	/** 从 Class 到 TypeVariable Map 的缓存。 */
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();


	private GenericTypeResolver() {
	}


	/**
	 * 确定给定泛型参数类型的目标类型。
	 * @param methodParameter 方法参数规范
	 * @param implementationClass 用于解析类型变量的类
	 * @return 对应的泛型参数或返回类型
	 * @deprecated since 5.2 in favor of {@code methodParameter.withContainingClass(implementationClass).getParameterType()}
	 */
	@Deprecated
	public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "Class must not be null");
		methodParameter.setContainingClass(implementationClass);
		return methodParameter.getParameterType();
	}

	/**
	 * 确定给定方法的泛型返回类型的目标类型，其中形式类型变量在给定的类上声明。
	 * @param method 要内省的方法
	 * @param clazz 用于解析类型变量的类
	 * @return 对应的泛型参数或返回类型
	 */
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
	}

	/**
	 * 针对给定的目标方法解析给定泛型接口的单个类型参数，该方法被假定为返回给定接口或其实现。
	 * @param method 要检查其返回类型的目标方法
	 * @param genericIfc 用于从中解析类型实参的泛型接口或超类
	 * @return 方法返回类型解析后的参数类型，如果无法解析或单个参数为 {@link WildcardType} 类型则为 {@code null}。
	 */
	@Nullable
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
		Assert.notNull(method, "Method must not be null");
		ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * 针对给定的目标类解析给定泛型接口的单个类型参数，该类被假定为实现该泛型接口，
	 * 并可能为其类型变量声明具体类型。
	 * @param clazz 要对照检查的目标类
	 * @param genericIfc 用于从中解析类型实参的泛型接口或超类
	 * @return 解析后的参数类型，若无法解析则为 {@code null}
	 */
	@Nullable
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	@Nullable
	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		Assert.isTrue(resolvableType.getGenerics().length == 1,
				() -> "Expected 1 type argument on generic interface [" + resolvableType +
				"] but found " + resolvableType.getGenerics().length);
		return resolvableType.getGeneric().resolve();
	}


	/**
	 * 针对给定的目标类解析给定泛型接口的类型参数，该类被假定为实现该泛型接口，
	 * 并可能为其类型变量声明具体类型。
	 * @param clazz 要对照检查的目标类
	 * @param genericIfc 用于从中解析类型实参的泛型接口或超类
	 * @return 每个实参解析后的类型，数组大小与实际类型实参的数量匹配；若无法解析则为 {@code null}
	 */
	@Nullable
	public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
		if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

	/**
	 * 针对给定的上下文类解析给定的泛型类型，尽可能替换类型变量。
	 * @param genericType （可能）是泛型的类型
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在其方法签名中的类（可以为 {@code null}）
	 * @return 解析后的类型（可能原样返回给定的泛型类型）
	 * @since 5.0
	 */
	public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
		if (contextClass != null) {
			if (genericType instanceof TypeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						(TypeVariable<?>) genericType, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable != ResolvableType.NONE) {
					Class<?> resolved = resolvedTypeVariable.resolve();
					if (resolved != null) {
						return resolved;
					}
				}
			}
			else if (genericType instanceof ParameterizedType) {
				ResolvableType resolvedType = ResolvableType.forType(genericType);
				if (resolvedType.hasUnresolvableGenerics()) {
					ParameterizedType parameterizedType = (ParameterizedType) genericType;
					Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
					Type[] typeArguments = parameterizedType.getActualTypeArguments();
					ResolvableType contextType = ResolvableType.forClass(contextClass);
					for (int i = 0; i < typeArguments.length; i++) {
						Type typeArgument = typeArguments[i];
						if (typeArgument instanceof TypeVariable) {
							ResolvableType resolvedTypeArgument = resolveVariable(
									(TypeVariable<?>) typeArgument, contextType);
							if (resolvedTypeArgument != ResolvableType.NONE) {
								generics[i] = resolvedTypeArgument.resolve();
							}
							else {
								generics[i] = ResolvableType.forType(typeArgument).resolve();
							}
						}
						else {
							generics[i] = ResolvableType.forType(typeArgument).resolve();
						}
					}
					Class<?> rawClass = resolvedType.getRawClass();
					if (rawClass != null) {
						return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
					}
				}
			}
		}
		return genericType;
	}

	private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			resolvedType = ResolvableType.forType(typeVariable, contextType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}

		ResolvableType superType = contextType.getSuperType();
		if (superType != ResolvableType.NONE) {
			resolvedType = resolveVariable(typeVariable, superType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * 针对给定的 TypeVariable 映射解析指定的泛型类型。
	 * <p>供 Spring Data 使用。
	 * @param genericType 要解析的泛型类型
	 * @param map 用于解析的 TypeVariable 映射
	 * @return 如果解析为某个类则返回该类型，否则返回 {@code Object.class}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
		return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
	}

	/**
	 * 为指定的 {@link Class} 构建从 {@link TypeVariable#getName TypeVariable names} 到
	 * {@link Class concrete classes} 的映射。
	 * 会搜索所有超类型、外围类型和接口。
	 * @see #resolveType(Type, Map)
	 */
	@SuppressWarnings("rawtypes")
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<>();
			buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	@SuppressWarnings("rawtypes")
	private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type != ResolvableType.NONE) {
			Class<?> resolved = type.resolve();
			if (resolved != null && type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = resolved.getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVariableMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVariableMap(interfaceType, typeVariableMap);
			}
			if (resolved != null && resolved.isMemberClass()) {
				buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
			}
		}
	}


	@SuppressWarnings({"serial", "rawtypes"})
	private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

		private final Map<TypeVariable, Type> typeVariableMap;

		public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
			this.typeVariableMap = typeVariableMap;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			Type type = this.typeVariableMap.get(variable);
			return (type != null ? ResolvableType.forType(type) : null);
		}

		@Override
		public Object getSource() {
			return this.typeVariableMap;
		}
	}

}
