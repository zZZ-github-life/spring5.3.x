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

import java.util.Collections;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于 {@code ResourceTransformer} 的基类，提供用于解析被转换资源中公共链接的可选辅助方法。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 配置在解析被转换资源中链接的公共 URL（例如 CSS 文件中的 import 链接）时要使用的 {@link ResourceUrlProvider}。仅对于以完整路径表示的链接需要此配置，相对链接不需要。
	 * @param resourceUrlProvider the URL provider to use
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * 返回配置的 {@code ResourceUrlProvider}。
	 */
	@Nullable
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * 当被转换的资源包含指向其他资源的链接时，转换器可以使用此方法。此类链接需要替换为资源解析器链确定的对外公共链接（例如公共 URL 中可能插入了版本）。
	 * @param resourcePath the path to a resource that needs to be re-written
	 * @param exchange the current exchange
	 * @param resource the resource being transformed
	 * @param transformerChain the transformer chain
	 * @return the resolved URL or an empty {@link Mono}
	 */
	protected Mono<String> resolveUrlPath(String resourcePath, ServerWebExchange exchange,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// full resource path
			ResourceUrlProvider urlProvider = getResourceUrlProvider();
			return (urlProvider != null ? urlProvider.getForUriString(resourcePath, exchange) : Mono.empty());
		}
		else {
			// try resolving as relative path
			return transformerChain.getResolverChain()
					.resolveUrlPath(resourcePath, Collections.singletonList(resource));
		}
	}

	/**
	 * 将给定的相对请求路径转换为绝对路径，并以给定请求的路径作为参考点。结果路径还会清除 "path/.." 之类的序列。
	 * @param path the relative path to transform
	 * @param exchange the current exchange
	 * @return the absolute request path for the given resource path
	 */
	protected String toAbsolutePath(String path, ServerWebExchange exchange) {
		String requestPath = exchange.getRequest().getURI().getPath();
		String absolutePath = (path.startsWith("/") ? path : StringUtils.applyRelativePath(requestPath, path));
		return StringUtils.cleanPath(absolutePath);
	}

}
