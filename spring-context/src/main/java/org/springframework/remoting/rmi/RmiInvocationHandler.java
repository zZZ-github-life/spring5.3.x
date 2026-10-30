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
import java.rmi.RemoteException;

import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocation;

/**
 * 服务器上 RMI 调用处理器实例的接口，用于包装所导出的服务。客户端使用实现此接口的存根来访问这样的
 * 服务。
 *
 * <p>这是一个 SPI 接口，不应由应用程序直接使用。
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public interface RmiInvocationHandler extends Remote {

	/**
	 * 返回此调用器所操作的目标接口的名称。
	 * @return 目标接口的名称，如果没有则为 {@code null}
	 * @throws RemoteException 在通信出错时
	 * @see RmiServiceExporter#getServiceInterface()
	 */
	@Nullable
	public String getTargetInterfaceName() throws RemoteException;

	/**
	 * 将给定的调用应用到目标对象。
	 * <p>由
	 * {@link RmiClientInterceptor#doInvoke(org.aopalliance.intercept.MethodInvocation, RmiInvocationHandler)}
	 * 调用。
	 * @param invocation 封装调用参数的对象
	 * @return 被调用方法返回的对象（如果有）
	 * @throws RemoteException 在通信出错时
	 * @throws NoSuchMethodException 如果无法解析方法名
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 */
	@Nullable
	public Object invoke(RemoteInvocation invocation)
			throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException;

}
