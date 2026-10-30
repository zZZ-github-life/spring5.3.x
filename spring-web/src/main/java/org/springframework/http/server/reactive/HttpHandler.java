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

package org.springframework.http.server.reactive;

import reactor.core.publisher.Mono;

/**
 * 响应式 HTTP 请求处理的最低层契约，充当不同运行时之间的
 * 公共基础。
 *
 * <p>更高层但仍通用的应用程序构建块（如
 * {@code WebFilter}、{@code WebSession}、{@code ServerWebExchange} 等）
 * 可在 {@code org.springframework.web.server} 包中使用。
 *
 * <p>应用级编程模型（如注解控制器和函数式处理器）可在
 * {@code spring-webflux} 模块中使用。
 *
 * <p>通常一个 {@link HttpHandler} 代表整个应用程序，其高层编程模型
 * 通过 {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder} 桥接。
 * 借助 {@link ContextPathCompositeHandler}，可以在唯一的上下文路径下
 * 接入多个应用程序。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ContextPathCompositeHandler
 */
public interface HttpHandler {

	/**
	 * 处理给定的请求并写入响应。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 表示请求处理已完成
	 */
	Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response);

}
