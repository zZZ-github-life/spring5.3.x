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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * 适用于响应状态 415（不支持的媒体类型）的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @deprecated in favor of {@link UnsupportedMediaTypeStatusException},
 * with this class never thrown by Spring code and to be removed in 6.0
 */
@Deprecated
@SuppressWarnings("serial")
public class MediaTypeNotSupportedStatusException extends ResponseStatusException {

	private final List<MediaType> supportedMediaTypes;


	/**
	 * 当 Content-Type 无效时使用的构造函数。
	 */
	public MediaTypeNotSupportedStatusException(String reason) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * 当 Content-Type 不受支持时使用的构造函数。
	 */
	public MediaTypeNotSupportedStatusException(List<MediaType> supportedMediaTypes) {
		super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", null);
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 返回受支持的内容类型列表：当 Accept 头部可以解析但不受支持时返回该列表，
	 * 否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
