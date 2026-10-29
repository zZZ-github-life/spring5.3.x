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

package org.springframework.web.servlet.resource;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code ResourceTransformer} 的基类，带有一个可选的辅助方法，
 * 用于解析被转换资源中的公共链接。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 配置一个 {@link ResourceUrlProvider}，用于解析被转换资源中链接的
	 * 公共 URL（例如 CSS 文件中的 import 链接）。
	 * 仅对以完整路径表示的链接必需，对相对链接不需要。
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * 返回已配置的 {@code ResourceUrlProvider}。
	 */
	@Nullable
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * 当被转换的资源包含指向其他资源的链接时，转换器可以使用此方法。
	 * 此类链接需要替换为由资源解析器链确定的
	 * 面向公众的链接（例如公共 URL 中可能插入了版本）。
	 * @param resourcePath the path to a resource that needs to be re-written
	 * @param request the current request
	 * @param resource the resource being transformed
	 * @param transformerChain the transformer chain
	 * @return the resolved URL, or {@code} if not resolvable
	 */
	@Nullable
	protected String resolveUrlPath(String resourcePath, HttpServletRequest request,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// full resource path
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			return (urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null);
		}
		else {
			// try resolving as relative path
			return transformerChain.getResolverChain().resolveUrlPath(
					resourcePath, Collections.singletonList(resource));
		}
	}

	/**
	 * 将给定相对请求路径转换为绝对路径，
	 * 并以给定请求的路径作为参考点。
	 * 结果路径还会清除 "path/.." 之类的序列。
	 * @param path the relative path to transform
	 * @param request the referer request
	 * @return the absolute request path for the given resource path
	 */
	protected String toAbsolutePath(String path, HttpServletRequest request) {
		String absolutePath = path;
		if (!path.startsWith("/")) {
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			Assert.state(urlProvider != null, "No ResourceUrlProvider");
			String requestPath = urlProvider.getUrlPathHelper().getRequestUri(request);
			absolutePath = StringUtils.applyRelativePath(requestPath, path);
		}
		return StringUtils.cleanPath(absolutePath);
	}

	@Nullable
	private ResourceUrlProvider findResourceUrlProvider(HttpServletRequest request) {
		if (this.resourceUrlProvider != null) {
			return this.resourceUrlProvider;
		}
		return (ResourceUrlProvider) request.getAttribute(
				ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
	}

}
