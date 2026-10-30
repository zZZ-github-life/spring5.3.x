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

import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 扩展 {@code PathExtensionContentNegotiationStrategy}，同时还使用
 * {@link ServletContext#getMimeType(String)} 来解析文件扩展名。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @deprecated as of 5.2.4. See class-level note in
 * {@link ContentNegotiationManagerFactoryBean} on the deprecation of path
 * extension config options.
 */
@Deprecated
public class ServletPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy {

	private final ServletContext servletContext;


	/**
	 * 创建一个不带任何初始映射的实例。之后当通过
	 * {@link ServletContext#getMimeType(String)} 或
	 * {@link org.springframework.http.MediaTypeFactory} 解析扩展名时，
	 * 可以添加映射。
	 */
	public ServletPathExtensionContentNegotiationStrategy(ServletContext context) {
		this(context, null);
	}

	/**
	 * 使用给定的扩展名到 MediaType 查找映射创建实例。
	 */
	public ServletPathExtensionContentNegotiationStrategy(
			ServletContext servletContext, @Nullable Map<String, MediaType> mediaTypes) {

		super(mediaTypes);
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 通过 {@link ServletContext#getMimeType(String)} 解析文件扩展名，
	 * 并委托给基类进行可能的
	 * {@link org.springframework.http.MediaTypeFactory} 查找。
	 */
	@Override
	@Nullable
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension)
			throws HttpMediaTypeNotAcceptableException {

		MediaType mediaType = null;
		String mimeType = this.servletContext.getMimeType("file." + extension);
		if (StringUtils.hasText(mimeType)) {
			mediaType = MediaType.parseMediaType(mimeType);
		}
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			MediaType superMediaType = super.handleNoMatch(webRequest, extension);
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		return mediaType;
	}

	/**
	 * 扩展基类
	 * {@link PathExtensionContentNegotiationStrategy#getMediaTypeForResource}，
	 * 使其还能通过 ServletContext 进行查找。
	 * @param resource 要查找的资源
	 * @return 该扩展名对应的 MediaType；如果未找到则为 {@code null}
	 * @since 4.3
	 */
	@Override
	public MediaType getMediaTypeForResource(Resource resource) {
		MediaType mediaType = null;
		String mimeType = this.servletContext.getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			mediaType = MediaType.parseMediaType(mimeType);
		}
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			MediaType superMediaType = super.getMediaTypeForResource(resource);
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		return mediaType;
	}

}
