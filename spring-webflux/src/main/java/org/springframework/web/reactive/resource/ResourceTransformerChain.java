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

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于调用 {@link ResourceTransformer ResourceTransformers} 链的契约，每个解析器都会获得对链的引用，以便在需要时进行委托。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceTransformerChain {

	/**
	 * 返回用于解析正在转换的 {@code Resource} 的 {@code ResourceResolverChain}。这可能用于解析相关资源，例如指向其他资源的链接。
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * 转换给定资源。
	 * @param exchange the current exchange
	 * @param resource the candidate resource to transform
	 * @return the transformed or the same resource, never empty
	 */
	Mono<Resource> transform(ServerWebExchange exchange, Resource resource);

}
