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

package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

/**
 * 基于 {@link RemoteInvocation} 对象反序列化的远程服务导出器的抽象基类。
 *
 * <p>提供 "remoteInvocationExecutor" 属性，默认策略为 {@link DefaultRemoteInvocationExecutor}。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocationExecutor
 * @see DefaultRemoteInvocationExecutor
 */
public abstract class RemoteInvocationBasedExporter extends RemoteExporter {

	private RemoteInvocationExecutor remoteInvocationExecutor = new DefaultRemoteInvocationExecutor();


	/**
	 * 设置此导出器要使用的 RemoteInvocationExecutor。默认为 DefaultRemoteInvocationExecutor。
	 * <p>自定义调用执行器可以从调用中提取更多上下文信息，例如用户凭据。
	 */
	public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor) {
		this.remoteInvocationExecutor = remoteInvocationExecutor;
	}

	/**
	 * 返回此导出器使用的 RemoteInvocationExecutor。
	 */
	public RemoteInvocationExecutor getRemoteInvocationExecutor() {
		return this.remoteInvocationExecutor;
	}


	/**
	 * 将给定的远程调用应用到给定的目标对象。默认实现委托给 RemoteInvocationExecutor。
	 * <p>可以在子类中覆盖此方法以实现自定义调用行为，例如应用来自自定义 RemoteInvocation 子类的额外调
	 * 用参数。请注意，最好使用可复用的自定义 RemoteInvocationExecutor 策略。
	 * @param invocation the remote invocation
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @throws NoSuchMethodException if the method name could not be resolved
	 * @throws IllegalAccessException if the method could not be accessed
	 * @throws InvocationTargetException if the method invocation resulted in an exception
	 * @see RemoteInvocationExecutor#invoke
	 */
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (logger.isTraceEnabled()) {
			logger.trace("Executing " + invocation);
		}
		try {
			return getRemoteInvocationExecutor().invoke(invocation, targetObject);
		}
		catch (NoSuchMethodException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (IllegalAccessException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (InvocationTargetException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Target method failed for " + invocation, ex.getTargetException());
			}
			throw ex;
		}
	}

	/**
	 * 将给定的远程调用应用到给定的目标对象，并把调用结果包装在可序列化的 RemoteInvocationResult 对象中。
	 * 默认实现创建一个普通的 RemoteInvocationResult。
	 * <p>可以在子类中覆盖此方法以实现自定义调用行为，例如返回额外的上下文信息。请注意，这不在
	 * RemoteInvocationExecutor 策略的覆盖范围内！
	 * @param invocation the remote invocation
	 * @param targetObject the target object to apply the invocation to
	 * @return the invocation result
	 * @see #invoke
	 */
	protected RemoteInvocationResult invokeAndCreateResult(RemoteInvocation invocation, Object targetObject) {
		try {
			Object value = invoke(invocation, targetObject);
			return new RemoteInvocationResult(value);
		}
		catch (Throwable ex) {
			return new RemoteInvocationResult(ex);
		}
	}

}
