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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * 本地无状态会话 Bean 的调用器。为 EJB 2.x 设计，但也适用于 EJB 3 会话 Bean。
 *
 * <p>缓存 home 对象，因为本地 EJB home 永远不会失效。有关如何指定目标 EJB 的 JNDI 位置的信息，请参
 * 见 {@link org.springframework.jndi.JndiObjectLocator}。
 *
 * <p>在 Bean 容器中，此类通常最好作为单例使用。但是，如果该 Bean 容器会预先实例化单例（就像 XML
 * ApplicationContext 的各个变体那样），而 Bean 容器在 EJB 容器加载目标 EJB 之前就已加载，则可能会
 * 出现问题。这是因为默认情况下，JNDI 查找会在此类的 init 方法中执行并缓存，但此时 EJB 尚未绑定到目
 * 标位置。最好的解决办法是将 lookupHomeOnStartup 属性设为 false，这样 home 将在首次访问 EJB 时获取。
 * （仅为向后兼容的原因，此标志默认才为 true）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AbstractSlsbInvokerInterceptor#setLookupHomeOnStartup
 * @see AbstractSlsbInvokerInterceptor#setCacheHome
 */
public class LocalSlsbInvokerInterceptor extends AbstractSlsbInvokerInterceptor {

	private volatile boolean homeAsComponent;


	/**
	 * 此实现为每次调用“创建”一个新的 EJB 实例。可覆盖以实现自定义调用策略。
	 * <p>或者，可以覆盖 {@link #getSessionBeanInstance} 和 {@link #releaseSessionBeanInstance} 来改变
	 * EJB 实例的创建方式，例如持有一个共享的 EJB 实例。
	 */
	@Override
	@Nullable
	public Object invokeInContext(MethodInvocation invocation) throws Throwable {
		Object ejb = null;
		try {
			ejb = getSessionBeanInstance();
			Method method = invocation.getMethod();
			if (method.getDeclaringClass().isInstance(ejb)) {
				// directly implemented
				return method.invoke(ejb, invocation.getArguments());
			}
			else {
				// not directly implemented
				Method ejbMethod = ejb.getClass().getMethod(method.getName(), method.getParameterTypes());
				return ejbMethod.invoke(ejb, invocation.getArguments());
			}
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (logger.isDebugEnabled()) {
				logger.debug("Method of local EJB [" + getJndiName() + "] threw exception", targetEx);
			}
			if (targetEx instanceof CreateException) {
				throw new EjbAccessException("Could not create local EJB [" + getJndiName() + "]", targetEx);
			}
			else {
				throw targetEx;
			}
		}
		catch (NamingException ex) {
			throw new EjbAccessException("Failed to locate local EJB [" + getJndiName() + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access method [" + invocation.getMethod().getName() +
				"] of local EJB [" + getJndiName() + "]", ex);
		}
		finally {
			if (ejb instanceof EJBLocalObject) {
				releaseSessionBeanInstance((EJBLocalObject) ejb);
			}
		}
	}

	/**
	 * 检查是否存在直接充当 EJB 组件的 EJB3 风格 home 对象。
	 */
	@Override
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		if (this.homeAsComponent) {
			return null;
		}
		if (!(home instanceof EJBLocalHome)) {
			// An EJB3 Session Bean...
			this.homeAsComponent = true;
			return null;
		}
		return super.getCreateMethod(home);
	}

	/**
	 * 返回用于委托调用的 EJB 实例。默认实现委托给 newSessionBeanInstance。
	 * @throws NamingException 如果由 JNDI 抛出
	 * @throws InvocationTargetException 如果由 create 方法抛出
	 * @see #newSessionBeanInstance
	 */
	protected Object getSessionBeanInstance() throws NamingException, InvocationTargetException {
		return newSessionBeanInstance();
	}

	/**
	 * 释放给定的 EJB 实例。默认实现委托给 removeSessionBeanInstance。
	 * @param ejb 要释放的 EJB 实例
	 * @see #removeSessionBeanInstance
	 */
	protected void releaseSessionBeanInstance(EJBLocalObject ejb) {
		removeSessionBeanInstance(ejb);
	}

	/**
	 * 返回无状态会话 Bean 的新实例。可以覆盖以改变算法。
	 * @throws NamingException 如果由 JNDI 抛出
	 * @throws InvocationTargetException 如果由 create 方法抛出
	 * @see #create
	 */
	protected Object newSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to create reference to local EJB");
		}
		Object ejbInstance = create();
		if (logger.isDebugEnabled()) {
			logger.debug("Obtained reference to local EJB: " + ejbInstance);
		}
		return ejbInstance;
	}

	/**
	 * 移除给定的 EJB 实例。
	 * @param ejb 要移除的 EJB 实例
	 * @see javax.ejb.EJBLocalObject#remove()
	 */
	protected void removeSessionBeanInstance(@Nullable EJBLocalObject ejb) {
		if (ejb != null && !this.homeAsComponent) {
			try {
				ejb.remove();
			}
			catch (Throwable ex) {
				logger.warn("Could not invoke 'remove' on local EJB proxy", ex);
			}
		}
	}

}
