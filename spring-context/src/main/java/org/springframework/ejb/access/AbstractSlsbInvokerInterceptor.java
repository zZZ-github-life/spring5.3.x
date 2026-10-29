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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.jndi.JndiObjectLocator;
import org.springframework.lang.Nullable;

/**
 * 用于调用本地或远程无状态会话 Bean 的 AOP 拦截器的基类。为 EJB 2.x 设计，但也适用于 EJB 3 会话
 * Bean。
 *
 * <p>这样的拦截器必须是通知链中的最后一个拦截器。在这种情况下，没有直接的目标对象：调用会以特殊方
 * 式处理，在通过 EJB home 获取的 EJB 实例上执行。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractSlsbInvokerInterceptor extends JndiObjectLocator
		implements MethodInterceptor {

	private boolean lookupHomeOnStartup = true;

	private boolean cacheHome = true;

	private boolean exposeAccessContext = false;

	/**
	 * EJB 的 home 对象，可能被缓存。类型必须是 Object，因为它可能是 EJBHome 或 EJBLocalHome。
	 */
	@Nullable
	private Object cachedHome;

	/**
	 * EJB home 上所需的无参 create() 方法，可能被缓存。
	 */
	@Nullable
	private Method createMethod;

	private final Object homeMonitor = new Object();


	/**
	 * 设置是否在启动时查找 EJB home 对象。默认为 "true"。
	 * <p>可以关闭此选项以允许 EJB 服务器延迟启动。在这种情况下，EJB home 对象将在首次访问时获取。
	 * @see #setCacheHome
	 */
	public void setLookupHomeOnStartup(boolean lookupHomeOnStartup) {
		this.lookupHomeOnStartup = lookupHomeOnStartup;
	}

	/**
	 * 设置是否在定位到 EJB home 对象后对其进行缓存。默认为 "true"。
	 * <p>可以关闭此选项以支持 EJB 服务器的热重启。在这种情况下，每次调用都会获取 EJB home 对象。
	 * @see #setLookupHomeOnStartup
	 */
	public void setCacheHome(boolean cacheHome) {
		this.cacheHome = cacheHome;
	}

	/**
	 * 设置是否在对目标 EJB 的所有访问（即对暴露的对象引用的所有方法调用）中暴露 JNDI 环境上下文。
	 * <p>默认为 "false"，即仅在对象查找时暴露 JNDI 上下文。将此标志切换为 "true" 可为每次 EJB 调用暴露
	 * JNDI 环境（包括授权上下文），这是 WebLogic 对有授权要求的 EJB 所需要的。
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	/**
	 * 如有必要，在启动时获取 EJB home。
	 * @see #setLookupHomeOnStartup
	 * @see #refreshHome
	 */
	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.lookupHomeOnStartup) {
			// look up EJB home and create method
			refreshHome();
		}
	}

	/**
	 * 如适用，刷新缓存的 home 对象。还会缓存 home 对象上的 create 方法。
	 * @throws NamingException if thrown by the JNDI lookup
	 * @see #lookup
	 * @see #getCreateMethod
	 */
	protected void refreshHome() throws NamingException {
		synchronized (this.homeMonitor) {
			Object home = lookup();
			if (this.cacheHome) {
				this.cachedHome = home;
				this.createMethod = getCreateMethod(home);
			}
		}
	}

	/**
	 * 确定给定 EJB home 对象的 create 方法。
	 * @param home the EJB home object
	 * @return the create method
	 * @throws EjbAccessException if the method couldn't be retrieved
	 */
	@Nullable
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		try {
			// Cache the EJB create() method that must be declared on the home interface.
			return home.getClass().getMethod("create");
		}
		catch (NoSuchMethodException ex) {
			throw new EjbAccessException("EJB home [" + home + "] has no no-arg create() method");
		}
	}

	/**
	 * 返回要使用的 EJB home 对象。每次调用时都会调用此方法。
	 * <p>默认实现返回初始化时创建的 home（如果有）；否则，它会调用 lookup 为每次调用获取一个新的代理。
	 * <p>可在子类中覆盖，例如在重新创建 home 对象之前将其缓存一段时间，或检测 home 对象是否仍存活。
	 * @return the EJB home object to use for an invocation
	 * @throws NamingException if proxy creation failed
	 * @see #lookup
	 * @see #getCreateMethod
	 */
	protected Object getHome() throws NamingException {
		if (!this.cacheHome || (this.lookupHomeOnStartup && !isHomeRefreshable())) {
			return (this.cachedHome != null ? this.cachedHome : lookup());
		}
		else {
			synchronized (this.homeMonitor) {
				if (this.cachedHome == null) {
					this.cachedHome = lookup();
					this.createMethod = getCreateMethod(this.cachedHome);
				}
				return this.cachedHome;
			}
		}
	}

	/**
	 * 返回缓存的 EJB home 对象是否可能按需刷新。默认为 "false"。
	 */
	protected boolean isHomeRefreshable() {
		return false;
	}


	/**
	 * 如有必要，准备线程上下文，并委托给 {@link #invokeInContext}。
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
		try {
			return invokeInContext(invocation);
		}
		finally {
			getJndiTemplate().releaseContext(ctx);
		}
	}

	/**
	 * 在相应准备好的线程上下文中，在当前 EJB home 上执行给定调用。由子类实现的模板方法。
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 */
	@Nullable
	protected abstract Object invokeInContext(MethodInvocation invocation) throws Throwable;


	/**
	 * 在缓存的 EJB home 对象上调用 {@code create()} 方法。
	 * @return a new EJBObject or EJBLocalObject
	 * @throws NamingException if thrown by JNDI
	 * @throws InvocationTargetException if thrown by the create method
	 */
	protected Object create() throws NamingException, InvocationTargetException {
		try {
			Object home = getHome();
			Method createMethodToUse = this.createMethod;
			if (createMethodToUse == null) {
				createMethodToUse = getCreateMethod(home);
			}
			if (createMethodToUse == null) {
				return home;
			}
			// Invoke create() method on EJB home object.
			return createMethodToUse.invoke(home, (Object[]) null);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access EJB home create() method", ex);
		}
	}

}
