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

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

import org.springframework.util.Assert;

/**
 * 简单的 AOP Alliance {@code MethodInterceptor}，可引入到链中以显示关于被拦截方法调用的详细跟踪信
 * 息，包括方法进入和方法退出信息。
 *
 * <p>如有更高级的需求，可考虑使用 {@code CustomizableTraceInterceptor}。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 1.2
 * @see CustomizableTraceInterceptor
 */
@SuppressWarnings("serial")
public class SimpleTraceInterceptor extends AbstractTraceInterceptor {

	/**
	 * 使用静态日志器创建一个新的 SimpleTraceInterceptor。
	 */
	public SimpleTraceInterceptor() {
	}

	/**
	 * 根据给定标志，使用动态或静态日志器创建一个新的 SimpleTraceInterceptor。
	 * @param useDynamicLogger whether to use a dynamic logger or a static logger
	 * @see #setUseDynamicLogger
	 */
	public SimpleTraceInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}


	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String invocationDescription = getInvocationDescription(invocation);
		writeToLog(logger, "Entering " + invocationDescription);
		try {
			Object rval = invocation.proceed();
			writeToLog(logger, "Exiting " + invocationDescription);
			return rval;
		}
		catch (Throwable ex) {
			writeToLog(logger, "Exception thrown in " + invocationDescription, ex);
			throw ex;
		}
	}

	/**
	 * 返回给定方法调用的描述。
	 * @param invocation the invocation to describe
	 * @return the description
	 */
	protected String getInvocationDescription(MethodInvocation invocation) {
		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		String className = target.getClass().getName();
		return "method '" + invocation.getMethod().getName() + "' of class [" + className + "]";
	}

}
