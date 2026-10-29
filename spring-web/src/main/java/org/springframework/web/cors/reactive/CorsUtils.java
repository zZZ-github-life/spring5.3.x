/*
 * Copyright 2002-2023 the original author or authors.
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

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 基于 <a href="https://www.w3.org/TR/cors/">CORS W3C 建议</a>
 * 进行响应式 CORS 请求处理的工具类。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class CorsUtils {

	/**
	 * 通过检查是否存在 {@code Origin} 头，并通过 {@link #isSameOrigin}
	 * 确保 origin 不同，如果请求是有效的 CORS 请求则返回 {@code true}。
	 */
	@SuppressWarnings("deprecation")
	public static boolean isCorsRequest(ServerHttpRequest request) {
		return request.getHeaders().containsKey(HttpHeaders.ORIGIN) && !isSameOrigin(request);
	}

	/**
	 * 通过检查 {@code OPTIONS} 方法以及 {@code Origin} 和
	 * {@code Access-Control-Request-Method} 头是否存在，如果请求是有效的
	 * CORS 预检请求则返回 {@code true}。
	 */
	public static boolean isPreFlightRequest(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		return (request.getMethod() == HttpMethod.OPTIONS
				&& headers.containsKey(HttpHeaders.ORIGIN)
				&& headers.containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * 基于 {@code Origin} 和 {@code Host} 头检查请求是否为同源请求。
	 *
	 * <p><strong>注意：</strong>从 5.1 开始，此方法会忽略指定客户端发起地址的
	 * {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。可以考虑使用
	 * {@code ForwardedHeaderFilter} 来提取并使用，或丢弃此类头。
	 * @return {@code true} if the request is a same-origin one, {@code false} in case
	 * of a cross-origin request
	 * @deprecated as of 5.2, same-origin checks are performed directly by {@link #isCorsRequest}
	 */
	@Deprecated
	public static boolean isSameOrigin(ServerHttpRequest request) {
		String origin = request.getHeaders().getOrigin();
		if (origin == null) {
			return true;
		}

		URI uri = request.getURI();
		String actualScheme = uri.getScheme();
		String actualHost = uri.getHost();
		int actualPort = getPort(uri.getScheme(), uri.getPort());
		Assert.notNull(actualScheme, "Actual request scheme must not be null");
		Assert.notNull(actualHost, "Actual request host must not be null");
		Assert.isTrue(actualPort != -1, "Actual request port must not be undefined");

		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
		return (actualScheme.equals(originUrl.getScheme()) &&
				actualHost.equals(originUrl.getHost()) &&
				actualPort == getPort(originUrl.getScheme(), originUrl.getPort()));
	}

	private static int getPort(@Nullable String scheme, int port) {
		if (port == -1) {
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			}
			else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		return port;
	}

}
