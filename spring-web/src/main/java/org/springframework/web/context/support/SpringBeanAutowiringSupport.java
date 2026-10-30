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

package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * 在基于 Spring 的 Web 应用中构造的、用于自自动装配（self-autowiring）类的便捷基类。会根据当前
 * Spring 根 Web 应用上下文（由当前线程的上下文 ClassLoader 确定，该 ClassLoader 需要是 Web 应用
 * 的 ClassLoader）中的 bean，解析端点类中的 {@code @Autowired} 注解。也可以作为委托使用，而不是
 * 作为基类。
 *
 * <p>此基类的一个典型用法是 JAX-WS 端点类：这种基于 Spring 的 JAX-WS 端点实现会遵循端点类的标准
 * JAX-WS 约定，但会保持“轻量”，即将实际工作委托给一个或多个由 Spring 管理的服务 bean —— 通常通过
 * {@code @Autowired} 获取。此类端点实例的生命周期由 JAX-WS 运行时管理，因此需要此基类基于当前
 * Spring 上下文提供 {@code @Autowired} 处理。
 *
 * <p><b>注意：</b>如果存在访问 ServletContext 的显式方式，请优先使用该方式，而不是使用此类。
 * {@link WebApplicationContextUtils} 类允许根据 ServletContext 轻松访问 Spring 根 Web 应用上下文。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see WebApplicationObjectSupport
 */
public abstract class SpringBeanAutowiringSupport {

	private static final Log logger = LogFactory.getLog(SpringBeanAutowiringSupport.class);


	/**
	 * 此构造函数会基于当前 Web 应用上下文对此实例执行注入。
	 * <p>用于作为基类。
	 * @see #processInjectionBasedOnCurrentContext
	 */
	public SpringBeanAutowiringSupport() {
		processInjectionBasedOnCurrentContext(this);
	}


	/**
	 * 基于当前 Web 应用上下文，为给定目标对象处理 {@code @Autowired} 注入。
	 * <p>用于作为委托。
	 * @param target 要处理的目标对象
	 * @see org.springframework.web.context.ContextLoader#getCurrentWebApplicationContext()
	 */
	public static void processInjectionBasedOnCurrentContext(Object target) {
		Assert.notNull(target, "Target object must not be null");
		WebApplicationContext cc = ContextLoader.getCurrentWebApplicationContext();
		if (cc != null) {
			AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
			bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());
			bpp.processInjection(target);
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("Current WebApplicationContext is not available for processing of " +
						ClassUtils.getShortName(target.getClass()) + ": " +
						"Make sure this class gets constructed in a Spring web application after the " +
						"Spring WebApplicationContext has been initialized. Proceeding without injection.");
			}
		}
	}


	/**
	 * 基于存储在 ServletContext 中的当前根 Web 应用上下文，为给定目标对象处理 {@code @Autowired} 注
	 * 入。
	 * <p>用于作为委托。
	 * @param target 要处理的目标对象
	 * @param servletContext 要在其中查找 Spring Web 应用上下文的 ServletContext
	 * @see WebApplicationContextUtils#getWebApplicationContext(javax.servlet.ServletContext)
	 */
	public static void processInjectionBasedOnServletContext(Object target, ServletContext servletContext) {
		Assert.notNull(target, "Target object must not be null");
		WebApplicationContext cc = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());
		bpp.processInjection(target);
	}

}
