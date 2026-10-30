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

package org.springframework.web.reactive.resource;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于调用 {@link ResourceResolver ResourceResolvers} 链的契约，每个解析器都会获得对链的引用，以便在需要时进行委托。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceResolverChain {

	/**
	 * 将提供的请求和请求路径解析为存在于给定资源位置之一下的 {@link Resource}。
	 * @param exchange 当前交换
	 * @param requestPath 要使用的请求路径部分
	 * @param locations 查找资源时要搜索的位置
	 * @return 解析出的资源；如果无法解析则为空的 {@code Mono}
	 */
	Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
			List<? extends Resource> locations);

	/**
	 * 为客户端解析对外公开的<em>公共</em> URL 路径，以访问位于给定<em>内部</em>资源路径的资源。
	 * <p>这在向客户端渲染 URL 链接时很有用。
	 * @param resourcePath 内部资源路径
	 * @param locations 查找资源时要搜索的位置
	 * @return 解析出的公开 URL 路径；如果无法解析则为空的 {@code Mono}
	 */
	Mono<String> resolveUrlPath(String resourcePath, List<? extends Resource> locations);

}
