/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodInvokingRunnable} 的变体，用于处理无参计划方法。将用户异常传播给调用方，前提是已为
 * Runnable 配置了错误处理策略。
 *
 * @author Juergen Hoeller
 * @since 3.0.6
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public class ScheduledMethodRunnable implements Runnable {

	private final Object target;

	private final Method method;


	/**
	 * 为给定目标实例创建一个 {@code ScheduledMethodRunnable}，调用指定的方法。
	 * @param target 调用方法的目标实例
	 * @param method 要调用的目标方法
	 */
	public ScheduledMethodRunnable(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	/**
	 * 为给定目标实例创建一个 {@code ScheduledMethodRunnable}，按名称调用指定的方法。
	 * @param target 调用方法的目标实例
	 * @param methodName 目标方法的名称
	 * @throws NoSuchMethodException 如果指定的方法不存在
	 */
	public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
		this.target = target;
		this.method = target.getClass().getMethod(methodName);
	}


	/**
	 * 返回要在其上调用方法的目标实例。
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * 返回要调用的目标方法。
	 */
	public Method getMethod() {
		return this.method;
	}


	@Override
	public void run() {
		try {
			ReflectionUtils.makeAccessible(this.method);
			this.method.invoke(this.target);
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
		}
		catch (IllegalAccessException ex) {
			throw new UndeclaredThrowableException(ex);
		}
	}

	@Override
	public String toString() {
		return this.method.getDeclaringClass().getName() + "." + this.method.getName();
	}

}
