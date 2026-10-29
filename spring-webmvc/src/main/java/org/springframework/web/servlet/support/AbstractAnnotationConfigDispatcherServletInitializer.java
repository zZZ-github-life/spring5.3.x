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

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}，用于注册 {@code DispatcherServlet} 并使用基于 Java 的 Spring 配置。
 *
 * <p>实现类必须实现：
 * <ul>
 * <li>{@link #getRootConfigClasses()} -- 用于 "root" 应用上下文（非 Web 基础设施）的配置。
 * <li>{@link #getServletConfigClasses()} -- 用于 {@code DispatcherServlet} 应用上下文（Spring MVC 基础设施）的配置。
 * </ul>
 *
 * <p>如果不需要应用上下文层次结构，应用可以通过 {@link #getRootConfigClasses()} 返回所有配置，并从 {@link #getServletConfigClasses()} 返回 {@code null}。
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.2
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer
		extends AbstractDispatcherServletInitializer {

	/**
	 * {@inheritDoc}
	 * <p>此实现会创建一个 {@link AnnotationConfigWebApplicationContext}，并向其提供 {@link #getRootConfigClasses()} 返回的注解类。如果 {@link #getRootConfigClasses()} 返回 {@code null}，则返回 {@code null}。
	 */
	@Override
	@Nullable
	protected WebApplicationContext createRootApplicationContext() {
		Class<?>[] configClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
			context.register(configClasses);
			return context;
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>此实现会创建一个 {@link AnnotationConfigWebApplicationContext}，并向其提供 {@link #getServletConfigClasses()} 返回的注解类。
	 */
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		Class<?>[] configClasses = getServletConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			context.register(configClasses);
		}
		return context;
	}

	/**
	 * 为{@linkplain #createRootApplicationContext() 根应用上下文}指定 {@code @Configuration} 和/或 {@code @Component} 类。
	 * @return the configuration for the root application context, or {@code null}
	 * if creation and registration of a root context is not desired
	 */
	@Nullable
	protected abstract Class<?>[] getRootConfigClasses();

	/**
	 * 为{@linkplain #createServletApplicationContext() Servlet 应用上下文}指定 {@code @Configuration} 和/或 {@code @Component} 类。
	 * @return the configuration for the Servlet application context, or
	 * {@code null} if all configuration is specified through root config classes.
	 */
	@Nullable
	protected abstract Class<?>[] getServletConfigClasses();

}
