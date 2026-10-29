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

package org.springframework.remoting.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationUtils;

/**
 * 用于访问常规 RMI 服务或 RMI 调用器的 {@link org.aopalliance.intercept.MethodInterceptor}。服务
 * URL 必须是有效的 RMI URL（例如 "rmi://localhost:1099/myservice"）。
 *
 * <p>RMI 调用器工作在 RmiInvocationHandler 层面，任何服务都只需要一个存根。服务接口不必扩展
 * {@code java.rmi.Remote}，也不必抛出 {@code java.rmi.RemoteException}。远程调用失败时会抛出
 * Spring 的非受检 RemoteAccessException。当然，入参和出参必须是可序列化的。
 *
 * <p>对于常规的 RMI 服务，此调用器通常与 RMI 服务接口一起使用。此外，此调用器还可以用匹配的非 RMI
 * 业务接口来代理远程 RMI 服务，即该接口镜像了 RMI 服务的方法但没有声明 RemoteExceptions。在后一种
 * 情况下，RMI 存根抛出的 RemoteExceptions 会自动转换为 Spring 的非受检 RemoteAccessException。
 *
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see RmiServiceExporter
 * @see RmiProxyFactoryBean
 * @see RmiInvocationHandler
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 * @see java.rmi.Remote
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class RmiClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor {

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private RMIClientSocketFactory registryClientSocketFactory;

	private Remote cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置是否在启动时查找 RMI 存根。默认值为 "true"。
	 * <p>可以关闭该功能，以便 RMI 服务器延迟启动。在这种情况下，RMI 存根将在首次访问时获取。
	 * @see #setCacheStub
	 */
	public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
		this.lookupStubOnStartup = lookupStubOnStartup;
	}

	/**
	 * 设置一旦定位到 RMI 存根后是否对其进行缓存。默认值为 "true"。
	 * <p>可以关闭该功能，以便 RMI 服务器热重启。在这种情况下，每次调用都会重新获取 RMI 存根。
	 * @see #setLookupStubOnStartup
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新 RMI 存根。默认值为 "false"。
	 * <p>可以开启该功能，以便 RMI 服务器热重启。如果缓存的 RMI 存根抛出的 RMI 异常表明远程连接失败，就
	 * 会获取新的代理并重试调用。
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置用于访问 RMI 注册表的自定义 RMI 客户端套接字工厂。
	 * @see java.rmi.server.RMIClientSocketFactory
	 * @see java.rmi.registry.LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
	 */
	public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
		this.registryClientSocketFactory = registryClientSocketFactory;
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 如有必要，在启动时获取 RMI 存根。
	 * @throws RemoteLookupFailureException if RMI stub creation failed
	 * @see #setLookupStubOnStartup
	 * @see #lookupStub
	 */
	public void prepare() throws RemoteLookupFailureException {
		// Cache RMI stub on initialization?
		if (this.lookupStubOnStartup) {
			Remote remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("RMI stub [" + getServiceUrl() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
						"] for RMI stub [" + getServiceUrl() + "] - " +
						(!isImpl ? "not " : "") + "directly implemented");
				}
			}
			if (this.cacheStub) {
				this.cachedStub = remoteObj;
			}
		}
	}

	/**
	 * 创建 RMI 存根，通常通过查找来完成。
	 * <p>如果 "cacheStub" 为 "true"，则在拦截器初始化时调用；否则由 {@link #getStub()} 在每次调用时调
	 * 用。
	 * <p>默认实现通过 {@code java.rmi.Naming} 查找服务 URL。可以在子类中重写。
	 * @return the RMI stub to store in this interceptor
	 * @throws RemoteLookupFailureException if RMI stub creation failed
	 * @see #setCacheStub
	 * @see java.rmi.Naming#lookup
	 */
	protected Remote lookupStub() throws RemoteLookupFailureException {
		try {
			Remote stub = null;
			if (this.registryClientSocketFactory != null) {
				// RMIClientSocketFactory specified for registry access.
				// Unfortunately, due to RMI API limitations, this means
				// that we need to parse the RMI URL ourselves and perform
				// straight LocateRegistry.getRegistry/Registry.lookup calls.
				URL url = new URL(null, getServiceUrl(), new DummyURLStreamHandler());
				String protocol = url.getProtocol();
				if (protocol != null && !"rmi".equals(protocol)) {
					throw new MalformedURLException("Invalid URL scheme '" + protocol + "'");
				}
				String host = url.getHost();
				int port = url.getPort();
				String name = url.getPath();
				if (name != null && name.startsWith("/")) {
					name = name.substring(1);
				}
				Registry registry = LocateRegistry.getRegistry(host, port, this.registryClientSocketFactory);
				stub = registry.lookup(name);
			}
			else {
				// Can proceed with standard RMI lookup API...
				stub = Naming.lookup(getServiceUrl());
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Located RMI stub with URL [" + getServiceUrl() + "]");
			}
			return stub;
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
		catch (NotBoundException ex) {
			throw new RemoteLookupFailureException(
					"Could not find RMI service [" + getServiceUrl() + "] in RMI registry", ex);
		}
		catch (RemoteException ex) {
			throw new RemoteLookupFailureException("Lookup of RMI stub failed", ex);
		}
	}

	/**
	 * 返回要使用的 RMI 存根。每次调用时都会调用此方法。
	 * <p>默认实现返回初始化时创建的存根（如果有）。否则，它会调用 {@link #lookupStub} 为每次调用获取新
	 * 的存根。可以在子类中重写，例如为了在重新创建存根之前将其缓存一段时间，或者测试存根是否仍然存活。
	 * @return the RMI stub to use for an invocation
	 * @throws RemoteLookupFailureException if RMI stub creation failed
	 * @see #lookupStub
	 */
	protected Remote getStub() throws RemoteLookupFailureException {
		if (!this.cacheStub || (this.lookupStubOnStartup && !this.refreshStubOnConnectFailure)) {
			return (this.cachedStub != null ? this.cachedStub : lookupStub());
		}
		else {
			synchronized (this.stubMonitor) {
				if (this.cachedStub == null) {
					this.cachedStub = lookupStub();
				}
				return this.cachedStub;
			}
		}
	}


	/**
	 * 获取 RMI 存根并委托给 {@code doInvoke}。如果配置为在连接失败时刷新，它会在相应的 RMI 异常上调用
	 * {@link #refreshAndRetry}。
	 * @see #getStub
	 * @see #doInvoke(MethodInvocation, Remote)
	 * @see #refreshAndRetry
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Remote stub = getStub();
		try {
			return doInvoke(invocation, stub);
		}
		catch (RemoteConnectFailureException ex) {
			return handleRemoteConnectFailure(invocation, ex);
		}
		catch (RemoteException ex) {
			if (isConnectFailure(ex)) {
				return handleRemoteConnectFailure(invocation, ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>默认实现委托给 {@link RmiClientInterceptorUtils#isConnectFailure}。
	 * @param ex the RMI exception to check
	 * @return whether the exception should be treated as connect failure
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	/**
	 * 如有必要，刷新存根并重试远程调用。
	 * <p>如果未配置为在连接失败时刷新，此方法只是重新抛出原始异常。
	 * @param invocation the invocation that failed
	 * @param ex the exception raised on remote invocation
	 * @return the result value of the new invocation, if succeeded
	 * @throws Throwable an exception raised by the new invocation,
	 * if it failed as well
	 * @see #setRefreshStubOnConnectFailure
	 * @see #doInvoke
	 */
	@Nullable
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			String msg = "Could not connect to RMI service [" + getServiceUrl() + "] - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新 RMI 存根并重试给定的调用。在连接失败时由 invoke 调用。
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #invoke
	 */
	@Nullable
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		Remote freshStub = null;
		synchronized (this.stubMonitor) {
			this.cachedStub = null;
			freshStub = lookupStub();
			if (this.cacheStub) {
				this.cachedStub = freshStub;
			}
		}
		return doInvoke(invocation, freshStub);
	}

	/**
	 * 在给定的 RMI 存根上执行给定的调用。
	 * @param invocation the AOP method invocation
	 * @param stub the RMI stub to invoke
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation, Remote stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI invoker
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw RmiClientInterceptorUtils.convertRmiAccessException(
					invocation.getMethod(), ex, isConnectFailure(ex), getServiceUrl());
			}
			catch (InvocationTargetException ex) {
				Throwable exToThrow = ex.getTargetException();
				RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
				throw exToThrow;
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getServiceUrl() + "]", ex);
			}
		}
		else {
			// traditional RMI stub
			try {
				return RmiClientInterceptorUtils.invokeRemoteMethod(invocation, stub);
			}
			catch (InvocationTargetException ex) {
				Throwable targetEx = ex.getTargetException();
				if (targetEx instanceof RemoteException) {
					RemoteException rex = (RemoteException) targetEx;
					throw RmiClientInterceptorUtils.convertRmiAccessException(
							invocation.getMethod(), rex, isConnectFailure(rex), getServiceUrl());
				}
				else {
					throw targetEx;
				}
			}
		}
	}

	/**
	 * 将给定的 AOP 方法调用应用到给定的 {@link RmiInvocationHandler}。
	 * <p>默认实现委托给 {@link #createRemoteInvocation}。
	 * @param methodInvocation the current AOP method invocation
	 * @param invocationHandler the RmiInvocationHandler to apply the invocation to
	 * @return the invocation result
	 * @throws RemoteException in case of communication errors
	 * @throws NoSuchMethodException if the method name could not be resolved
	 * @throws IllegalAccessException if the method could not be accessed
	 * @throws InvocationTargetException if the method invocation resulted in an exception
	 * @see org.springframework.remoting.support.RemoteInvocation
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
		throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "RMI invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}


	/**
	 * 占位的 URLStreamHandler，仅用于抑制标准的 {@code java.net.URL} URLStreamHandler 查找，从而能够使
	 * 用标准 URL 类解析 "rmi:..." URL。
	 */
	private static class DummyURLStreamHandler extends URLStreamHandler {

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}
