/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.accept;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 一种 {@code ContentNegotiationStrategy}，将请求路径中的文件扩展名解析为
 * 用于查找媒体类型的键。
 *
 * <p>如果在提供给构造函数的显式注册中找不到该文件扩展名，则使用
 * {@link MediaTypeFactory} 作为回退机制。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @deprecated as of 5.2.4. See class-level note in
 * {@link ContentNegotiationManagerFactoryBean} on the deprecation of path
 * extension config options.
 */
@Deprecated
public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 创建一个不带任何初始映射的实例。之后如果通过 Java Activation 框架
	 * 解析出任何扩展名，则可以添加映射。
	 */
	public PathExtensionContentNegotiationStrategy() {
		this(null);
	}

	/**
	 * 使用给定的文件扩展名与媒体类型映射创建实例。
	 */
	public PathExtensionContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
		setUseRegisteredExtensionsOnly(false);
		setIgnoreUnknownExtensions(true);
		this.urlPathHelper.setUrlDecode(false);
	}


	/**
	 * 配置一个 {@code UrlPathHelper}，用于在 {@link #getMediaTypeKey} 中
	 * 推导目标请求 URL 路径的查找路径。
	 * @since 4.2.8
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 指示是否使用 Java Activation 框架作为从文件扩展名映射到媒体类型的回退选项。
	 * @deprecated as of 5.0, in favor of {@link #setUseRegisteredExtensionsOnly(boolean)}.
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			return null;
		}
		// Ignore LOOKUP_PATH attribute, use our own "fixed" UrlPathHelper with decoding off
		String path = this.urlPathHelper.getLookupPathForRequest(request);
		String extension = UriUtils.extractFileExtension(path);
		return (StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null);
	}

	/**
	 * 一个公开方法，将路径扩展名策略的知识暴露出来，以便针对给定的
	 * {@link Resource} 将文件扩展名解析为 {@link MediaType}。此方法首先查找
	 * 任何显式注册的文件扩展名，然后如果可用则回退到 {@link MediaTypeFactory}。
	 * @param resource 要查找的资源
	 * @return 该扩展名对应的 MediaType；如果未找到则为 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public MediaType getMediaTypeForResource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		MediaType mediaType = null;
		String filename = resource.getFilename();
		String extension = StringUtils.getFilenameExtension(filename);
		if (extension != null) {
			mediaType = lookupMediaType(extension);
		}
		if (mediaType == null) {
			mediaType = MediaTypeFactory.getMediaType(filename).orElse(null);
		}
		return mediaType;
	}

}
