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

package org.springframework.web.servlet.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.webjars.WebJarAssetLocator;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * {@code ResourceResolver}，它委托给链以定位资源，然后
 * 尝试查找 WebJar JAR 文件中包含的匹配版本化资源。
 *
 * <p>这使 WebJars.org 用户可以在模板中编写与版本无关的路径，
 * 例如 {@code <script src="/webjars/jquery/jquery.min.js"/>}。
 * 该路径将被解析为唯一版本 {@code <script src="/webjars/jquery/1.2.0/jquery.min.js"/>}，
 * 这更适合应用程序中的 HTTP 缓存和版本管理。
 *
 * <p>这也会为与版本无关的 HTTP 请求 {@code "GET /jquery/jquery.min.js"}
 * 解析资源。
 *
 * <p>此解析器需要在类路径上提供 {@code org.webjars:webjars-locator-core} 库，
 * 如果该库存在，则会自动注册。
 *
 * <p>请注意，{@code WebJarAssetLocator} 构造函数会执行类路径扫描，
 * 这可能会减慢应用程序的启动速度。
 * @author Brian Clozel
 * @since 4.2
 * @see org.springframework.web.servlet.config.annotation.ResourceChainRegistration
 * @see <a href="https://www.webjars.org">webjars.org</a>
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	private static final String WEBJARS_LOCATION = "META-INF/resources/webjars/";

	private static final int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();


	private final WebJarAssetLocator webJarAssetLocator;


	/**
	 * 使用默认 {@code WebJarAssetLocator} 实例创建一个 {@code WebJarsResourceResolver}。
	 */
	public WebJarsResourceResolver() {
		this(new WebJarAssetLocator());
	}

	/**
	 * 使用自定义 {@code WebJarAssetLocator} 实例创建一个 {@code WebJarsResourceResolver}，
	 * 例如使用自定义索引。
	 * @since 4.3
	 */
	public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
		this.webJarAssetLocator = webJarAssetLocator;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved == null) {
			String webJarResourcePath = findWebJarResourcePath(requestPath);
			if (webJarResourcePath != null) {
				return chain.resolveResource(request, webJarResourcePath, locations);
			}
		}
		return resolved;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		if (path == null) {
			String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
			if (webJarResourcePath != null) {
				return chain.resolveUrlPath(webJarResourcePath, locations);
			}
		}
		return path;
	}

	@Nullable
	protected String findWebJarResourcePath(String path) {
		int startOffset = (path.startsWith("/") ? 1 : 0);
		int endOffset = path.indexOf('/', 1);
		if (endOffset != -1) {
			String webjar = path.substring(startOffset, endOffset);
			String partialPath = path.substring(endOffset + 1);
			String webJarPath = this.webJarAssetLocator.getFullPathExact(webjar, partialPath);
			if (webJarPath != null) {
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		return null;
	}

}
