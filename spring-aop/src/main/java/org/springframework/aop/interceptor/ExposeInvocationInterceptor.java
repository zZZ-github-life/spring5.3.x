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

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * 将当前 {@link org.aopalliance.intercept.MethodInvocation} 作为线程局部对象暴露出来的拦截器。我们
 * 偶尔需要这样做；例如，当切点（如 AspectJ 表达式切点）需要了解完整的调用上下文时。
 *
 * <p>除非确实必要，否则不要使用此拦截器。目标对象通常不应了解 Spring AOP，因为这会形成对 Spring
 * API 的依赖。目标对象应尽可能保持为普通 POJO。
 *
 * <p>如果使用，此拦截器通常是拦截器链中的第一个。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public final class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	/** 此类的单例实例。 */
	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	/**
	 * 此类的单例通知器。使用 Spring AOP 时优先使用它而不是 INSTANCE，因为这样可避免为包装该实例而创建
	 * 新的 Advisor。
	 */
	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<>("Current AOP method invocation");


	/**
	 * 返回与当前调用关联的 AOP Alliance MethodInvocation 对象。
	 * @return the invocation object associated with the current invocation
	 * @throws IllegalStateException if there is no AOP invocation in progress,
	 * or if the ExposeInvocationInterceptor was not added to this interceptor chain
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		MethodInvocation mi = invocation.get();
		if (mi == null) {
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress and that the " +
					"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
					"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor! " +
					"In addition, ExposeInvocationInterceptor and ExposeInvocationInterceptor.currentInvocation() " +
					"must be invoked from the same thread.");
		}
		return mi;
	}


	/**
	 * 确保只能创建规范实例。
	 */
	private ExposeInvocationInterceptor() {
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		MethodInvocation oldInvocation = invocation.get();
		invocation.set(mi);
		try {
			return mi.proceed();
		}
		finally {
			invocation.set(oldInvocation);
		}
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * 支持序列化所必需。反序列化时会替换为规范实例，从而保护单例模式。
	 * <p>这是重写 {@code equals} 方法的替代方案。
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
