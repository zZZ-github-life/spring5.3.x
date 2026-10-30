/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.context.support;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * ServletContext 的 {@link Scope} 包装器，即用于全局 Web 应用属性。
 *
 * <p>与传统 Spring 单例不同，此作用域将属性暴露在 ServletContext 中。
 * 这些属性会在整个应用关闭时销毁，这可能会早于或晚于包含它们的 Spring
 * ApplicationContext 的关闭。
 *
 * <p>相关的销毁机制依赖于在 {@code web.xml} 中注册的
 * {@link org.springframework.web.context.ContextCleanupListener}。请注意，
 * {@link org.springframework.web.context.ContextLoaderListener}
 * 已包含 ContextCleanupListener 的功能。
 *
 * <p>此作用域以键
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}
 * 注册为默认作用域。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.context.ContextCleanupListener
 */
public class ServletContextScope implements Scope, DisposableBean {

	private final ServletContext servletContext;

	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();


	/**
	 * 为给定的 ServletContext 创建一个新的 Scope 包装器。
	 * @param servletContext 要包装的 ServletContext
	 */
	public ServletContextScope(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			this.servletContext.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject != null) {
			synchronized (this.destructionCallbacks) {
				this.destructionCallbacks.remove(name);
			}
			this.servletContext.removeAttribute(name);
			return scopedObject;
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (this.destructionCallbacks) {
			this.destructionCallbacks.put(name, callback);
		}
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	@Nullable
	public String getConversationId() {
		return null;
	}


	/**
	 * 调用所有已注册的销毁回调。
	 * 在 ServletContext 关闭时调用。
	 * @see org.springframework.web.context.ContextCleanupListener
	 */
	@Override
	public void destroy() {
		synchronized (this.destructionCallbacks) {
			for (Runnable runnable : this.destructionCallbacks.values()) {
				runnable.run();
			}
			this.destructionCallbacks.clear();
		}
	}

}
