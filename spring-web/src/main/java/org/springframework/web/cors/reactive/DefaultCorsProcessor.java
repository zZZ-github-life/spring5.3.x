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

package org.springframework.web.cors.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link CorsProcessor} 的默认实现，符合
 * <a href="https://www.w3.org/TR/cors/">CORS W3C 建议</a>。
 *
 * <p>请注意，当输入的 {@link CorsConfiguration} 为 {@code null} 时，
 * 此实现不会立即拒绝简单请求或实际请求，而只是避免向响应添加 CORS 头。
 * 如果响应中已包含 CORS 头，也会跳过 CORS 处理。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultCorsProcessor implements CorsProcessor {

	private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);

	private static final List<String> VARY_HEADERS = Arrays.asList(
			HttpHeaders.ORIGIN, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);


	@Override
	public boolean process(@Nullable CorsConfiguration config, ServerWebExchange exchange) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders responseHeaders = response.getHeaders();

		List<String> varyHeaders = responseHeaders.get(HttpHeaders.VARY);
		if (varyHeaders == null) {
			responseHeaders.addAll(HttpHeaders.VARY, VARY_HEADERS);
		}
		else {
			for (String header : VARY_HEADERS) {
				if (!varyHeaders.contains(header)) {
					responseHeaders.add(HttpHeaders.VARY, header);
				}
			}
		}

		if (!CorsUtils.isCorsRequest(request)) {
			return true;
		}

		if (responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
			logger.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
			return true;
		}

		boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);
		if (config == null) {
			if (preFlightRequest) {
				rejectRequest(response);
				return false;
			}
			else {
				return true;
			}
		}

		return handleInternal(exchange, config, preFlightRequest);
	}

	/**
	 * 当某一项 CORS 检查失败时调用。
	 */
	protected void rejectRequest(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.FORBIDDEN);
	}

	/**
	 * 处理给定的请求。
	 */
	protected boolean handleInternal(ServerWebExchange exchange,
			CorsConfiguration config, boolean preFlightRequest) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders responseHeaders = response.getHeaders();

		String requestOrigin = request.getHeaders().getOrigin();
		String allowOrigin = checkOrigin(config, requestOrigin);
		if (allowOrigin == null) {
			logger.debug("Reject: '" + requestOrigin + "' origin is not allowed");
			rejectRequest(response);
			return false;
		}

		HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
		if (allowMethods == null) {
			logger.debug("Reject: HTTP '" + requestMethod + "' is not allowed");
			rejectRequest(response);
			return false;
		}

		List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
		List<String> allowHeaders = checkHeaders(config, requestHeaders);
		if (preFlightRequest && allowHeaders == null) {
			logger.debug("Reject: headers '" + requestHeaders + "' are not allowed");
			rejectRequest(response);
			return false;
		}

		responseHeaders.setAccessControlAllowOrigin(allowOrigin);

		if (preFlightRequest) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		if (preFlightRequest && !allowHeaders.isEmpty()) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		if (preFlightRequest && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		return true;
	}

	/**
	 * 检查 origin 并确定响应使用的 origin。默认实现仅委托给
	 * {@link CorsConfiguration#checkOrigin(String)}。
	 */
	@Nullable
	protected String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * 检查 HTTP 方法并确定预检请求响应中要列出的方法。默认实现仅委托给
	 * {@link CorsConfiguration#checkHttpMethod(HttpMethod)}。
	 */
	@Nullable
	protected List<HttpMethod> checkMethods(CorsConfiguration config, @Nullable HttpMethod requestMethod) {
		return config.checkHttpMethod(requestMethod);
	}

	@Nullable
	private HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
		return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
	}

	/**
	 * 检查请求头并确定预检请求响应中要列出的请求头。默认实现仅委托给
	 * {@link CorsConfiguration#checkOrigin(String)}。
	 */
	@Nullable

	protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		HttpHeaders headers = request.getHeaders();
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<>(headers.keySet()));
	}

}
