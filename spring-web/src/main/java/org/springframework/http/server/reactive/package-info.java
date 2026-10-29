/**
 * 响应式 HTTP 服务器支持的抽象，包括
 * {@link org.springframework.http.server.reactive.ServerHttpRequest} 和
 * {@link org.springframework.http.server.reactive.ServerHttpResponse}，以及用于处理的
 * {@link org.springframework.http.server.reactive.HttpHandler}。
 *
 * <p>还提供了适配不同运行时的实现，
 * 包括 Servlet 3.1 容器、Netty + Reactor IO 和 Undertow。
 */
@NonNullApi
@NonNullFields
package org.springframework.http.server.reactive;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
