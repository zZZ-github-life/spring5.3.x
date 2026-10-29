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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * {@link javax.servlet.Filter}，用于处理 CORS 预检请求，并通过
 * {@link CorsProcessor} 拦截 CORS 简单请求和实际请求，以及基于通过所提供的
 * {@link CorsConfigurationSource} 匹配到的策略来更新响应，
 * 例如添加 CORS 响应头。
 *
 * <p>这是在 Spring MVC Java 配置和 Spring MVC XML 命名空间中配置 CORS 的替代方案。
 * 它适用于仅依赖 spring-web（而不依赖 spring-webmvc）的应用程序，
 * 或者适用于要求在 {@link javax.servlet.Filter} 级别执行 CORS 检查的安全约束。
 *
 * <p>此过滤器可以与 {@link DelegatingFilterProxy} 配合使用，以便于其初始化。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @see UrlBasedCorsConfigurationSource
 */
public class CorsFilter extends OncePerRequestFilter {

	private final CorsConfigurationSource configSource;

	private CorsProcessor processor = new DefaultCorsProcessor();


	/**
	 * 构造函数，接受过滤器用来为每个传入请求查找要使用的
	 * {@link CorsConfiguration} 的 {@link CorsConfigurationSource}。
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsFilter(CorsConfigurationSource configSource) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		this.configSource = configSource;
	}


	/**
	 * 配置自定义的 {@link CorsProcessor}，用于对请求应用匹配到的
	 * {@link CorsConfiguration}。
	 * <p>默认使用 {@link DefaultCorsProcessor}。
	 */
	public void setCorsProcessor(CorsProcessor processor) {
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.processor = processor;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(request);
		boolean isValid = this.processor.processRequest(corsConfiguration, request, response);
		if (!isValid || CorsUtils.isPreFlightRequest(request)) {
			return;
		}
		filterChain.doFilter(request, response);
	}

}
