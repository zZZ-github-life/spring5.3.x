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

package org.springframework.test.web.servlet.client;

import java.util.function.Supplier;

import javax.servlet.Filter;

import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 通过 {@link WebTestClient} 并使用 {@link MockMvc} 处理服务器请求来测试 Spring MVC 应用程序的主类。
 *
 * <p>提供静态工厂方法和规范来初始化 {@code MockMvc}，
 * 以便 {@code WebTestClient} 连接到该实例。例如：
 * <pre class="code">
 * WebTestClient client = MockMvcWebTestClient.bindToController(myController)
 * .controllerAdvice(myControllerAdvice)
 * .validator(myValidator)
 * .build()
 * </pre>
 *
 * <p>客户端本身也可以配置。例如：
 * <pre class="code">
 * WebTestClient client = MockMvcWebTestClient.bindToController(myController)
 * .validator(myValidator)
 * .configureClient()
 * .baseUrl("/path")
 * .build();
 * </pre>
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public interface MockMvcWebTestClient {

	/**
	 * 通过提供用于处理请求的 {@code @Controller} 实例开始创建 {@link WebTestClient}。
	 * <p>在内部，这被委托给并等价于使用
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#standaloneSetup(Object...)}
	 * 初始化 {@link MockMvc}。
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new StandaloneMockMvcSpec(controllers);
	}

	/**
	 * 通过提供包含 Spring MVC 基础设施和控制器的
	 * {@link WebApplicationContext} 开始创建 {@link WebTestClient}。
	 * <p>在内部，这被委托给并等价于使用
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#webAppContextSetup(WebApplicationContext)}
	 * 初始化 {@code MockMvc}。
	 */
	static MockMvcServerSpec<?> bindToApplicationContext(WebApplicationContext context) {
		return new ApplicationContextMockMvcSpec(context);
	}

	/**
	 * 通过提供已初始化的 {@link MockMvc} 实例作为服务器开始创建 {@link WebTestClient}。
	 */
	static WebTestClient.Builder bindTo(MockMvc mockMvc) {
		ClientHttpConnector connector = new MockMvcHttpConnector(mockMvc);
		return WebTestClient.bindToServer(connector);
	}

	/**
	 * 此方法可用于根据服务器响应的状态对给定 {@link ExchangeResult} 应用进一步断言。
	 * <p>通常 {@link WebTestClient} 用于断言客户端响应，包括 HTTP 状态、头和正文。通过 HTTP 发出实际请求时，这些就是可用的全部内容。但是当服务器是
	 * {@link MockMvc} 时，可以针对服务器响应进行更多断言，例如模型属性、Flash 属性等。
	 *
	 * <p>示例：
	 * <pre class="code">
	 * EntityExchangeResult&lt;Void&gt; result =
	 * webTestClient.post().uri("/people/123")
	 * .exchange()
	 * .expectStatus().isFound()
	 * .expectHeader().location("/persons/Joe")
	 * .expectBody().isEmpty();
	 *
	 * MockMvcWebTestClient.resultActionsFor(result)
	 * .andExpect(model().size(1))
	 * .andExpect(model().attributeExists("name"))
	 * .andExpect(flash().attributeCount(1))
	 * .andExpect(flash().attribute("message", "success!"));
	 * </pre>
	 * <p>注意：仅当用于执行请求的 {@link WebTestClient} 是通过此类中的某个 bind 方法初始化，
	 * 因而请求由 {@link MockMvc} 处理时，此方法才有效。
	 */
	static ResultActions resultActionsFor(ExchangeResult exchangeResult) {
		Object serverResult = exchangeResult.getMockServerResult();
		Assert.notNull(serverResult, "No MvcResult");
		Assert.isInstanceOf(MvcResult.class, serverResult);
		return new ResultActions() {
			@Override
			public ResultActions andExpect(ResultMatcher matcher) throws Exception {
				matcher.match((MvcResult) serverResult);
				return this;
			}
			@Override
			public ResultActions andDo(ResultHandler handler) throws Exception {
				handler.handle((MvcResult) serverResult);
				return this;
			}
			@Override
			public MvcResult andReturn() {
				return (MvcResult) serverResult;
			}
		};
	}


	/**
	 * 用于配置 {@link MockMvc} 的基础规范，以及
	 * {@link ConfigurableMockMvcBuilder} 的简单外观。
	 * @param <B> a self reference to the builder type
	 */
	interface MockMvcServerSpec<B extends MockMvcServerSpec<B>> {

		/**
		 * 添加全局过滤器。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#addFilters(Filter...)}。
		 */
		<T extends B> T filters(Filter... filters);

		/**
		 * 为特定 URL 模式添加过滤器。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#addFilter(Filter, String...)}。
		 */
		<T extends B> T filter(Filter filter, String... urlPatterns);

		/**
		 * 定义应合并到所有已执行请求中的默认请求属性，
		 * 以便来自客户端请求的输入覆盖此处定义的默认属性。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}。
		 */
		<T extends B> T defaultRequest(RequestBuilder requestBuilder);

		/**
		 * 定义应<em>始终</em>应用于
		 * 每个响应的全局预期。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#alwaysExpect(ResultMatcher)}。
		 */
		<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

		/**
		 * 是否处理 HTTP OPTIONS 请求。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#dispatchOptions(boolean)}。
		 */
		<T extends B> T dispatchOptions(boolean dispatchOptions);

		/**
		 * 允许自定义 {@code DispatcherServlet}。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#addDispatcherServletCustomizer(DispatcherServletCustomizer)}。
		 */
		<T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer);

		/**
		 * 添加可自动完成 MockMvc 设置的 {@code MockMvcConfigurer}。
		 * <p>委托给
		 * {@link ConfigurableMockMvcBuilder#apply(MockMvcConfigurer)}。
		 */
		<T extends B> T apply(MockMvcConfigurer configurer);

		/**
		 * 继续配置并构建测试客户端。
		 */
		WebTestClient.Builder configureClient();

		/**
		 * 构建测试客户端的快捷方式。
		 */
		WebTestClient build();
	}


	/**
	 * 用于配置 {@link MockMvc} 以直接测试一个或多个
	 * 控制器的规范，以及 {@link StandaloneMockMvcBuilder} 的简单外观。
	 */
	interface ControllerSpec extends MockMvcServerSpec<ControllerSpec> {

		/**
		 * 注册 {@link org.springframework.web.bind.annotation.ControllerAdvice}
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setControllerAdvice(Object...)}。
		 */
		ControllerSpec controllerAdvice(Object... controllerAdvice);

		/**
		 * 设置要使用的消息转换器。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}。
		 */
		ControllerSpec messageConverters(HttpMessageConverter<?>... messageConverters);

		/**
		 * 提供自定义 {@link Validator}。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setValidator(Validator)}。
		 */
		ControllerSpec validator(Validator validator);

		/**
		 * 提供转换服务。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setConversionService(FormattingConversionService)}。
		 */
		ControllerSpec conversionService(FormattingConversionService conversionService);

		/**
		 * 添加全局拦截器。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#addInterceptors(HandlerInterceptor...)}。
		 */
		ControllerSpec interceptors(HandlerInterceptor... interceptors);

		/**
		 * 为特定模式添加拦截器。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#addMappedInterceptors(String[], HandlerInterceptor...)}。
		 */
		ControllerSpec mappedInterceptors(
				@Nullable String[] pathPatterns, HandlerInterceptor... interceptors);

		/**
		 * 设置 ContentNegotiationManager。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setContentNegotiationManager(ContentNegotiationManager)}。
		 */
		ControllerSpec contentNegotiationManager(ContentNegotiationManager manager);

		/**
		 * 指定异步执行的超时值。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setAsyncRequestTimeout(long)}。
		 */
		ControllerSpec asyncRequestTimeout(long timeout);

		/**
		 * 提供自定义参数解析器。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setCustomArgumentResolvers(HandlerMethodArgumentResolver...)}。
		 */
		ControllerSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers);

		/**
		 * 提供自定义返回值处理器。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setCustomReturnValueHandlers(HandlerMethodReturnValueHandler...)}。
		 */
		ControllerSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers);

		/**
		 * 设置要使用的 HandlerExceptionResolver 类型。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setHandlerExceptionResolvers(HandlerExceptionResolver...)}。
		 */
		ControllerSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers);

		/**
		 * 设置视图解析。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setViewResolvers(ViewResolver...)}。
		 */
		ControllerSpec viewResolvers(ViewResolver... resolvers);

		/**
		 * 使用固定视图设置单个 {@link ViewResolver}。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setSingleView(View)}。
		 */
		ControllerSpec singleView(View view);

		/**
		 * 提供要使用的 LocaleResolver。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setLocaleResolver(LocaleResolver)}。
		 */
		ControllerSpec localeResolver(LocaleResolver localeResolver);

		/**
		 * 提供自定义 FlashMapManager。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setFlashMapManager(FlashMapManager)}。
		 */
		ControllerSpec flashMapManager(FlashMapManager flashMapManager);

		/**
		 * 启用使用解析后的
		 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns} 进行 URL 路径匹配。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setPatternParser(PathPatternParser)}。
		 */
		ControllerSpec patternParser(PathPatternParser parser);

		/**
		 * 是否匹配尾部斜杠。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setUseTrailingSlashPatternMatch(boolean)}。
		 */
		ControllerSpec useTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch);

		/**
		 * 配置要使用的占位符值。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#addPlaceholderValue(String, String)}。
		 */
		ControllerSpec placeholderValue(String name, String value);

		/**
		 * 为自定义 {@link RequestMappingHandlerMapping} 配置工厂。
		 * <p>委托给
		 * {@link StandaloneMockMvcBuilder#setCustomHandlerMapping(Supplier)}。
		 */
		ControllerSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory);
	}

}
