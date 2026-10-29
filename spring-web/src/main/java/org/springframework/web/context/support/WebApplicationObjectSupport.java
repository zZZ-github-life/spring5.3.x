/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.File;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.WebUtils;

/**
 * 在 {@link WebApplicationContext} 中运行的应用对象的便捷父类。
 * 提供 {@code getWebApplicationContext()}、{@code getServletContext()} 和
 * {@code getTempDir()} 访问器。
 *
 * <p>注意：通常建议针对实际需要的回调使用单独的回调接口。
 * 这个宽泛的基类主要面向框架内部使用，适用于通常需要访问
 * {@link ServletContext} 等场景。
 *
 * @author Juergen Hoeller
 * @since 28.08.2003
 * @see SpringBeanAutowiringSupport
 */
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {

	@Nullable
	private ServletContext servletContext;


	@Override
	public final void setServletContext(ServletContext servletContext) {
		if (servletContext != this.servletContext) {
			this.servletContext = servletContext;
			initServletContext(servletContext);
		}
	}

	/**
	 * 重写基类行为，强制要求在 ApplicationContext 中运行。
	 * 若未在上下文中运行，所有访问器都会抛出 IllegalStateException。
	 * @see #getApplicationContext()
	 * @see #getMessageSourceAccessor()
	 * @see #getWebApplicationContext()
	 * @see #getServletContext()
	 * @see #getTempDir()
	 */
	@Override
	protected boolean isContextRequired() {
		return true;
	}

	/**
	 * 如果给定的 ApplicationContext 是 {@link WebApplicationContext}，
	 * 则调用 {@link #initServletContext(javax.servlet.ServletContext)}。
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		if (this.servletContext == null && context instanceof WebApplicationContext) {
			this.servletContext = ((WebApplicationContext) context).getServletContext();
			if (this.servletContext != null) {
				initServletContext(this.servletContext);
			}
		}
	}

	/**
	 * 子类可以重写此方法，基于该应用对象所运行的 ServletContext 执行自定义初始化。
	 * <p>默认实现为空。由
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}
	 * 以及 {@link #setServletContext(javax.servlet.ServletContext)} 调用。
	 * @param servletContext the ServletContext that this application object runs in
	 * (never {@code null})
	 */
	protected void initServletContext(ServletContext servletContext) {
	}

	/**
	 * 将当前应用上下文作为 WebApplicationContext 返回。
	 * <p><b>注意：</b>仅当你确实需要访问 WebApplicationContext 特有功能时才使用此方法。
	 * 其他情况下最好使用 {@code getApplicationContext()} 或
	 * {@code getServletContext()}，以便同样能够在非 WebApplicationContext
	 * 环境中运行。
	 * @throws IllegalStateException if not running in a WebApplicationContext
	 * @see #getApplicationContext()
	 */
	@Nullable
	protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
		ApplicationContext ctx = getApplicationContext();
		if (ctx instanceof WebApplicationContext) {
			return (WebApplicationContext) getApplicationContext();
		}
		else if (isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run in a WebApplicationContext but in: " + ctx);
		}
		else {
			return null;
		}
	}

	/**
	 * 返回当前的 ServletContext。
	 * @throws IllegalStateException if not running within a required ServletContext
	 * @see #isContextRequired()
	 */
	@Nullable
	protected final ServletContext getServletContext() throws IllegalStateException {
		if (this.servletContext != null) {
			return this.servletContext;
		}
		ServletContext servletContext = null;
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			servletContext = wac.getServletContext();
		}
		if (servletContext == null && isContextRequired()) {
			throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
					"] does not run within a ServletContext. Make sure the object is fully configured!");
		}
		return servletContext;
	}

	/**
	 * 返回当前 Web 应用的临时目录，由 Servlet 容器提供。
	 * @return the File representing the temporary directory
	 * @throws IllegalStateException if not running within a ServletContext
	 * @see org.springframework.web.util.WebUtils#getTempDir(javax.servlet.ServletContext)
	 */
	protected final File getTempDir() throws IllegalStateException {
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "ServletContext is required");
		return WebUtils.getTempDir(servletContext);
	}

}
