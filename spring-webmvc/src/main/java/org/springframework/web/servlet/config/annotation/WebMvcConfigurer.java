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

package org.springframework.web.servlet.config.annotation;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * 定义用于自定义通过 {@code @EnableWebMvc} 启用的 Spring MVC
 * 基于 Java 的配置的回调方法。
 *
 * <p>带有 {@code @EnableWebMvc} 注解的配置类可以实现此接口以获得回调，
 * 并有机会自定义默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface WebMvcConfigurer {

	/**
	 * 辅助配置 {@link HandlerMapping} 的路径匹配选项，例如是使用解析后的
	 * {@code PathPatterns} 还是使用 {@code PathMatcher} 进行字符串模式匹配、
	 * 是否匹配尾部斜杠等。
	 * @since 4.0.3
	 * @see PathMatchConfigurer
	 */
	default void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 配置内容协商选项。
	 */
	default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 配置异步请求处理选项。
	 */
	default void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 配置一个处理器，通过转发到 Servlet 容器的"default" servlet 来处理未处理的请求。
	 * 一个常见用例是当 {@link DispatcherServlet} 映射到 "/"，
	 * 从而覆盖了 Servlet 容器对静态资源的默认处理时。
	 */
	default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 除默认注册的转换器外，再添加 {@link Converter Converters} 和 {@link Formatter Formatters}。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 添加 Spring MVC 生命周期拦截器，用于对控制器方法调用和资源处理器请求进行前后处理。
	 * 拦截器可以注册为应用于所有请求，或限制在部分 URL 模式内。
	 */
	default void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 添加处理器，以便从 Web 应用根目录、类路径及其他位置的特定位置
	 * 提供静态资源（如图片、js 和 css 文件）。
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 配置"全局"跨域请求处理。配置的 CORS 映射适用于注解控制器、函数式端点和静态资源。
	 * <p>注解控制器可以通过
	 * {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}
	 * 进一步声明更细粒度的配置。在这种情况下，此处声明的"全局"CORS 配置会与
	 * 控制器方法上定义的本地 CORS 配置
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) 合并}。
	 * @since 4.2
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 配置预先配置了响应状态码和/或用于渲染响应体的视图的简单自动化控制器。
	 * 在不需要自定义控制器逻辑的场景中很有用——例如渲染主页、执行简单的站点 URL 重定向、
	 * 返回带 HTML 内容的 404 状态、返回无内容的 204 等。
	 * @see ViewControllerRegistry
	 */
	default void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * 配置视图解析器，将控制器返回的基于字符串的视图名称转换为具体的
	 * {@link org.springframework.web.servlet.View} 实现以执行渲染。
	 * @since 4.1
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * 添加解析器以支持自定义控制器方法参数类型。
	 * <p>这不会覆盖解析处理器方法参数的内置支持。要自定义内置的参数解析支持，
	 * 请直接配置 {@link RequestMappingHandlerAdapter}。
	 * @param resolvers 初始为空的列表
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * 添加处理器以支持自定义控制器方法返回值类型。
	 * <p>使用此选项不会覆盖处理返回值的的内置支持。要自定义内置的返回值处理支持，
	 * 请直接配置 RequestMappingHandlerAdapter。
	 * @param handlers 初始为空的列表
	 */
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
	}

	/**
	 * 配置用于从请求体读取和向响应体写入的 {@link HttpMessageConverter HttpMessageConverter}。
	 * <p>默认情况下，只要类路径上存在相应的第三方库（如 Jackson JSON、JAXB2 等），
	 * 就会配置所有内置转换器。
	 * <p><strong>注意</strong> 使用此方法会关闭默认转换器注册。或者，
	 * 使用 {@link #extendMessageConverters(java.util.List)} 修改默认的转换器列表。
	 * @param converters 初始为空的转换器列表
	 */
	default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 在转换器列表被 {@link #configureMessageConverters(List) 配置}或使用默认列表
	 * 初始化之后，扩展或修改该列表。
	 * <p>注意转换器注册的顺序很重要。特别是在客户端接受
	 * {@link org.springframework.http.MediaType#ALL} 的情况下，较早配置的转换器将被优先使用。
	 * @param converters 要扩展的已配置转换器列表
	 * @since 4.1.3
	 */
	default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 配置异常解析器。
	 * <p>给定列表初始为空。如果保持为空，框架将配置一组默认解析器，
	 * 请参见
	 * {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)}。
	 * 或者，如果向列表中添加了任何异常解析器，则应用实际上接管了控制权，
	 * 必须提供完全初始化的异常解析器。
	 * <p>或者，您可以使用
	 * {@link #extendHandlerExceptionResolvers(List)}，它允许扩展或修改
	 * 默认配置的异常解析器列表。
	 * @param resolvers 初始为空的列表
	 * @see #extendHandlerExceptionResolvers(List)
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * 扩展或修改默认配置的异常解析器列表。
	 * 这对于在不干扰默认解析器的情况下插入自定义异常解析器很有用。
	 * @param resolvers 要扩展的已配置解析器列表
	 * @since 4.3
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * 提供自定义的 {@link Validator} 以替代默认创建的校验器。
	 * 默认实现（假设类路径上有 JSR-303）为：
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}。
	 * 将返回值保留为 {@code null} 以保持默认。
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * 提供自定义的 {@link MessageCodesResolver}，用于根据数据绑定和校验错误码
	 * 构建消息码。将返回值保留为 {@code null} 以保持默认。
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

}
