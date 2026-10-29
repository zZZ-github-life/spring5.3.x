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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.WebUtils;

/**
 * HTTP 请求处理器/控制器的中央调度器，例如用于 Web UI 控制器
 * 或基于 HTTP 的远程服务导出器。为处理 Web 请求分派到已注册的处理器，
 * 并提供便捷的映射和异常处理设施。
 *
 * <p>此 Servlet 非常灵活：几乎可以与任何工作流配合使用，
 * 只需安装相应的适配器类。它提供以下功能，
 * 使其有别于其他请求驱动的 Web MVC 框架：
 *
 * <ul>
 * <li>它基于 JavaBeans 配置机制。
 *
 * <li>它可以采用任何 {@link HandlerMapping} 实现——无论是预构建的，还是作为应用的一部分提供的
 * ——来控制请求到处理器对象的路由。默认是
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping} 和
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}。
 * HandlerMapping 对象可以在 Servlet 的应用上下文中定义为实现 HandlerMapping 接口的 bean，
 * 若存在默认 HandlerMapping 则将其覆盖。HandlerMapping 可以使用任意 bean 名称
 * （它们按类型进行检测）。
 *
 * <li>它可以采用任何 {@link HandlerAdapter}；这样就可以使用任何处理器接口。
 * 默认适配器为 {@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter}
 * 和 {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}，
 * 分别用于 Spring 的 {@link org.springframework.web.HttpRequestHandler} 和
 * {@link org.springframework.web.servlet.mvc.Controller} 接口。同时还会注册一个默认的
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}。
 * HandlerAdapter 对象可以作为 bean 添加到应用上下文中，覆盖默认的 HandlerAdapter。
 * 与 HandlerMapping 一样，HandlerAdapter 可以使用任意 bean 名称（它们按类型进行检测）。
 *
 * <li>调度器的异常解析策略可以通过 {@link HandlerExceptionResolver} 指定，
 * 例如将某些异常映射到错误页面。默认包括
 * {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver}、
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver} 和
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}。
 * 这些 HandlerExceptionResolver 可以通过应用上下文覆盖。
 * HandlerExceptionResolver 可以使用任意 bean 名称（它们按类型进行检测）。
 *
 * <li>其视图解析策略可以通过 {@link ViewResolver} 实现指定，
 * 将符号视图名称解析为 View 对象。默认是
 * {@link org.springframework.web.servlet.view.InternalResourceViewResolver}。
 * ViewResolver 对象可以作为 bean 添加到应用上下文中，覆盖默认的 ViewResolver。
 * ViewResolver 可以使用任意 bean 名称（它们按类型进行检测）。
 *
 * <li>如果用户未提供 {@link View} 或视图名称，则配置的
 * {@link RequestToViewNameTranslator} 会将当前请求转换为视图名称。
 * 对应的 bean 名称为 "viewNameTranslator"；默认是
 * {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}。
 *
 * <li>调度器解析 multipart 请求的策略由
 * {@link org.springframework.web.multipart.MultipartResolver} 实现决定。
 * 框架包含 Apache Commons FileUpload 和 Servlet 3 的实现；
 * 典型选择是 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}。
 * MultipartResolver 的 bean 名称为 "multipartResolver"；默认没有。
 *
 * <li>其区域设置解析策略由 {@link LocaleResolver} 决定。
 * 开箱即用的实现可基于 HTTP accept 头、Cookie 或会话工作。
 * LocaleResolver 的 bean 名称为 "localeResolver"；默认是
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}。
 *
 * <li>其主题解析策略由 {@link ThemeResolver} 决定。
 * 框架包含固定主题以及 Cookie 和会话存储的实现。
 * ThemeResolver 的 bean 名称为 "themeResolver"；默认是
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}。
 * </ul>
 *
 * <p><b>注意：只有当调度器中存在相应的 {@code HandlerMapping}（用于类型级注解）
 * 和/或 {@code HandlerAdapter}（用于方法级注解）时，才会处理 {@code @RequestMapping} 注解。</b>
 * 默认情况下即是如此。不过，如果你定义自定义的 {@code HandlerMapping}
 * 或 {@code HandlerAdapter}，则需要确保同时定义了相应的自定义
 * {@code RequestMappingHandlerMapping} 和/或 {@code RequestMappingHandlerAdapter}
 * ——前提是你打算使用 {@code @RequestMapping}。
 *
 * <p><b>一个 Web 应用可以定义任意数量的 DispatcherServlet。</b>
 * 每个 Servlet 都在自己的命名空间中运行，加载自己的应用上下文，
 * 其中包含映射、处理器等。只有由
 * {@link org.springframework.web.context.ContextLoaderListener} 加载的根应用上下文（如果有）会被共享。
 *
 * <p>从 Spring 3.1 开始，{@code DispatcherServlet} 可以注入一个 Web 应用上下文，
 * 而不再在内部自行创建。这在支持以编程方式注册 Servlet 实例的
 * Servlet 3.0+ 环境中很有用。详情请参见
 * {@link #DispatcherServlet(WebApplicationContext)} 的 javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/** 此命名空间的 bean 工厂中 MultipartResolver 对象的众所周知名称。 */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/** 此命名空间的 bean 工厂中 LocaleResolver 对象的众所周知名称。 */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/** 此命名空间的 bean 工厂中 ThemeResolver 对象的众所周知名称。 */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

	/**
	 * 此命名空间的 bean 工厂中 HandlerMapping 对象的众所周知名称。
	 * 仅在关闭 "detectAllHandlerMappings" 时使用。
	 * @see #setDetectAllHandlerMappings
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * 此命名空间的 bean 工厂中 HandlerAdapter 对象的众所周知名称。
	 * 仅在关闭 "detectAllHandlerAdapters" 时使用。
	 * @see #setDetectAllHandlerAdapters
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * 此命名空间的 bean 工厂中 HandlerExceptionResolver 对象的众所周知名称。
	 * 仅在关闭 "detectAllHandlerExceptionResolvers" 时使用。
	 * @see #setDetectAllHandlerExceptionResolvers
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * 此命名空间的 bean 工厂中 RequestToViewNameTranslator 对象的众所周知名称。
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * 此命名空间的 bean 工厂中 ViewResolver 对象的众所周知名称。
	 * 仅在关闭 "detectAllViewResolvers" 时使用。
	 * @see #setDetectAllViewResolvers
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * 此命名空间的 bean 工厂中 FlashMapManager 对象的众所周知名称。
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * 用于保存当前 Web 应用上下文的请求属性。
	 * 否则只能通过标签等方式获取全局的 Web 应用上下文。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#findWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * 用于保存当前 LocaleResolver 的请求属性，可供视图获取。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * 用于保存当前 ThemeResolver 的请求属性，可供视图获取。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * 用于保存当前 ThemeSource 的请求属性，可供视图获取。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * 请求属性的名称，该属性保存一个只读的 {@code Map<String,?>}，
	 * 其中包含上一次请求保存的 "input" flash 属性（如果有）。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * 请求属性的名称，该属性保存 "output" {@link FlashMap}，
	 * 其中包含要为后续请求保存的属性。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * 请求属性的名称，该属性保存 {@link FlashMapManager}。
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/**
	 * 请求属性的名称，该属性暴露一个由
	 * {@link HandlerExceptionResolver} 解析但未渲染任何视图
	 * （例如仅设置状态码）的 Exception。
	 */
	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

	/** 当请求找不到映射的处理器时使用的日志类别。 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 类路径资源的名称（相对于 DispatcherServlet 类），
	 * 该资源定义了 DispatcherServlet 的默认策略名称。
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

	/**
	 * DispatcherServlet 默认策略属性共有的前缀。
	 */
	private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";


	/** 当请求找不到映射的处理器时使用的额外日志记录器。 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/** 存储默认策略实现。 */
	@Nullable
	private static Properties defaultStrategies;

	/** 检测所有 HandlerMapping，还是仅期望 "handlerMapping" bean？。 */
	private boolean detectAllHandlerMappings = true;

	/** 检测所有 HandlerAdapter，还是仅期望 "handlerAdapter" bean？。 */
	private boolean detectAllHandlerAdapters = true;

	/** 检测所有 HandlerExceptionResolver，还是仅期望 "handlerExceptionResolver" bean？。 */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** 检测所有 ViewResolver，还是仅期望 "viewResolver" bean？。 */
	private boolean detectAllViewResolvers = true;

	/** 如果未找到可处理此请求的 Handler，是否抛出 NoHandlerFoundException？ *。 */
	private boolean throwExceptionIfNoHandlerFound = false;

	/** 是否在 include 请求之后清理请求属性？。 */
	private boolean cleanupAfterInclude = true;

	/** 此 Servlet 使用的 MultipartResolver。 */
	@Nullable
	private MultipartResolver multipartResolver;

	/** 此 Servlet 使用的 LocaleResolver。 */
	@Nullable
	private LocaleResolver localeResolver;

	/** 此 Servlet 使用的 ThemeResolver。 */
	@Nullable
	private ThemeResolver themeResolver;

	/** 此 Servlet 使用的 HandlerMapping 列表。 */
	@Nullable
	private List<HandlerMapping> handlerMappings;

	/** 此 Servlet 使用的 HandlerAdapter 列表。 */
	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	/** 此 Servlet 使用的 HandlerExceptionResolver 列表。 */
	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** 此 Servlet 使用的 RequestToViewNameTranslator。 */
	@Nullable
	private RequestToViewNameTranslator viewNameTranslator;

	/** 此 Servlet 使用的 FlashMapManager。 */
	@Nullable
	private FlashMapManager flashMapManager;

	/** 此 Servlet 使用的 ViewResolver 列表。 */
	@Nullable
	private List<ViewResolver> viewResolvers;

	private boolean parseRequestPath;


	/**
	 * 创建一个新的 {@code DispatcherServlet}，它将根据默认值
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
	 * 表示应使用哪些 {@code ApplicationContextInitializer} 类
	 * 在 refresh() 之前进一步配置内部应用上下文。
	 * @see #DispatcherServlet(WebApplicationContext)
	 */
	public DispatcherServlet() {
		super();
		setDispatchOptionsRequest(true);
	}

	/**
	 * 使用给定的 Web 应用上下文创建一个新的 {@code DispatcherServlet}。
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
	 * ConfigurableApplicationContext#refresh() 刷新}。如果它<strong>尚未</strong>
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
	 * {@code ApplicationContextInitializer} 都将被应用。</li>
	 * <li>如果上下文实现了 {@link ConfigurableApplicationContext}，
	 * 则会调用 {@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已被刷新，则上述操作都不会执行，
	 * 因为假定用户已根据其具体需求完成了（或未完成）这些操作。
	 * <p>用法示例请参见 {@link org.springframework.web.WebApplicationInitializer}。
	 * @param webApplicationContext the context to use
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
		setDispatchOptionsRequest(true);
	}


	/**
	 * 设置是否检测此 Servlet 上下文中的所有 HandlerMapping bean。
	 * 否则仅期望一个名为 "handlerMapping" 的 bean。
	 * <p>默认值为 "true"。如果你希望此 Servlet 只使用单个 HandlerMapping，
	 * 即使上下文中定义了多个 HandlerMapping bean，也请关闭此选项。
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * 设置是否检测此 Servlet 上下文中的所有 HandlerAdapter bean。
	 * 否则仅期望一个名为 "handlerAdapter" 的 bean。
	 * <p>默认值为 "true"。如果你希望此 Servlet 只使用单个 HandlerAdapter，
	 * 即使上下文中定义了多个 HandlerAdapter bean，也请关闭此选项。
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * 设置是否检测此 Servlet 上下文中的所有 HandlerExceptionResolver bean。
	 * 否则仅期望一个名为 "handlerExceptionResolver" 的 bean。
	 * <p>默认值为 "true"。如果你希望此 Servlet 只使用单个 HandlerExceptionResolver，
	 * 即使上下文中定义了多个 HandlerExceptionResolver bean，也请关闭此选项。
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * 设置是否检测此 Servlet 上下文中的所有 ViewResolver bean。
	 * 否则仅期望一个名为 "viewResolver" 的 bean。
	 * <p>默认值为 "true"。如果你希望此 Servlet 只使用单个 ViewResolver，
	 * 即使上下文中定义了多个 ViewResolver bean，也请关闭此选项。
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * 设置当未找到可处理此请求的 Handler 时是否抛出 NoHandlerFoundException。
	 * 随后可以通过 HandlerExceptionResolver 或
	 * {@code @ExceptionHandler} 控制器方法捕获该异常。
	 * <p>请注意，如果使用了 {@link org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler}，
	 * 那么请求将始终被转发到默认 Servlet，此时永远不会抛出 NoHandlerFoundException。
	 * <p>默认值为 "false"，意味着 DispatcherServlet 通过 Servlet 响应发送 NOT_FOUND 错误。
	 * @since 4.0
	 */
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * 设置是否在 include 请求之后清理请求属性，即 DispatcherServlet
	 * 在 include 请求中完成处理后，是否重置所有请求属性的原始状态。
	 * 否则只重置 DispatcherServlet 自身的请求属性，而不会重置 JSP 的模型属性
	 * 或视图设置的特殊属性（例如 JSTL 的属性）。
	 * <p>默认值为 "true"，强烈推荐使用。视图不应依赖（动态）include 所设置的请求属性。
	 * 这样，由被包含的控制器渲染的 JSP 视图可以使用任意模型属性，
	 * 即使名称与主 JSP 中的相同，也不会产生副作用。只有在有特殊需求时才关闭此选项，
	 * 例如有意允许主 JSP 访问由被包含的控制器渲染的 JSP 视图中的属性。
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}


	/**
	 * 此实现会调用 {@link #initStrategies}。
	 */
	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * 初始化此 Servlet 使用的策略对象。
	 * <p>可以在子类中重写，以初始化更多策略对象。
	 */
	protected void initStrategies(ApplicationContext context) {
		initMultipartResolver(context);
		initLocaleResolver(context);
		initThemeResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initRequestToViewNameTranslator(context);
		initViewResolvers(context);
		initFlashMapManager(context);
	}

	/**
	 * 初始化此类使用的 MultipartResolver。
	 * <p>如果此命名空间的 BeanFactory 中没有定义具有给定名称的 bean，
	 * 则不会提供 multipart 处理。
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.multipartResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isTraceEnabled()) {
				logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");
			}
		}
	}

	/**
	 * 初始化此类使用的 LocaleResolver。
	 * <p>如果此命名空间的 BeanFactory 中没有定义具有给定名称的 bean，
	 * 则默认使用 AcceptHeaderLocaleResolver。
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.localeResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.localeResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No LocaleResolver '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的 ThemeResolver。
	 * <p>如果此命名空间的 BeanFactory 中没有定义具有给定名称的 bean，
	 * 则默认使用 FixedThemeResolver。
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.themeResolver);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.themeResolver.getClass().getSimpleName());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No ThemeResolver '" + THEME_RESOLVER_BEAN_NAME +
						"': using default [" + this.themeResolver.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的 HandlerMapping。
	 * <p>如果此命名空间的 BeanFactory 中没有定义任何 HandlerMapping bean，
	 * 则默认使用 BeanNameUrlHandlerMapping。
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<>(matchingBeans.values());
				// We keep HandlerMappings in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerMapping later.
			}
		}

		// Ensure we have at least one HandlerMapping, by registering
		// a default HandlerMapping if no other mappings are found.
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerMappings declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}

		for (HandlerMapping mapping : this.handlerMappings) {
			if (mapping.usesPathPatterns()) {
				this.parseRequestPath = true;
				break;
			}
		}
	}

	/**
	 * 初始化此类使用的 HandlerAdapter。
	 * <p>如果此命名空间的 BeanFactory 中没有定义任何 HandlerAdapter bean，
	 * 则默认使用 SimpleControllerHandlerAdapter。
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<>(matchingBeans.values());
				// We keep HandlerAdapters in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerAdapter later.
			}
		}

		// Ensure we have at least some HandlerAdapters, by registering
		// default HandlerAdapters if no other adapters are found.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此类使用的 HandlerExceptionResolver。
	 * <p>如果此命名空间的 BeanFactory 中没有定义具有给定名称的 bean，
	 * 则默认不使用任何异常解析器。
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
				// We keep HandlerExceptionResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, no HandlerExceptionResolver is fine too.
			}
		}

		// Ensure we have at least some HandlerExceptionResolvers, by registering
		// default HandlerExceptionResolvers if no other resolvers are found.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此 Servlet 实例使用的 RequestToViewNameTranslator。
	 * <p>如果未配置任何实现，则默认使用 DefaultRequestToViewNameTranslator。
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.viewNameTranslator.getClass().getSimpleName());
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.viewNameTranslator);
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No RequestToViewNameTranslator '" + REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME +
						"': using default [" + this.viewNameTranslator.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的 ViewResolver。
	 * <p>如果此命名空间的 BeanFactory 中没有定义任何 ViewResolver bean，
	 * 则默认使用 InternalResourceViewResolver。
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<>(matchingBeans.values());
				// We keep ViewResolvers in sorted order.
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
			}
		}
		else {
			try {
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				this.viewResolvers = Collections.singletonList(vr);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default ViewResolver later.
			}
		}

		// Ensure we have at least one ViewResolver, by registering
		// a default ViewResolver if no other resolvers are found.
		if (this.viewResolvers == null) {
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No ViewResolvers declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此 Servlet 实例使用的 {@link FlashMapManager}。
	 * <p>如果未配置任何实现，则默认使用
	 * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}。
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.flashMapManager.getClass().getSimpleName());
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.flashMapManager);
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No FlashMapManager '" + FLASH_MAP_MANAGER_BEAN_NAME +
						"': using default [" + this.flashMapManager.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 返回此 Servlet 的 ThemeSource（如果有）；否则返回 {@code null}。
	 * <p>默认将 WebApplicationContext 作为 ThemeSource 返回，
	 * 前提是它实现了 ThemeSource 接口。
	 * @return the ThemeSource, if any
	 * @see #getWebApplicationContext()
	 */
	@Nullable
	public final ThemeSource getThemeSource() {
		return (getWebApplicationContext() instanceof ThemeSource ? (ThemeSource) getWebApplicationContext() : null);
	}

	/**
	 * 获取此 Servlet 的 MultipartResolver（如果有）。
	 * @return the MultipartResolver used by this servlet, or {@code null} if none
	 * (indicating that no multipart support is available)
	 */
	@Nullable
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * 返回在 {@link WebApplicationContext} 中按类型检测到的、
	 * 或基于 {@literal DispatcherServlet.properties} 中的默认策略集初始化的
	 * 已配置 {@link HandlerMapping} bean。
	 * <p><strong>注意：</strong>如果在 {@link #onRefresh(ApplicationContext)} 之前调用，
	 * 此方法可能返回 {@code null}。
	 * @return an immutable list with the configured mappings, or {@code null}
	 * if not initialized yet
	 * @since 5.0
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? Collections.unmodifiableList(this.handlerMappings) : null);
	}

	/**
	 * 返回给定策略接口的默认策略对象。
	 * <p>默认实现委托给 {@link #getDefaultStrategies}，
	 * 并期望列表中只有一个对象。
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the corresponding strategy object
	 * @see #getDefaultStrategies
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * 为给定策略接口创建一个默认策略对象列表。
	 * <p>默认实现使用 "DispatcherServlet.properties" 文件（与 DispatcherServlet 类位于同一包中）
	 * 来确定类名。它通过上下文的 BeanFactory 实例化策略对象。
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the List of corresponding strategy objects
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		if (defaultStrategies == null) {
			try {
				// Load default strategy implementations from properties file.
				// This is currently strictly internal and not meant to be customized
				// by application developers.
				ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
				defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
			}
		}

		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
							"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Unresolvable class definition for DispatcherServlet's default strategy class [" +
							className + "] for interface [" + key + "]", err);
				}
			}
			return strategies;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * 创建一个默认策略。
	 * <p>默认实现使用
	 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}。
	 * @param context the current WebApplicationContext
	 * @param clazz the strategy implementation class to instantiate
	 * @return the fully configured strategy instance
	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * 暴露特定于 DispatcherServlet 的请求属性，并将实际分派委托给 {@link #doDispatch}。
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logRequest(request);

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		if (this.flashMapManager != null) {
			FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
			if (inputFlashMap != null) {
				request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
			}
			request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
			request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
		}

		RequestPath previousRequestPath = null;
		if (this.parseRequestPath) {
			previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.parseAndCache(request);
		}

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// Restore the original attribute snapshot, in case of an include.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
			if (this.parseRequestPath) {
				ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
			}
		}
	}

	private void logRequest(HttpServletRequest request) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String params;
			String contentType = request.getContentType();
			if (StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
				params = "multipart";
			}
			else if (isEnableLoggingRequestDetails()) {
				params = request.getParameterMap().entrySet().stream()
						.map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
						.collect(Collectors.joining(", "));
			}
			else {
				// Avoid request body parsing for form data
				params = (StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_FORM_URLENCODED_VALUE) ||
						!request.getParameterMap().isEmpty() ? "masked" : "");
			}

			String queryString = request.getQueryString();
			String queryClause = (StringUtils.hasLength(queryString) ? "?" + queryString : "");
			String dispatchType = (!DispatcherType.REQUEST.equals(request.getDispatcherType()) ?
					"\"" + request.getDispatcherType() + "\" dispatch for " : "");
			String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) +
					queryClause + "\", parameters={" + params + "}");

			if (traceOn) {
				List<String> values = Collections.list(request.getHeaderNames());
				String headers = values.size() > 0 ? "masked" : "";
				if (isEnableLoggingRequestDetails()) {
					headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name)))
							.collect(Collectors.joining(", "));
				}
				return message + ", headers={" + headers + "} in DispatcherServlet '" + getServletName() + "'";
			}
			else {
				return message;
			}
		});
	}

	/**
	 * 执行到处理器的实际分派。
	 * <p>处理器通过按顺序应用此 Servlet 的 HandlerMapping 来获取。
	 * HandlerAdapter 则通过查询此 Servlet 已安装的 HandlerAdapter，
	 * 找出第一个支持该处理器类的适配器来获取。
	 * <p>所有 HTTP 方法都由该方法处理。具体接受哪些方法，
	 * 由 HandlerAdapter 或处理器本身决定。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 */
	@SuppressWarnings("deprecation")
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				processedRequest = checkMultipart(request);
				multipartRequestParsed = (processedRequest != request);

				// Determine handler for the current request.
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
				String method = request.getMethod();
				boolean isGet = HttpMethod.GET.matches(method);
				if (isGet || HttpMethod.HEAD.matches(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// Actually invoke the handler.
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}

				applyDefaultViewName(processedRequest, mv);
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	/**
	 * 我们需要进行视图名称转换吗？
	 */
	private void applyDefaultViewName(HttpServletRequest request, @Nullable ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			String defaultViewName = getDefaultViewName(request);
			if (defaultViewName != null) {
				mv.setViewName(defaultViewName);
			}
		}
	}

	/**
	 * 处理处理器选择和处理器调用的结果，
	 * 该结果要么是 ModelAndView，要么是需要解析为 ModelAndView 的 Exception。
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			@Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
			@Nullable Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		if (mv != null && !mv.wasCleared()) {
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("No view rendering, null ModelAndView returned.");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
			// Exception (if any) is already handled..
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * 为给定请求构建一个 LocaleContext，将请求的主区域设置作为当前区域设置暴露。
	 * <p>默认实现使用调度器的 LocaleResolver 获取当前区域设置，
	 * 该设置可能在请求过程中发生变化。
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		LocaleResolver lr = this.localeResolver;
		if (lr instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) lr).resolveLocaleContext(request);
		}
		else {
			return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
		}
	}

	/**
	 * 将请求转换为 multipart 请求，并使 multipart 解析器可用。
	 * <p>如果未设置 multipart 解析器，则直接使用现有的请求。
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 * @see MultipartResolver#resolveMultipart
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				if (DispatcherType.REQUEST.equals(request.getDispatcherType())) {
					logger.trace("Request already resolved to MultipartHttpServletRequest, e.g. by MultipartFilter");
				}
			}
			else if (hasMultipartException(request)) {
				logger.debug("Multipart resolution previously failed for current request - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				try {
					return this.multipartResolver.resolveMultipart(request);
				}
				catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// Keep processing error dispatch with regular request handle below
					}
					else {
						throw ex;
					}
				}
			}
		}
		// If not returned before: return original request.
		return request;
	}

	/**
	 * 检查 "javax.servlet.error.exception" 属性中是否存在 multipart 异常。
	 */
	private boolean hasMultipartException(HttpServletRequest request) {
		Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
		while (error != null) {
			if (error instanceof MultipartException) {
				return true;
			}
			error = error.getCause();
		}
		return false;
	}

	/**
	 * 清理给定 multipart 请求所使用的所有资源（如果有）。
	 * @param request current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	protected void cleanupMultipart(HttpServletRequest request) {
		if (this.multipartResolver != null) {
			MultipartHttpServletRequest multipartRequest =
					WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
			if (multipartRequest != null) {
				this.multipartResolver.cleanupMultipart(multipartRequest);
			}
		}
	}

	/**
	 * 返回此请求的 HandlerExecutionChain。
	 * <p>按顺序尝试所有处理器映射。
	 * @param request current HTTP request
	 * @return the HandlerExecutionChain, or {@code null} if no handler could be found
	 */
	@Nullable
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			for (HandlerMapping mapping : this.handlerMappings) {
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					return handler;
				}
			}
		}
		return null;
	}

	/**
	 * 未找到处理器 &rarr; 设置合适的 HTTP 响应状态。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if preparing the response failed
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
		}
		if (this.throwExceptionIfNoHandlerFound) {
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * 返回此处理器对象对应的 HandlerAdapter。
	 * @param handler the handler object to find an adapter for
	 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter adapter : this.handlerAdapters) {
				if (adapter.supports(handler)) {
					return adapter;
				}
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * 通过已注册的 HandlerExceptionResolver 确定错误 ModelAndView。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
	 * (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to
	 * @throws Exception if no error ModelAndView found
	 */
	@Nullable
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) throws Exception {

		// Success and error responses may use different content types
		request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		if (this.handlerExceptionResolvers != null) {
			for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
				exMv = resolver.resolveException(request, response, handler, ex);
				if (exMv != null) {
					break;
				}
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				String defaultViewName = getDefaultViewName(request);
				if (defaultViewName != null) {
					exMv.setViewName(defaultViewName);
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using resolved error view: " + exMv, ex);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Using resolved error view: " + exMv);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

	/**
	 * 渲染给定的 ModelAndView。
	 * <p>这是处理请求的最后阶段。可能涉及按名称解析视图。
	 * @param mv the ModelAndView to render
	 * @param request current HTTP servlet request
	 * @param response current HTTP servlet response
	 * @throws ServletException if view is missing or cannot be resolved
	 * @throws Exception if there's a problem rendering the view
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale =
				(this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
		response.setLocale(locale);

		View view;
		String viewName = mv.getViewName();
		if (viewName != null) {
			// We need to resolve the view name.
			view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view [" + view + "] ");
		}
		try {
			if (mv.getStatus() != null) {
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, mv.getStatus());
				response.setStatus(mv.getStatus().value());
			}
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "]", ex);
			}
			throw ex;
		}
	}

	/**
	 * 将提供的请求转换为默认视图名称。
	 * @param request current HTTP servlet request
	 * @return the view name (or {@code null} if no default found)
	 * @throws Exception if view name translation failed
	 */
	@Nullable
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return (this.viewNameTranslator != null ? this.viewNameTranslator.getViewName(request) : null);
	}

	/**
	 * 将给定的视图名称解析为 View 对象（用于渲染）。
	 * <p>默认实现会询问此调度器的所有 ViewResolver。
	 * 可以重写以实现自定义解析策略，可能基于特定的模型属性或请求参数。
	 * @param viewName the name of the view to resolve
	 * @param model the model to be passed to the view
	 * @param locale the current locale
	 * @param request current HTTP servlet request
	 * @return the View object, or {@code null} if none found
	 * @throws Exception if the view cannot be resolved
	 * (typically in case of problems creating an actual View object)
	 * @see ViewResolver#resolveViewName
	 */
	@Nullable
	protected View resolveViewName(String viewName, @Nullable Map<String, Object> model,
			Locale locale, HttpServletRequest request) throws Exception {

		if (this.viewResolvers != null) {
			for (ViewResolver viewResolver : this.viewResolvers) {
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					return view;
				}
			}
		}
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
			@Nullable HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * 在 include 之后恢复请求属性。
	 * @param request current HTTP request
	 * @param attributesSnapshot the snapshot of the request attributes before the include
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?, ?> attributesSnapshot) {
		// Need to copy into separate Collection here, to avoid side effects
		// on the Enumeration when removing attributes.
		Set<String> attrsToCheck = new HashSet<>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
				attrsToCheck.add(attrName);
			}
		}

		// Add attributes that may have been removed
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// Iterate over the attributes to check, restoring the original value
		// or removing the attribute, respectively, if appropriate.
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null) {
				request.removeAttribute(attrName);
			}
			else if (attrValue != request.getAttribute(attrName)) {
				request.setAttribute(attrName, attrValue);
			}
		}
	}

	private static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return uri;
	}

}
