/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.springframework.http.MediaType;

/**
 * 与媒体类型相关异常的抽象基类。添加了受支持的 {@link MediaType MediaTypes} 列表。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class HttpMediaTypeException extends ServletException {

	private final List<MediaType> supportedMediaTypes;


	/**
	 * 创建一个新的 HttpMediaTypeException。
	 * @param message the exception message
	 */
	protected HttpMediaTypeException(String message) {
		super(message);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * 使用受支持媒体类型列表创建一个新的 HttpMediaTypeException。
	 * @param supportedMediaTypes the list of supported media types
	 */
	protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
		super(message);
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 返回受支持的媒体类型列表。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
