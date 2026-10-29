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

package org.springframework.web.reactive.config;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * Spring WebFlux 配置的主类。
 *
 * <p>可直接导入，或继承并重写受保护方法以进行自定义。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class WebFluxConfigurationSupport implements ApplicationContextAware {

	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;

	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	@Nullable
	private ViewResolverRegistry viewResolverRegistry;

	@Nullable
	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext != null) {
				Assert.state(!applicationContext.containsBean("mvcContentNegotiationManager"),
						"The Java/XML config for Spring MVC and Spring WebFlux cannot both be enabled, " +
						"e.g. via @EnableWebMvc and @EnableWebFlux, in the same application.");
		}
	}

	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public DispatcherHandler webHandler() {
		return new DispatcherHandler();
	}

	@Bean
	@Order(0)
	public WebExceptionHandler responseStatusExceptionHandler() {
		return new WebFluxResponseStatusExceptionHandler();
	}

	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping(
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setContentTypeResolver(contentTypeResolver);
		PathMatchConfigurer configurer = getPathMatchConfigurer();
		configureAbstractHandlerMapping(mapping, configurer);
		Map<String, Predicate<Class<?>>> pathPrefixes = configurer.getPathPrefixes();
		if (pathPrefixes != null) {
			mapping.setPathPrefixes(pathPrefixes);
		}

		return mapping;
	}

	private void configureAbstractHandlerMapping(AbstractHandlerMapping mapping, PathMatchConfigurer configurer) {
		mapping.setCorsConfigurations(getCorsConfigurations());
		Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}
		Boolean useCaseSensitiveMatch = configurer.isUseCaseSensitiveMatch();
		if (useCaseSensitiveMatch != null) {
			mapping.setUseCaseSensitiveMatch(useCaseSensitiveMatch);
		}
	}

	/**
	 * 重写以插入 {@link RequestMappingHandlerMapping} 的子类。
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	@Bean
	public RequestedContentTypeResolver webFluxContentTypeResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		configureContentTypeResolver(builder);
		return builder.build();
	}

	/**
	 * 重写以配置如何解析所请求的内容类型。
	 */
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * 用于构建全局 CORS 配置的回调。此方法为 final。请使用 {@link #addCorsMappings(CorsRegistry)} 自定义 CORS 配置。
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
	 * 重写此方法以配置跨源请求处理。
	 * @see CorsRegistry
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 用于构建 {@link PathMatchConfigurer} 的回调。此方法为 final，请使用 {@link #configurePathMatching} 自定义路径匹配。
	 */
	protected final PathMatchConfigurer getPathMatchConfigurer() {
		if (this.pathMatchConfigurer == null) {
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatching(this.pathMatchConfigurer);
		}
		return this.pathMatchConfigurer;
	}

	/**
	 * 重写以配置路径匹配选项。
	 */
	public void configurePathMatching(PathMatchConfigurer configurer) {
	}

	@Bean
	public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
		RouterFunctionMapping mapping = createRouterFunctionMapping();
		mapping.setOrder(-1);  // go before RequestMappingHandlerMapping
		mapping.setMessageReaders(serverCodecConfigurer.getReaders());
		configureAbstractHandlerMapping(mapping, getPathMatchConfigurer());
		return mapping;
	}

	/**
	 * 重写以插入 {@link RouterFunctionMapping} 的子类。
	 */
	protected RouterFunctionMapping createRouterFunctionMapping() {
		return new RouterFunctionMapping();
	}

	/**
	 * 返回顺序为 Integer.MAX_VALUE-1 并包含所映射资源处理器的处理器映射。要配置资源处理，请重写 {@link #addResourceHandlers}。
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping(ResourceUrlProvider resourceUrlProvider) {
		ResourceLoader resourceLoader = this.applicationContext;
		if (resourceLoader == null) {
			resourceLoader = new DefaultResourceLoader();
		}
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(resourceLoader);
		registry.setResourceUrlProvider(resourceUrlProvider);
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping != null) {
			configureAbstractHandlerMapping(handlerMapping, getPathMatchConfigurer());
		}
		else {
			handlerMapping = new EmptyHandlerMapping();
		}
		return handlerMapping;
	}

	@Bean
	public ResourceUrlProvider resourceUrlProvider() {
		return new ResourceUrlProvider();
	}

	/**
	 * 重写此方法以添加用于提供静态资源的资源处理器。
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxConversionService") FormattingConversionService conversionService,
			@Qualifier("webFluxValidator") Validator validator) {

		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setMessageReaders(serverCodecConfigurer.getReaders());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
		adapter.setReactiveAdapterRegistry(reactiveAdapterRegistry);

		ArgumentResolverConfigurer configurer = new ArgumentResolverConfigurer();
		configureArgumentResolvers(configurer);
		adapter.setArgumentResolverConfigurer(configurer);

		return adapter;
	}

	/**
	 * 重写以插入 {@link RequestMappingHandlerAdapter} 的子类。
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * 为自定义控制器方法参数配置解析器。
	 */
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * 返回 HTTP 消息读写器的配置器。
	 * <p>使用 {@link #configureHttpMessageCodecs(ServerCodecConfigurer)} 配置读写器。
	 */
	@Bean
	public ServerCodecConfigurer serverCodecConfigurer() {
		ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();
		configureHttpMessageCodecs(serverCodecConfigurer);
		return serverCodecConfigurer;
	}

	/**
	 * 重写以插入 {@link LocaleContextResolver} 的子类。
	 */
	protected LocaleContextResolver createLocaleContextResolver() {
		return new AcceptHeaderLocaleContextResolver();
	}

	@Bean
	public LocaleContextResolver localeContextResolver() {
		return createLocaleContextResolver();
	}

	/**
	 * 重写以配置要使用的 HTTP 消息读写器。
	 */
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * 返回用于初始化所有 {@link WebDataBinder} 实例的 {@link ConfigurableWebBindingInitializer}。
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
			FormattingConversionService webFluxConversionService, Validator webFluxValidator) {

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(webFluxConversionService);
		initializer.setValidator(webFluxValidator);
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
		if (messageCodesResolver != null) {
			initializer.setMessageCodesResolver(messageCodesResolver);
		}
		return initializer;
	}

	/**
	 * 返回供注解式控制器使用的 {@link FormattingConversionService}。
	 * <p>作为重写此方法的替代方案，请参见 {@link #addFormatters}。
	 */
	@Bean
	public FormattingConversionService webFluxConversionService() {
		FormattingConversionService service = new DefaultFormattingConversionService();
		addFormatters(service);
		return service;
	}

	/**
	 * 重写此方法以向公共的 {@link FormattingConversionService} 添加自定义 {@link Converter} 和/或 {@link Formatter} 委托。
	 * @see #webFluxConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 返回用于适配响应式类型的 {@link ReactiveAdapterRegistry}。
	 */
	@Bean
	public ReactiveAdapterRegistry webFluxAdapterRegistry() {
		return new ReactiveAdapterRegistry();
	}

	/**
	 * 返回全局 {@link Validator} 实例，例如用于校验 {@code @RequestBody} 方法参数。
	 * <p>首先委托给 {@link #getValidator()}。如果该方法返回 {@code null}，则在创建 {@code OptionalValidatorFactoryBean} 之前检查类路径上是否存在 JSR-303 实现。如果 JSR-303 实现不可用，则返回“无操作”的 {@link Validator}。
	 */
	@Bean
	public Validator webFluxValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String name = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(name, getClass().getClassLoader());
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
	 * 重写此方法以提供自定义 {@link Validator}。
	 */
	@Nullable
	protected Validator getValidator() {
		return null;
	}

	/**
	 * 重写此方法以提供自定义 {@link MessageCodesResolver}。
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	@Bean
	public HandlerFunctionAdapter handlerFunctionAdapter() {
		return new HandlerFunctionAdapter();
	}

	@Bean
	public SimpleHandlerAdapter simpleHandlerAdapter() {
		return new SimpleHandlerAdapter();
	}

	@Bean
	public WebSocketHandlerAdapter webFluxWebSocketHandlerAdapter() {
		WebSocketHandlerAdapter adapter = new WebSocketHandlerAdapter(initWebSocketService());

		// Lower the (default) priority for now, for backwards compatibility
		int defaultOrder = adapter.getOrder();
		adapter.setOrder(defaultOrder + 1);

		return adapter;
	}

	private WebSocketService initWebSocketService() {
		WebSocketService service = getWebSocketService();
		if (service == null) {
			try {
				service = new HandshakeWebSocketService();
			}
			catch (IllegalStateException ex) {
				// Don't fail, test environment perhaps
				service = new NoUpgradeStrategyWebSocketService();
			}
		}
		return service;
	}

	@Nullable
	protected WebSocketService getWebSocketService() {
		return null;
	}

	@Bean
	public ResponseEntityResultHandler responseEntityResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseEntityResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry);
	}

	@Bean
	public ResponseBodyResultHandler responseBodyResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseBodyResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry);
	}

	@Bean
	public ViewResolutionResultHandler viewResolutionResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		ViewResolverRegistry registry = getViewResolverRegistry();
		List<ViewResolver> resolvers = registry.getViewResolvers();
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(
				resolvers, contentTypeResolver, reactiveAdapterRegistry);
		handler.setDefaultViews(registry.getDefaultViews());
		handler.setOrder(registry.getOrder());
		return handler;
	}

	@Bean
	public ServerResponseResultHandler serverResponseResultHandler(ServerCodecConfigurer serverCodecConfigurer) {
		List<ViewResolver> resolvers = getViewResolverRegistry().getViewResolvers();
		ServerResponseResultHandler handler = new ServerResponseResultHandler();
		handler.setMessageWriters(serverCodecConfigurer.getWriters());
		handler.setViewResolvers(resolvers);
		return handler;
	}

	/**
	 * 用于构建 {@link ViewResolverRegistry} 的回调。此方法为 final，请使用 {@link #configureViewResolvers} 自定义视图解析器。
	 */
	protected final ViewResolverRegistry getViewResolverRegistry() {
		if (this.viewResolverRegistry == null) {
			this.viewResolverRegistry = new ViewResolverRegistry(this.applicationContext);
			configureViewResolvers(this.viewResolverRegistry);
		}
		return this.viewResolverRegistry;
	}

	/**
	 * 配置视图解析以支持模板引擎。
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}


	private static final class EmptyHandlerMapping extends AbstractHandlerMapping {

		@Override
		public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
			return Mono.empty();
		}
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


	private static final class NoUpgradeStrategyWebSocketService implements WebSocketService {

		@Override
		public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {
			return Mono.error(new IllegalStateException("No suitable RequestUpgradeStrategy"));
		}
	}

}
