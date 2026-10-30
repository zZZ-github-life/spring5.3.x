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

package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code ContentNegotiationStrategy} 实现的基类，包含将请求解析为媒体类型的步骤。
 *
 * <p>首先必须从请求中提取一个键（例如 "json"、"pdf"）（例如文件扩展名、查询参数）。
 * 然后必须通过基类 {@link MappingMediaTypeFileExtensionResolver} 将该键解析为
 * 媒体类型，该基类存储此类映射。
 *
 * <p>{@link #handleNoMatch} 方法允许子类插入额外的媒体类型查找方式
 * （例如通过 Java Activation 框架或
 * {@link jakarta.servlet.ServletContext#getMimeType}）。通过基类解析的媒体类型
 * 随后会被添加到基类 {@link MappingMediaTypeFileExtensionResolver} 中，
 * 即为新的查找进行缓存。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class AbstractMappingContentNegotiationStrategy extends MappingMediaTypeFileExtensionResolver
		implements ContentNegotiationStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean useRegisteredExtensionsOnly = false;

	private boolean ignoreUnknownExtensions = false;


	/**
	 * 使用给定的文件扩展名与媒体类型映射创建实例。
	 */
	public AbstractMappingContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * 是仅使用已注册的映射来查找文件扩展名，还是也使用动态解析
	 * （例如通过 {@link MediaTypeFactory}）。
	 * <p>默认情况下设置为 {@code false}。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	public boolean isUseRegisteredExtensionsOnly() {
		return this.useRegisteredExtensionsOnly;
	}

	/**
	 * 是否忽略带有未知文件扩展名的请求。将其设置为
	 * {@code false} 将导致 {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认情况下设置为 {@literal false}，但在
	 * {@link PathExtensionContentNegotiationStrategy} 中被重写为 {@literal true}。
	 */
	public void setIgnoreUnknownExtensions(boolean ignoreUnknownExtensions) {
		this.ignoreUnknownExtensions = ignoreUnknownExtensions;
	}

	public boolean isIgnoreUnknownExtensions() {
		return this.ignoreUnknownExtensions;
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException {

		return resolveMediaTypeKey(webRequest, getMediaTypeKey(webRequest));
	}

	/**
	 * {@link #resolveMediaTypes(NativeWebRequest)} 的替代方法，接受
	 * 已提取的键。
	 * @since 3.2.16
	 */
	public List<MediaType> resolveMediaTypeKey(NativeWebRequest webRequest, @Nullable String key)
			throws HttpMediaTypeNotAcceptableException {

		if (StringUtils.hasText(key)) {
			MediaType mediaType = lookupMediaType(key);
			if (mediaType != null) {
				handleMatch(key, mediaType);
				return Collections.singletonList(mediaType);
			}
			mediaType = handleNoMatch(webRequest, key);
			if (mediaType != null) {
				addMapping(key, mediaType);
				return Collections.singletonList(mediaType);
			}
		}
		return MEDIA_TYPE_ALL_LIST;
	}


	/**
	 * 从请求中提取一个键，用于查找媒体类型。
	 * @return 查找键，如果没有则为 {@code null}
	 */
	@Nullable
	protected abstract String getMediaTypeKey(NativeWebRequest request);

	/**
	 * 重写此方法以在通过 {@link #lookupMediaType} 成功解析键时提供处理逻辑。
	 */
	protected void handleMatch(String key, MediaType mediaType) {
	}

	/**
	 * 重写此方法以在通过 {@link #lookupMediaType} 无法解析键时提供处理逻辑。
	 * 子类可以采取进一步步骤来确定媒体类型。如果此方法返回 MediaType，
	 * 它将被添加到基类中的缓存。
	 */
	@Nullable
	protected MediaType handleNoMatch(NativeWebRequest request, String key)
			throws HttpMediaTypeNotAcceptableException {

		if (!isUseRegisteredExtensionsOnly()) {
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType("file." + key);
			if (mediaType.isPresent()) {
				return mediaType.get();
			}
		}
		if (isIgnoreUnknownExtensions()) {
			return null;
		}
		throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
	}

}
