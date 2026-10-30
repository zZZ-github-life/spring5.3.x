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

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

/**
 * 定义用于自定义通过 {@link EnableWebFlux @EnableWebFlux} 启用的 WebFlux 应用配置的回调方法。
 *
 * <p>带有 {@code @EnableWebFlux} 注解的配置类可以实现此接口，以便被回调并有机会自定义默认配置。可考虑实现此接口并重写相关方法以满足需求。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 */
public interface WebFluxConfigurer {

	/**
	 * 配置在使用注解式控制器处理请求时如何解析为响应请求的内容类型。
	 * @param builder 用于配置要使用的解析器
	 */
	default void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * 配置“全局”跨源请求处理。所配置的 CORS 映射适用于注解式控制器、函数式端点和静态资源。
	 * <p>注解式控制器还可以通过 {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin} 声明更细粒度的配置。在这种情况下，此处声明的“全局” CORS 配置会与控制器方法上定义的局部 CORS 配置{@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) 合并}。
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 配置路径匹配选项。
	 * <p>所配置的路径匹配选项将用于映射到注解式控制器以及{@link #addResourceHandlers(ResourceHandlerRegistry) 静态资源}。
	 * @param configurer {@link PathMatchConfigurer} 实例
	 */
	default void configurePathMatching(PathMatchConfigurer configurer) {
	}

	/**
	 * 添加用于提供静态资源的资源处理器。
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 为自定义 {@code @RequestMapping} 方法参数配置解析器。
	 * @param configurer 要使用的配置器
	 */
	default void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * 配置 HTTP 消息读写器，用于在注解式控制器和函数式端点中读取请求体以及写入响应体。
	 * <p>默认情况下，只要类路径上存在相应的第三方库（如 Jackson JSON、JAXB2 等），就会配置所有内置读写器。
	 * @param configurer 用于自定义读取器和写入器的配置器
	 */
	default void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * 添加自定义 {@link Converter Converter} 和 {@link Formatter Formatter}，用于对注解式控制器方法参数执行类型转换和格式化。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 提供自定义 {@link Validator}。
	 * <p>默认情况下，如果类路径上存在 bean validation API，则会为标准 bean 校验创建校验器。
	 * <p>所配置的校验器用于校验注解式控制器方法参数。
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * 提供自定义 {@link MessageCodesResolver}，用于注解式控制器方法参数的数据绑定，替代 {@link org.springframework.validation.DataBinder} 中默认创建的实例。
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 提供用于创建 {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter} 的 {@link WebSocketService}。可通过 {@link org.springframework.web.reactive.socket.server.RequestUpgradeStrategy} 配置服务器特定的属性。
	 * @since 5.3
	 */
	@Nullable
	default WebSocketService getWebSocketService() {
		return null;
	}

	/**
	 * 配置视图解析，以便使用视图和模型渲染响应，其中视图通常是 HTML 模板，但也可以基于 HTTP 消息读写器（例如 JSON、XML）。
	 * <p>所配置的视图解析器将同时用于注解式控制器和函数式端点。
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

}
