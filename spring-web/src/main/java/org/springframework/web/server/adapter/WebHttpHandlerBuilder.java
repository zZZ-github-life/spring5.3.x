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

package org.springframework.web.server.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * 此构建器有两个用途：
 *
 * <p>其一，组装一条处理链：由目标 {@link WebHandler} 构成，
 * 然后用一组 {@link WebFilter WebFilter} 进行装饰，
 * 再用一组 {@link WebExceptionHandler WebExceptionHandler} 进一步装饰。
 *
 * <p>其二，将生成的处理链适配为 {@link HttpHandler}：
 * 这是最低层级的响应式 HTTP 处理抽象，之后可与任何受支持的运行时配合使用。
 * 该适配借助 {@link HttpWebHandlerAdapter} 完成。
 *
 * <p>处理链可以通过构建器方法手动组装，也可以通过 {@link #applicationContext}
 * 从 Spring {@link ApplicationContext} 中检测获得，或者两者兼用。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 * @see HttpWebHandlerAdapter
 */
public final class WebHttpHandlerBuilder {

	/** Bean 工厂中目标 WebHandler 的众所周知名称。 */
	public static final String WEB_HANDLER_BEAN_NAME = "webHandler";

	/** Bean 工厂中 WebSessionManager 的众所周知名称。 */
	public static final String WEB_SESSION_MANAGER_BEAN_NAME = "webSessionManager";

	/** Bean 工厂中 ServerCodecConfigurer 的众所周知名称。 */
	public static final String SERVER_CODEC_CONFIGURER_BEAN_NAME = "serverCodecConfigurer";

	/** Bean 工厂中 LocaleContextResolver 的众所周知名称。 */
	public static final String LOCALE_CONTEXT_RESOLVER_BEAN_NAME = "localeContextResolver";

	/** Bean 工厂中 ForwardedHeaderTransformer 的众所周知名称。 */
	public static final String FORWARDED_HEADER_TRANSFORMER_BEAN_NAME = "forwardedHeaderTransformer";

	private final WebHandler webHandler;

	@Nullable
	private final ApplicationContext applicationContext;

	private final List<WebFilter> filters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	@Nullable
	private Function<HttpHandler, HttpHandler> httpHandlerDecorator;

	@Nullable
	private WebSessionManager sessionManager;

	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	@Nullable
	private LocaleContextResolver localeContextResolver;

	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;


	/**
	 * 从 ApplicationContext 初始化时使用的私有构造函数。
	 */
	private WebHttpHandlerBuilder(WebHandler webHandler, @Nullable ApplicationContext applicationContext) {
		Assert.notNull(webHandler, "WebHandler must not be null");
		this.webHandler = webHandler;
		this.applicationContext = applicationContext;
	}

	/**
	 * 拷贝构造函数。
	 */
	private WebHttpHandlerBuilder(WebHttpHandlerBuilder other) {
		this.webHandler = other.webHandler;
		this.applicationContext = other.applicationContext;
		this.filters.addAll(other.filters);
		this.exceptionHandlers.addAll(other.exceptionHandlers);
		this.sessionManager = other.sessionManager;
		this.codecConfigurer = other.codecConfigurer;
		this.localeContextResolver = other.localeContextResolver;
		this.forwardedHeaderTransformer = other.forwardedHeaderTransformer;
		this.httpHandlerDecorator = other.httpHandlerDecorator;
	}


	/**
	 * 创建新构建器实例的静态工厂方法。
	 * @param webHandler the target handler for the request
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebHttpHandlerBuilder(webHandler, null);
	}

	/**
	 * 通过在 {@link ApplicationContext} 中检测 Bean 来创建新构建器实例的静态工厂方法。
	 * 将检测以下内容：
	 * <ul>
	 * <li>{@link WebHandler} [1] -- 通过名称
	 * {@link #WEB_HANDLER_BEAN_NAME} 查找。
	 * <li>{@link WebFilter} [0..N] -- 按类型检测并排序，
	 * 请参阅 {@link AnnotationAwareOrderComparator}。
	 * <li>{@link WebExceptionHandler} [0..N] -- 按类型检测并排序。
	 * <li>{@link HttpHandlerDecoratorFactory} [0..N] -- 按类型检测并排序。
	 * <li>{@link WebSessionManager} [0..1] -- 通过名称
	 * {@link #WEB_SESSION_MANAGER_BEAN_NAME} 查找。
	 * <li>{@link ServerCodecConfigurer} [0..1] -- 通过名称
	 * {@link #SERVER_CODEC_CONFIGURER_BEAN_NAME} 查找。
	 * <li>{@link LocaleContextResolver} [0..1] -- 通过名称
	 * {@link #LOCALE_CONTEXT_RESOLVER_BEAN_NAME} 查找。
	 * </ul>
	 * @param context the application context to use for the lookup
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder applicationContext(ApplicationContext context) {

		WebHttpHandlerBuilder builder = new WebHttpHandlerBuilder(
				context.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class), context);

		List<WebFilter> webFilters = context
				.getBeanProvider(WebFilter.class)
				.orderedStream()
				.collect(Collectors.toList());
		builder.filters(filters -> filters.addAll(webFilters));

		List<WebExceptionHandler> exceptionHandlers = context
				.getBeanProvider(WebExceptionHandler.class)
				.orderedStream()
				.collect(Collectors.toList());
		builder.exceptionHandlers(handlers -> handlers.addAll(exceptionHandlers));

		context.getBeanProvider(HttpHandlerDecoratorFactory.class)
				.orderedStream()
				.forEach(builder::httpHandlerDecorator);

		try {
			builder.sessionManager(
					context.getBean(WEB_SESSION_MANAGER_BEAN_NAME, WebSessionManager.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.codecConfigurer(
					context.getBean(SERVER_CODEC_CONFIGURER_BEAN_NAME, ServerCodecConfigurer.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.localeContextResolver(
					context.getBean(LOCALE_CONTEXT_RESOLVER_BEAN_NAME, LocaleContextResolver.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.forwardedHeaderTransformer(
					context.getBean(FORWARDED_HEADER_TRANSFORMER_BEAN_NAME, ForwardedHeaderTransformer.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		return builder;
	}


	/**
	 * 添加给定的过滤器。
	 * @param filters the filter(s) to add that's
	 */
	public WebHttpHandlerBuilder filter(WebFilter... filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			this.filters.addAll(Arrays.asList(filters));
			updateFilters();
		}
		return this;
	}

	/**
	 * 操作当前已配置过滤器的"实时"列表。
	 * @param consumer the consumer to use
	 */
	public WebHttpHandlerBuilder filters(Consumer<List<WebFilter>> consumer) {
		consumer.accept(this.filters);
		updateFilters();
		return this;
	}

	private void updateFilters() {
		if (this.filters.isEmpty()) {
			return;
		}

		List<WebFilter> filtersToUse = this.filters.stream()
				.peek(filter -> {
					if (filter instanceof ForwardedHeaderTransformer && this.forwardedHeaderTransformer == null) {
						this.forwardedHeaderTransformer = (ForwardedHeaderTransformer) filter;
					}
				})
				.filter(filter -> !(filter instanceof ForwardedHeaderTransformer))
				.collect(Collectors.toList());

		this.filters.clear();
		this.filters.addAll(filtersToUse);
	}

	/**
	 * 添加给定的异常处理器。
	 * @param handlers the exception handler(s)
	 */
	public WebHttpHandlerBuilder exceptionHandler(WebExceptionHandler... handlers) {
		if (!ObjectUtils.isEmpty(handlers)) {
			this.exceptionHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	/**
	 * 操作当前已配置异常处理器的"实时"列表。
	 * @param consumer the consumer to use
	 */
	public WebHttpHandlerBuilder exceptionHandlers(Consumer<List<WebExceptionHandler>> consumer) {
		consumer.accept(this.exceptionHandlers);
		return this;
	}

	/**
	 * 配置要设置到 {@link ServerWebExchange WebServerExchange} 上的
	 * {@link WebSessionManager}。
	 * <p>默认使用 {@link DefaultWebSessionManager}。
	 * @param manager the session manager
	 * @see HttpWebHandlerAdapter#setSessionManager(WebSessionManager)
	 */
	public WebHttpHandlerBuilder sessionManager(WebSessionManager manager) {
		this.sessionManager = manager;
		return this;
	}

	/**
	 * 是否配置了 {@code WebSessionManager}：可以是从 {@code ApplicationContext}
	 * 检测到的，也可以是通过 {@link #sessionManager} 显式配置的。
	 * @since 5.0.9
	 */
	public boolean hasSessionManager() {
		return (this.sessionManager != null);
	}

	/**
	 * 配置要设置到 {@code WebServerExchange} 上的 {@link ServerCodecConfigurer}。
	 * @param codecConfigurer the codec configurer
	 */
	public WebHttpHandlerBuilder codecConfigurer(ServerCodecConfigurer codecConfigurer) {
		this.codecConfigurer = codecConfigurer;
		return this;
	}


	/**
	 * 是否配置了 {@code ServerCodecConfigurer}：可以是从 {@code ApplicationContext}
	 * 检测到的，也可以是通过 {@link #codecConfigurer} 显式配置的。
	 * @since 5.0.9
	 */
	public boolean hasCodecConfigurer() {
		return (this.codecConfigurer != null);
	}

	/**
	 * 配置要设置到 {@link ServerWebExchange WebServerExchange} 上的
	 * {@link LocaleContextResolver}。
	 * @param localeContextResolver the locale context resolver
	 */
	public WebHttpHandlerBuilder localeContextResolver(LocaleContextResolver localeContextResolver) {
		this.localeContextResolver = localeContextResolver;
		return this;
	}

	/**
	 * 是否配置了 {@code LocaleContextResolver}：可以是从 {@code ApplicationContext}
	 * 检测到的，也可以是通过 {@link #localeContextResolver} 显式配置的。
	 * @since 5.0.9
	 */
	public boolean hasLocaleContextResolver() {
		return (this.localeContextResolver != null);
	}

	/**
	 * 配置用于提取和/或移除转发头部的 {@link ForwardedHeaderTransformer}。
	 * @param transformer the transformer
	 * @since 5.1
	 */
	public WebHttpHandlerBuilder forwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		this.forwardedHeaderTransformer = transformer;
		return this;
	}

	/**
	 * 是否配置了 {@code ForwardedHeaderTransformer}：可以是从 {@code ApplicationContext}
	 * 检测到的，也可以是通过
	 * {@link #forwardedHeaderTransformer(ForwardedHeaderTransformer)} 显式配置的。
	 * @since 5.1
	 */
	public boolean hasForwardedHeaderTransformer() {
		return (this.forwardedHeaderTransformer != null);
	}

	/**
	 * 配置一个 {@link Function} 来装饰此构建器返回的 {@link HttpHandler}，
	 * 该函数实际上会包装整个
	 * {@link WebExceptionHandler} - {@link WebFilter} - {@link WebHandler}
	 * 处理链。这样就可以在整个处理链之前访问请求和响应，
	 * 同样也可以观察整个处理链的结果。
	 * @param handlerDecorator the decorator to apply
	 * @since 5.3
	 */
	public WebHttpHandlerBuilder httpHandlerDecorator(Function<HttpHandler, HttpHandler> handlerDecorator) {
		this.httpHandlerDecorator = (this.httpHandlerDecorator != null ?
				handlerDecorator.andThen(this.httpHandlerDecorator) : handlerDecorator);
		return this;
	}

	/**
	 * 是否通过 {@link #httpHandlerDecorator(Function)} 配置了
	 * {@link HttpHandler} 的装饰器。
	 * @since 5.3
	 */
	public boolean hasHttpHandlerDecorator() {
		return (this.httpHandlerDecorator != null);
	}

	/**
	 * 构建 {@link HttpHandler}。
	 */
	public HttpHandler build() {
		WebHandler decorated = new FilteringWebHandler(this.webHandler, this.filters);
		decorated = new ExceptionHandlingWebHandler(decorated,  this.exceptionHandlers);

		HttpWebHandlerAdapter adapted = new HttpWebHandlerAdapter(decorated);
		if (this.sessionManager != null) {
			adapted.setSessionManager(this.sessionManager);
		}
		if (this.codecConfigurer != null) {
			adapted.setCodecConfigurer(this.codecConfigurer);
		}
		if (this.localeContextResolver != null) {
			adapted.setLocaleContextResolver(this.localeContextResolver);
		}
		if (this.forwardedHeaderTransformer != null) {
			adapted.setForwardedHeaderTransformer(this.forwardedHeaderTransformer);
		}
		if (this.applicationContext != null) {
			adapted.setApplicationContext(this.applicationContext);
		}
		adapted.afterPropertiesSet();

		return (this.httpHandlerDecorator != null ? this.httpHandlerDecorator.apply(adapted) : adapted);
	}

	/**
	 * 克隆此 {@link WebHttpHandlerBuilder}。
	 * @return the cloned builder instance
	 */
	@Override
	public WebHttpHandlerBuilder clone() {
		return new WebHttpHandlerBuilder(this);
	}


	/**
	 * 适用于 spring-web 类的 {@code BlockHoundIntegration}。
	 * @since 5.3.6
	 */
	public static class SpringWebBlockHoundIntegration implements BlockHoundIntegration {

		@Override
		public void applyTo(BlockHound.Builder builder) {

			// Avoid hard references potentially anywhere in spring-web (no need for structural dependency)

			builder.allowBlockingCallsInside("org.springframework.http.MediaTypeFactory", "<clinit>");
			builder.allowBlockingCallsInside("org.springframework.web.util.HtmlUtils", "<clinit>");
		}
	}

}
