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

package org.springframework.jndi;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 简化 JNDI 操作的辅助类。它提供了查找和绑定对象的方法，并允许 {@link JndiCallback} 接口的实现利用
 * 所提供的 JNDI 命名上下文执行任意操作。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JndiCallback
 * @see #execute
 */
public class JndiTemplate {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Properties environment;


	/**
	 * 创建一个新的 JndiTemplate 实例。
	 */
	public JndiTemplate() {
	}

	/**
	 * 使用给定的环境创建一个新的 JndiTemplate 实例。
	 */
	public JndiTemplate(@Nullable Properties environment) {
		this.environment = environment;
	}


	/**
	 * 设置 JNDI InitialContext 的环境。
	 */
	public void setEnvironment(@Nullable Properties environment) {
		this.environment = environment;
	}

	/**
	 * 返回 JNDI InitialContext 的环境（如果有）。
	 */
	@Nullable
	public Properties getEnvironment() {
		return this.environment;
	}


	/**
	 * 执行给定的 JNDI 上下文回调实现。
	 * @param contextCallback the JndiCallback implementation to use
	 * @return a result object returned by the callback, or {@code null}
	 * @throws NamingException thrown by the callback implementation
	 * @see #createInitialContext
	 */
	@Nullable
	public <T> T execute(JndiCallback<T> contextCallback) throws NamingException {
		Context ctx = getContext();
		try {
			return contextCallback.doInContext(ctx);
		}
		finally {
			releaseContext(ctx);
		}
	}

	/**
	 * 获取与此模板配置对应的 JNDI 上下文。由 {@link #execute} 调用；也可以直接调用。
	 * <p>默认实现委托给 {@link #createInitialContext()}。
	 * @return the JNDI context (never {@code null})
	 * @throws NamingException if context retrieval failed
	 * @see #releaseContext
	 */
	public Context getContext() throws NamingException {
		return createInitialContext();
	}

	/**
	 * 释放从 {@link #getContext()} 获取的 JNDI 上下文。
	 * @param ctx the JNDI context to release (may be {@code null})
	 * @see #getContext
	 */
	public void releaseContext(@Nullable Context ctx) {
		if (ctx != null) {
			try {
				ctx.close();
			}
			catch (NamingException ex) {
				logger.debug("Could not close JNDI InitialContext", ex);
			}
		}
	}

	/**
	 * 创建一个新的 JNDI 初始上下文。由 {@link #getContext} 调用。
	 * <p>默认实现使用此模板的环境设置。可以为自定义上下文（例如用于测试）而子类化。
	 * @return the initial Context instance
	 * @throws NamingException in case of initialization errors
	 */
	protected Context createInitialContext() throws NamingException {
		Hashtable<?, ?> icEnv = null;
		Properties env = getEnvironment();
		if (env != null) {
			icEnv = new Hashtable<>(env.size());
			CollectionUtils.mergePropertiesIntoMap(env, icEnv);
		}
		return new InitialContext(icEnv);
	}


	/**
	 * 在当前 JNDI 上下文中查找具有给定名称的对象。
	 * @param name the JNDI name of the object
	 * @return object found (cannot be {@code null}; if a not so well-behaved
	 * JNDI implementations returns null, a NamingException gets thrown)
	 * @throws NamingException if there is no object with the given
	 * name bound to JNDI
	 */
	public Object lookup(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up JNDI object with name [" + name + "]");
		}
		Object result = execute(ctx -> ctx.lookup(name));
		if (result == null) {
			throw new NameNotFoundException(
					"JNDI object with [" + name + "] not found: JNDI implementation returned null");
		}
		return result;
	}

	/**
	 * 在当前 JNDI 上下文中查找具有给定名称的对象。
	 * @param name the JNDI name of the object
	 * @param requiredType type the JNDI object must match. Can be an interface or
	 * superclass of the actual class, or {@code null} for any match. For example,
	 * if the value is {@code Object.class}, this method will succeed whatever
	 * the class of the returned instance.
	 * @return object found (cannot be {@code null}; if a not so well-behaved
	 * JNDI implementations returns null, a NamingException gets thrown)
	 * @throws NamingException if there is no object with the given
	 * name bound to JNDI
	 */
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name, @Nullable Class<T> requiredType) throws NamingException {
		Object jndiObject = lookup(name);
		if (requiredType != null && !requiredType.isInstance(jndiObject)) {
			throw new TypeMismatchNamingException(name, requiredType, jndiObject.getClass());
		}
		return (T) jndiObject;
	}

	/**
	 * 使用给定名称将给定对象绑定到当前 JNDI 上下文。
	 * @param name the JNDI name of the object
	 * @param object the object to bind
	 * @throws NamingException thrown by JNDI, mostly name already bound
	 */
	public void bind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.bind(name, object);
			return null;
		});
	}

	/**
	 * 使用给定名称将给定对象重新绑定到当前 JNDI 上下文。会覆盖任何现有的绑定。
	 * @param name the JNDI name of the object
	 * @param object the object to rebind
	 * @throws NamingException thrown by JNDI
	 */
	public void rebind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Rebinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.rebind(name, object);
			return null;
		});
	}

	/**
	 * 从当前 JNDI 上下文中移除给定名称的绑定。
	 * @param name the JNDI name of the object
	 * @throws NamingException thrown by JNDI, mostly name not found
	 */
	public void unbind(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.unbind(name);
			return null;
		});
	}

}
