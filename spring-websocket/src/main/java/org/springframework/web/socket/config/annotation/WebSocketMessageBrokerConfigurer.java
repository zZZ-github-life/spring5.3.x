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

package org.springframework.web.socket.config.annotation;

import java.util.List;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

/**
 * 定义用于配置来自 WebSocket 客户端的简单消息传递协议（例如 STOMP）
 * 消息处理的方法。
 *
 * <p>通常用于定制通过
 * {@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker}
 * 提供的配置。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketMessageBrokerConfigurer {

	/**
	 * 注册 STOMP 端点，将每个端点映射到特定的 URL，并（可选地）
	 * 启用和配置 SockJS 回退选项。
	 */
	default void registerStompEndpoints(StompEndpointRegistry registry) {
	}

	/**
	 * 配置与接收和发送 WebSocket 客户端消息相关的处理选项。
	 */
	default void configureWebSocketTransport(WebSocketTransportRegistration registry) {
	}

	/**
	 * 配置用于接收来自 WebSocket 客户端传入消息的
	 * {@link org.springframework.messaging.MessageChannel}。默认情况下，
	 * 该通道由大小为 1 的线程池支持。对于生产环境，
	 * 建议自定义线程池设置。
	 */
	default void configureClientInboundChannel(ChannelRegistration registration) {
	}

	/**
	 * 配置用于向 WebSocket 客户端发送传出消息的
	 * {@link org.springframework.messaging.MessageChannel}。默认情况下，
	 * 该通道由大小为 1 的线程池支持。对于生产环境，
	 * 建议自定义线程池设置。
	 */
	default void configureClientOutboundChannel(ChannelRegistration registration) {
	}

	/**
	 * 添加解析器以支持自定义控制器方法参数类型。
	 * <p>这不会覆盖解析处理器方法参数的内置支持。要定制参数解析的内置支持，
	 * 请直接配置 {@code SimpAnnotationMethodMessageHandler}。
	 * @param argumentResolvers the resolvers to register (initially an empty list)
	 * @since 4.1.1
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * 添加处理器以支持自定义控制器方法返回值类型。
	 * <p>使用此选项不会覆盖处理返回值的内置支持。要定制处理返回值的内置支持，
	 * 请直接配置 {@code SimpAnnotationMethodMessageHandler}。
	 * @param returnValueHandlers the handlers to register (initially an empty list)
	 * @since 4.1.1
	 */
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * 配置在注解方法中提取消息负载以及发送消息（例如通过 "broker"
	 * SimpMessagingTemplate）时要使用的消息转换器。
	 * <p>提供的列表初始为空，可用于添加消息转换器，而布尔返回值
	 * 用于确定是否还应添加默认消息转换器。
	 * @param messageConverters the converters to configure (initially an empty list)
	 * @return whether to also add default converter or not
	 */
	default boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return true;
	}

	/**
	 * 配置消息代理选项。
	 */
	default void configureMessageBroker(MessageBrokerRegistry registry) {
	}

}
