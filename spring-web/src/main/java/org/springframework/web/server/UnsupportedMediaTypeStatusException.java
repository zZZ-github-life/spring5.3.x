/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.server;

import java.util.Collections;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 适用于响应状态 415（不支持的媒体类型）的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeStatusException extends ResponseStatusException {

	@Nullable
	private final MediaType contentType;

	private final List<MediaType> supportedMediaTypes;

	@Nullable
	private final ResolvableType bodyType;

	@Nullable
	private final HttpMethod method;


	/**
	 * 当指定的 Content-Type 无效时使用的构造函数。
	 */
	public UnsupportedMediaTypeStatusException(@Nullable String reason) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
		this.contentType = null;
		this.supportedMediaTypes = Collections.emptyList();
		this.bodyType = null;
		this.method = null;
	}

	/**
	 * 当 Content-Type 可以解析但不受支持时使用的构造函数。
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
		this(contentType, supportedTypes, null, null);
	}

	/**
	 * 在尝试从特定 Java 类型编码或解码到特定 Java 类型时使用的构造函数。
	 * @since 5.1
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable ResolvableType bodyType) {
		this(contentType, supportedTypes, bodyType, null);
	}

	/**
	 * 提供 HTTP 方法的构造函数。
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable HttpMethod method) {
		this(contentType, supportedTypes, null, method);
	}

	/**
	 * 在尝试从特定 Java 类型编码或解码到特定 Java 类型时使用的构造函数。
	 * @since 5.3.6
	 */
	public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
			@Nullable ResolvableType bodyType, @Nullable HttpMethod method) {

		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, initReason(contentType, bodyType));
		this.contentType = contentType;
		this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
		this.bodyType = bodyType;
		this.method = method;
	}

	private static String initReason(@Nullable MediaType contentType, @Nullable ResolvableType bodyType) {
		return "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
				(bodyType != null ? " for bodyType=" + bodyType.toString() : "");
	}


	/**
	 * 如果请求的 Content-Type 头部解析成功，则返回该头部，
	 * 否则返回 {@code null}。
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

	/**
	 * 返回受支持的内容类型列表：当 Content-Type 头部可以解析但不受支持时返回该列表，
	 * 否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * 返回生成此异常时所处上下文中的主体类型。
	 * <p>当异常是由于尝试从特定 Java 类型编码或解码到特定 Java 类型而引发时，
	 * 该信息适用。
	 * @return 正文类型；如果不可用则返回 {@code null}
	 * @since 5.1
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

	@Override
	public HttpHeaders getResponseHeaders() {
		if (HttpMethod.PATCH != this.method || CollectionUtils.isEmpty(this.supportedMediaTypes) ) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAcceptPatch(this.supportedMediaTypes);
		return headers;
	}

}
