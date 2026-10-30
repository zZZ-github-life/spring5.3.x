/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * 当客户端 POST、PUT 或 PATCH 的内容类型不受请求处理器支持时抛出该异常。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

	@Nullable
	private final MediaType contentType;


	/**
	 * 创建一个新的 HttpMediaTypeNotSupportedException。
	 * @param message 异常消息
	 */
	public HttpMediaTypeNotSupportedException(String message) {
		super(message);
		this.contentType = null;
	}

	/**
	 * 创建一个新的 HttpMediaTypeNotSupportedException。
	 * @param contentType 不受支持的内容类型
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, List<MediaType> supportedMediaTypes) {
		this(contentType, supportedMediaTypes, "Content type '" +
				(contentType != null ? contentType : "") + "' not supported");
	}

	/**
	 * 创建一个新的 HttpMediaTypeNotSupportedException。
	 * @param contentType 不受支持的内容类型
	 * @param supportedMediaTypes 支持的媒体类型列表
	 * @param msg 详细消息
	 */
	public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType,
			List<MediaType> supportedMediaTypes, String msg) {

		super(msg, supportedMediaTypes);
		this.contentType = contentType;
	}


	/**
	 * 返回导致失败的 HTTP 请求内容类型。
	 */
	@Nullable
	public MediaType getContentType() {
		return this.contentType;
	}

}
