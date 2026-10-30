/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.jsf;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * 便捷方法，用于为给定的 JSF {@link FacesContext} 获取 Spring 的根
 * {@link WebApplicationContext}。这对于从基于 JSF 的自定义代码访问 Spring
 * 应用上下文非常有用。
 *
 * <p>类似于 Spring 针对 ServletContext 的 WebApplicationContextUtils。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.context.support.WebApplicationContextUtils
 */
public abstract class FacesContextUtils {

	/**
	 * 查找此 Web 应用的根 {@link WebApplicationContext}，通常通过
	 * {@link org.springframework.web.context.ContextLoaderListener} 加载。
	 * <p>将重新抛出根上下文启动时发生的异常，
	 * 以区分上下文启动失败和完全没有上下文两种情况。
	 * @param fc 要为其查找 Web 应用上下文的 FacesContext
	 * @return 此 Web 应用的根 WebApplicationContext，如果没有则为 {@code null}
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		Object attr = fc.getExternalContext().getApplicationMap().get(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}

	/**
	 * 查找此 Web 应用的根 {@link WebApplicationContext}，通常通过
	 * {@link org.springframework.web.context.ContextLoaderListener} 加载。
	 * <p>将重新抛出根上下文启动时发生的异常，
	 * 以区分上下文启动失败和完全没有上下文两种情况。
	 * @param fc 要为其查找 Web 应用上下文的 FacesContext
	 * @return 此 Web 应用的根 WebApplicationContext
	 * @throws IllegalStateException 如果找不到根 WebApplicationContext
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(FacesContext fc) throws IllegalStateException {
		WebApplicationContext wac = getWebApplicationContext(fc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * 返回给定会话的最佳可用互斥锁：即用于对给定会话进行同步的对象。
	 * <p>如果会话互斥属性可用则返回它；通常这意味着需要在 {@code web.xml} 中定义
	 * HttpSessionMutexListener。如果未找到互斥属性，则回退到 Session 引用本身。
	 * <p>会话互斥锁保证在整个会话生命周期内都是同一个对象，可通过
	 * {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键获取。它可作为对当前会话加锁时
	 * 用于同步的安全引用。
	 * <p>在许多情况下，Session 引用本身也是安全的互斥锁，
	 * 因为对于同一个活动逻辑会话，它始终是同一个对象引用。不过，这在不同 Servlet
	 * 容器之间并无保证；唯一 100% 安全的方式是使用会话互斥锁。
	 * @param fc 要为其查找会话互斥体的 FacesContext
	 * @return 互斥体对象（永不为 {@code null}）
	 * @see org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 */
	@Nullable
	public static Object getSessionMutex(FacesContext fc) {
		Assert.notNull(fc, "FacesContext must not be null");
		ExternalContext ec = fc.getExternalContext();
		Object mutex = ec.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = ec.getSession(true);
		}
		return mutex;
	}

}
