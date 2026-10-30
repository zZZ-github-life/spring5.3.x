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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiObjectLocator;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.util.Assert;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}，用于从 JNDI 访问 RMI 服务。通常用于
 * RMI-IIOP，但也可用于 EJB home 对象（例如有状态会话 Bean 的 home）。与普通的 JNDI 查找不同，此访
 * 问器还会通过 PortableRemoteObject 执行窄化转换。
 *
 * <p>对于传统的 RMI 服务，此调用器通常与 RMI 服务接口一起使用。或者，此调用器也可以用一个匹配的非
 * RMI 业务接口来代理远程 RMI 服务，即一个镜像 RMI 服务方法但不声明 RemoteExceptions 的接口。在后一
 * 种情况下，RMI stub 抛出的 RemoteExceptions 会自动转换为 Spring 的非受检 RemoteAccessException。
 *
 * <p>JNDI 环境可以通过 "jndiEnvironment" 属性指定，也可以在 {@code jndi.properties} 文件中或作为系
 * 统属性配置。例如：
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props&gt;
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setJndiName
 * @see JndiRmiServiceExporter
 * @see JndiRmiProxyFactoryBean
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 * @see java.rmi.Remote
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class JndiRmiClientInterceptor extends JndiObjectLocator implements MethodInterceptor, InitializingBean {

	private Class<?> serviceInterface;

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private boolean lookupStubOnStartup = true;

	private boolean cacheStub = true;

	private boolean refreshStubOnConnectFailure = false;

	private boolean exposeAccessContext = false;

	private Object cachedStub;

	private final Object stubMonitor = new Object();


	/**
	 * 设置要访问的服务的接口。该接口必须适合特定的服务和 remoting 工具。
	 * <p>通常需要用它来创建合适的服务代理，但如果查找返回的是带类型的 stub，则也可以省略。
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要访问的服务的接口。
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置此访问器要使用的 RemoteInvocationFactory。默认为 {@link DefaultRemoteInvocationFactory}。
	 * <p>自定义调用工厂可以向调用中添加更多上下文信息，例如用户凭证。
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory = remoteInvocationFactory;
	}

	/**
	 * 返回此访问器使用的 RemoteInvocationFactory。
	 */
	public RemoteInvocationFactory getRemoteInvocationFactory() {
		return this.remoteInvocationFactory;
	}

	/**
	 * 设置是否在启动时查找 RMI stub。默认值为 "true"。
	 * <p>可以关闭该选项，以允许 RMI 服务器延迟启动。在这种情况下，RMI stub 将在首次访问时获取。
	 * @see #setCacheStub
	 */
	public void setLookupStubOnStartup(boolean lookupStubOnStartup) {
		this.lookupStubOnStartup = lookupStubOnStartup;
	}

	/**
	 * 设置 RMI stub 被定位后是否对其进行缓存。默认值为 "true"。
	 * <p>可以关闭该选项，以允许热重启 RMI 服务器。在这种情况下，每次调用都会获取 RMI stub。
	 * @see #setLookupStubOnStartup
	 */
	public void setCacheStub(boolean cacheStub) {
		this.cacheStub = cacheStub;
	}

	/**
	 * 设置是否在连接失败时刷新 RMI stub。默认值为 "false"。
	 * <p>可以开启该选项，以允许热重启 RMI 服务器。如果缓存的 RMI stub 抛出表示远程连接失败的 RMI 异常，
	 * 则会获取一个新的代理并重试该调用。
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	public void setRefreshStubOnConnectFailure(boolean refreshStubOnConnectFailure) {
		this.refreshStubOnConnectFailure = refreshStubOnConnectFailure;
	}

	/**
	 * 设置是否对所有访问目标 RMI stub 的操作（即对暴露的对象引用上的所有方法调用）暴露 JNDI 环境上下文。
	 * <p>默认值为 "false"，即只在对象查找时暴露 JNDI 上下文。将该标志切换为 "true"，以便在每次 RMI 调
	 * 用时暴露 JNDI 环境（包括授权上下文），这是 WebLogic 对带授权要求的 RMI stub 的需要。
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 如有必要，在启动时获取 RMI stub。
	 * @throws RemoteLookupFailureException 如果 RMI 桩创建失败
	 * @see #setLookupStubOnStartup
	 * @see #lookupStub
	 */
	public void prepare() throws RemoteLookupFailureException {
		// Cache RMI stub on initialization?
		if (this.lookupStubOnStartup) {
			Object remoteObj = lookupStub();
			if (logger.isDebugEnabled()) {
				if (remoteObj instanceof RmiInvocationHandler) {
					logger.debug("JNDI RMI object [" + getJndiName() + "] is an RMI invoker");
				}
				else if (getServiceInterface() != null) {
					boolean isImpl = getServiceInterface().isInstance(remoteObj);
					logger.debug("Using service interface [" + getServiceInterface().getName() +
							"] for JNDI RMI object [" + getJndiName() + "] - " +
							(!isImpl ? "not " : "") + "directly implemented");
				}
			}
			if (this.cacheStub) {
				this.cachedStub = remoteObj;
			}
		}
	}

	/**
	 * 创建 RMI stub，通常通过查找来完成。
	 * <p>如果 "cacheStub" 为 "true"，则在拦截器初始化时调用；否则由 {@link #getStub()} 在每次调用时调
	 * 用。
	 * <p>默认实现从 JNDI 环境中获取服务。子类可以覆盖此方法。
	 * @return 要存储在此拦截器中的 RMI 桩
	 * @throws RemoteLookupFailureException 如果 RMI 桩创建失败
	 * @see #setCacheStub
	 * @see #lookup
	 */
	protected Object lookupStub() throws RemoteLookupFailureException {
		try {
			return lookup();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}
	}

	/**
	 * 返回要使用的 RMI stub。每次调用时都会调用。
	 * <p>默认实现返回初始化时创建的 stub（如果有）。否则，它会调用 {@link #lookupStub} 为每次调用获取
	 * 新的 stub。子类可以覆盖此方法，例如为了在重新创建之前将 stub 缓存一段时间，或测试 stub 是否仍然
	 * 存活。
	 * @return 要用于调用的 RMI 桩
	 * @throws NamingException 如果桩创建失败
	 * @throws RemoteLookupFailureException 如果 RMI 桩创建失败
	 */
	protected Object getStub() throws NamingException, RemoteLookupFailureException {
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
	 * 获取 RMI stub 并委托给 {@link #doInvoke}。如果配置为在连接失败时刷新，则会在相应的 RMI 异常上调
	 * 用 {@link #refreshAndRetry}。
	 * @see #getStub
	 * @see #doInvoke
	 * @see #refreshAndRetry
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object stub;
		try {
			stub = getStub();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("JNDI lookup for RMI service [" + getJndiName() + "] failed", ex);
		}

		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
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
		finally {
			getJndiTemplate().releaseContext(ctx);
		}
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>默认实现委托给 {@link RmiClientInterceptorUtils#isConnectFailure}。
	 * @param ex 要检查的 RMI 异常
	 * @return 该异常是否应被视为连接失败
	 */
	protected boolean isConnectFailure(RemoteException ex) {
		return RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	/**
	 * 如有必要，刷新 stub 并重试远程调用。
	 * <p>如果未配置为在连接失败时刷新，此方法只会重新抛出原始异常。
	 * @param invocation 失败的调用
	 * @param ex 远程调用时引发的异常
	 * @return 新调用的结果值（如果成功）
	 * @throws Throwable 新调用同样失败时引发的异常。
	 */
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshStubOnConnectFailure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not connect to RMI service [" + getJndiName() + "] - retrying", ex);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Could not connect to RMI service [" + getJndiName() + "] - retrying");
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新 RMI stub 并重试给定的调用。由 invoke 在连接失败时调用。
	 * @param invocation AOP 方法调用
	 * @return 调用结果（如果有）
	 * @throws Throwable 在调用失败时
	 * @see #invoke
	 */
	@Nullable
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		Object freshStub;
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
	 * 在给定的 RMI stub 上执行给定的调用。
	 * @param invocation AOP 方法调用
	 * @param stub 要调用的 RMI 桩
	 * @return 调用结果（如果有）
	 * @throws Throwable 在调用失败时
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation, Object stub) throws Throwable {
		if (stub instanceof RmiInvocationHandler) {
			// RMI invoker
			try {
				return doInvoke(invocation, (RmiInvocationHandler) stub);
			}
			catch (RemoteException ex) {
				throw convertRmiAccessException(ex, invocation.getMethod());
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			catch (Throwable ex) {
				throw new RemoteInvocationFailureException("Invocation of method [" + invocation.getMethod() +
						"] failed in RMI service [" + getJndiName() + "]", ex);
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
					throw convertRmiAccessException((RemoteException) targetEx, invocation.getMethod());
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
	 * @param methodInvocation 当前 AOP 方法调用
	 * @param invocationHandler 要应用该调用的 RmiInvocationHandler
	 * @return 调用结果
	 * @throws RemoteException 在通信出错时
	 * @throws NoSuchMethodException 如果无法解析方法名
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 * @see org.springframework.remoting.support.RemoteInvocation
	 */
	protected Object doInvoke(MethodInvocation methodInvocation, RmiInvocationHandler invocationHandler)
			throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "RMI invoker proxy for service URL [" + getJndiName() + "]";
		}

		return invocationHandler.invoke(createRemoteInvocation(methodInvocation));
	}

	/**
	 * 为给定的 AOP 方法调用创建一个新的 RemoteInvocation 对象。
	 * <p>默认实现委托给已配置的 {@link #setRemoteInvocationFactory RemoteInvocationFactory}。子类可以
	 * 覆盖此方法，以提供包含附加调用参数（例如用户凭证）的自定义 RemoteInvocation 子类。
	 * <p>注意，最好将自定义 RemoteInvocationFactory 构建为可复用的策略，而不是覆盖此方法。
	 * @param methodInvocation 当前 AOP 方法调用
	 * @return RemoteInvocation 对象
	 * @see RemoteInvocationFactory#createRemoteInvocation
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return getRemoteInvocationFactory().createRemoteInvocation(methodInvocation);
	}

	/**
	 * 将远程访问期间发生的给定 RMI RemoteException 转换为 Spring 的 RemoteAccessException（如果方法签
	 * 名未声明 RemoteException）。否则，返回原始的 RemoteException。
	 * @param method 被调用的方法
	 * @param ex 发生的 RemoteException
	 * @return 要抛出给调用方的异常
	 */
	private Exception convertRmiAccessException(RemoteException ex, Method method) {
		return RmiClientInterceptorUtils.convertRmiAccessException(method, ex, isConnectFailure(ex), getJndiName());
	}

}
