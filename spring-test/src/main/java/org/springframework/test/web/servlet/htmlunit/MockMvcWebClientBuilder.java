/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.WebClient;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcWebClientBuilder} 简化了创建委托给 {@link MockMvc} 实例的 HtmlUnit
 * {@link WebClient} 的过程。
 *
 * <p>构建器使用的 {@code MockMvc} 实例可以通过
 * {@linkplain #mockMvcSetup 直接提供}，也可以从
 * {@link #webAppContextSetup WebApplicationContext} 透明地创建。
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see #mockMvcSetup(MockMvc)
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #webAppContextSetup(WebApplicationContext, MockMvcConfigurer)
 * @see #withDelegate(WebClient)
 * @see #build()
 */
public class MockMvcWebClientBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcWebClientBuilder> {

	@Nullable
	private WebClient webClient;


	protected MockMvcWebClientBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcWebClientBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}


	/**
	 * 基于提供的 {@link MockMvc} 实例创建新的 {@code MockMvcWebClientBuilder}。
	 * @param mockMvc 要使用的 {@code MockMvc} 实例；永不为 {@code null}
	 * @return 要定制的 MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcWebClientBuilder(mockMvc);
	}

	/**
	 * 基于提供的 {@link WebApplicationContext} 创建新的 {@code MockMvcWebClientBuilder}。
	 * @param context 用于从中创建 {@link MockMvc} 实例的 {@code WebApplicationContext}；
	 * 永不为 {@code null}
	 * @return 要定制的 MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcWebClientBuilder(context);
	}

	/**
	 * 基于提供的 {@link WebApplicationContext} 和 {@link MockMvcConfigurer} 创建新的
	 * {@code MockMvcWebClientBuilder}。
	 * @param context 用于从中创建 {@link MockMvc} 实例的 {@code WebApplicationContext}；
	 * 永不为 {@code null}
	 * @param configurer 要应用的 {@code MockMvcConfigurer}；永不为 {@code null}
	 * @return 要定制的 MockMvcWebClientBuilder
	 */
	public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcWebClientBuilder(context, configurer);
	}

	/**
	 * 提供此构建器 {@linkplain #build 构建} 的客户端在处理
	 * 非{@linkplain WebRequestMatcher 匹配}请求时应委托给
	 * 的 {@code WebClient}。
	 * @param webClient 用于委托处理不匹配请求的 {@code WebClient}；
	 * 永不为 {@code null}
	 * @return 该构建器，用于进一步定制
	 * @see #build()
	 */
	public MockMvcWebClientBuilder withDelegate(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		webClient.setWebConnection(createConnection(webClient));
		this.webClient = webClient;
		return this;
	}

	/**
	 * 构建通过此构建器配置的 {@link WebClient}。
	 * <p>返回的客户端将使用配置的 {@link MockMvc} 实例
	 * 处理任何 {@linkplain WebRequestMatcher 匹配}请求，
	 * 并对所有其他请求使用委托的 {@code WebClient}。
	 * <p>如果已显式配置{@linkplain #withDelegate 委托}，
	 * 则使用该委托；否则，将配置默认的 {@code WebClient}
	 * 作为委托。
	 * @return 要使用的 {@code WebClient}
	 * @see #withDelegate(WebClient)
	 */
	public WebClient build() {
		return (this.webClient != null ? this.webClient : withDelegate(new WebClient()).build());
	}

}
