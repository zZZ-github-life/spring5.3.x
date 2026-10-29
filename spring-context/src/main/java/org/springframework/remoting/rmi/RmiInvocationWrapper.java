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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.util.Assert;

/**
 * {@link RmiInvocationHandler} 的服务器端实现。每个远程对象都有一个此类的实例。对于非 RMI 服务实现，
 * 由 {@link RmiServiceExporter} 自动创建。
 *
 * <p>这是一个 SPI 类，不打算供应用程序使用。
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 * @see RmiServiceExporter
 */
@Deprecated
class RmiInvocationWrapper implements RmiInvocationHandler {

	private final Object wrappedObject;

	private final RmiBasedExporter rmiExporter;


	/**
	 * 为给定的对象创建一个新的 RmiInvocationWrapper。
	 * @param wrappedObject the object to wrap with an RmiInvocationHandler
	 * @param rmiExporter the RMI exporter to handle the actual invocation
	 */
	public RmiInvocationWrapper(Object wrappedObject, RmiBasedExporter rmiExporter) {
		Assert.notNull(wrappedObject, "Object to wrap is required");
		Assert.notNull(rmiExporter, "RMI exporter is required");
		this.wrappedObject = wrappedObject;
		this.rmiExporter = rmiExporter;
	}


	/**
	 * 将导出器的服务接口（如果有）作为目标接口暴露。
	 * @see RmiBasedExporter#getServiceInterface()
	 */
	@Override
	@Nullable
	public String getTargetInterfaceName() {
		Class<?> ifc = this.rmiExporter.getServiceInterface();
		return (ifc != null ? ifc.getName() : null);
	}

	/**
	 * 将实际的调用处理委托给 RMI 导出器。
	 * @see RmiBasedExporter#invoke(org.springframework.remoting.support.RemoteInvocation, Object)
	 */
	@Override
	@Nullable
	public Object invoke(RemoteInvocation invocation)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		return this.rmiExporter.invoke(invocation, this.wrappedObject);
	}

}
