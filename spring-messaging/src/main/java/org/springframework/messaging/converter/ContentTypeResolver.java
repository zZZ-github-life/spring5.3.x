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

package org.springframework.messaging.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

/**
 * 解析消息的内容类型。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface ContentTypeResolver {

	/**
	 * 根据给定的 MessageHeaders 确定消息的 {@link MimeType}。
	 * @param headers 用于解析的消息头
	 * @return 解析得到的 {@code MimeType}；如果未找到则返回 {@code null}
	 * @throws InvalidMimeTypeException 如果内容类型是无法解析的 String
	 * @throws IllegalArgumentException 如果存在内容类型但其类型未知
	 */
	@Nullable
	MimeType resolve(@Nullable MessageHeaders headers) throws InvalidMimeTypeException;

}
