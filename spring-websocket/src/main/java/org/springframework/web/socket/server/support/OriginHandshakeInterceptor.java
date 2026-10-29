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

package org.springframework.web.socket.server.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * 一个拦截器，用于根据允许的来源集合检查请求的 {@code Origin} 头值。
 *
 * @author Sebastien Deleuze
 * @since 4.1.2
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CorsConfiguration corsConfiguration = new CorsConfiguration();


	/**
	 * 仅允许同源请求的默认构造函数。
	 */
	public OriginHandshakeInterceptor() {
	}

	/**
	 * 使用指定的允许来源值构造。
	 * @see #setAllowedOrigins(Collection)
	 */
	public OriginHandshakeInterceptor(Collection<String> allowedOrigins) {
		setAllowedOrigins(allowedOrigins);
	}


	/**
	 * 设置允许浏览器发起跨域请求的来源。
	 * 有关格式细节和注意事项，请参阅 {@link CorsConfiguration#setAllowedOrigins(List)}，
	 * 并记住 CORS 规范不允许在 {@code allowCredentials=true} 时使用 {@code "*"}。
	 * 如需更灵活的来源模式，请改用 {@link #setAllowedOriginPatterns}。
	 *
	 * <p>默认情况下，不允许任何来源。当同时设置了
	 * {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns} 时，
	 * 该属性优先于此属性。
	 *
	 * <p>请注意，当启用 SockJS 且限制了来源时，不允许检查请求来源的
	 * 传输类型（基于 Iframe 的传输）将被禁用。因此，在限制来源时
	 * 不支持 IE 6 到 9。
	 * @see #setAllowedOriginPatterns(Collection)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * 返回 {@link #setAllowedOriginPatterns(Collection) 已配置的}允许来源。
	 * @since 4.1.5
	 */
	public Collection<String> getAllowedOrigins() {
		List<String> allowedOrigins = this.corsConfiguration.getAllowedOrigins();
		return (CollectionUtils.isEmpty(allowedOrigins) ? Collections.emptySet() :
				Collections.unmodifiableSet(new LinkedHashSet<>(allowedOrigins)));
	}

	/**
	 * {@link #setAllowedOrigins(Collection)} 的替代方案，支持更灵活的模式
	 * 来指定允许浏览器发起跨域请求的来源。有关格式细节和其他注意事项，
	 * 请参阅 {@link CorsConfiguration#setAllowedOriginPatterns(List)}。
	 * <p>默认情况下未设置。
	 * @since 5.3.2
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * 返回 {@link #setAllowedOriginPatterns(Collection) 已配置的}允许来源模式。
	 * @since 5.3.2
	 */
	public Collection<String> getAllowedOriginPatterns() {
		List<String> allowedOriginPatterns = this.corsConfiguration.getAllowedOriginPatterns();
		return (CollectionUtils.isEmpty(allowedOriginPatterns) ? Collections.emptySet() :
				Collections.unmodifiableSet(new LinkedHashSet<>(allowedOriginPatterns)));
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		if (!WebUtils.isSameOrigin(request) &&
				this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request rejected, Origin header value " +
						request.getHeaders().getOrigin() + " not allowed");
			}
			return false;
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, @Nullable Exception exception) {
	}

}
