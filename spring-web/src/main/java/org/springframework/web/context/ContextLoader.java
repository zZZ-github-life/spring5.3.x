/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 执行根应用上下文的实际初始化工作。
 * 由 {@link ContextLoaderListener} 调用。
 *
 * <p>在 {@code web.xml} 的 context-param 级别查找
 * {@link #CONTEXT_CLASS_PARAM "contextClass"} 参数以指定上下文类类型，
 * 如果未找到，则回退到
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 * 使用默认的 ContextLoader 实现时，指定的任何上下文类都需要实现
 * {@link ConfigurableWebApplicationContext} 接口。
 *
 * <p>处理 {@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param
 * 并将其值传递给上下文实例，将其解析为可能多个文件路径，
 * 这些路径可由任意数量的逗号和空格分隔，例如
 * "WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml"。
 * 也支持 Ant 风格的路径模式，例如
 * "WEB-INF/*Context.xml,WEB-INF/spring*.xml" 或 "WEB-INF/&#42;&#42;/*Context.xml"。
 * 如果未显式指定，上下文实现应使用默认位置
 * （对于 XmlWebApplicationContext："/WEB-INF/applicationContext.xml"）。
 *
 * <p>注意：在存在多个配置位置的情况下，后加载的 bean 定义将覆盖之前加载文件中
 * 定义的内容，至少在使用 Spring 默认 ApplicationContext 实现之一时如此。
 * 可以利用这一点，通过一个额外的 XML 文件来有意覆盖某些 bean 定义。
 *
 * <p>除了加载根应用上下文之外，此类还可以选择加载或获取一个共享的父上下文，
 * 并将其挂接到根应用上下文。更多信息请参见
 * {@link #loadParentContext(ServletContext)} 方法。
 *
 * <p>自 Spring 3.1 起，{@code ContextLoader} 支持通过
 * {@link #ContextLoader(WebApplicationContext)} 构造函数注入根 Web 应用上下文，
 * 从而允许在 Servlet 3.0+ 环境中进行编程式配置。
 * 有关用法示例，请参见 {@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @since 17.02.2003
 * @see ContextLoaderListener
 * @see ConfigurableWebApplicationContext
 * @see org.springframework.web.context.support.XmlWebApplicationContext
 */
public class ContextLoader {

	/**
	 * 根 WebApplicationContext id 的配置参数，
	 * 用作底层 BeanFactory 的序列化 id：{@value}。
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * servlet 上下文参数的名称（即 {@value}），可用于指定根上下文的
	 * 配置位置，否则回退到实现的默认值。
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * 要使用的根 WebApplicationContext 实现类的配置参数：{@value}。
	 * @see #determineContextClass(ServletContext)
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * 用于初始化根 Web 应用上下文的 {@link ApplicationContextInitializer} 类的
	 * 配置参数：{@value}。
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * 用于初始化当前应用程序中所有 Web 应用上下文的全局
	 * {@link ApplicationContextInitializer} 类的配置参数：{@value}。
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

	/**
	 * 这些字符中的任意数量都被视为单个 init-param 字符串值中多个值之间的分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

	/**
	 * 类路径资源的名称（相对于 ContextLoader 类），
	 * 该资源定义了 ContextLoader 的默认策略名称。
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";


	/**
	 * 从（线程上下文）ClassLoader 到对应“当前”WebApplicationContext 的映射。
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<>(1);

	/**
	 * “当前”WebApplicationContext，前提是 ContextLoader 类
	 * 部署在 Web 应用 ClassLoader 本身中。
	 */
	@Nullable
	private static volatile WebApplicationContext currentContext;

	@Nullable
	private static Properties defaultStrategies;

	/**
	 * 此加载器管理的根 WebApplicationContext 实例。
	 */
	@Nullable
	private WebApplicationContext context;

	/** 要应用于上下文的实际 ApplicationContextInitializer 实例。 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();


	/**
	 * 创建一个新的 {@code ContextLoader}，它将基于 "contextClass" 和
	 * "contextConfigLocation" servlet 上下文参数创建 Web 应用上下文。
	 * 有关各自默认值的详细信息，请参见类级文档。
	 * <p>此构造函数通常在 {@code web.xml} 中将 {@code
	 * ContextLoaderListener} 子类声明为 {@code <listener>} 时使用，
	 * 因为此时需要一个无参构造函数。
	 * <p>创建的应用上下文将以
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * 属性名注册到 ServletContext 中，子类可自由地在容器关闭时调用
	 * {@link #closeWebApplicationContext} 方法来关闭应用上下文。
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader() {
	}

	/**
	 * 使用给定的应用上下文创建一个新的 {@code ContextLoader}。
	 * 在 Servlet 3.0+ 环境中，可以通过 {@link ServletContext#addListener} API
	 * 进行基于实例的监听器注册，此时此构造函数非常有用。
	 * <p>该上下文可能已经或尚未 {@linkplain
	 * ConfigurableApplicationContext#refresh() 刷新}。如果它 (a) 是
	 * {@link ConfigurableWebApplicationContext} 的实现，并且 (b) <strong>尚未</strong>
	 * 被刷新（推荐做法），那么将发生以下情况：
	 * <ul>
	 * <li>如果给定的上下文尚未被分配 {@linkplain
	 * ConfigurableApplicationContext#setId id}，则会为其分配一个</li>
	 * <li>{@code ServletContext} 和 {@code ServletConfig} 对象将被委托给应用上下文</li>
	 * <li>将调用 {@link #customizeContext}</li>
	 * <li>通过 "contextInitializerClasses" init-param 指定的任何
	 * {@link ApplicationContextInitializer ApplicationContextInitializers} 都将被应用。</li>
	 * <li>将调用 {@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已被刷新，或者未实现 {@code ConfigurableWebApplicationContext}，
	 * 则上述任何操作都不会发生，假设用户已根据其具体需求执行了（或未执行）这些操作。
	 * <p>有关用法示例，请参见 {@link org.springframework.web.WebApplicationInitializer}。
	 * <p>无论如何，给定的应用上下文都将以
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} 属性名
	 * 注册到 ServletContext 中，子类可自由地在容器关闭时调用
	 * {@link #closeWebApplicationContext} 方法来关闭应用上下文。
	 * @param context the application context to manage
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}


	/**
	 * 指定应使用哪些 {@link ApplicationContextInitializer} 实例
	 * 来初始化此 {@code ContextLoader} 所使用的应用上下文。
	 * @since 4.2
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #customizeContext
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}


	/**
	 * 为给定的 servlet 上下文初始化 Spring 的 Web 应用上下文，
	 * 使用构造时提供的应用上下文，或者根据
	 * "{@link #CONTEXT_CLASS_PARAM contextClass}" 和
	 * "{@link #CONFIG_LOCATION_PARAM contextConfigLocation}" 上下文参数新建一个。
	 * @param servletContext current servlet context
	 * @return the new WebApplicationContext
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		servletContext.log("Initializing Spring root WebApplicationContext");
		Log logger = LogFactory.getLog(ContextLoader.class);
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// Store context in local instance variable, to guarantee that
			// it is available on ServletContext shutdown.
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext initialized in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException | Error ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
	}

	/**
	 * 为当前加载器实例化根 WebApplicationContext，使用默认的上下文类，或者在指定了自定义上下文类时使用该自定义
	 * 类。
	 * <p>此实现要求自定义上下文实现 {@link ConfigurableWebApplicationContext} 接口。可在子类中覆盖。
	 * <p>此外，在刷新上下文之前还会调用 {@link #customizeContext}，允许子类对上下文执行自定义修改。
	 * @param sc current servlet context
	 * @return the root WebApplicationContext
	 * @see ConfigurableWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		Class<?> contextClass = determineContextClass(sc);
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * 返回要使用的 WebApplicationContext 实现类，即默认的 XmlWebApplicationContext，或者在指定了自定义上下文类
	 * 时使用该自定义类。
	 * @param servletContext current servlet context
	 * @return the WebApplicationContext implementation class to use
	 * @see #CONTEXT_CLASS_PARAM
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
			if (defaultStrategies == null) {
				// Load default strategy implementations from properties file.
				// This is currently strictly internal and not meant to be customized
				// by application developers.
				try {
					ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
					defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
				}
				catch (IOException ex) {
					throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
				}
			}
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				wac.setId(idParam);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		wac.setServletContext(sc);
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}

		customizeContext(sc, wac);
		wac.refresh();
	}

	/**
	 * 在配置位置已提供给上下文之后、上下文被<em>刷新</em>之前，定制由此 ContextLoader 创建的
	 * {@link ConfigurableWebApplicationContext}。
	 * <p>默认实现会通过 {@linkplain #determineContextInitializerClasses(ServletContext)
	 * 确定} 通过 {@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM context 初始化参数}
	 * 指定了哪些（如果有）上下文初始化器类，并对每一个调用
	 * {@linkplain ApplicationContextInitializer#initialize 初始化}，传入给定的
	 * Web 应用上下文。
	 * <p>任何实现了 {@link org.springframework.core.Ordered Ordered} 或标注了 @{@link
	 * org.springframework.core.annotation.Order Order} 的 {@code ApplicationContextInitializers}
	 * 都会按相应顺序排序。
	 * @param sc the current servlet context
	 * @param wac the newly created application context
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 */
	protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(sc);

		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	/**
	 * 返回要使用的 {@link ApplicationContextInitializer} 实现类（如果有通过
	 * {@link #CONTEXT_INITIALIZER_CLASSES_PARAM} 指定的实现类）。
	 * @param servletContext current servlet context
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 */
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
			determineContextInitializerClasses(ServletContext servletContext) {

		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
				new ArrayList<>();

		String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		if (localClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		return classes;
	}

	@SuppressWarnings("unchecked")
	private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
		try {
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
				throw new ApplicationContextException(
						"Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
			}
			return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

	/**
	 * 模板方法，带有默认实现（可由子类覆盖），用于加载或获取一个 ApplicationContext 实例，该实例将用作根
	 * WebApplicationContext 的父上下文。如果该方法返回 null，则不设置父上下文。
	 * <p>在此处加载父上下文的主要原因，是允许多个根 Web 应用上下文都作为共享 EAR 上下文的子上下文，或者同时共
	 * 享对 EJB 可见的同一个父上下文。对于纯 Web 应用，通常无需为根 Web 应用上下文设置父上下文。
	 * <p>自 5.0 起，默认实现仅返回 {@code null}。
	 * @param servletContext current servlet context
	 * @return the parent application context, or {@code null} if none
	 */
	@Nullable
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return null;
	}

	/**
	 * 关闭针对给定 Servlet 上下文的 Spring Web 应用上下文。<p>如果覆盖了
	 * {@link #loadParentContext(ServletContext)}，则可能也需要覆盖此方法。
	 * @param servletContext the ServletContext that the WebApplicationContext runs in
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		}
		finally {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = null;
			}
			else if (ccl != null) {
				currentContextPerThread.remove(ccl);
			}
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}


	/**
	 * 获取当前线程（即当前线程的上下文 ClassLoader，它需要是 Web 应用的 ClassLoader）的 Spring 根 Web 应用上下
	 * 文。
	 * @return the current root web application context, or {@code null}
	 * if none found
	 * @see org.springframework.web.context.support.SpringBeanAutowiringSupport
	 */
	@Nullable
	public static WebApplicationContext getCurrentWebApplicationContext() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl != null) {
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			if (ccpt != null) {
				return ccpt;
			}
		}
		return currentContext;
	}

}
