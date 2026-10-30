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

package org.springframework.web.context.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.context.request.WebRequest;

/**
 * 便捷方法，用于获取给定 {@link ServletContext} 的根 {@link WebApplicationContext}。
 * 这对于在自定义 Web 视图或 MVC 动作中以编程方式访问 Spring 应用上下文很有用。
 *
 * <p>注意，许多 Web 框架（无论是 Spring 的一部分，还是作为外部库提供）都提供了
 * 更方便的根上下文访问方式。此辅助类只是访问根上下文最通用的方式。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.jsf.FacesContextUtils
 * @see org.springframework.web.jsf.el.SpringBeanFacesELResolver
 */
public abstract class WebApplicationContextUtils {

	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());


	/**
	 * 查找此 Web 应用的根 {@code WebApplicationContext}，通常通过
	 * {@link org.springframework.web.context.ContextLoaderListener} 加载。
	 * <p>会重新抛出根上下文启动时发生的异常，
	 * 以区分上下文启动失败和根本没有上下文的情况。
	 * @param sc 要为其查找 Web 应用上下文的 ServletContext
	 * @return 此 Web 应用的根 WebApplicationContext
	 * @throws IllegalStateException 如果找不到根 WebApplicationContext
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc) throws IllegalStateException {
		WebApplicationContext wac = getWebApplicationContext(sc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * 查找此 Web 应用的根 {@code WebApplicationContext}，通常通过
	 * {@link org.springframework.web.context.ContextLoaderListener} 加载。
	 * <p>会重新抛出根上下文启动时发生的异常，
	 * 以区分上下文启动失败和根本没有上下文的情况。
	 * @param sc 要为其查找 Web 应用上下文的 ServletContext
	 * @return 此 Web 应用的根 WebApplicationContext，如果没有则为 {@code null}
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
		return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 查找此 Web 应用的自定义 {@code WebApplicationContext}。
	 * @param sc 要为其查找 Web 应用上下文的 ServletContext
	 * @param attrName 要查找的 ServletContext 属性的名称
	 * @return 此 Web 应用所需的 WebApplicationContext，如果没有则为 {@code null}
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
		Assert.notNull(sc, "ServletContext must not be null");
		Object attr = sc.getAttribute(attrName);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (attr instanceof Exception) {
			throw new IllegalStateException((Exception) attr);
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}

	/**
	 * 为此 Web 应用查找唯一的 {@code WebApplicationContext}：优先使用根 Web 应用上下文，
	 * 或者在已注册的 {@code ServletContext} 属性中查找唯一的 {@code WebApplicationContext}
	 * （通常来自当前 Web 应用中的单个 {@code DispatcherServlet}）。
	 * <p>注意，{@code DispatcherServlet} 是否暴露其上下文可以通过其 {@code publishContext}
	 * 属性控制，该属性默认为 {@code true}，但即使 Web 应用中注册了多个
	 * {@code DispatcherServlet}，也可以有选择地切换为只发布单个上下文。
	 * @param sc 要为其查找 Web 应用上下文的 ServletContext
	 * @return 此 Web 应用所需的 WebApplicationContext，如果没有则为 {@code null}
	 * @since 4.2
	 * @see #getWebApplicationContext(ServletContext)
	 * @see ServletContext#getAttributeNames()
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
		WebApplicationContext wac = getWebApplicationContext(sc);
		if (wac == null) {
			Enumeration<String> attrNames = sc.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = sc.getAttribute(attrName);
				if (attrValue instanceof WebApplicationContext) {
					if (wac != null) {
						throw new IllegalStateException("No unique WebApplicationContext found: more than one " +
								"DispatcherServlet registered with publishContext=true?");
					}
					wac = (WebApplicationContext) attrValue;
				}
			}
		}
		return wac;
	}


	/**
	 * 向给定的 BeanFactory 注册 Web 特定的作用域（"request"、"session"、"globalSession"），供
	 * WebApplicationContext 使用。
	 * @param beanFactory 要配置的 BeanFactory
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		registerWebApplicationScopes(beanFactory, null);
	}

	/**
	 * 向给定的 BeanFactory 注册 Web 特定的作用域（"request"、"session"、"globalSession"、
	 * "application"），供 WebApplicationContext 使用。
	 * @param beanFactory 要配置的 BeanFactory
	 * @param sc 我们运行所在的 ServletContext
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory,
			@Nullable ServletContext sc) {

		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope());
		if (sc != null) {
			ServletContextScope appScope = new ServletContextScope(sc);
			beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// Register as ServletContext attribute, for ContextCleanupListener to detect it.
			sc.setAttribute(ServletContextScope.class.getName(), appScope);
		}

		beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
		beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());
		beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
		beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
		if (jsfPresent) {
			FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
		}
	}

	/**
	 * 向给定的 BeanFactory 注册 Web 特定的环境 bean（"contextParameters"、"contextAttributes"），供
	 * WebApplicationContext 使用。
	 * @param bf 要配置的 BeanFactory
	 * @param sc 我们运行所在的 ServletContext
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, @Nullable ServletContext sc) {
		registerEnvironmentBeans(bf, sc, null);
	}

	/**
	 * 向给定的 BeanFactory 注册 Web 特定的环境 bean（"contextParameters"、"contextAttributes"），供
	 * WebApplicationContext 使用。
	 * @param bf 要配置的 BeanFactory
	 * @param servletContext 我们运行所在的 ServletContext
	 * @param servletConfig ServletConfig 实例
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf,
			@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

		if (servletContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
		}

		if (servletConfig != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
			bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, servletConfig);
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<>();
			if (servletContext != null) {
				Enumeration<?> paramNameEnum = servletContext.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, servletContext.getInitParameter(paramName));
				}
			}
			if (servletConfig != null) {
				Enumeration<?> paramNameEnum = servletConfig.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, servletConfig.getInitParameter(paramName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			Map<String, Object> attributeMap = new HashMap<>();
			if (servletContext != null) {
				Enumeration<?> attrNameEnum = servletContext.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = (String) attrNameEnum.nextElement();
					attributeMap.put(attrName, servletContext.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * {@link #initServletPropertySources(MutablePropertySources,
	 * ServletContext, ServletConfig)} 的便捷变体，始终为
	 * {@link ServletConfig} 参数提供 {@code null}。
	 * @see #initServletPropertySources(MutablePropertySources, ServletContext, ServletConfig)
	 */
	public static void initServletPropertySources(MutablePropertySources propertySources, ServletContext servletContext) {
		initServletPropertySources(propertySources, servletContext, null);
	}

	/**
	 * 用给定 {@code servletContext} 和 {@code servletConfig} 对象填充的实际实例
	 * 替换基于 {@code Servlet} 的 {@link StubPropertySource 存根属性源}。
	 * <p>该方法具有幂等性：它可以被调用任意多次，
	 * 但只会执行一次将存根属性源替换为对应实际属性源的操作。
	 * @param sources 要初始化的 {@link MutablePropertySources}（不能为 {@code null}）
	 * @param servletContext 当前的 {@link ServletContext}（如果为 {@code null}，或
	 * {@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME servlet context property source}
	 * 已初始化，则忽略）
	 * @param servletConfig 当前的 {@link ServletConfig}（如果为 {@code null}，或
	 * {@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME servlet config property source}
	 * 已初始化，则忽略）
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
	 */
	public static void initServletPropertySources(MutablePropertySources sources,
			@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

		Assert.notNull(sources, "'propertySources' must not be null");
		String name = StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;
		if (servletContext != null && sources.get(name) instanceof StubPropertySource) {
			sources.replace(name, new ServletContextPropertySource(name, servletContext));
		}
		name = StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;
		if (servletConfig != null && sources.get(name) instanceof StubPropertySource) {
			sources.replace(name, new ServletConfigPropertySource(name, servletConfig));
		}
	}

	/**
	 * 将当前 RequestAttributes 实例作为 ServletRequestAttributes 返回。
	 * @see RequestContextHolder#currentRequestAttributes()
	 */
	private static ServletRequestAttributes currentRequestAttributes() {
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
		if (!(requestAttr instanceof ServletRequestAttributes)) {
			throw new IllegalStateException("Current request is not a servlet request");
		}
		return (ServletRequestAttributes) requestAttr;
	}


	/**
	 * 按需暴露当前请求对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {

		@Override
		public ServletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current HttpServletRequest";
		}
	}


	/**
	 * 按需暴露当前响应对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class ResponseObjectFactory implements ObjectFactory<ServletResponse>, Serializable {

		@Override
		public ServletResponse getObject() {
			ServletResponse response = currentRequestAttributes().getResponse();
			if (response == null) {
				throw new IllegalStateException("Current servlet response not available - " +
						"consider using RequestContextFilter instead of RequestContextListener");
			}
			return response;
		}

		@Override
		public String toString() {
			return "Current HttpServletResponse";
		}
	}


	/**
	 * 按需暴露当前会话对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class SessionObjectFactory implements ObjectFactory<HttpSession>, Serializable {

		@Override
		public HttpSession getObject() {
			return currentRequestAttributes().getRequest().getSession();
		}

		@Override
		public String toString() {
			return "Current HttpSession";
		}
	}


	/**
	 * 按需暴露当前 WebRequest 对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		@Override
		public WebRequest getObject() {
			ServletRequestAttributes requestAttr = currentRequestAttributes();
			return new ServletWebRequest(requestAttr.getRequest(), requestAttr.getResponse());
		}

		@Override
		public String toString() {
			return "Current ServletWebRequest";
		}
	}


	/**
	 * 内部类，用于避免硬编码的 JSF 依赖。
 	 */
	private static class FacesDependencyRegistrar {

		public static void registerFacesDependencies(ConfigurableListableBeanFactory beanFactory) {
			beanFactory.registerResolvableDependency(FacesContext.class, new ObjectFactory<FacesContext>() {
				@Override
				public FacesContext getObject() {
					return FacesContext.getCurrentInstance();
				}
				@Override
				public String toString() {
					return "Current JSF FacesContext";
				}
			});
			beanFactory.registerResolvableDependency(ExternalContext.class, new ObjectFactory<ExternalContext>() {
				@Override
				public ExternalContext getObject() {
					return FacesContext.getCurrentInstance().getExternalContext();
				}
				@Override
				public String toString() {
					return "Current JSF ExternalContext";
				}
			});
		}
	}

}
