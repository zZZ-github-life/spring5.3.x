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

package org.springframework.web.servlet;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * Spring Web 框架的基础 Servlet。在基于 JavaBean 的整体解决方案中
 * 提供与 Spring 应用上下文的集成。
 *
 * <p>此类提供以下功能：
 * <ul>
 * <li>为每个 Servlet 管理一个 {@link org.springframework.web.context.WebApplicationContext
 * WebApplicationContext} 实例。Servlet 的配置由该 Servlet 命名空间中的 bean 决定。
 * <li>在请求处理时发布事件，无论请求是否被成功处理。
 * </ul>
 *
 * <p>子类必须实现 {@link #doService} 来处理请求。由于本类继承的是
 * {@link HttpServletBean} 而非直接继承 HttpServlet，因此 bean 属性会被自动映射到它上面。
 * 子类可以重写 {@link #initFrameworkServlet()} 以进行自定义初始化。
 *
 * <p>在 Servlet 初始化参数级别检测 "contextClass" 参数，
 * 如果未找到则回退到默认的上下文类
 * {@link org.springframework.web.context.support.XmlWebApplicationContext XmlWebApplicationContext}。
 * 请注意，使用默认的 {@code FrameworkServlet} 时，自定义上下文类需要实现
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext
 * ConfigurableWebApplicationContext} SPI。
 *
 * <p>接受可选的 "contextInitializerClasses" Servlet 初始化参数，用于指定一个或多个
 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer} 类。
 * 所管理的 Web 应用上下文将委托给这些初始化器，从而支持额外的编程式配置，
 * 例如添加属性源或针对{@linkplain
 * org.springframework.context.ConfigurableApplicationContext#getEnvironment() 上下文的
 * 环境}激活 profile。另请参见 {@link org.springframework.web.context.ContextLoader}，
 * 它为 "root" Web 应用上下文支持语义相同的 "contextInitializerClasses" 上下文参数。
 *
 * <p>将 "contextConfigLocation" Servlet 初始化参数传递给上下文实例，
 * 将其解析为可能多个文件路径，这些路径可以用任意数量的逗号和空格分隔，
 * 例如 "test-servlet.xml, myServlet.xml"。
 * 如果未显式指定，上下文实现应根据 Servlet 的命名空间构建默认位置。
 *
 * <p>注意：在存在多个配置位置的情况下，后加载的 bean 定义将覆盖先前加载文件中定义的 bean，
 * 至少在使用 Spring 默认的 ApplicationContext 实现时是如此。
 * 可以利用这一点，通过额外的 XML 文件有意覆盖某些 bean 定义。
 *
 * <p>默认命名空间为 "'servlet-name'-servlet"，例如对于 servlet-name 为 "test" 的 Servlet，
 * 为 "test-servlet"（在 XmlWebApplicationContext 下会得到默认位置 "/WEB-INF/test-servlet.xml"）。
 * 命名空间也可以通过 "namespace" Servlet 初始化参数显式设置。
 *
 * <p>从 Spring 3.1 开始，{@code FrameworkServlet} 可以注入一个 Web 应用上下文，
 * 而不再在内部自行创建。这在支持以编程方式注册 Servlet 实例的
 * Servlet 3.0+ 环境中很有用。详情请参见
 * {@link #FrameworkServlet(WebApplicationContext)} 的 Javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see #doService
 * @see #setContextClass
 * @see #setContextConfigLocation
 * @see #setContextInitializerClasses
 * @see #setNamespace
 */
@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	/**
	 * WebApplicationContext 命名空间的后缀。如果在上下文中给此类的 Servlet
	 * 命名为 "test"，则该 Servlet 使用的命名空间将解析为 "test-servlet"。
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * FrameworkServlet 的默认上下文类。
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * WebApplicationContext 对应的 ServletContext 属性的前缀。
	 * 后缀是 Servlet 名称。
	 */
	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

	/**
	 * 这些字符中的任意数量都被视为单个初始化参数字符串值中
	 * 多个值之间的分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";


	/** 用于查找 WebApplicationContext 的 ServletContext 属性。 */
	@Nullable
	private String contextAttribute;

	/** 要创建的 WebApplicationContext 实现类。 */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/** 要分配的 WebApplicationContext id。 */
	@Nullable
	private String contextId;

	/** 此 Servlet 的命名空间。 */
	@Nullable
	private String namespace;

	/** 显式的上下文配置位置。 */
	@Nullable
	private String contextConfigLocation;

	/** 要应用到上下文上的实际 ApplicationContextInitializer 实例。 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();

	/** 通过初始化参数设置的、以逗号分隔的 ApplicationContextInitializer 类名。 */
	@Nullable
	private String contextInitializerClasses;

	/** 是否应将上下文作为 ServletContext 属性发布？。 */
	private boolean publishContext = true;

	/** 是否应在每个请求结束时发布 ServletRequestHandledEvent？。 */
	private boolean publishEvents = true;

	/** 是否将 LocaleContext 和 RequestAttributes 暴露为可被子线程继承？。 */
	private boolean threadContextInheritable = false;

	/** 是否应将 HTTP OPTIONS 请求分派给 {@link #doService}？。 */
	private boolean dispatchOptionsRequest = false;

	/** 是否应将 HTTP TRACE 请求分派给 {@link #doService}？。 */
	private boolean dispatchTraceRequest = false;

	/** 是否记录潜在的敏感信息（DEBUG 级别的请求参数和 TRACE 级别的请求头）。 */
	private boolean enableLoggingRequestDetails = false;

	/** 此 Servlet 的 WebApplicationContext。 */
	@Nullable
	private WebApplicationContext webApplicationContext;

	/** WebApplicationContext 是否通过 {@link #setApplicationContext} 注入。 */
	private boolean webApplicationContextInjected = false;

	/** 用于检测 onRefresh 是否已被调用的标志。 */
	private volatile boolean refreshEventReceived;

	/** 用于同步 onRefresh 执行的监视器。 */
	private final Object onRefreshMonitor = new Object();


	/**
	 * 创建一个新的 {@code FrameworkServlet}，它将根据默认值
	 * 以及通过 Servlet 初始化参数提供的值创建自己的内部 Web 应用上下文。
	 * 通常用于 Servlet 2.5 或更早的环境中，此时注册 Servlet 的唯一方式
	 * 是通过 {@code web.xml}，而这需要使用无参构造函数。
	 * <p>调用 {@link #setContextConfigLocation}（初始化参数 'contextConfigLocation'）
	 * 将决定由 {@linkplain #DEFAULT_CONTEXT_CLASS 默认的 XmlWebApplicationContext}
	 * 加载哪些 XML 文件。
	 * <p>调用 {@link #setContextClass}（初始化参数 'contextClass'）会覆盖默认的
	 * {@code XmlWebApplicationContext}，并允许指定替代类，例如
	 * {@code AnnotationConfigWebApplicationContext}。
	 * <p>调用 {@link #setContextInitializerClasses}（初始化参数 'contextInitializerClasses'）
	 * 表示应使用哪些 {@link ApplicationContextInitializer} 类
	 * 在 refresh() 之前进一步配置内部应用上下文。
	 * @see #FrameworkServlet(WebApplicationContext)
	 */
	public FrameworkServlet() {
	}

	/**
	 * 使用给定的 Web 应用上下文创建一个新的 {@code FrameworkServlet}。
	 * 该构造函数在 Servlet 3.0+ 环境中很有用，在那里可以通过
	 * {@link ServletContext#addServlet} API 以基于实例的方式注册 Servlet。
	 * <p>使用该构造函数意味着以下属性/初始化参数将被忽略：
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>给定的 Web 应用上下文可能尚未 {@linkplain
	 * ConfigurableApplicationContext#refresh() 刷新}。如果它 (a) 是
	 * {@link ConfigurableWebApplicationContext} 的实现，并且 (b) <strong>尚未</strong>
	 * 被刷新（这是推荐的做法），则会发生以下情况：
	 * <ul>
	 * <li>如果给定的上下文尚未具有{@linkplain
	 * ConfigurableApplicationContext#setParent 父上下文}，则将根应用上下文设置为其父上下文。</li>
	 * <li>如果给定的上下文尚未分配{@linkplain
	 * ConfigurableApplicationContext#setId id}，则会为其分配一个 id</li>
	 * <li>{@code ServletContext} 和 {@code ServletConfig} 对象将被委托给应用上下文</li>
	 * <li>将调用 {@link #postProcessWebApplicationContext}</li>
	 * <li>通过 "contextInitializerClasses" 初始化参数或通过
	 * {@link #setContextInitializers} 属性指定的任何
	 * {@link ApplicationContextInitializer ApplicationContextInitializers} 都将被应用。</li>
	 * <li>将调用 {@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已被刷新，或者未实现 {@code ConfigurableWebApplicationContext}，
	 * 则上述操作都不会执行，因为假定用户已根据其具体需求完成了（或未完成）这些操作。
	 * <p>用法示例请参见 {@link org.springframework.web.WebApplicationInitializer}。
	 * @param webApplicationContext the context to use
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public FrameworkServlet(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}


	/**
	 * 设置 ServletContext 属性的名称，该属性应用于获取此 Servlet 应使用的
	 * {@link WebApplicationContext}。
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回 ServletContext 属性的名称，该属性应用于获取此 Servlet 应使用的
	 * {@link WebApplicationContext}。
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置自定义上下文类。该类必须是
	 * {@link org.springframework.web.context.WebApplicationContext} 类型。
	 * <p>在使用默认的 FrameworkServlet 实现时，
	 * 上下文类还必须实现
	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext} 接口。
	 * @see #createWebApplicationContext
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * 返回自定义上下文类。
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * 指定自定义的 WebApplicationContext id，
	 * 用作底层 BeanFactory 的序列化 id。
	 */
	public void setContextId(@Nullable String contextId) {
		this.contextId = contextId;
	}

	/**
	 * 返回自定义的 WebApplicationContext id（如果有）。
	 */
	@Nullable
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * 为此 Servlet 设置自定义命名空间，
	 * 用于构建默认的上下文配置文件位置。
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * 返回此 Servlet 的命名空间；如果未设置自定义命名空间，
	 * 则回退到默认方案：例如名为 "test" 的 Servlet 返回 "test-servlet"。
	 */
	public String getNamespace() {
		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
	}

	/**
	 * 显式设置上下文配置位置，而不是依赖基于命名空间构建的默认位置。
	 * 该位置字符串可以由多个位置组成，
	 * 位置之间可用任意数量的逗号和空格分隔。
	 */
	public void setContextConfigLocation(@Nullable String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回显式的上下文配置位置（如果有）。
	 */
	@Nullable
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 指定应使用哪些 {@link ApplicationContextInitializer} 实例
	 * 来初始化此 {@code FrameworkServlet} 所使用的应用上下文。
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #applyInitializers
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
	 * 按照可选的 "contextInitializerClasses" Servlet 初始化参数，
	 * 指定一组全限定的 {@link ApplicationContextInitializer} 类名。
	 * @see #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext)
	 * @see #applyInitializers(ConfigurableApplicationContext)
	 */
	public void setContextInitializerClasses(String contextInitializerClasses) {
		this.contextInitializerClasses = contextInitializerClasses;
	}

	/**
	 * 设置是否将此 Servlet 的上下文作为 ServletContext 属性发布，
	 * 使 Web 容器中的所有对象都能访问。默认值为 "true"。
	 * <p>这在测试期间尤其方便，不过让其他应用对象以这种方式访问上下文
	 * 是否属于良好实践尚有争议。
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * 设置此 Servlet 是否应在每个请求结束时发布 ServletRequestHandledEvent。
	 * 默认值为 "true"；如果没有任何 ApplicationListener 依赖此类事件，
	 * 可以关闭它以获得轻微的性能提升。
	 * @see org.springframework.web.context.support.ServletRequestHandledEvent
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * 设置是否将 LocaleContext 和 RequestAttributes 暴露为可被子线程继承
	 * （使用 {@link java.lang.inheritableThreadLocal}）。
	 * <p>默认值为 "false"，以避免对派生的后台线程产生副作用。
	 * 将其切换为 "true" 可为在请求处理期间派生、且仅用于该请求的自定义子线程
	 * （即在其初始任务结束后即终止，线程不被复用）启用继承。
	 * <p><b>警告：</b>如果你访问的是配置为可能按需添加新线程的线程池
	 * （例如 JDK 的 {@link java.util.concurrent.ThreadPoolExecutor}），
	 * 请不要对子线程使用继承，因为这会将被继承的上下文暴露给这样的池化线程。
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * 设置此 Servlet 是否应将 HTTP OPTIONS 请求分派给 {@link #doService} 方法。
	 * <p>在 {@code FrameworkServlet} 中默认值为 "false"，即采用
	 * {@link javax.servlet.http.HttpServlet} 的默认行为
	 * （也就是将所有标准 HTTP 请求方法枚举作为对 OPTIONS 请求的响应）。
	 * 但请注意，从 4.3 开始，由于 {@code DispatcherServlet} 内置了对 OPTIONS 的支持，
	 * 它默认将此属性设置为 "true"。
	 * <p>如果你希望 OPTIONS 请求像其他 HTTP 请求一样经过常规分派链，
	 * 请开启此标志。这通常意味着你的控制器将接收到这些请求；
	 * 请确保这些端点确实能够处理 OPTIONS 请求。
	 * <p>请注意，如果你的控制器恰好没有设置 'Allow' 头
	 * （这是 OPTIONS 响应所必需的），那么 HttpServlet 的默认 OPTIONS 处理
	 * 在任何情况下都会被应用。
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * 设置此 Servlet 是否应将 HTTP TRACE 请求分派给 {@link #doService} 方法。
	 * <p>默认值为 "false"，即采用 {@link javax.servlet.http.HttpServlet} 的默认行为
	 * （也就是将收到的消息原样返回给客户端）。
	 * <p>如果你希望 TRACE 请求像其他 HTTP 请求一样经过常规分派链，
	 * 请开启此标志。这通常意味着你的控制器将接收到这些请求；
	 * 请确保这些端点确实能够处理 TRACE 请求。
	 * <p>请注意，如果你的控制器恰好没有生成内容类型为 'message/http' 的响应
	 * （这是 TRACE 响应所必需的），那么 HttpServlet 的默认 TRACE 处理
	 * 在任何情况下都会被应用。
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * 是否在 DEBUG 级别记录请求参数、在 TRACE 级别记录请求头。
	 * 二者都可能包含敏感信息。
	 * <p>默认设置为 {@code false}，以便不显示请求细节。
	 * @param enable whether to enable or not
	 * @since 5.1
	 */
	public void setEnableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	/**
	 * 是否允许在 DEBUG 和 TRACE 级别记录可能敏感的请求细节。
	 * @since 5.1
	 */
	public boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * 由 Spring 通过 {@link ApplicationContextAware} 调用，以注入当前应用上下文。
	 * 该方法允许 FrameworkServlet 作为 Spring bean 注册到现有的
	 * {@link WebApplicationContext} 中，而不是{@link #findWebApplicationContext() 查找}
	 * 由 {@link org.springframework.web.context.ContextLoaderListener 引导}的上下文。
	 * <p>主要是为了支持在嵌入式 Servlet 容器中使用而添加的。
	 * @since 4.0
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
			this.webApplicationContext = (WebApplicationContext) applicationContext;
			this.webApplicationContextInjected = true;
		}
	}


	/**
	 * {@link HttpServletBean} 的重写方法，在任何 bean 属性设置完成之后调用。
	 * 创建此 Servlet 的 WebApplicationContext。
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("Initializing Servlet '" + getServletName() + "'");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.webApplicationContext = initWebApplicationContext();
			initFrameworkServlet();
		}
		catch (ServletException | RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': request parameters and headers will be " + value);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	/**
	 * 初始化并发布此 Servlet 的 WebApplicationContext。
	 * <p>实际的上下文创建委托给 {@link #createWebApplicationContext}。
	 * 可以在子类中重写。
	 * @return the WebApplicationContext instance
	 * @see #FrameworkServlet(WebApplicationContext)
	 * @see #setContextClass
	 * @see #setContextConfigLocation
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		if (this.webApplicationContext != null) {
			// A context instance was injected at construction time -> use it
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
						// the root application context (if any; may be null) as the parent
						cwac.setParent(rootContext);
					}
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		if (wac == null) {
			// No context instance was injected at construction time -> see if one
			// has been registered in the servlet context. If one exists, it is assumed
			// that the parent context (if any) has already been set and that the
			// user has performed any initialization such as setting the context id
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			// No context instance is defined for this servlet -> create a local one
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
			// support or the context injected at construction time had already been
			// refreshed -> trigger initial onRefresh manually here.
			synchronized (this.onRefreshMonitor) {
				onRefresh(wac);
			}
		}

		if (this.publishContext) {
			// Publish the context as a servlet context attribute.
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
		}

		return wac;
	}

	/**
	 * 从 {@code ServletContext} 属性中按{@link #setContextAttribute 配置的名称}
	 * 获取一个 {@code WebApplicationContext}。
	 * 该 {@code WebApplicationContext} 必须在此 Servlet 初始化（或被调用）之前
	 * 已经加载并存储在 {@code ServletContext} 中。
	 * <p>子类可以重写此方法以提供不同的 {@code WebApplicationContext} 获取策略。
	 * @return the WebApplicationContext for this servlet, or {@code null} if not found
	 * @see #getContextAttribute()
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName == null) {
			return null;
		}
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		return wac;
	}

	/**
	 * 为此 Servlet 实例化 WebApplicationContext，可以是默认的
	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}，
	 * 也可以是{@link #setContextClass 自定义上下文类}（如果已设置）。
	 * <p>此实现期望自定义上下文实现
	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext} 接口。
	 * 可以在子类中重写。
	 * <p>不要忘记将此 Servlet 实例注册为所创建上下文的应用监听器
	 * （用于触发其 {@link #onRefresh 回调}），并在返回上下文实例之前
	 * 调用 {@link org.springframework.context.ConfigurableApplicationContext#refresh()}。
	 * @param parent the parent ApplicationContext to use, or {@code null} if none
	 * @return the WebApplicationContext for this servlet
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
		Class<?> contextClass = getContextClass();
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
					"': custom WebApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurableWebApplicationContext");
		}
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

		wac.setEnvironment(getEnvironment());
		wac.setParent(parent);
		String configLocation = getContextConfigLocation();
		if (configLocation != null) {
			wac.setConfigLocation(configLocation);
		}
		configureAndRefreshWebApplicationContext(wac);

		return wac;
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			if (this.contextId != null) {
				wac.setId(this.contextId);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		wac.setNamespace(getNamespace());
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		postProcessWebApplicationContext(wac);
		applyInitializers(wac);
		wac.refresh();
	}

	/**
	 * 为此 Servlet 实例化 WebApplicationContext，可以是默认的
	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}，
	 * 也可以是{@link #setContextClass 自定义上下文类}（如果已设置）。
	 * 委托给 #createWebApplicationContext(ApplicationContext)。
	 * @param parent the parent WebApplicationContext to use, or {@code null} if none
	 * @return the WebApplicationContext for this servlet
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 * @see #createWebApplicationContext(ApplicationContext)
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
	}

	/**
	 * 在给定的 WebApplicationContext 被刷新并作为此 Servlet 的上下文激活之前，
	 * 对其进行后处理。
	 * <p>默认实现为空。此方法返回后，{@code refresh()} 会被自动调用。
	 * <p>请注意，此方法旨在允许子类修改应用上下文，而
	 * {@link #initWebApplicationContext} 则旨在允许最终用户
	 * 通过使用 {@link ApplicationContextInitializer ApplicationContextInitializers} 修改上下文。
	 * @param wac the configured WebApplicationContext (not refreshed yet)
	 * @see #createWebApplicationContext
	 * @see #initWebApplicationContext
	 * @see ConfigurableWebApplicationContext#refresh()
	 */
	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
	}

	/**
	 * 在 WebApplicationContext 被刷新之前，将其委托给由
	 * "contextInitializerClasses" Servlet 初始化参数指定的所有
	 * {@link ApplicationContextInitializer} 实例。
	 * <p>另请参见 {@link #postProcessWebApplicationContext}，它旨在允许子类
	 * （而非最终用户）修改应用上下文，并且紧接在此方法之前调用。
	 * @param wac the configured WebApplicationContext (not refreshed yet)
	 * @see #createWebApplicationContext
	 * @see #postProcessWebApplicationContext
	 * @see ConfigurableApplicationContext#refresh()
	 */
	protected void applyInitializers(ConfigurableApplicationContext wac) {
		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		if (this.contextInitializerClasses != null) {
			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, ConfigurableApplicationContext wac) {
		try {
			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
					"via 'contextInitializerClasses' init-param", className), ex);
		}
	}

	/**
	 * 返回此 Servlet 的 WebApplicationContext 对应的 ServletContext 属性名称。
	 * <p>默认实现返回 {@code SERVLET_CONTEXT_PREFIX + servlet name}。
	 * @see #SERVLET_CONTEXT_PREFIX
	 * @see #getServletName
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getServletName();
	}

	/**
	 * 返回此 Servlet 的 WebApplicationContext。
	 */
	@Nullable
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	/**
	 * 此方法会在所有 bean 属性设置完成且 WebApplicationContext 加载完成后被调用。
	 * 默认实现为空；子类可以重写此方法以执行所需的任何初始化。
	 * @throws ServletException in case of an initialization exception
	 */
	protected void initFrameworkServlet() throws ServletException {
	}

	/**
	 * 刷新此 Servlet 的应用上下文，以及该 Servlet 依赖于上下文的状态。
	 * @see #getWebApplicationContext()
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	public void refresh() {
		WebApplicationContext wac = getWebApplicationContext();
		if (!(wac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
		}
		((ConfigurableApplicationContext) wac).refresh();
	}

	/**
	 * 接收来自此 Servlet 的 WebApplicationContext 的刷新事件的回调。
	 * <p>默认实现调用 {@link #onRefresh}，
	 * 触发此 Servlet 依赖上下文的状态的刷新。
	 * @param event the incoming ApplicationContext event
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		synchronized (this.onRefreshMonitor) {
			onRefresh(event.getApplicationContext());
		}
	}

	/**
	 * 模板方法，可以被重写以添加特定于 Servlet 的刷新工作。
	 * 在上下文刷新成功后调用。
	 * <p>此实现为空。
	 * @param context the current WebApplicationContext
	 * @see #refresh()
	 */
	protected void onRefresh(ApplicationContext context) {
		// For subclasses: do nothing by default.
	}

	/**
	 * 关闭此 Servlet 的 WebApplicationContext。
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	@Override
	public void destroy() {
		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
		// Only call close() on WebApplicationContext if locally managed...
		if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
			((ConfigurableApplicationContext) this.webApplicationContext).close();
		}
	}


	/**
	 * 重写父类的实现，以便拦截 PATCH 请求。
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
		if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
			processRequest(request, response);
		}
		else {
			super.service(request, response);
		}
	}

	/**
	 * 将 GET 请求委托给 processRequest/doService。
	 * <p>HttpServlet 默认的 {@code doHead} 实现也会调用此方法，
	 * 并传入一个仅捕获内容长度的 {@code NoBodyResponse}。
	 * @see #doService
	 * @see #doHead
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 POST 请求委托给 {@link #processRequest}。
	 * @see #doService
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 PUT 请求委托给 {@link #processRequest}。
	 * @see #doService
	 */
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将 DELETE 请求委托给 {@link #processRequest}。
	 * @see #doService
	 */
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 如需要，将 OPTIONS 请求委托给 {@link #processRequest}。
	 * <p>否则（以及分派之后仍未设置 'Allow' 头时），
	 * 采用 HttpServlet 的标准 OPTIONS 处理。
	 * @see #doService
	 */
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
			processRequest(request, response);
			if (response.containsHeader("Allow")) {
				// Proper OPTIONS response coming from a handler - we're done.
				return;
			}
		}

		// Use response wrapper in order to always add PATCH to the allowed methods
		super.doOptions(request, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				if ("Allow".equals(name)) {
					value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
				}
				super.setHeader(name, value);
			}
		});
	}

	/**
	 * 如需要，将 TRACE 请求委托给 {@link #processRequest}。
	 * <p>否则采用 HttpServlet 的标准 TRACE 处理。
	 * @see #doService
	 */
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchTraceRequest) {
			processRequest(request, response);
			if ("message/http".equals(response.getContentType())) {
				// Proper TRACE response coming from a handler - we're done.
				return;
			}
		}
		super.doTrace(request, response);
	}

	/**
	 * 处理此请求，无论结果如何都会发布事件。
	 * <p>实际的事件处理由抽象的 {@link #doService} 模板方法执行。
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException | IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}
			logResult(request, response, failureCause, asyncManager);
			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

	/**
	 * 为给定请求构建一个 LocaleContext，将请求的主区域设置作为当前区域设置暴露。
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext, or {@code null} if none to bind
	 * @see LocaleContextHolder#setLocaleContext
	 */
	@Nullable
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * 为给定请求构建 ServletRequestAttributes（可能还持有对响应的引用），
	 * 同时考虑预先绑定的属性（及其类型）。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param previousAttributes pre-bound RequestAttributes instance, if any
	 * @return the ServletRequestAttributes to bind, or {@code null} to preserve
	 * the previously bound instance (or not binding any, if none bound before)
	 * @see RequestContextHolder#setRequestAttributes
	 */
	@Nullable
	protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request,
			@Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {

		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
			return new ServletRequestAttributes(request, response);
		}
		else {
			return null;  // preserve the pre-bound RequestAttributes instance
		}
	}

	private void initContextHolders(HttpServletRequest request,
			@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {

		if (localeContext != null) {
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
	}

	private void resetContextHolders(HttpServletRequest request,
			@Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {

		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
	}

	private void logResult(HttpServletRequest request, HttpServletResponse response,
			@Nullable Throwable failureCause, WebAsyncManager asyncManager) {

		if (!logger.isDebugEnabled()) {
			return;
		}

		DispatcherType dispatchType = request.getDispatcherType();
		boolean initialDispatch = (dispatchType == DispatcherType.REQUEST);

		if (failureCause != null) {
			if (!initialDispatch) {
				// FORWARD/ERROR/ASYNC: minimal message (there should be enough context already)
				if (logger.isDebugEnabled()) {
					logger.debug("Unresolved failure from \"" + dispatchType + "\" dispatch: " + failureCause);
				}
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Failed to complete request", failureCause);
			}
			else {
				logger.debug("Failed to complete request: " + failureCause);
			}
			return;
		}

		if (asyncManager.isConcurrentHandlingStarted()) {
			logger.debug("Exiting but response remains open for further handling");
			return;
		}

		int status = response.getStatus();
		String headers = "";  // nothing below trace

		if (logger.isTraceEnabled()) {
			Collection<String> names = response.getHeaderNames();
			if (this.enableLoggingRequestDetails) {
				headers = names.stream().map(name -> name + ":" + response.getHeaders(name))
						.collect(Collectors.joining(", "));
			}
			else {
				headers = names.isEmpty() ? "" : "masked";
			}
			headers = ", headers={" + headers + "}";
		}

		if (!initialDispatch) {
			logger.debug("Exiting from \"" + dispatchType + "\" dispatch, status " + status + headers);
		}
		else {
			HttpStatus httpStatus = HttpStatus.resolve(status);
			logger.debug("Completed " + (httpStatus != null ? httpStatus : status) + headers);
		}
	}

	private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response,
			long startTime, @Nullable Throwable failureCause) {

		if (this.publishEvents && this.webApplicationContext != null) {
			// Whether or not we succeeded, publish an event.
			long processingTime = System.currentTimeMillis() - startTime;
			this.webApplicationContext.publishEvent(
					new ServletRequestHandledEvent(this,
							request.getRequestURI(), request.getRemoteAddr(),
							request.getMethod(), getServletConfig().getServletName(),
							WebUtils.getSessionId(request), getUsernameForRequest(request),
							processingTime, failureCause, response.getStatus()));
		}
	}

	/**
	 * 确定给定请求的用户名。
	 * <p>默认实现采用 UserPrincipal 的名称（如果有）。
	 * 可以在子类中重写。
	 * @param request current HTTP request
	 * @return the username, or {@code null} if none found
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	protected String getUsernameForRequest(HttpServletRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		return (userPrincipal != null ? userPrincipal.getName() : null);
	}


	/**
	 * 子类必须实现此方法来完成请求处理工作，
	 * 它将作为 GET、POST、PUT 和 DELETE 请求的集中式回调。
	 * <p>该约定与常用的 HttpServlet {@code doGet} 或 {@code doPost} 重写方法基本相同。
	 * <p>本类会拦截这些调用，以确保执行异常处理和事件发布。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 * @see javax.servlet.http.HttpServlet#doGet
	 * @see javax.servlet.http.HttpServlet#doPost
	 */
	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;


	/**
	 * ApplicationListener 端点，仅接收来自此 Servlet 的 WebApplicationContext 的事件，
	 * 并委托给 FrameworkServlet 实例上的 {@code onApplicationEvent}。
	 */
	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			FrameworkServlet.this.onApplicationEvent(event);
		}
	}


	/**
	 * CallableProcessingInterceptor 实现，用于初始化和重置
	 * FrameworkServlet 的上下文持有者，即 LocaleContextHolder 和 RequestContextHolder。
	 */
	private class RequestBindingInterceptor implements CallableProcessingInterceptor {

		@Override
		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
				initContextHolders(request, buildLocaleContext(request),
						buildRequestAttributes(request, response, null));
			}
		}
		@Override
		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				resetContextHolders(request, null, null);
			}
		}
	}

}
