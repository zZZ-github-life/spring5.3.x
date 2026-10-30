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

package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 一个接受 {@code @Controller} 注册的 {@code MockMvcBuilder}，
 * 因此可以像普通单元测试一样完全控制控制器及其依赖项的实例化和初始化，
 * 并且可以一次测试一个控制器。
 *
 * <p>此构建器创建 {@link DispatcherServlet} 使用注解控制器处理请求
 * 所需的最低基础设施，并提供自定义方法。生成的配置和自定义选项
 * 等效于使用 MVC Java 配置，只是使用构建器风格的方法。
 *
 * <p>要配置视图解析，可以选择用于每个已执行请求的“固定”视图
 * （请参见 {@link #setSingleView(View)}），或提供
 * {@code ViewResolver} 列表（请参见 {@link #setViewResolvers(ViewResolver...)}）。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandaloneMockMvcBuilder extends AbstractMockMvcBuilder<StandaloneMockMvcBuilder> {

	private final List<Object> controllers;

	@Nullable
	private List<Object> controllerAdvice;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>();

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<>();

	@Nullable
	private Validator validator;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private FormattingConversionService conversionService;

	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	@Nullable
	private Long asyncRequestTimeout;

	@Nullable
	private List<ViewResolver> viewResolvers;

	private LocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

	@Nullable
	private FlashMapManager flashMapManager;

	@Nullable
	private PathPatternParser patternParser;

	private boolean useSuffixPatternMatch = false;

	private boolean useTrailingSlashPatternMatch = true;

	@Nullable
	private Boolean removeSemicolonContent;

	private Map<String, String> placeholderValues = new HashMap<>();

	private Supplier<RequestMappingHandlerMapping> handlerMappingFactory = RequestMappingHandlerMapping::new;


	/**
	 * 受保护的构造函数。不用于直接实例化。
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	protected StandaloneMockMvcBuilder(Object... controllers) {
		this.controllers = instantiateIfNecessary(controllers);
	}

	private static List<Object> instantiateIfNecessary(Object[] specified) {
		List<Object> instances = new ArrayList<>(specified.length);
		for (Object obj : specified) {
			instances.add(obj instanceof Class ? BeanUtils.instantiateClass((Class<?>) obj) : obj);
		}
		return instances;
	}


	/**
	 * 注册一个或多个要在测试中使用的
	 * {@link org.springframework.web.bind.annotation.ControllerAdvice} 实例
	 * （指定的 {@code Class} 将被转换为实例）。
	 * <p>通常，只要 {@code @ControllerAdvice} 声明为 Spring bean，
	 * 它们就会被自动检测到。然而，由于独立设置不加载任何 Spring 配置，
	 * 因此需要像控制器一样在此处显式注册它们。
	 * @since 4.2
	 */
	public StandaloneMockMvcBuilder setControllerAdvice(Object... controllerAdvice) {
		this.controllerAdvice = instantiateIfNecessary(controllerAdvice);
		return this;
	}

	/**
	 * 设置用于参数解析器和返回值处理器的消息转换器，
	 * 这些转换器支持读取和/或写入请求体和响应体。
	 * 如果未向列表添加任何消息转换器，则会改为添加默认的转换器列表。
	 */
	public StandaloneMockMvcBuilder setMessageConverters(HttpMessageConverter<?>...messageConverters) {
		this.messageConverters = Arrays.asList(messageConverters);
		return this;
	}

	/**
	 * 提供自定义 {@link Validator}，而不是默认创建的验证器。
	 * 假设类路径上有 JSR-303，使用的默认实现是
	 * {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}。
	 */
	public StandaloneMockMvcBuilder setValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * 提供带有自定义格式化器和转换器的转换服务。
	 * 如果未设置，则默认使用 {@link DefaultFormattingConversionService}。
	 */
	public StandaloneMockMvcBuilder setConversionService(FormattingConversionService conversionService) {
		this.conversionService = conversionService;
		return this;
	}

	/**
	 * 添加映射到所有传入请求的拦截器。
	 */
	public StandaloneMockMvcBuilder addInterceptors(HandlerInterceptor... interceptors) {
		addMappedInterceptors(null, interceptors);
		return this;
	}

	/**
	 * 添加映射到一组路径模式的拦截器。
	 */
	public StandaloneMockMvcBuilder addMappedInterceptors(
			@Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {

		for (HandlerInterceptor interceptor : interceptors) {
			this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, null, interceptor));
		}
		return this;
	}

	/**
	 * 设置 ContentNegotiationManager。
	 */
	public StandaloneMockMvcBuilder setContentNegotiationManager(ContentNegotiationManager manager) {
		this.contentNegotiationManager = manager;
		return this;
	}

	/**
	 * 指定异步执行的超时值。在 Spring MVC Test 中，该值用于确定
	 * 等待异步执行完成的时间，以便测试可以同步验证结果。
	 * @param timeout 超时值，单位为毫秒
	 */
	public StandaloneMockMvcBuilder setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
		return this;
	}

	/**
	 * 为控制器方法参数提供自定义解析器。
	 */
	public StandaloneMockMvcBuilder setCustomArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
		this.customArgumentResolvers = Arrays.asList(argumentResolvers);
		return this;
	}

	/**
	 * 为控制器方法返回值提供自定义处理器。
	 */
	public StandaloneMockMvcBuilder setCustomReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
		this.customReturnValueHandlers = Arrays.asList(handlers);
		return this;
	}

	/**
	 * 以列表形式设置要使用的 HandlerExceptionResolver 类型。
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.handlerExceptionResolvers = exceptionResolvers;
		return this;
	}

	/**
	 * 以数组形式设置要使用的 HandlerExceptionResolver 类型。
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.handlerExceptionResolvers = Arrays.asList(exceptionResolvers);
		return this;
	}

	/**
	 * 使用给定的 {@link ViewResolver ViewResolvers} 设置视图解析。
	 * 如果未设置，则默认使用 {@link InternalResourceViewResolver}。
	 */
	public StandaloneMockMvcBuilder setViewResolvers(ViewResolver...resolvers) {
		this.viewResolvers = Arrays.asList(resolvers);
		return this;
	}

	/**
	 * 设置一个始终返回所提供视图实例的单个 {@link ViewResolver}。
	 * 如果你只需要使用一个 View 实例（例如渲染生成的内容（JSON、XML、Atom）），
	 * 这是一个方便的快捷方式。
	 */
	public StandaloneMockMvcBuilder setSingleView(View view) {
		this.viewResolvers = Collections.<ViewResolver>singletonList(new StaticViewResolver(view));
		return this;
	}

	/**
	 * 提供 LocaleResolver 实例。
	 * 如果未提供，则使用的默认实例是 {@link AcceptHeaderLocaleResolver}。
	 */
	public StandaloneMockMvcBuilder setLocaleResolver(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
		return this;
	}

	/**
	 * 提供自定义 FlashMapManager 实例。
	 * 如果未提供，则默认使用 {@code SessionFlashMapManager}。
	 */
	public StandaloneMockMvcBuilder setFlashMapManager(FlashMapManager flashMapManager) {
		this.flashMapManager = flashMapManager;
		return this;
	}

	/**
	 * 启用使用解析后的
	 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}
	 * 进行 URL 路径匹配，而不是使用
	 * {@link org.springframework.util.PathMatcher} 进行字符串模式匹配。
	 * @param parser 要使用的解析器
	 * @since 5.3
	 */
	public void setPatternParser(PathPatternParser parser) {
		this.patternParser = parser;
	}

	/**
	 * 在将模式与请求匹配时是否使用后缀模式匹配（“.*”）。
	 * 如果启用，映射到 “/users” 的方法也会匹配 “/users.*”。
	 * <p>默认值为 {@code false}。
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Deprecated
	public StandaloneMockMvcBuilder setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		return this;
	}

	/**
	 * 是否匹配 URL 而不考虑是否存在尾随斜杠。
	 * 如果启用，映射到 “/users” 的方法也会匹配 “/users/”。
	 * <p>默认值为 {@code true}。
	 */
	public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
		this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
		return this;
	}

	/**
	 * 设置是否应从请求 URI 中剥离 “;”（分号）内容。如果提供了该值，
	 * 则会依次设置到 {@link AbstractHandlerMapping#setRemoveSemicolonContent(boolean)}。
	 */
	public StandaloneMockMvcBuilder setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
		return this;
	}

	/**
	 * 在独立设置中，不支持请求映射中嵌入的占位符值。
	 * 此方法允许手动提供占位符值，以便解析它们。
	 * 或者，考虑创建一个初始化 {@link WebApplicationContext} 的测试。
	 * @since 4.2.8
	 */
	public StandaloneMockMvcBuilder addPlaceholderValue(String name, String value) {
		this.placeholderValues.put(name, value);
		return this;
	}

	/**
	 * 配置工厂以创建自定义 {@link RequestMappingHandlerMapping}。
	 * @param factory 该工厂
	 * @since 5.0
	 */
	public StandaloneMockMvcBuilder setCustomHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
		this.handlerMappingFactory = factory;
		return this;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		MockServletContext servletContext = new MockServletContext();
		StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);
		registerMvcSingletons(wac);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		return wac;
	}

	private void registerMvcSingletons(StubWebApplicationContext wac) {
		StandaloneConfiguration config = new StandaloneConfiguration();
		config.setApplicationContext(wac);
		ServletContext sc = wac.getServletContext();

		wac.addBeans(this.controllers);
		wac.addBeans(this.controllerAdvice);

		FormattingConversionService mvcConversionService = config.mvcConversionService();
		wac.addBean("mvcConversionService", mvcConversionService);
		ResourceUrlProvider resourceUrlProvider = config.mvcResourceUrlProvider();
		wac.addBean("mvcResourceUrlProvider", resourceUrlProvider);
		ContentNegotiationManager mvcContentNegotiationManager = config.mvcContentNegotiationManager();
		wac.addBean("mvcContentNegotiationManager", mvcContentNegotiationManager);
		Validator mvcValidator = config.mvcValidator();
		wac.addBean("mvcValidator", mvcValidator);

		RequestMappingHandlerMapping hm = config.getHandlerMapping(mvcConversionService, resourceUrlProvider);
		if (sc != null) {
			hm.setServletContext(sc);
		}
		hm.setApplicationContext(wac);
		hm.afterPropertiesSet();
		wac.addBean("requestMappingHandlerMapping", hm);

		RequestMappingHandlerAdapter ha = config.requestMappingHandlerAdapter(mvcContentNegotiationManager,
				mvcConversionService, mvcValidator);
		if (sc != null) {
			ha.setServletContext(sc);
		}
		ha.setApplicationContext(wac);
		ha.afterPropertiesSet();
		wac.addBean("requestMappingHandlerAdapter", ha);

		wac.addBean("handlerExceptionResolver", config.handlerExceptionResolver(mvcContentNegotiationManager));

		wac.addBeans(initViewResolvers(wac));
		wac.addBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, this.localeResolver);
		wac.addBean(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, new FixedThemeResolver());
		wac.addBean(DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, new DefaultRequestToViewNameTranslator());

		this.flashMapManager = new SessionFlashMapManager();
		wac.addBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, this.flashMapManager);

		extendMvcSingletons(sc).forEach(wac::addBean);
	}

	private List<ViewResolver> initViewResolvers(WebApplicationContext wac) {
		this.viewResolvers = (this.viewResolvers != null ? this.viewResolvers :
				Collections.singletonList(new InternalResourceViewResolver()));
		for (Object viewResolver : this.viewResolvers) {
			if (viewResolver instanceof WebApplicationObjectSupport) {
				((WebApplicationObjectSupport) viewResolver).setApplicationContext(wac);
			}
		}
		return this.viewResolvers;
	}

	/**
	 * 此方法可以在子类中使用，以注册额外的 Spring MVC 基础设施，
	 * 例如额外的 {@code HandlerMapping}、{@code HandlerAdapter} 等。
	 * @param servletContext 该 ServletContext
	 * @return 包含附加 MVC 基础设施对象实例的 Map
	 * @since 5.1.4
	 */
	protected Map<String, Object> extendMvcSingletons(@Nullable ServletContext servletContext) {
		return Collections.emptyMap();
	}


	/** 使用 MVC Java 配置作为“独立”设置的起点。 */
	private class StandaloneConfiguration extends WebMvcConfigurationSupport {

		@SuppressWarnings("deprecation")
		public RequestMappingHandlerMapping getHandlerMapping(
				FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider) {

			RequestMappingHandlerMapping handlerMapping = handlerMappingFactory.get();
			handlerMapping.setEmbeddedValueResolver(new StaticStringValueResolver(placeholderValues));
			if (patternParser != null) {
				handlerMapping.setPatternParser(patternParser);
			}
			else {
				handlerMapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
				if (removeSemicolonContent != null) {
					handlerMapping.setRemoveSemicolonContent(removeSemicolonContent);
				}
			}
			handlerMapping.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
			handlerMapping.setOrder(0);
			handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
			return handlerMapping;
		}

		@Override
		protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.addAll(messageConverters);
		}

		@Override
		protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.addAll(customArgumentResolvers);
		}

		@Override
		protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.addAll(customReturnValueHandlers);
		}

		@Override
		protected void addInterceptors(InterceptorRegistry registry) {
			for (MappedInterceptor interceptor : mappedInterceptors) {
				InterceptorRegistration registration = registry.addInterceptor(interceptor.getInterceptor());
				if (interceptor.getPathPatterns() != null) {
					registration.addPathPatterns(interceptor.getPathPatterns());
				}
			}
		}

		@Override
		public ContentNegotiationManager mvcContentNegotiationManager() {
			return (contentNegotiationManager != null) ? contentNegotiationManager : super.mvcContentNegotiationManager();
		}

		@Override
		public FormattingConversionService mvcConversionService() {
			return (conversionService != null ? conversionService : super.mvcConversionService());
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (asyncRequestTimeout != null) {
				configurer.setDefaultTimeout(asyncRequestTimeout);
			}
		}

		@Override
		public Validator mvcValidator() {
			Validator mvcValidator = (validator != null) ? validator : super.mvcValidator();
			if (mvcValidator instanceof InitializingBean) {
				try {
					((InitializingBean) mvcValidator).afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Failed to initialize Validator", ex);
				}
			}
			return mvcValidator;
		}

		@Override
		protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			if (handlerExceptionResolvers == null) {
				return;
			}
			for (HandlerExceptionResolver resolver : handlerExceptionResolvers) {
				if (resolver instanceof ApplicationContextAware) {
					ApplicationContext applicationContext = getApplicationContext();
					if (applicationContext != null) {
						((ApplicationContextAware) resolver).setApplicationContext(applicationContext);
					}
				}
				if (resolver instanceof InitializingBean) {
					try {
						((InitializingBean) resolver).afterPropertiesSet();
					}
					catch (Exception ex) {
						throw new IllegalStateException("Failure from afterPropertiesSet", ex);
					}
				}
				exceptionResolvers.add(resolver);
			}
		}
	}

	/**
	 * 用于请求映射中嵌入值的静态解析器占位符。
	 */
	private static class StaticStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public StaticStringValueResolver(Map<String, String> values) {
			this.helper = new PropertyPlaceholderHelper("${", "}", ":", false);
			this.resolver = values::get;
		}

		@Override
		public String resolveStringValue(String strVal) throws BeansException {
			return this.helper.replacePlaceholders(strVal, this.resolver);
		}
	}


	/**
	 * 一个始终返回同一 View 的 {@link ViewResolver}。
	 */
	private static class StaticViewResolver implements ViewResolver {

		private final View view;

		public StaticViewResolver(View view) {
			this.view = view;
		}

		@Override
		@Nullable
		public View resolveViewName(String viewName, Locale locale) {
			return this.view;
		}
	}

}
