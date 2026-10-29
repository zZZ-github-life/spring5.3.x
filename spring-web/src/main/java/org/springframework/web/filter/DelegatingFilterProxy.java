/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 标准 Servlet Filter 的代理，委托给实现 Filter 接口的 Spring 管理 Bean。
 * 支持在 {@code web.xml} 中通过 "targetBeanName" 过滤器初始化参数指定
 * Spring 应用上下文中目标 Bean 的名称。
 *
 * <p>{@code web.xml} 中通常会包含一个 {@code DelegatingFilterProxy} 定义，
 * 其中指定的 {@code filter-name} 对应于 Spring 根应用上下文中的 Bean 名称。
 * 之后，所有对过滤器代理的调用都会被委托给 Spring 上下文中的那个 Bean，
 * 该 Bean 必须实现标准的 Servlet Filter 接口。
 *
 * <p>这种方式对于初始化需求复杂的 Filter 实现特别有用，可以将完整的 Spring Bean
 * 定义机制应用于 Filter 实例。另一种选择是采用标准的 Filter 设置，
 * 并结合从 Spring 根应用上下文中查找服务 Bean。
 *
 * <p><b>注意：</b>默认情况下，Servlet Filter 接口定义的生命周期方法<i>不会</i>
 * 被委托给目标 Bean，而是依赖 Spring 应用上下文管理该 Bean 的生命周期。
 * 将 "targetFilterLifecycle" 过滤器初始化参数指定为 "true" 会强制在目标 Bean 上
 * 调用 {@code Filter.init} 和 {@code Filter.destroy} 生命周期方法，
 * 从而让 Servlet 容器管理过滤器生命周期。
 *
 * <p>从 Spring 3.1 开始，{@code DelegatingFilterProxy} 已更新为在使用 Servlet
 * 容器基于实例的过滤器注册方法时可以选择接受构造参数，通常与 Spring 的
 * {@link org.springframework.web.WebApplicationInitializer} SPI 配合使用。
 * 这些构造函数允许直接提供委托 Filter Bean，或者提供应用上下文和要获取的
 * Bean 名称，从而避免从 ServletContext 查找应用上下文。
 *
 * <p>此类最初受到 Ben Alex 编写的 Spring Security {@code FilterToBeanProxy}
 * 类的启发。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @since 1.2
 * @see #setTargetBeanName
 * @see #setTargetFilterLifecycle
 * @see javax.servlet.Filter#doFilter
 * @see javax.servlet.Filter#init
 * @see javax.servlet.Filter#destroy
 * @see #DelegatingFilterProxy(Filter)
 * @see #DelegatingFilterProxy(String)
 * @see #DelegatingFilterProxy(String, WebApplicationContext)
 * @see javax.servlet.ServletContext#addFilter(String, Filter)
 * @see org.springframework.web.WebApplicationInitializer
 */
public class DelegatingFilterProxy extends GenericFilterBean {

	@Nullable
	private String contextAttribute;

	@Nullable
	private WebApplicationContext webApplicationContext;

	@Nullable
	private String targetBeanName;

	private boolean targetFilterLifecycle = false;

	@Nullable
	private volatile Filter delegate;

