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

package org.springframework.web.socket.server.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 一个拦截器，用于将信息从 HTTP 会话复制到“握手属性”映射中，
 * 以便通过 {@link WebSocketSession#getAttributes()} 获取。
 *
 * <p>在键 {@link #HTTP_SESSION_ID_ATTR_NAME} 下复制部分或全部 HTTP 会话属性
 * 和/或 HTTP 会话 ID。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {

	/**
	 * 当 {@link #setCopyHttpSessionId(boolean) copyHttpSessionId} 为 "true" 时，
	 * 暴露 HTTP 会话 ID 的属性名称。
	 */
	public static final String HTTP_SESSION_ID_ATTR_NAME = "HTTP.SESSION.ID";


	private final Collection<String> attributeNames;

	private boolean copyAllAttributes;

	private boolean copyHttpSessionId = true;

	private boolean createSession;


	/**
	 * 用于复制所有 HTTP 会话属性及 HTTP 会话 ID 的默认构造函数。
	 * @see #setCopyAllAttributes
	 * @see #setCopyHttpSessionId
	 */
	public HttpSessionHandshakeInterceptor() {
		this.attributeNames = Collections.emptyList();
		this.copyAllAttributes = true;
	}

	/**
	 * 用于复制特定 HTTP 会话属性及 HTTP 会话 ID 的构造函数。
	 * @param attributeNames session attributes to copy
	 * @see #setCopyAllAttributes
	 * @see #setCopyHttpSessionId
	 */
	public HttpSessionHandshakeInterceptor(Collection<String> attributeNames) {
		this.attributeNames = Collections.unmodifiableCollection(attributeNames);
		this.copyAllAttributes = false;
	}


	/**
	 * 返回配置的要复制的属性名称（只读）。
	 */
	public Collection<String> getAttributeNames() {
		return this.attributeNames;
	}

	/**
	 * 是否复制 HTTP 会话中的所有属性。如果设置为 "true"，
	 * 则忽略任何显式配置的属性名称。
	 * <p>默认情况下，此值根据所使用的构造函数（分别为默认构造函数或带属性名称的构造函数）
	 * 设置为 "true" 或 "false"。
	 * @param copyAllAttributes whether to copy all attributes
	 */
	public void setCopyAllAttributes(boolean copyAllAttributes) {
		this.copyAllAttributes = copyAllAttributes;
	}

	/**
	 * 是否复制所有 HTTP 会话属性。
	 */
	public boolean isCopyAllAttributes() {
		return this.copyAllAttributes;
	}

	/**
	 * 是否应将 HTTP 会话 ID 复制到握手属性中
	 * 键 {@link #HTTP_SESSION_ID_ATTR_NAME} 下。
	 * <p>默认情况下为 "true"。
	 * @param copyHttpSessionId whether to copy the HTTP session id.
	 */
	public void setCopyHttpSessionId(boolean copyHttpSessionId) {
		this.copyHttpSessionId = copyHttpSessionId;
	}

	/**
	 * 是否将 HTTP 会话 ID 复制到握手属性中。
	 */
	public boolean isCopyHttpSessionId() {
		return this.copyHttpSessionId;
	}

	/**
	 * 是否允许在访问 HTTP 会话时创建它。
	 * <p>默认设置为 {@code false}。
	 * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
	 */
	public void setCreateSession(boolean createSession) {
		this.createSession = createSession;
	}

	/**
	 * 是否允许创建 HTTP 会话。
	 */
	public boolean isCreateSession() {
		return this.createSession;
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		HttpSession session = getSession(request);
		if (session != null) {
			if (isCopyHttpSessionId()) {
				attributes.put(HTTP_SESSION_ID_ATTR_NAME, session.getId());
			}
			Enumeration<String> names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (isCopyAllAttributes() || getAttributeNames().contains(name)) {
					attributes.put(name, session.getAttribute(name));
				}
			}
		}
		return true;
	}

	@Nullable
	private HttpSession getSession(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
			return serverRequest.getServletRequest().getSession(isCreateSession());
		}
		return null;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, @Nullable Exception ex) {
	}

}
