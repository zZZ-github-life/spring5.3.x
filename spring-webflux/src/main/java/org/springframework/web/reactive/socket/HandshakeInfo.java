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

package org.springframework.web.reactive.socket;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * 与启动 {@link WebSocketSession} 会话的握手请求相关信息的简单容器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSocketSession#getHandshakeInfo()
 */
public class HandshakeInfo {

	private static final MultiValueMap<String, HttpCookie> EMPTY_COOKIES =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	private final URI uri;

	private final Mono<Principal> principalMono;

	private final HttpHeaders headers;

	private final MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private final String protocol;

	@Nullable
	private final InetSocketAddress remoteAddress;

	private final Map<String, Object> attributes;

	@Nullable
	private final String logPrefix;


	/**
	 * 使用有关握手的基本信息构造。
	 * @param uri the endpoint URL
	 * @param headers request headers for server or response headers or client
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal, @Nullable String protocol) {
		this(uri, headers, EMPTY_COOKIES, principal, protocol, null, Collections.emptyMap(), null);
	}

	/**
	 * 针对服务器端使用的构造函数，包含远程地址、属性和日志前缀等额外信息。
	 * @param uri the endpoint URL
	 * @param headers server request headers
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 * @param remoteAddress the remote address of the client
	 * @param attributes initial attributes for the WebSocket session
	 * @param logPrefix the log prefix for the handshake request.
	 * @since 5.1
	 * @deprecated as of 5.3.5 in favor of
	 * {@link #HandshakeInfo(URI, HttpHeaders, MultiValueMap, Mono, String, InetSocketAddress, Map, String)}
	 */
	@Deprecated
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal,
				@Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
				Map<String, Object> attributes, @Nullable String logPrefix) {

		this(uri, headers, EMPTY_COOKIES, principal, protocol, remoteAddress, attributes, logPrefix);
	}

	/**
	 * 针对服务器端使用的构造函数，包含 Cookie、远程地址、属性和日志前缀等
	 * 额外信息。
	 * @param uri the endpoint URL
	 * @param headers server request headers
	 * @param cookies server request cookies
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 * @param remoteAddress the remote address of the client
	 * @param attributes initial attributes for the WebSocket session
	 * @param logPrefix the log prefix for the handshake request.
	 * @since 5.3.5
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
				Mono<Principal> principal, @Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
				Map<String, Object> attributes, @Nullable String logPrefix) {

		Assert.notNull(uri, "URI is required");
		Assert.notNull(headers, "HttpHeaders are required");
		Assert.notNull(cookies, "`cookies` are required");
		Assert.notNull(principal, "Principal is required");
		Assert.notNull(attributes, "'attributes' is required");

		this.uri = uri;
		this.headers = headers;
		this.cookies = cookies;
		this.principalMono = principal;
		this.protocol = protocol;
		this.remoteAddress = remoteAddress;
		this.attributes = attributes;
		this.logPrefix = logPrefix;
	}


	/**
	 * 返回 WebSocket 端点的 URL。
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * 返回握手请求中的 HTTP 头：对于服务器会话是服务器请求头，对于客户端会话是
	 * 客户端响应头。
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 对于服务器会话，返回握手请求中的服务器请求 Cookie。对于客户端会话，
	 * 返回空 Map。
	 * @since 5.3.5
	 */
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.cookies;
	}

	/**
	 * 返回与握手请求关联的主体（如果有）。
	 */
	public Mono<Principal> getPrincipal() {
		return this.principalMono;
	}

	/**
	 * 握手时协商的子协议，如果没有则为 {@code null}。
	 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">
	 * https://tools.ietf.org/html/rfc6455#section-1.9</a>
	 */
	@Nullable
	public String getSubProtocol() {
		return this.protocol;
	}

	/**
	 * 对于服务器会话，这是握手请求的来源远程地址。对于客户端会话，为 {@code null}。
	 * @since 5.1
	 */
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * 从握手请求中提取并添加到会话的属性。
	 * @since 5.1
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * 在握手中用于关联日志消息的日志前缀（如果有）。
	 * @return a log prefix, or {@code null} if not specified
	 * @since 5.1
	 */
	@Nullable
	public String getLogPrefix() {
		return this.logPrefix;
	}


	@Override
	public String toString() {
		return "HandshakeInfo[uri=" + this.uri + "]";
	}

}
