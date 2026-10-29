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

package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * 用于注册基于 WebSocket 的 STOMP 端点的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface StompEndpointRegistry {

	/**
	 * 在给定的映射路径上注册一个基于 WebSocket 的 STOMP 端点。
	 */
	StompWebSocketEndpointRegistration addEndpoint(String... paths);

	/**
	 * 设置用于 STOMP 端点的 {@link org.springframework.web.servlet.HandlerMapping}
	 * 相对于其他 Spring MVC 处理器映射的顺序。
	 * <p>默认情况下设置为 1。
	 */
	void setOrder(int order);

	/**
	 * 为 STOMP 端点的
	 * {@link org.springframework.web.servlet.HandlerMapping HandlerMapping}
	 * 配置自定义的 {@link UrlPathHelper}。
	 */
	void setUrlPathHelper(UrlPathHelper urlPathHelper);

	/**
	 * 配置一个处理器，用于定制或处理发送给客户端的 STOMP ERROR 帧。
	 * @param errorHandler the error handler
	 * @since 4.2
	 */
	WebMvcStompEndpointRegistry setErrorHandler(StompSubProtocolErrorHandler errorHandler);

}
