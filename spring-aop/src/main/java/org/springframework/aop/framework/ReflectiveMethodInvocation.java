/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

/**
 * Spring 对 AOP Alliance {@link org.aopalliance.intercept.MethodInvocation} 接口的实现，并实现了扩
 * 展的 {@link org.springframework.aop.ProxyMethodInvocation} 接口。
 *
 * <p>使用反射调用目标对象。子类可以覆盖 {@link #invokeJoinpoint()} 方法以改变此行为，因此它也是更
 * 专用的 MethodInvocation 实现的有用基类。
 *
 * <p>可以克隆一次调用，并使用 {@link #invocableClone()} 方法反复调用 {@link #proceed()}（每个克隆
 * 一次）。还可以使用 {@link #setUserAttribute} / {@link #getUserAttribute} 方法为调用附加自定义属
 * 性。
 *
 * <p><b>注意：</b>此类被视为内部类，不应直接访问。它之所以是 public，唯一原因是为了与现有框架集成
 * （例如 Pitchfork）保持兼容。对于其他任何用途，请改用 {@link ProxyMethodInvocation} 接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	protected final Object proxy;

	@Nullable
	protected final Object target;

	protected final Method method;

	protected Object[] arguments;

	@Nullable
	private final Class<?> targetClass;

	/**
	 * 此调用中用户特定属性的延迟初始化映射。
	 */
	@Nullable
	private Map<String, Object> userAttributes;

	/**
	 * 需要进行动态检查的 MethodInterceptor 和 InterceptorAndDynamicMethodMatcher 列表。
	 */
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * 我们正在调用的当前拦截器的索引，从 0 开始。-1 表示尚未调用：调用后即为当前拦截器。
	 */
	private int currentInterceptorIndex = -1;


	/**
	 * 使用给定参数构造一个新的 ReflectiveMethodInvocation。
	 * @param proxy 发起调用的代理对象
	 * @param target 要调用的目标对象
	 * @param method 要调用的方法
	 * @param arguments 调用方法时使用的参数
	 * @param targetClass 目标类，用于 MethodMatcher 调用
	 * @param interceptorsAndDynamicMethodMatchers 应应用的拦截器，以及需要在运行时求值的所有
	 * InterceptorAndDynamicMethodMatchers。此结构中包含的 MethodMatchers
	 * 必须已被判定为尽可能静态匹配。传入数组可能快约 10%，
	 * 但会使代码复杂化，而且只适用于静态切点。
	 */
	protected ReflectiveMethodInvocation(
			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
	}


	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	@Override
	@Nullable
	public final Object getThis() {
		return this.target;
	}

	@Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * 返回在代理接口上被调用的方法。它可能与在该接口的底层实现上被调用的方法并不一致。
	 */
	@Override
	public final Method getMethod() {
		return this.method;
	}

	@Override
	public final Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	@Override
	@Nullable
	public Object proceed() throws Throwable {
		// We start with an index of -1 and increment early.
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			return invokeJoinpoint();
		}

		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// Dynamic matching failed.
				// Skip this interceptor and invoke the next in the chain.
				return proceed();
			}
		}
		else {
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * 使用反射调用连接点。子类可以覆盖此方法以使用自定义调用方式。
	 * @return 连接点的返回值
	 * @throws Throwable 如果调用连接点导致异常
	 */
	@Nullable
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * 此实现返回此调用对象的浅拷贝，包括原始参数数组的独立副本。
	 * <p>此处我们需要浅拷贝：我们希望使用相同的拦截器链和其他对象引用，但当前拦截器索引需要独立的值。
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = this.arguments;
		if (this.arguments.length > 0) {
			// Build an independent copy of the arguments array.
			cloneArguments = this.arguments.clone();
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * 此实现返回此调用对象的浅拷贝，并使用给定参数数组作为克隆的参数。
	 * <p>此处我们需要浅拷贝：我们希望使用相同的拦截器链和其他对象引用，但当前拦截器索引需要独立的值。
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// Force initialization of the user attributes Map,
		// for having a shared Map reference in the clone.
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}

		// Create the MethodInvocation clone.
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, @Nullable Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	@Nullable
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * 返回与此调用关联的用户属性。此方法提供了绑定到调用的 ThreadLocal 替代方案。
	 * <p>此映射采用延迟初始化，AOP 框架本身并不使用它。
	 * @return 与此调用关联的所有用户属性（永不为 {@code null}）
	 */
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// Don't do toString on target, it may be proxied.
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		}
		else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}

}
