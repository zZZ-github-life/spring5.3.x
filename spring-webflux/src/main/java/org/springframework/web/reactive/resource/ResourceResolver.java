/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 将请求解析为服务器端资源的策略。
 *
 * <p>提供将传入请求解析为实际 {@link Resource} 的机制，以及获取客户端在请求资源时应使用的公共 URL 路径的机制。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceResolver {

	/**
	 * 将提供的请求和请求路径解析为存在于给定资源位置之一下的 {@link Resource}。
	 * @param exchange the current exchange
	 * @param requestPath the portion of the request path to use. This is
	 * expected to be the encoded path, i.e. {@link RequestPath#value()}.
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of remaining resolvers to delegate to
	 * @return the resolved resource or an empty {@code Mono} if unresolved
	 */
	Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * 为客户端解析对外公开的<em>公共</em> URL 路径，以访问位于给定<em>内部</em>资源路径的资源。
	 * <p>这在向客户端渲染 URL 链接时很有用。
	 * @param resourcePath the "internal" resource path to resolve a path for
	 * public use. This is expected to be the encoded path.
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of resolvers to delegate to
	 * @return the resolved public URL path or an empty {@code Mono} if unresolved
	 */
	Mono<String> resolveUrlPath(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain);

}
