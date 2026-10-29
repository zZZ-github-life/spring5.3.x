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

package org.springframework.http;

import org.springframework.util.InvalidMimeTypeException;

/**
 * 当遇到无效的媒体类型规范字符串时，由 {@link MediaType#parseMediaType(String)} 抛出。
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 */
@SuppressWarnings("serial")
public class InvalidMediaTypeException extends IllegalArgumentException {

	private final String mediaType;


	/**
	 * 为给定的媒体类型创建一个新的 InvalidMediaTypeException。
	 * @param mediaType the offending media type
	 * @param message a detail message indicating the invalid part
	 */
	public InvalidMediaTypeException(String mediaType, String message) {
		super("Invalid media type \"" + mediaType + "\": " + message);
		this.mediaType = mediaType;
	}

	/**
	 * 允许包装 {@link InvalidMimeTypeException} 的构造函数。
	 */
	InvalidMediaTypeException(InvalidMimeTypeException ex) {
		super(ex.getMessage(), ex);
		this.mediaType = ex.getMimeType();
	}


	/**
	 * 返回有问题的媒体类型。
	 */
	public String getMediaType() {
		return this.mediaType;
	}

}
