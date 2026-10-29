/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;

/**
 * 基于 RMI 的远程导出器的便捷超类。提供一种机制，自动用 RmiInvocationWrapper 包装给定的普通 Java
 * 服务对象，从而暴露 {@link RmiInvocationHandler} 远程接口。
 *
 * <p>使用 RMI 调用器机制时，RMI 通信工作在 {@link RmiInvocationHandler} 层面，任意数量的服务共享同
 * 一个调用器存根。服务接口<i>不</i>需要扩展 {@code java.rmi.Remote}，也不需要在所有服务方法上声明
 * {@code java.rmi.RemoteException}。不过，入参和出参仍然必须是可序列化的。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see RmiServiceExporter
 * @see JndiRmiServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public abstract class RmiBasedExporter extends RemoteInvocationBasedExporter {

	/**
	 * 确定要导出的对象：服务对象本身，或者在非 RMI 服务对象的情况下为其提供一个 RmiInvocationWrapper。
	 * @return the RMI object to export
	 * @see #setService
	 * @see #setServiceInterface
	 */
	protected Remote getObjectToExport() {
		// determine remote object
		if (getService() instanceof Remote &&
				(getServiceInterface() == null || Remote.class.isAssignableFrom(getServiceInterface()))) {
			// conventional RMI service
			return (Remote) getService();
		}
		else {
			// RMI invoker
			if (logger.isDebugEnabled()) {
				logger.debug("RMI service [" + getService() + "] is an RMI invoker");
			}
			return new RmiInvocationWrapper(getProxyForService(), this);
		}
	}

	/**
	 * 在此重新定义，以便对 RmiInvocationWrapper 可见。只是委托给相应的超类方法。
	 */
	@Override
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return super.invoke(invocation, targetObject);
	}

}
