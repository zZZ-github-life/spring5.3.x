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

package org.springframework.web.server.adapter;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 从 "Forwarded" 和 "X-Forwarded-*" 头部中提取值，以覆盖请求 URI
 * （即 {@link ServerHttpRequest#getURI()}），使其反映客户端发起的协议和地址。
 *
 * <p>此类的实例通常被声明为名为 "forwardedHeaderTransformer" 的 Bean，
 * 并由 {@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)} 检测到，
 * 也可以通过 {@link WebHttpHandlerBuilder#forwardedHeaderTransformer(ForwardedHeaderTransformer)}
 * 直接注册。
 *
 * <p>转发头部存在安全性方面的考量，因为应用无法知道这些头部是按预期由代理添加的，
 * 还是由恶意客户端添加的。因此，位于信任边界的代理应配置为移除来自外部的
 * 不可信 Forwarded 头部。
 *
 * <p>你也可以通过 {@link #setRemoveOnly removeOnly} 配置 ForwardedHeaderFilter，
 * 在这种情况下它只会移除头部，而不会使用这些头部。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
public class ForwardedHeaderTransformer implements Function<ServerHttpRequest, ServerHttpRequest> {

	static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
	}


	private boolean removeOnly;


	/**
	 * 启用一种模式：仅移除任何 "Forwarded" 或 "X-Forwarded-*" 头部，
	 * 并忽略其中的信息。
	 * @param removeOnly whether to discard and ignore forwarded headers
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * "仅移除"模式是否已启用。
	 * @see #setRemoveOnly
	 */
	public boolean isRemoveOnly() {
		return this.removeOnly;
	}


	/**
	 * 应用并移除 Forwarded 类型的头部，或仅移除它们。
	 * @param request the request
	 */
	@Override
	public ServerHttpRequest apply(ServerHttpRequest request) {
		if (hasForwardedHeaders(request)) {
			ServerHttpRequest.Builder builder = request.mutate();
			if (!this.removeOnly) {
				URI uri = UriComponentsBuilder.fromHttpRequest(request).build(true).toUri();
				builder.uri(uri);
				String prefix = getForwardedPrefix(request);
				if (prefix != null) {
					builder.path(prefix + uri.getRawPath());
					builder.contextPath(prefix);
				}
				InetSocketAddress remoteAddress = request.getRemoteAddress();
				remoteAddress = UriComponentsBuilder.parseForwardedFor(request, remoteAddress);
				if (remoteAddress != null) {
					builder.remoteAddress(remoteAddress);
				}
			}
			removeForwardedHeaders(builder);
			request = builder.build();
		}
		return request;
	}

	/**
	 * 请求是否包含任何 Forwarded 头部。
	 * @param request the request
	 */
	protected boolean hasForwardedHeaders(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		for (String headerName : FORWARDED_HEADER_NAMES) {
			if (headers.containsKey(headerName)) {
				return true;
			}
		}
		return false;
	}

	private void removeForwardedHeaders(ServerHttpRequest.Builder builder) {
		builder.headers(map -> FORWARDED_HEADER_NAMES.forEach(map::remove));
	}


	@Nullable
	private static String getForwardedPrefix(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		String header = headers.getFirst("X-Forwarded-Prefix");
		if (header == null) {
			return null;
		}
		StringBuilder prefix = new StringBuilder(header.length());
		String[] rawPrefixes = StringUtils.tokenizeToStringArray(header, ",");
		for (String rawPrefix : rawPrefixes) {
			int endIndex = rawPrefix.length();
			while (endIndex > 1 && rawPrefix.charAt(endIndex - 1) == '/') {
				endIndex--;
			}
			prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
		}
		return prefix.toString();
	}

}
