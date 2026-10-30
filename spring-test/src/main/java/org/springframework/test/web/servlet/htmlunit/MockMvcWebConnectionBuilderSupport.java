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

package org.springframework.test.web.servlet.htmlunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * 支持类，用于简化创建使用 {@link MockMvc} 并可针对特定请求
 * 委托给真实 {@link WebConnection} 的 {@link WebConnection}。
 *
 * <p>默认情况下，对 {@code localhost} 的请求使用 {@link MockMvc}，
 * 其他请求使用真实的 {@link WebConnection}。
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @param <T> 构建器类型的自引用
 */
public abstract class MockMvcWebConnectionBuilderSupport<T extends MockMvcWebConnectionBuilderSupport<T>> {

	private final MockMvc mockMvc;

	private final List<WebRequestMatcher> requestMatchers = new ArrayList<>();

	private String contextPath = "";

	private boolean alwaysUseMockMvc;


	/**
	 * 使用提供的 {@link MockMvc} 实例创建新实例。
	 * @param mockMvc 要使用的 {@code MockMvc} 实例；永不为 {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
		this.requestMatchers.add(new HostRequestMatcher("localhost"));
	}

	/**
	 * 使用提供的 {@link WebApplicationContext} 创建新实例。
	 * @param context 用于从中创建 {@code MockMvc} 实例的 {@code WebApplicationContext}；
	 * 永不为 {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context) {
		this(MockMvcBuilders.webAppContextSetup(context).build());
	}

	/**
	 * 使用提供的 {@link WebApplicationContext}
	 * 和 {@link MockMvcConfigurer} 创建新实例。
	 * @param context 用于从中创建 {@code MockMvc} 实例的 {@code WebApplicationContext}；
	 * 永不为 {@code null}
	 * @param configurer 要应用的 MockMvcConfigurer；永不为 {@code null}
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context, MockMvcConfigurer configurer) {
		this(MockMvcBuilders.webAppContextSetup(context).apply(configurer).build());
	}


	/**
	 * 设置要使用的上下文路径。
	 * <p>如果提供的值为 {@code null} 或空，则假定请求 URL 的第一个路径
	 * 段为上下文路径。
	 * <p>默认为 {@code ""}。
	 * @param contextPath 要使用的上下文路径
	 * @return 该构建器，用于进一步定制
	 */
	@SuppressWarnings("unchecked")
	public T contextPath(String contextPath) {
		this.contextPath = contextPath;
		return (T) this;
	}

	/**
	 * 指定无论请求看起来如何，都应始终使用 {@link MockMvc}。
	 * @return 该构建器，用于进一步定制
	 */
	@SuppressWarnings("unchecked")
	public T alwaysUseMockMvc() {
		this.alwaysUseMockMvc = true;
		return (T) this;
	}

	/**
	 * 添加额外的 {@link WebRequestMatcher} 实例，以确保在
	 * 此类匹配器与 Web 请求匹配时使用 {@link MockMvc} 处理该请求。
	 * @param matchers 额外的 {@code WebRequestMatcher} 实例
	 * @return 该构建器，用于进一步定制
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvc(WebRequestMatcher... matchers) {
		Collections.addAll(this.requestMatchers, matchers);
		return (T) this;
	}

	/**
	 * 添加额外的 {@link WebRequestMatcher} 实例，当提供的主机匹配时
	 * 返回 {@code true} &mdash; 例如 {@code "example.com"} 或
	 * {@code "example.com:8080"}。
	 * @param hosts 额外的、确保调用 {@code MockMvc} 的主机
	 * @return 该构建器，用于进一步定制
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvcForHosts(String... hosts) {
		this.requestMatchers.add(new HostRequestMatcher(hosts));
		return (T) this;
	}

	/**
	 * 创建一个新的 {@link WebConnection}，当指定的某个 {@link WebRequestMatcher} 实例
	 * 匹配时，它将使用 {@link MockMvc} 实例。
	 * @param webClient 当指定的任何 {@code WebRequestMatcher} 都不匹配时
	 * 要使用的 WebClient（永不为 {@code null}）
	 * @return 一个新的 {@code WebConnection}，当指定的
	 * {@code WebRequestMatcher} 之一匹配时将使用 {@code MockMvc} 实例
	 * @since 4.3
	 * @see #alwaysUseMockMvc()
	 * @see #useMockMvc(WebRequestMatcher...)
	 * @see #useMockMvcForHosts(String...)
	 */
	protected final WebConnection createConnection(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		return createConnection(webClient, webClient.getWebConnection());
	}

	private WebConnection createConnection(WebClient webClient, WebConnection defaultConnection) {
		WebConnection connection = new MockMvcWebConnection(this.mockMvc, webClient, this.contextPath);
		if (this.alwaysUseMockMvc) {
			return connection;
		}
		List<DelegateWebConnection> delegates = new ArrayList<>(this.requestMatchers.size());
		for (WebRequestMatcher matcher : this.requestMatchers) {
			delegates.add(new DelegateWebConnection(matcher, connection));
		}
		return new DelegatingWebConnection(defaultConnection, delegates);
	}

}