	private final Object delegateMonitor = new Object();


	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}。用于在 {@code web.xml} 中的传统用法。
	 * @see #setTargetBeanName(String)
	 */
	public DelegatingFilterProxy() {
	}

	/**
	 * 使用给定的 {@link Filter} 委托创建一个新的 {@code DelegatingFilterProxy}。
	 * 完全无需与 Spring 应用上下文交互、指定
	 * {@linkplain #setTargetBeanName 目标 Bean 名称}等。
	 * <p>用于基于实例的过滤器注册。
	 * @param delegate the {@code Filter} instance that this proxy will delegate to and
	 * manage the lifecycle for (must not be {@code null}).
	 * @see #doFilter(ServletRequest, ServletResponse, FilterChain)
	 * @see #invokeDelegate(Filter, ServletRequest, ServletResponse, FilterChain)
	 * @see #destroy()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(Filter delegate) {
		Assert.notNull(delegate, "Delegate Filter must not be null");
		this.delegate = delegate;
	}

	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}，它将从 {@code ServletContext}
	 * 中找到的 Spring {@code WebApplicationContext}（“根”应用上下文，或由
	 * {@link #setContextAttribute} 命名的上下文）中获取指定名称的目标 Bean。
	 * <p>用于基于实例的过滤器注册。
	 * <p>目标 Bean 必须实现标准的 Servlet Filter 接口。
	 * @param targetBeanName name of the target filter bean to look up in the Spring
	 * application context (must not be {@code null}).
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName) {
		this(targetBeanName, null);
	}

	/**
	 * 创建一个新的 {@code DelegatingFilterProxy}，它将从给定的 Spring
	 * {@code WebApplicationContext} 中获取指定名称的目标 Bean。
	 * <p>用于基于实例的过滤器注册。
	 * <p>目标 Bean 必须实现标准的 Servlet Filter 接口。
	 * <p>传入的 {@code WebApplicationContext} 可能已刷新，也可能尚未刷新。
	 * 如果尚未刷新，并且该上下文实现了 {@link ConfigurableApplicationContext}，
	 * 则在获取指定名称的目标 Bean 之前会尝试调用
	 * {@link ConfigurableApplicationContext#refresh() refresh()}。
	 * <p>此代理的 {@code Environment} 将从给定的
	 * {@code WebApplicationContext} 继承。
	 * @param targetBeanName name of the target filter bean in the Spring application
	 * context (must not be {@code null}).
	 * @param wac the application context from which the target filter will be retrieved;
	 * if {@code null}, an application context will be looked up from {@code ServletContext}
	 * as a fallback.
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName, @Nullable WebApplicationContext wac) {
		Assert.hasText(targetBeanName, "Target Filter bean name must not be null or empty");
		this.setTargetBeanName(targetBeanName);
		this.webApplicationContext = wac;
		if (wac != null) {
			this.setEnvironment(wac.getEnvironment());
		}
	}

	/**
	 * 设置应用于从其中加载委托 {@link Filter} Bean 的 ServletContext 属性名，
	 * 该属性保存着 {@link WebApplicationContext}。
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回应用于从其中加载委托 {@link Filter} Bean 的 ServletContext 属性名，
	 * 该属性保存着 {@link WebApplicationContext}。
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置目标 Bean 在 Spring 应用上下文中的名称。
	 * 目标 Bean 必须实现标准的 Servlet Filter 接口。
	 * <p>默认情况下，将使用在 {@code web.xml} 中为 DelegatingFilterProxy
	 * 指定的 {@code filter-name}。
	 */
	public void setTargetBeanName(@Nullable String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 返回目标 Bean 在 Spring 应用上下文中的名称。
	 */
	@Nullable
	protected String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * 设置是否在目标 Bean 上调用 {@code Filter.init} 和
	 * {@code Filter.destroy} 生命周期方法。
	 * <p>默认值为 "false"；目标 Bean 通常依赖 Spring 应用上下文管理其生命周期。
	 * 将此标志设置为 "true" 意味着 Servlet 容器将控制目标 Filter 的生命周期，
	 * 并由该代理委托相应的调用。
	 */
	public void setTargetFilterLifecycle(boolean targetFilterLifecycle) {
		this.targetFilterLifecycle = targetFilterLifecycle;
	}

	/**
	 * 返回是否在目标 Bean 上调用 {@code Filter.init} 和
	 * {@code Filter.destroy} 生命周期方法。
	 */
	protected boolean isTargetFilterLifecycle() {
		return this.targetFilterLifecycle;
	}


	@Override
	protected void initFilterBean() throws ServletException {
		synchronized (this.delegateMonitor) {
			if (this.delegate == null) {
				// If no target bean name specified, use filter name.
				if (this.targetBeanName == null) {
					this.targetBeanName = getFilterName();
				}
				// Fetch Spring root application context and initialize the delegate early,
				// if possible. If the root application context will be started after this
				// filter proxy, we'll have to resort to lazy initialization.
				WebApplicationContext wac = findWebApplicationContext();
				if (wac != null) {
					this.delegate = initDelegate(wac);
				}
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Lazily initialize the delegate if necessary.
		Filter delegateToUse = this.delegate;
		if (delegateToUse == null) {
			synchronized (this.delegateMonitor) {
				delegateToUse = this.delegate;
				if (delegateToUse == null) {
					WebApplicationContext wac = findWebApplicationContext();
					if (wac == null) {
						throw new IllegalStateException("No WebApplicationContext found: " +
								"no ContextLoaderListener or DispatcherServlet registered?");
					}
					delegateToUse = initDelegate(wac);
				}
				this.delegate = delegateToUse;
			}
		}

		// Let the delegate perform the actual doFilter operation.
		invokeDelegate(delegateToUse, request, response, filterChain);
	}

	@Override
	public void destroy() {
		Filter delegateToUse = this.delegate;
		if (delegateToUse != null) {
			destroyDelegate(delegateToUse);
		}
	}


	/**
	 * 返回构造时传入的 {@code WebApplicationContext}（如果可用）。
	 * 否则，如果设置了 {@linkplain #setContextAttribute 配置的名称}，
	 * 则尝试从该名称对应的 {@code ServletContext} 属性中获取
	 * {@code WebApplicationContext}。再否则，在众所周知的“根”应用上下文属性下
	 * 查找 {@code WebApplicationContext}。在此过滤器初始化（或被调用）之前，
	 * {@code WebApplicationContext} 必须已加载并存储在 {@code ServletContext} 中。
	 * <p>子类可以重写此方法以提供不同的 {@code WebApplicationContext} 获取策略。
	 * @return the {@code WebApplicationContext} for this proxy, or {@code null} if not found
	 * @see #DelegatingFilterProxy(String, WebApplicationContext)
	 * @see #getContextAttribute()
	 * @see WebApplicationContextUtils#getWebApplicationContext(javax.servlet.ServletContext)
	 * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		if (this.webApplicationContext != null) {
			// The user has injected a context at construction time -> use it...
			if (this.webApplicationContext instanceof ConfigurableApplicationContext) {
				ConfigurableApplicationContext cac = (ConfigurableApplicationContext) this.webApplicationContext;
				if (!cac.isActive()) {
					// The context has not yet been refreshed -> do so before returning it...
					cac.refresh();
				}
			}
			return this.webApplicationContext;
		}
		String attrName = getContextAttribute();
		if (attrName != null) {
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		}
		else {
			return WebApplicationContextUtils.findWebApplicationContext(getServletContext());
		}
	}

	/**
	 * 初始化 Filter 委托，即在给定的 Spring 应用上下文中定义的 Bean。
	 * <p>默认实现从应用上下文中获取该 Bean，并在其上调用标准的
	 * {@code Filter.init} 方法，传入此 Filter 代理的 FilterConfig。
	 * @param wac the root application context
	 * @return the initialized delegate Filter
	 * @throws ServletException if thrown by the Filter
	 * @see #getTargetBeanName()
	 * @see #isTargetFilterLifecycle()
	 * @see #getFilterConfig()
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
		String targetBeanName = getTargetBeanName();
		Assert.state(targetBeanName != null, "No target bean name set");
		Filter delegate = wac.getBean(targetBeanName, Filter.class);
		if (isTargetFilterLifecycle()) {
			delegate.init(getFilterConfig());
		}
		return delegate;
	}

	/**
	 * 使用给定的请求和响应实际调用委托 Filter。
	 * @param delegate the delegate Filter
	 * @param request the current HTTP request
	 * @param response the current HTTP response
	 * @param filterChain the current FilterChain
	 * @throws ServletException if thrown by the Filter
	 * @throws IOException if thrown by the Filter
	 */
	protected void invokeDelegate(
			Filter delegate, ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		delegate.doFilter(request, response, filterChain);
	}

	/**
	 * 销毁 Filter 委托。
	 * 默认实现仅在其上调用 {@code Filter.destroy}。
	 * @param delegate the Filter delegate (never {@code null})
	 * @see #isTargetFilterLifecycle()
	 * @see javax.servlet.Filter#destroy()
	 */
	protected void destroyDelegate(Filter delegate) {
		if (isTargetFilterLifecycle()) {
			delegate.destroy();
		}
	}

}
