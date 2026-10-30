/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 这是提供 MVC Java 配置背后支持的主要类。
 * 通常通过在应用的 {@link Configuration @Configuration} 类上添加
 * {@link EnableWebMvc @EnableWebMvc} 来导入它。另一种更高级的选择是
 * 直接继承此类并按需重写方法，同时记得为子类添加
 * {@link Configuration @Configuration}，并为被重写的 {@link Bean @Bean} 方法
 * 添加 {@link Bean @Bean}。更多详情请参见 {@link EnableWebMvc @EnableWebMvc} 的 javadoc。
 *
 * <p>此类注册以下 {@link HandlerMapping HandlerMappings}：</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}
 * 排序为 0，用于将请求映射到注解控制器方法。
 * <li>{@link HandlerMapping}
 * 排序为 1，用于将 URL 路径直接映射到视图名称。
 * <li>{@link BeanNameUrlHandlerMapping}
 * 排序为 2，用于将 URL 路径映射到控制器 bean 名称。
 * <li>{@link RouterFunctionMapping}
 * 排序为 3，用于映射{@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}。
 * <li>{@link HandlerMapping}
 * 排序为 {@code Integer.MAX_VALUE-1}，用于提供静态资源请求。
 * <li>{@link HandlerMapping}
 * 排序为 {@code Integer.MAX_VALUE}，用于将请求转发到默认 servlet。
 * </ul>
 *
 * <p>注册以下 {@link HandlerAdapter HandlerAdapters}：
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}
 * 用于以注解控制器方法处理请求。
 * <li>{@link HttpRequestHandlerAdapter}
 * 用于以 {@link HttpRequestHandler HttpRequestHandlers} 处理请求。
 * <li>{@link SimpleControllerHandlerAdapter}
 * 用于以基于接口的 {@link Controller Controllers} 处理请求。
 * <li>{@link HandlerFunctionAdapter}
 * 用于以{@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}处理请求。
 * </ul>
 *
 * <p>注册一个 {@link HandlerExceptionResolverComposite}，其包含以下异常解析器链：
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver} 用于通过
 * {@link org.springframework.web.bind.annotation.ExceptionHandler} 方法处理异常。
 * <li>{@link ResponseStatusExceptionResolver} 用于处理带有
 * {@link org.springframework.web.bind.annotation.ResponseStatus} 注解的异常。
 * <li>{@link DefaultHandlerExceptionResolver} 用于解析已知的 Spring 异常类型
 * </ul>
 *
 * <p>注册一个 {@link AntPathMatcher} 和一个 {@link UrlPathHelper}，
 * 以供以下组件使用：
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}，
 * <li>用于 ViewController 的 {@link HandlerMapping}
 * <li>以及用于提供资源的 {@link HandlerMapping}
 * </ul>
 * 请注意，这些 bean 可以通过 {@link PathMatchConfigurer} 进行配置。
 *
 * <p>{@link RequestMappingHandlerAdapter} 和
 * {@link ExceptionHandlerExceptionResolver} 默认都会使用以下内容的默认实例进行配置：
 * <ul>
 * <li>一个 {@link ContentNegotiationManager}
 * <li>一个 {@link DefaultFormattingConversionService}
 * <li>一个 {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}
 * （如果类路径上有 JSR-303 实现可用）
 * <li>一系列 {@link HttpMessageConverter HttpMessageConverters}，具体取决于类路径上
 * 可用的第三方库。
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 3.1
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 */
public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring 忽略 XML，
	 * 即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	private static final boolean romePresent;

	private static final boolean jaxb2Present;

	private static final boolean jackson2Present;

	private static final boolean jackson2XmlPresent;

	private static final boolean jackson2SmilePresent;

	private static final boolean jackson2CborPresent;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationJsonPresent;

	static {
		ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private List<Object> interceptors;

	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private List<HandlerMethodArgumentResolver> argumentResolvers;

	@Nullable
	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	@Nullable
	private List<HttpMessageConverter<?>> messageConverters;

	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;

	@Nullable
	private AsyncSupportConfigurer asyncSupportConfigurer;


	/**
	 * 设置 Spring {@link ApplicationContext}，例如用于资源加载。
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回关联的 Spring {@link ApplicationContext}。
	 * @since 4.2
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 设置 {@link javax.servlet.ServletContext}，例如用于资源处理、
	 * 查找文件扩展名等。
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 返回关联的 {@link javax.servlet.ServletContext}。
	 * @since 4.2
	 */
	@Nullable
	public final ServletContext getServletContext() {
		return this.servletContext;
	}


	/**
	 * 返回一个排序为 0 的 {@link RequestMappingHandlerMapping}，
	 * 用于将请求映射到注解控制器。
	 */
	@Bean
	@SuppressWarnings("deprecation")
	public RequestMappingHandlerMapping requestMappingHandlerMapping(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		mapping.setContentNegotiationManager(contentNegotiationManager);
		mapping.setCorsConfigurations(getCorsConfigurations());

		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		if (pathConfig.getPatternParser() != null) {
			mapping.setPatternParser(pathConfig.getPatternParser());
		}
		else {
			mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());

			Boolean useSuffixPatternMatch = pathConfig.isUseSuffixPatternMatch();
			if (useSuffixPatternMatch != null) {
				mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
			}
			Boolean useRegisteredSuffixPatternMatch = pathConfig.isUseRegisteredSuffixPatternMatch();
			if (useRegisteredSuffixPatternMatch != null) {
				mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);
			}
		}
		Boolean useTrailingSlashMatch = pathConfig.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}
		if (pathConfig.getPathPrefixes() != null) {
			mapping.setPathPrefixes(pathConfig.getPathPrefixes());
		}

		return mapping;
	}

	/**
	 * 用于插入自定义 {@link RequestMappingHandlerMapping} 子类的受保护方法。
	 * @since 4.0
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	/**
	 * 提供对共享处理器拦截器的访问，这些拦截器用于配置
	 * {@link HandlerMapping} 实例。
	 * <p>此方法不能被重写；请改用 {@link #addInterceptors}。
	 */
	protected final Object[] getInterceptors(
			FormattingConversionService mvcConversionService,
			ResourceUrlProvider mvcResourceUrlProvider) {

		if (this.interceptors == null) {
			InterceptorRegistry registry = new InterceptorRegistry();
			addInterceptors(registry);
			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
			registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));
			this.interceptors = registry.getInterceptors();
		}
		return this.interceptors.toArray();
	}

	/**
	 * 重写此方法以添加用于控制器调用前后处理的 Spring MVC 拦截器。
	 * @see InterceptorRegistry
	 */
	protected void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 用于构建 {@link PathMatchConfigurer} 的回调。
	 * 委托给 {@link #configurePathMatch}。
	 * @since 4.1
	 */
	protected PathMatchConfigurer getPathMatchConfigurer() {
		if (this.pathMatchConfigurer == null) {
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatch(this.pathMatchConfigurer);
		}
		return this.pathMatchConfigurer;
	}

	/**
	 * 重写此方法以配置路径匹配选项。
	 * @since 4.0.3
	 * @see PathMatchConfigurer
	 */
	protected void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 返回一个全局 {@link PathPatternParser} 实例，用于解析要与
	 * {@link org.springframework.http.server.RequestPath} 匹配的模式。
	 * 返回的实例可以通过
	 * {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 * @since 5.3.4
	 */
	@Bean
	public PathPatternParser mvcPatternParser() {
		return getPathMatchConfigurer().getPatternParserOrDefault();
	}

	/**
	 * 返回一个全局 {@link UrlPathHelper} 实例，用于解析应用的请求映射路径。
	 * 该实例可以通过 {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 * <p><b>注意：</b>仅在未{@link PathMatchConfigurer#setPatternParser 启用}
	 * 解析模式时使用。
	 * @since 4.1
	 */
	@Bean
	public UrlPathHelper mvcUrlPathHelper() {
		return getPathMatchConfigurer().getUrlPathHelperOrDefault();
	}

	/**
	 * 返回一个全局 {@link PathMatcher} 实例，用于使用字符串模式进行 URL 路径匹配。
	 * 返回的实例可以通过 {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 * <p><b>注意：</b>仅在未{@link PathMatchConfigurer#setPatternParser 启用}
	 * 解析模式时使用。
	 * @since 4.1
	 */
	@Bean
	public PathMatcher mvcPathMatcher() {
		return getPathMatchConfigurer().getPathMatcherOrDefault();
	}

	/**
	 * 返回一个 {@link ContentNegotiationManager} 实例，用于确定给定请求中
	 * 请求的{@linkplain MediaType 媒体类型}。
	 */
	@Bean
	public ContentNegotiationManager mvcContentNegotiationManager() {
		if (this.contentNegotiationManager == null) {
			ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer(this.servletContext);
			configurer.mediaTypes(getDefaultMediaTypes());
			configureContentNegotiation(configurer);
			this.contentNegotiationManager = configurer.buildContentNegotiationManager();
		}
		return this.contentNegotiationManager;
	}

	protected Map<String, MediaType> getDefaultMediaTypes() {
		Map<String, MediaType> map = new HashMap<>(4);
		if (romePresent) {
			map.put("atom", MediaType.APPLICATION_ATOM_XML);
			map.put("rss", MediaType.APPLICATION_RSS_XML);
		}
		if (!shouldIgnoreXml && (jaxb2Present || jackson2XmlPresent)) {
			map.put("xml", MediaType.APPLICATION_XML);
		}
		if (jackson2Present || gsonPresent || jsonbPresent) {
			map.put("json", MediaType.APPLICATION_JSON);
		}
		if (jackson2SmilePresent) {
			map.put("smile", MediaType.valueOf("application/x-jackson-smile"));
		}
		if (jackson2CborPresent) {
			map.put("cbor", MediaType.APPLICATION_CBOR);
		}
		return map;
	}

	/**
	 * 重写此方法以配置内容协商。
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 返回一个排序为 1 的处理器映射，用于将 URL 路径直接映射到视图名称。
	 * 要配置视图控制器，请重写 {@link #addViewControllers}。
	 */
	@Bean
	@Nullable
	public HandlerMapping viewControllerHandlerMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
		addViewControllers(registry);

		AbstractHandlerMapping handlerMapping = registry.buildHandlerMapping();
		if (handlerMapping == null) {
			return null;
		}
		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		if (pathConfig.getPatternParser() != null) {
			handlerMapping.setPatternParser(pathConfig.getPatternParser());
		}
		else {
			handlerMapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			handlerMapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}
		handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		handlerMapping.setCorsConfigurations(getCorsConfigurations());
		return handlerMapping;
	}

	/**
	 * 重写此方法以添加视图控制器。
	 * @see ViewControllerRegistry
	 */
	protected void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * 返回一个排序为 2 的 {@link BeanNameUrlHandlerMapping}，
	 * 用于将 URL 路径映射到控制器 bean 名称。
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);

		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		if (pathConfig.getPatternParser() != null) {
			mapping.setPatternParser(pathConfig.getPatternParser());
		}
		else {
			mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}

		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		mapping.setCorsConfigurations(getCorsConfigurations());
		return mapping;
	}

	/**
	 * 返回一个排序为 3 的 {@link RouterFunctionMapping}，用于映射
	 * {@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}。
	 * 考虑重写以下其他更细粒度的方法之一：
	 * <ul>
	 * <li>{@link #addInterceptors} 用于添加处理器拦截器。
	 * <li>{@link #addCorsMappings} 用于配置跨域请求处理。
	 * <li>{@link #configureMessageConverters} 用于添加自定义消息转换器。
	 * <li>{@link #configurePathMatch(PathMatchConfigurer)} 用于自定义 {@link PathPatternParser}。
	 * </ul>
	 * @since 5.2
	 */
	@Bean
	public RouterFunctionMapping routerFunctionMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		RouterFunctionMapping mapping = new RouterFunctionMapping();
		mapping.setOrder(3);
		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		mapping.setCorsConfigurations(getCorsConfigurations());
		mapping.setMessageConverters(getMessageConverters());

		PathPatternParser patternParser = getPathMatchConfigurer().getPatternParser();
		if (patternParser != null) {
			mapping.setPatternParser(patternParser);
		}

		return mapping;
	}

	/**
	 * 返回一个排序为 Integer.MAX_VALUE-1 的处理器映射，其中包含已映射的资源处理器。
	 * 要配置资源处理，请重写 {@link #addResourceHandlers}。
	 */
	@Bean
	@Nullable
	public HandlerMapping resourceHandlerMapping(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		Assert.state(this.servletContext != null, "No ServletContext set");

		PathMatchConfigurer pathConfig = getPathMatchConfigurer();

		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext,
				this.servletContext, contentNegotiationManager, pathConfig.getUrlPathHelper());
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping == null) {
			return null;
		}
		if (pathConfig.getPatternParser() != null) {
			handlerMapping.setPatternParser(pathConfig.getPatternParser());
		}
		else {
			handlerMapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			handlerMapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}
		handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		handlerMapping.setCorsConfigurations(getCorsConfigurations());
		return handlerMapping;
	}

	/**
	 * 重写此方法以添加用于提供静态资源的资源处理器。
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 供 MVC 分发器使用的 {@link ResourceUrlProvider} bean。
	 * @since 4.1
	 */
	@Bean
	public ResourceUrlProvider mvcResourceUrlProvider() {
		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		urlProvider.setUrlPathHelper(getPathMatchConfigurer().getUrlPathHelperOrDefault());
		urlProvider.setPathMatcher(getPathMatchConfigurer().getPathMatcherOrDefault());
		return urlProvider;
	}

	/**
	 * 返回一个排序为 Integer.MAX_VALUE 的处理器映射，其中包含已映射的
	 * 默认 servlet 处理器。要配置"default" Servlet 处理，
	 * 请重写 {@link #configureDefaultServletHandling}。
	 */
	@Bean
	@Nullable
	public HandlerMapping defaultServletHandlerMapping() {
		Assert.state(this.servletContext != null, "No ServletContext set");
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(this.servletContext);
		configureDefaultServletHandling(configurer);
		return configurer.buildHandlerMapping();
	}

	/**
	 * 重写此方法以配置"default" Servlet 处理。
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 返回一个 {@link RequestMappingHandlerAdapter}，用于通过注解控制器方法处理请求。
	 * 考虑重写以下其他更细粒度的方法之一：
	 * <ul>
	 * <li>{@link #addArgumentResolvers} 用于添加自定义参数解析器。
	 * <li>{@link #addReturnValueHandlers} 用于添加自定义返回值处理器。
	 * <li>{@link #configureMessageConverters} 用于添加自定义消息转换器。
	 * </ul>
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcValidator") Validator validator) {

		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setContentNegotiationManager(contentNegotiationManager);
		adapter.setMessageConverters(getMessageConverters());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
		adapter.setCustomArgumentResolvers(getArgumentResolvers());
		adapter.setCustomReturnValueHandlers(getReturnValueHandlers());

		if (jackson2Present) {
			adapter.setRequestBodyAdvice(Collections.singletonList(new JsonViewRequestBodyAdvice()));
			adapter.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}

		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
		if (configurer.getTaskExecutor() != null) {
			adapter.setTaskExecutor(configurer.getTaskExecutor());
		}
		if (configurer.getTimeout() != null) {
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}
		adapter.setCallableInterceptors(configurer.getCallableInterceptors());
		adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());

		return adapter;
	}

	/**
	 * 用于插入自定义 {@link RequestMappingHandlerAdapter} 子类的受保护方法。
	 * @since 4.3
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * 返回一个 {@link HandlerFunctionAdapter}，用于通过
	 * {@linkplain org.springframework.web.servlet.function.HandlerFunction 处理器函数}处理请求。
	 * @since 5.2
	 */
	@Bean
	public HandlerFunctionAdapter handlerFunctionAdapter() {
		HandlerFunctionAdapter adapter = new HandlerFunctionAdapter();

		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
		if (configurer.getTimeout() != null) {
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}
		return adapter;
	}

	/**
	 * 返回用于初始化所有 {@link WebDataBinder} 实例的
	 * {@link ConfigurableWebBindingInitializer}。
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
			FormattingConversionService mvcConversionService, Validator mvcValidator) {

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(mvcConversionService);
		initializer.setValidator(mvcValidator);
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
		if (messageCodesResolver != null) {
			initializer.setMessageCodesResolver(messageCodesResolver);
		}
		return initializer;
	}

	/**
	 * 重写此方法以提供自定义的 {@link MessageCodesResolver}。
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 返回用于注解控制器的 {@link FormattingConversionService}。
	 * <p>作为重写此方法的替代方案，请参见 {@link #addFormatters}。
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		addFormatters(conversionService);
		return conversionService;
	}

	/**
	 * 重写此方法以向通用的 {@link FormattingConversionService} 添加自定义的
	 * {@link Converter} 和/或 {@link Formatter} 委托。
	 * @see #mvcConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 返回一个全局 {@link Validator} 实例，例如用于校验
	 * {@code @ModelAttribute} 和 {@code @RequestBody} 方法参数。
	 * 首先委托给 {@link #getValidator()}，如果该方法返回 {@code null}，
	 * 则在创建 {@code OptionalValidatorFactoryBean} 之前检查类路径上是否存在
	 * JSR-303 实现。如果 JSR-303 实现不可用，则返回一个空操作的 {@link Validator}。
	 */
	@Bean
	public Validator mvcValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
				}
				catch (ClassNotFoundException | LinkageError ex) {
					throw new BeanInitializationException("Failed to resolve default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new NoOpValidator();
			}
		}
		return validator;
	}

	/**
	 * 重写此方法以提供自定义的 {@link Validator}。
	 */
	@Nullable
	protected Validator getValidator() {
		return null;
	}

	/**
	 * 提供对共享自定义参数解析器的访问，这些解析器由
	 * {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 使用。
	 * <p>此方法不能被重写；请改用 {@link #addArgumentResolvers}。
	 * @since 4.3
	 */
	protected final List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		if (this.argumentResolvers == null) {
			this.argumentResolvers = new ArrayList<>();
			addArgumentResolvers(this.argumentResolvers);
		}
		return this.argumentResolvers;
	}

	/**
	 * 添加自定义的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}，
	 * 与默认注册的解析器一起使用。
	 * <p>自定义参数解析器会在内置解析器之前调用，但那些依赖注解存在的解析器
	 * （例如 {@code @RequestParameter}、{@code @PathVariable} 等）除外。
	 * 后者可以通过直接配置 {@link RequestMappingHandlerAdapter} 来自定义。
	 * @param argumentResolvers 自定义转换器的列表（初始为空列表）
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * 提供对共享返回值处理器的访问，这些处理器由
	 * {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 使用。
	 * <p>此方法不能被重写；请改用 {@link #addReturnValueHandlers}。
	 * @since 4.3
	 */
	protected final List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		if (this.returnValueHandlers == null) {
			this.returnValueHandlers = new ArrayList<>();
			addReturnValueHandlers(this.returnValueHandlers);
		}
		return this.returnValueHandlers;
	}

	/**
	 * 添加自定义的 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}，
	 * 与默认注册的处理器一起使用。
	 * <p>自定义返回值处理器会在内置处理器之前调用，但那些依赖注解存在的处理器
	 * （例如 {@code @ResponseBody}、{@code @ModelAttribute} 等）除外。
	 * 后者可以通过直接配置 {@link RequestMappingHandlerAdapter} 来自定义。
	 * @param returnValueHandlers 自定义处理器的列表（初始为空列表）
	 */
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * 提供对共享 {@link HttpMessageConverter HttpMessageConverters} 的访问，
	 * 这些转换器由 {@link RequestMappingHandlerAdapter} 和
	 * {@link ExceptionHandlerExceptionResolver} 使用。
	 * <p>此方法不能被重写；请改用 {@link #configureMessageConverters}。
	 * 另请参见 {@link #addDefaultHttpMessageConverters} 以添加默认消息转换器。
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<>();
			configureMessageConverters(this.messageConverters);
			if (this.messageConverters.isEmpty()) {
				addDefaultHttpMessageConverters(this.messageConverters);
			}
			extendMessageConverters(this.messageConverters);
		}
		return this.messageConverters;
	}

	/**
	 * 重写此方法以添加自定义的 {@link HttpMessageConverter HttpMessageConverters}，
	 * 供 {@link RequestMappingHandlerAdapter} 和
	 * {@link ExceptionHandlerExceptionResolver} 使用。
	 * <p>向列表中添加转换器会关闭原本会默认注册的转换器。
	 * 另请参见 {@link #addDefaultHttpMessageConverters} 以添加默认消息转换器。
	 * @param converters 要向其添加消息转换器的列表（初始为空列表）
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 重写此方法以在转换器列表配置完成后扩展或修改该列表。
	 * 例如，这对于先注册默认转换器，然后通过此方法插入自定义转换器很有用。
	 * @param converters 要扩展的已配置转换器列表
	 * @since 4.1.3
	 */
	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 向给定列表添加一组默认 HttpMessageConverter 实例。
	 * 子类可以从 {@link #configureMessageConverters} 调用此方法。
	 * @param messageConverters 要向其添加默认消息转换器的列表
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new ResourceRegionHttpMessageConverter());
		if (!shouldIgnoreXml) {
			try {
				messageConverters.add(new SourceHttpMessageConverter<>());
			}
			catch (Error err) {
				// Ignore when no TransformerFactory implementation is available
			}
		}
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		if (romePresent) {
			messageConverters.add(new AtomFeedHttpMessageConverter());
			messageConverters.add(new RssChannelHttpMessageConverter());
		}

		if (!shouldIgnoreXml) {
			if (jackson2XmlPresent) {
				Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
				if (this.applicationContext != null) {
					builder.applicationContext(this.applicationContext);
				}
				messageConverters.add(new MappingJackson2XmlHttpMessageConverter(builder.build()));
			}
			else if (jaxb2Present) {
				messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			}
		}

		if (kotlinSerializationJsonPresent) {
			messageConverters.add(new KotlinSerializationJsonHttpMessageConverter());
		}
		if (jackson2Present) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2HttpMessageConverter(builder.build()));
		}
		else if (gsonPresent) {
			messageConverters.add(new GsonHttpMessageConverter());
		}
		else if (jsonbPresent) {
			messageConverters.add(new JsonbHttpMessageConverter());
		}

		if (jackson2SmilePresent) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.smile();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2SmileHttpMessageConverter(builder.build()));
		}
		if (jackson2CborPresent) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.cbor();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2CborHttpMessageConverter(builder.build()));
		}
	}

	/**
	 * 用于构建 {@link AsyncSupportConfigurer} 的回调。
	 * 委托给 {@link #configureAsyncSupport(AsyncSupportConfigurer)}。
	 * @since 5.3.2
	 */
	protected AsyncSupportConfigurer getAsyncSupportConfigurer() {
		if (this.asyncSupportConfigurer == null) {
			this.asyncSupportConfigurer = new AsyncSupportConfigurer();
			configureAsyncSupport(this.asyncSupportConfigurer);
		}
		return this.asyncSupportConfigurer;
	}

	/**
	 * 重写此方法以配置异步请求处理选项。
	 * @see AsyncSupportConfigurer
	 */
	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 返回一个 {@link CompositeUriComponentsContributor} 实例，供
	 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder} 使用。
	 * @since 4.0
	 */
	@Bean
	public CompositeUriComponentsContributor mvcUriComponentsContributor(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("requestMappingHandlerAdapter") RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
		return new CompositeUriComponentsContributor(
				requestMappingHandlerAdapter.getArgumentResolvers(), conversionService);
	}

	/**
	 * 返回一个 {@link HttpRequestHandlerAdapter}，用于通过
	 * {@link HttpRequestHandler HttpRequestHandlers} 处理请求。
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * 返回一个 {@link SimpleControllerHandlerAdapter}，用于通过基于接口的控制器处理请求。
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * 返回一个 {@link HandlerExceptionResolverComposite}，其中包含通过
	 * {@link #configureHandlerExceptionResolvers} 或
	 * {@link #addDefaultHandlerExceptionResolvers} 获取的异常解析器列表。
	 * <p><strong>注意：</strong>由于 CGLIB 的限制，此方法不能声明为 final。
	 * 与其重写此方法，不如考虑重写
	 * {@link #configureHandlerExceptionResolvers}，它允许提供一个解析器列表。
	 */
	@Bean
	public HandlerExceptionResolver handlerExceptionResolver(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<>();
		configureHandlerExceptionResolvers(exceptionResolvers);
		if (exceptionResolvers.isEmpty()) {
			addDefaultHandlerExceptionResolvers(exceptionResolvers, contentNegotiationManager);
		}
		extendHandlerExceptionResolvers(exceptionResolvers);
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(exceptionResolvers);
		return composite;
	}

	/**
	 * 重写此方法以配置要使用的
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} 列表。
	 * <p>向列表中添加解析器会关闭原本会默认注册的默认解析器。
	 * 另请参见 {@link #addDefaultHandlerExceptionResolvers}，
	 * 它可用于添加默认异常解析器。
	 * @param exceptionResolvers 要向其添加异常解析器的列表（初始为空列表）
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 重写此方法以在
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} 列表配置完成后扩展或修改该列表。
	 * <p>例如，这对于先注册默认解析器，然后通过此方法插入自定义解析器很有用。
	 * @param exceptionResolvers 要扩展的已配置解析器列表。
	 * @since 4.3
	 */
	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 可供子类用于添加默认
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} 的方法。
	 * <p>添加以下异常解析器：
	 * <ul>
	 * <li>{@link ExceptionHandlerExceptionResolver} 用于通过
	 * {@link org.springframework.web.bind.annotation.ExceptionHandler} 方法处理异常。
	 * <li>{@link ResponseStatusExceptionResolver} 用于处理带有
	 * {@link org.springframework.web.bind.annotation.ResponseStatus} 注解的异常。
	 * <li>{@link DefaultHandlerExceptionResolver} 用于解析已知的 Spring 异常类型
	 * </ul>
	 */
	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers,
			ContentNegotiationManager mvcContentNegotiationManager) {

		ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();
		exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager);
		exceptionHandlerResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());
		exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());
		if (jackson2Present) {
			exceptionHandlerResolver.setResponseBodyAdvice(
					Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}
		if (this.applicationContext != null) {
			exceptionHandlerResolver.setApplicationContext(this.applicationContext);
		}
		exceptionHandlerResolver.afterPropertiesSet();
		exceptionResolvers.add(exceptionHandlerResolver);

		ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();
		responseStatusResolver.setMessageSource(this.applicationContext);
		exceptionResolvers.add(responseStatusResolver);

		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	/**
	 * 用于插入自定义 {@link ExceptionHandlerExceptionResolver} 子类的受保护方法。
	 * @since 4.3
	 */
	protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
		return new ExceptionHandlerExceptionResolver();
	}

	/**
	 * 注册一个 {@link ViewResolverComposite}，其中包含用于视图解析的视图解析器链。
	 * 默认情况下，此解析器排序为 0，除非使用了内容协商视图解析，
	 * 此时顺序将提升为
	 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE
	 * Ordered.HIGHEST_PRECEDENCE}。
	 * <p>如果未配置其他解析器，
	 * {@link ViewResolverComposite#resolveViewName(String, Locale)} 将返回 null，
	 * 以便允许其他可能的 {@link ViewResolver} bean 解析视图。
	 * @since 4.1
	 */
	@Bean
	public ViewResolver mvcViewResolver(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
		ViewResolverRegistry registry =
				new ViewResolverRegistry(contentNegotiationManager, this.applicationContext);
		configureViewResolvers(registry);

		if (registry.getViewResolvers().isEmpty() && this.applicationContext != null) {
			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.applicationContext, ViewResolver.class, true, false);
			if (names.length == 1) {
				registry.getViewResolvers().add(new InternalResourceViewResolver());
			}
		}

		ViewResolverComposite composite = new ViewResolverComposite();
		composite.setOrder(registry.getOrder());
		composite.setViewResolvers(registry.getViewResolvers());
		if (this.applicationContext != null) {
			composite.setApplicationContext(this.applicationContext);
		}
		if (this.servletContext != null) {
			composite.setServletContext(this.servletContext);
		}
		return composite;
	}

	/**
	 * 重写此方法以配置视图解析。
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * 返回已注册的 {@link CorsConfiguration} 对象，以路径模式为键。
	 * @since 4.2
	 */
	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
		if (this.corsConfigurations == null) {
			CorsRegistry registry = new CorsRegistry();
			addCorsMappings(registry);
			this.corsConfigurations = registry.getCorsConfigurations();
		}
		return this.corsConfigurations;
	}

	/**
	 * 重写此方法以配置跨域请求处理。
	 * @since 4.2
	 * @see CorsRegistry
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	@Bean
	@Lazy
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}

	@Bean
	public LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver();
	}

	@Bean
	public ThemeResolver themeResolver() {
		return new FixedThemeResolver();
	}

	@Bean
	public FlashMapManager flashMapManager() {
		return new SessionFlashMapManager();
	}

	@Bean
	public RequestToViewNameTranslator viewNameTranslator() {
		return new DefaultRequestToViewNameTranslator();
	}


	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}

}
