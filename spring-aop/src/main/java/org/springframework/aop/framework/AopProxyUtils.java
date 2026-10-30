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

package org.springframework.aop.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOP 代理工厂的工具方法。主要供 AOP 框架内部使用。
 *
 * <p>有关不依赖 AOP 框架内部实现的通用 AOP 工具方法集合，请参见
 * {@link org.springframework.aop.support.AopUtils}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.aop.support.AopUtils
 */
public abstract class AopProxyUtils {

	// JDK 17 Class.isSealed() method available?
	@Nullable
	private static final Method isSealedMethod = ClassUtils.getMethodIfAvailable(Class.class, "isSealed");


	/**
	 * 获取给定代理背后的单例目标对象（如果有）。
	 * @param candidate 要检查的（潜在）代理
	 * @return 在 {@link SingletonTargetSource} 中管理的单例目标对象，
	 * 其他情况下（不是代理、不是现有的单例目标）则为 {@code null}
	 * @since 4.3.8
	 * @see Advised#getTargetSource()
	 * @see SingletonTargetSource#getTarget()
	 */
	@Nullable
	public static Object getSingletonTarget(Object candidate) {
		if (candidate instanceof Advised) {
			TargetSource targetSource = ((Advised) candidate).getTargetSource();
			if (targetSource instanceof SingletonTargetSource) {
				return ((SingletonTargetSource) targetSource).getTarget();
			}
		}
		return null;
	}

	/**
	 * 确定给定 Bean 实例的最终目标类，不仅会遍历顶层代理，还会遍历任意数量的嵌套代理 &mdash; 只要不产
	 * 生副作用就尽可能深入，也就是说，仅针对单例目标。
	 * @param candidate 要检查的实例（可能是 AOP 代理）
	 * @return 最终的目标类（如果无法确定，则回退为给定对象的普通类；
	 * 永不为 {@code null}）
	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
	 * @see Advised#getTargetSource()
	 */
	public static Class<?> ultimateTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Object current = candidate;
		Class<?> result = null;
		while (current instanceof TargetClassAware) {
			result = ((TargetClassAware) current).getTargetClass();
			current = getSingletonTarget(current);
		}
		if (result == null) {
			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * 为给定的 AOP 配置确定要代理的完整接口集合。
	 * <p>此方法始终会添加 {@link Advised} 接口，除非 AdvisedSupport 的
	 * {@link AdvisedSupport#setOpaque "opaque"} 标志已开启。并且始终添加
	 * {@link org.springframework.aop.SpringProxy} 标记接口。
	 * @param advised 代理配置
	 * @return 要代理的完整接口集合
	 * @see SpringProxy
	 * @see Advised
	 */
	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
		return completeProxiedInterfaces(advised, false);
	}

	/**
	 * 为给定的 AOP 配置确定要代理的完整接口集合。
	 * <p>此方法始终会添加 {@link Advised} 接口，除非 AdvisedSupport 的
	 * {@link AdvisedSupport#setOpaque "opaque"} 标志已开启。并且始终添加
	 * {@link org.springframework.aop.SpringProxy} 标记接口。
	 * @param advised 代理配置
	 * @param decoratingProxy 是否暴露 {@link DecoratingProxy} 接口
	 * @return 要代理的完整接口集合
	 * @since 4.3
	 * @see SpringProxy
	 * @see Advised
	 * @see DecoratingProxy
	 */
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		if (specifiedInterfaces.length == 0) {
			// No user-specified interfaces: check whether target class is an interface.
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				if (targetClass.isInterface()) {
					advised.setInterfaces(targetClass);
				}
				else if (Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
					advised.setInterfaces(targetClass.getInterfaces());
				}
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}
		List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
		for (Class<?> ifc : specifiedInterfaces) {
			// Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
			if (isSealedMethod == null || Boolean.FALSE.equals(ReflectionUtils.invokeMethod(isSealedMethod, ifc))) {
				proxiedInterfaces.add(ifc);
			}
		}
		if (!advised.isInterfaceProxied(SpringProxy.class)) {
			proxiedInterfaces.add(SpringProxy.class);
		}
		if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
			proxiedInterfaces.add(Advised.class);
		}
		if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
			proxiedInterfaces.add(DecoratingProxy.class);
		}
		return ClassUtils.toClassArray(proxiedInterfaces);
	}

	/**
	 * 提取给定代理所实现的用户指定接口，即该代理实现的所有非 Advised 接口。
	 * @param proxy 要分析的代理（通常是 JDK 动态代理）
	 * @return 该代理实现的所有用户指定的接口，按原始顺序排列
	 * （永不为 {@code null} 或空）
	 * @see Advised
	 */
	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
		int nonUserIfcCount = 0;
		if (proxy instanceof SpringProxy) {
			nonUserIfcCount++;
		}
		if (proxy instanceof Advised) {
			nonUserIfcCount++;
		}
		if (proxy instanceof DecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
		return userInterfaces;
	}

	/**
	 * 检查给定 AdvisedSupport 对象背后的代理是否相等。这不同于 AdvisedSupport 对象本身的相等性：这里比
	 * 较的是接口、advisor 和目标源的相等性。
	 */
	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
		return (a == b ||
				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
	}

	/**
	 * 检查给定 AdvisedSupport 对象背后被代理接口的相等性。
	 */
	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
	}

	/**
	 * 检查给定 AdvisedSupport 对象背后 advisor 的相等性。
	 */
	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
		return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
	}


	/**
	 * 如有必要，将给定参数适配为给定方法中的目标签名：特别是当给定的可变参数数组与方法中声明的可变参数
	 * 的数组类型不匹配时。
	 * @param method 目标方法
	 * @param arguments 给定的参数
	 * @return 克隆的参数数组，如果无需适配则为原数组
	 * @since 4.2.3
	 */
	static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
		if (ObjectUtils.isEmpty(arguments)) {
			return new Object[0];
		}
		if (method.isVarArgs()) {
			if (method.getParameterCount() == arguments.length) {
				Class<?>[] paramTypes = method.getParameterTypes();
				int varargIndex = paramTypes.length - 1;
				Class<?> varargType = paramTypes[varargIndex];
				if (varargType.isArray()) {
					Object varargArray = arguments[varargIndex];
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						Object[] newArguments = new Object[arguments.length];
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}

}
