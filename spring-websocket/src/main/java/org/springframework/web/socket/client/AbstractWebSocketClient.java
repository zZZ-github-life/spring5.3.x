/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link WebSocketClient} 实现的抽象基类。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

	private static final Set<String> specialHeaders = new HashSet<>();

	static {
		specialHeaders.add("cache-control");
		specialHeaders.add("connection");
		specialHeaders.add("host");
		specialHeaders.add("sec-websocket-extensions");
		specialHeaders.add("sec-websocket-key");
		specialHeaders.add("sec-websocket-protocol");
		specialHeaders.add("sec-websocket-version");
		specialHeaders.add("pragma");
		specialHeaders.add("upgrade");
	}


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "'uriTemplate' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(webSocketHandler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			@Nullable WebSocketHttpHeaders headers, URI uri) {

		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		assertUri(uri);

		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to " + uri);
		}

		HttpHeaders headersToUse = new HttpHeaders();
		if (headers != null) {
			headers.forEach((header, values) -> {
				if (values != null && !specialHeaders.contains(header.toLowerCase())) {
					headersToUse.put(header, values);
				}
			});
		}

		List<String> subProtocols =
				(headers != null ? headers.getSecWebSocketProtocol() : Collections.emptyList());
		List<WebSocketExtension> extensions =
				(headers != null ? headers.getSecWebSocketExtensions() : Collections.emptyList());

		return doHandshakeInternal(webSocketHandler, headersToUse, uri, subProtocols, extensions,
				Collections.emptyMap());
	}

	protected void assertUri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		String scheme = uri.getScheme();
		if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: " + scheme);
		}
	}

	/**
	 * 执行实际的握手以建立到服务器的连接。
	 * @param webSocketHandler 客户端侧的 WebSocket 消息处理器
	 * @param headers 用于握手的 HTTP 头，其中不需要（被禁止）的头
	 * 已被过滤掉（永不为 {@code null}）
	 * @param uri 握手的目标 URI（永不为 {@code null}）
	 * @param subProtocols 请求的子协议，或空列表
	 * @param extensions 请求的 WebSocket 扩展，或空列表
	 * @param attributes 要与 WebSocketSession 关联的属性，即通过
	 * {@link WebSocketSession#getAttributes()} 获取；目前始终为空 Map。
	 * @return 已建立的 WebSocket 会话，封装在 ListenableFuture 中。
	 */
	protected abstract ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions,
			Map<String, Object> attributes);

}
