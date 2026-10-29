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

import java.nio.charset.Charset;

import javax.servlet.Filter;

import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 定义用于构建 {@code MockMvc} 的通用方法。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @param <B> a self reference to the builder type
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {

	/**
	 * 添加映射到任意请求（即 “/*”）的过滤器。例如：
	 * <pre class="code">
	 * mockMvcBuilder.addFilters(springSecurityFilterChain);
	 * </pre>
	 * <p>它等效于以下 web.xml 配置：
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 * <p>过滤器将按提供它们的顺序被调用。
	 * @param filters the filters to add
	 */
	<T extends B> T addFilters(Filter... filters);

	/**
	 * 添加映射到特定模式集的过滤器。例如：
	 * <pre class="code">
	 * mockMvcBuilder.addFilter(myResourceFilter, "/resources/*");
	 * </pre>
	 * <p>它等效于：
	 * <pre class="code">
	 * &lt;filter-mapping&gt;
	 *     &lt;filter-name&gt;myResourceFilter&lt;/filter-name&gt;
	 *     &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
	 * &lt;/filter-mapping&gt;
	 * </pre>
	 * <p>过滤器将按提供它们的顺序被调用。
	 * @param filter the filter to add
	 * @param urlPatterns the URL patterns to map to; if empty, "/*" is used by default
	 */
	<T extends B> T addFilter(Filter filter, String... urlPatterns);

	/**
	 * 定义应合并到所有已执行请求中的默认请求属性。
	 * 实际上，这提供了一种为所有请求定义通用初始化的机制，
	 * 例如内容类型、请求参数、会话属性以及任何其他请求属性。
	 *
	 * <p>执行请求时指定的属性会覆盖此处定义的默认属性。
	 * @param requestBuilder a RequestBuilder; see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 */
	<T extends B> T defaultRequest(RequestBuilder requestBuilder);

	/**
	 * 定义应用于每个响应的默认字符编码。
	 * <p>此方法的默认实现会抛出
	 * {@link UnsupportedOperationException}。因此，鼓励具体实现
	 * 重写此方法。
	 * @param defaultResponseCharacterEncoding the default response character encoding
	 * @since 5.3.10
	 */
	default <T extends B> T defaultResponseCharacterEncoding(Charset defaultResponseCharacterEncoding) {
		throw new UnsupportedOperationException("defaultResponseCharacterEncoding is not supported by this MockMvcBuilder");
	}

	/**
	 * 定义一个应<em>始终</em>应用于每个响应的全局预期。
	 * 例如，状态码 200（OK）、内容类型
	 * {@code "application/json"} 等。
	 * @param resultMatcher a ResultMatcher; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers}
	 */
	<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

	/**
	 * 定义一个应<em>始终</em>应用于每个响应的全局操作。
	 * 例如，将已执行请求和结果响应的详细信息写入 {@code System.out}。
	 * @param resultHandler a ResultHandler; see static factory methods in
	 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers}
	 */
	<T extends B> T alwaysDo(ResultHandler resultHandler);

	/**
	 * 是否启用 DispatcherServlet 属性
	 * {@link org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest dispatchOptionsRequest}，
	 * 该属性允许处理 HTTP OPTIONS 请求。
	 */
	<T extends B> T dispatchOptions(boolean dispatchOptions);

	/**
	 * {@link #dispatchOptions(boolean)} 的更高级变体，
	 * 允许自定义任何 {@link org.springframework.web.servlet.DispatcherServlet} 属性。
	 * @since 5.3
	 */
	<T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer);

	/**
	 * 添加一个 {@code MockMvcConfigurer}，用于自动化 MockMvc 设置，
	 * 并针对某些特定用途（如安全）对其进行配置。
	 * <p>有一个内置的 {@link SharedHttpSessionConfigurer}，
	 * 可用于跨请求重用 HTTP 会话。像 Spring Security 这样的第三方框架
	 * 也使用此机制来提供配置快捷方式。
	 * @see SharedHttpSessionConfigurer
	 */
	<T extends B> T apply(MockMvcConfigurer configurer);

}
