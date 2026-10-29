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

package org.springframework.web.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

/**
 * 适用于响应状态 406（不可接受）的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class NotAcceptableStatusException extends ResponseStatusException {

	private final List<MediaType> supportedMediaTypes;


	/**
	 * 当请求的 Content-Type 无效时使用的构造函数。
	 */
	public NotAcceptableStatusException(String reason) {
		super(HttpStatus.NOT_ACCEPTABLE, reason);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * 当请求的 Content-Type 不受支持时使用的构造函数。
	 */
	public NotAcceptableStatusException(List<MediaType> supportedMediaTypes) {
		super(HttpStatus.NOT_ACCEPTABLE, "Could not find acceptable representation");
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 返回包含 "Accept" 头部的 Map。
	 * @since 5.1.11
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Map<String, String> getHeaders() {
		return getResponseHeaders().toSingleValueMap();
	}

	/**
	 * 返回包含 "Accept" 头部的 HttpHeaders，或空实例。
	 * @since 5.1.13
	 */
	@Override
	public HttpHeaders getResponseHeaders() {
		if (CollectionUtils.isEmpty(this.supportedMediaTypes)) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(this.supportedMediaTypes);
		return headers;
	}

	/**
	 * 返回受支持的内容类型列表：当 Accept 头部可以解析但不受支持时返回该列表，
	 * 否则返回空列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
