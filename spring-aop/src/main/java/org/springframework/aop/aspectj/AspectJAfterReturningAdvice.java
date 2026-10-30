/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

/**
 * 包装 AspectJ 返回后通知方法的 Spring AOP 通知。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
		implements AfterReturningAdvice, AfterAdvice, Serializable {

	public AspectJAfterReturningAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

	@Override
	public void setReturningName(String name) {
		setReturningNameNoCheck(name);
	}

	@Override
	public void afterReturning(@Nullable Object returnValue, Method method, Object[] args, @Nullable Object target) throws Throwable {
		if (shouldInvokeOnReturnValueOf(method, returnValue)) {
			invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
		}
	}


	/**
	 * 遵循 AspectJ 语义，如果指定了 returning 子句，那么仅当返回值是给定返回类型（以及泛型类型参数，如
	 * 果有的话）的实例，并且符合赋值规则时，才会调用该通知。如果返回类型是 Object，则通知*始终*会被调
	 * 用。
	 * @param returnValue 目标方法的返回值
	 * @return 是否针对给定的返回值调用通知方法
	 */
	private boolean shouldInvokeOnReturnValueOf(Method method, @Nullable Object returnValue) {
		Class<?> type = getDiscoveredReturningType();
		Type genericType = getDiscoveredReturningGenericType();
		// If we aren't dealing with a raw type, check if generic parameters are assignable.
		return (matchesReturnValue(type, method, returnValue) &&
				(genericType == null || genericType == type ||
						TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
	}

	/**
	 * 遵循 AspectJ 语义，如果返回值为 null（或返回类型为 void），则应使用目标方法的返回类型来确定是否
	 * 调用通知。此外，即使返回类型为 void，如果通知方法中声明的参数类型为 Object，那么通知仍然必须被调
	 * 用。
	 * @param type 通知方法中声明的参数类型
	 * @param method 通知方法
	 * @param returnValue 目标方法的返回值
	 * @return 是否针对给定的返回值和类型调用通知方法
	 */
	private boolean matchesReturnValue(Class<?> type, Method method, @Nullable Object returnValue) {
		if (returnValue != null) {
			return ClassUtils.isAssignableValue(type, returnValue);
		}
		else if (Object.class == type && void.class == method.getReturnType()) {
			return true;
		}
		else {
			return ClassUtils.isAssignable(type, method.getReturnType());
		}
	}

}
