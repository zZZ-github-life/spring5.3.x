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

package org.springframework.web.servlet.support;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * {@link org.springframework.web.WebApplicationInitializer} 实现的基类，这些实现会在 servlet 上下文中注册 {@link DispatcherServlet}。
 *
 * <p>大多数应用应考虑继承 Spring Java 配置子类 {@link AbstractAnnotationConfigDispatcherServletInitializer}。
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 */
public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

	/**
	 * 默认的 servlet 名称。可以通过重写 {@link #getServletName} 进行自定义。
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		registerDispatcherServlet(servletContext);
	}

	/**
	 * 针对给定的 servlet 上下文注册 {@link DispatcherServlet}。
	 * <p>此方法将使用 {@link #getServletName()} 返回的名称创建一个 {@code DispatcherServlet}，用 {@link #createServletApplicationContext()} 返回的应用上下文对其进行初始化，并将其映射到 {@link #getServletMappings()} 返回的模式。
	 * <p>可以通过重写 {@link #customizeRegistration(ServletRegistration.Dynamic)} 或 {@link #createDispatcherServlet(WebApplicationContext)} 实现进一步的定制。
	 * @param servletContext 要在其中注册该 servlet 的上下文
	 */
	protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext, "createServletApplicationContext() must not return null");

		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		Assert.notNull(dispatcherServlet, "createDispatcherServlet(WebApplicationContext) must not return null");
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		if (registration == null) {
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());
		registration.setAsyncSupported(isAsyncSupported());

		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

		customizeRegistration(registration);
	}

	/**
	 * 返回 {@link DispatcherServlet} 注册时使用的名称。默认为 {@link #DEFAULT_SERVLET_NAME}。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
	 * 创建要提供给 {@code DispatcherServlet} 的 servlet 应用上下文。
	 * <p>返回的上下文会委托给 Spring 的 {@link DispatcherServlet#DispatcherServlet(WebApplicationContext)}。因此，它通常包含控制器、视图解析器、区域设置解析器以及其他与 Web 相关的 bean。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * 使用指定的 {@link WebApplicationContext} 创建 {@link DispatcherServlet}（或其他类型的、派生自 {@link FrameworkServlet} 的 dispatcher）。
	 * <p>注意：从 4.2.3 开始，允许使用任何 {@link FrameworkServlet} 子类。在此之前，它要求返回 {@link DispatcherServlet} 或其子类。
	 */
	protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
		return new DispatcherServlet(servletAppContext);
	}

	/**
	 * 指定要应用于创建 {@code DispatcherServlet} 时所用 servlet 特定应用上下文的应用上下文初始化器。
	 * @since 4.2
	 * @see #createServletApplicationContext()
	 * @see DispatcherServlet#setContextInitializers
	 * @see #getRootApplicationContextInitializers()
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
		return null;
	}

	/**
	 * 指定 {@code DispatcherServlet} 的 servlet 映射 &mdash; 例如 {@code "/"}、{@code "/app"} 等。
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract String[] getServletMappings();

	/**
	 * 指定要添加并映射到 {@code DispatcherServlet} 的过滤器。
	 * @return 过滤器数组，或 {@code null}
	 * @see #registerServletFilter(ServletContext, Filter)
	 */
	@Nullable
	protected Filter[] getServletFilters() {
		return null;
	}

	/**
	 * 将给定过滤器添加到 ServletContext 并按如下方式将其映射到 {@code DispatcherServlet}：
	 * <ul>
	 * <li>根据其具体类型选择一个默认的过滤器名称
	 * <li>根据 {@link #isAsyncSupported() asyncSupported} 的返回值设置 {@code asyncSupported} 标志
	 * <li>创建一个过滤器映射，其 dispatcher 类型为 {@code REQUEST}、{@code FORWARD}、{@code INCLUDE}，并根据 {@link #isAsyncSupported() asyncSupported} 的返回值有条件地包含 {@code ASYNC}
	 * </ul>
	 * <p>如果上述默认设置不合适或不充分，请重写此方法并直接将过滤器注册到 {@code ServletContext}。
	 * @param servletContext 要在其中注册过滤器的 servlet 上下文
	 * @param filter 要注册的过滤器
	 * @return 过滤器注册对象
	 */
	protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
		String filterName = Conventions.getVariableName(filter);
		Dynamic registration = servletContext.addFilter(filterName, filter);

		if (registration == null) {
			int counter = 0;
			while (registration == null) {
				if (counter == 100) {
					throw new IllegalStateException("Failed to register filter with name '" + filterName + "'. " +
							"Check if there is another filter registered under the same name.");
				}
				registration = servletContext.addFilter(filterName + "#" + counter, filter);
				counter++;
			}
		}

		registration.setAsyncSupported(isAsyncSupported());
		registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return (isAsyncSupported() ?
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) :
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
	}

	/**
	 * 统一控制 {@code DispatcherServlet} 以及通过 {@link #getServletFilters()} 添加的所有过滤器的 {@code asyncSupported} 标志的地方。
	 * <p>默认值为 "true"。
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * 可选：在 {@link #registerDispatcherServlet(ServletContext)} 完成后执行进一步的注册定制。
	 * @param registration 要自定义的 {@code DispatcherServlet} 注册对象
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
