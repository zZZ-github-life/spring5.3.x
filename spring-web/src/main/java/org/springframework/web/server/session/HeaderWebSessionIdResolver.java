/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.server.session;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * 基于请求头和响应头的 {@link WebSessionIdResolver}。
 *
 * @author Greg Turnquist
 * @author Rob Winch
 * @since 5.0
 */
public class HeaderWebSessionIdResolver implements WebSessionIdResolver {

	/** {@link #setHeaderName(String)} 的默认值。 */
	public static final String DEFAULT_HEADER_NAME = "SESSION";


	private String headerName = DEFAULT_HEADER_NAME;


	/**
	 * 设置用于保存会话 ID 的会话头名称。
	 * <p>该名称既用于从请求头中提取会话 ID，也用于在响应头中设置会话 ID。
	 * <p>默认设置为 {@code DEFAULT_HEADER_NAME}
	 * @param headerName 头名称
	 */
	public void setHeaderName(String headerName) {
		Assert.hasText(headerName, "'headerName' must not be empty");
		this.headerName = headerName;
	}

	/**
	 * 获取已配置的头名称。
	 * @return 已配置的头名称
	 */
	public String getHeaderName() {
		return this.headerName;
	}


	@Override
	public List<String> resolveSessionIds(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		return headers.getOrDefault(getHeaderName(), Collections.emptyList());
	}

	@Override
	public void setSessionId(ServerWebExchange exchange, String id) {
		Assert.notNull(id, "'id' is required.");
		exchange.getResponse().getHeaders().set(getHeaderName(), id);
	}

	@Override
	public void expireSession(ServerWebExchange exchange) {
		this.setSessionId(exchange, "");
	}

}
