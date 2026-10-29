/**
 * {@code spring-webflux} 模块的顶层包，包含 {@link org.springframework.web.reactive.DispatcherHandler}，它是 WebFlux 服务端端点处理的主要入口，包括用于将请求映射到处理器、调用处理器以及处理结果的关键契约。
 *
 * <p>该模块为响应式服务端端点提供两种编程模型：一种基于注解式 {@code @Controller}，另一种基于函数式路由和处理。该模块还包含函数式、响应式的 {@code WebClient}，以及客户端和服务端的响应式 WebSocket 支持。
 */
@NonNullApi
@NonNullFields
package org.springframework.web.reactive;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
