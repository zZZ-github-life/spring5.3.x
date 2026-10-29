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

package org.springframework.web.socket.config.annotation;

import java.util.List;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 用于配置基于 WebSocket 的 STOMP 端点的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface StompWebSocketEndpointRegistration {

	/**
	 * 启用 SockJS 回退选项。
	 */
	SockJsServiceRegistration withSockJS();

	/**
	 * 配置要使用的 HandshakeHandler。
	 */
	StompWebSocketEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

	/**
	 * 配置要使用的 HandshakeInterceptor。
	 */
	StompWebSocketEndpointRegistration addInterceptors(HandshakeInterceptor... interceptors);

	/**
	 * 设置允许浏览器发起跨域请求的源。
	 * 请参阅 {@link CorsConfiguration#setAllowedOrigins(List)} 了解
	 * 格式细节和注意事项，并记住 CORS 规范不允许在
	 * {@code allowCredentials=true} 时使用 {@code "*"}。
	 * 如需更灵活的源模式，请改用 {@link #setAllowedOriginPatterns}。
	 *
	 * <p>默认情况下，不允许任何源。当同时设置了
	 * {@link #setAllowedOriginPatterns(String...) allowedOriginPatterns} 时，
	 * 该设置优先于此属性。
	 *
	 * <p>请注意，当启用 SockJS 并限制源时，不允许检查请求源的传输类型
	 * （基于 Iframe 的传输）将被禁用。因此，限制源时不支持 IE 6 到 9。
	 * @since 4.1.2
	 * @see #setAllowedOriginPatterns(String...)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	StompWebSocketEndpointRegistration setAllowedOrigins(String... origins);

	/**
	 * {@link #setAllowedOrigins(String...)} 的替代方法，支持更灵活的模式来
	 * 指定允许浏览器发起跨域请求的源。请参阅
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)}
	 * 了解格式细节和其他注意事项。
	 * <p>默认情况下不设置此项。
	 * @since 5.3.2
	 */
	StompWebSocketEndpointRegistration setAllowedOriginPatterns(String... originPatterns);

}
