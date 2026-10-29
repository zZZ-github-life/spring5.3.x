/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * 定义用于处理 {@link HandlerFunction HandlerFunctions} 的策略。
 *
 * <p>此类的实例是不可变的。实例通常通过可变的 {@link Builder} 创建：使用 {@link #builder()} 设置默认策略，或使用 {@link #empty()} 从零开始。
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RouterFunctions#toHttpHandler(RouterFunction, HandlerStrategies)
 */
public interface HandlerStrategies {

	/**
	 * 返回用于请求正文转换的 {@link HttpMessageReader HttpMessageReaders}。
	 * @return the message readers
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * 返回用于响应正文转换的 {@link HttpMessageWriter HttpMessageWriters}。
	 * @return the message writers
	 */
	List<HttpMessageWriter<?>> messageWriters();

	/**
	 * 返回用于视图名称解析的 {@link ViewResolver ViewResolvers}。
	 * @return the view resolvers
	 */
	List<ViewResolver> viewResolvers();

	/**
	 * 返回用于过滤请求和响应的 {@link WebFilter WebFilters}。
	 * @return the web filters
	 */
	List<WebFilter> webFilters();

	/**
	 * 返回用于处理异常的 {@link WebExceptionHandler WebExceptionHandlers}。
	 * @return the exception handlers
	 */
	List<WebExceptionHandler> exceptionHandlers();

	/**
	 * 返回用于解析区域上下文的 {@link LocaleContextResolver}。
	 * @return the locale context resolver
	 */
	LocaleContextResolver localeContextResolver();


	// Static builder methods

	/**
	 * 返回具有默认初始化的新 {@code HandlerStrategies}。
	 * @return the new {@code HandlerStrategies}
	 */
	static HandlerStrategies withDefaults() {
		return builder().build();
	}

	/**
	 * 返回具有默认初始化的 {@code HandlerStrategies} 可变构建器。
	 * @return the builder
	 */
	static Builder builder() {
		DefaultHandlerStrategiesBuilder builder = new DefaultHandlerStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * 返回 {@code HandlerStrategies} 的可变空构建器。
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultHandlerStrategiesBuilder();
	}


	/**
	 * 用于 {@link HandlerStrategies} 的可变构建器。
	 */
	interface Builder {

		/**
		 * 自定义服务器端 HTTP 消息读写器列表。
		 * @param consumer the consumer to customize the codecs
		 * @return this builder
		 */
		Builder codecs(Consumer<ServerCodecConfigurer> consumer);

		/**
		 * 将给定视图解析器添加到此构建器。
		 * @param viewResolver the view resolver to add
		 * @return this builder
		 */
		Builder viewResolver(ViewResolver viewResolver);

		/**
		 * 将给定 Web 过滤器添加到此构建器。
		 * @param filter the filter to add
		 * @return this builder
		 */
		Builder webFilter(WebFilter filter);

		/**
		 * 将给定异常处理器添加到此构建器。
		 * @param exceptionHandler the exception handler to add
		 * @return this builder
		 */
		Builder exceptionHandler(WebExceptionHandler exceptionHandler);

		/**
		 * 将给定区域上下文解析器添加到此构建器。
		 * @param localeContextResolver the locale context resolver to add
		 * @return this builder
		 */
		Builder localeContextResolver(LocaleContextResolver localeContextResolver);

		/**
		 * 构建 {@link HandlerStrategies}。
		 * @return the built strategies
		 */
		HandlerStrategies build();
	}

}
