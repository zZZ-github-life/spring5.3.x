/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.util.Map;

import io.rsocket.Payload;

import org.springframework.core.codec.DecodingException;
import org.springframework.util.MimeType;

/**
 * 从 {@link Payload} 元数据（可能是包含多个条目的复合元数据）中提取值映射的策略。
 * 每个元数据条目根据其 {@code MimeType} 进行解码，并为解码后的值分配一个名称。
 * 生成的名称-值对可以添加到
 * {@link org.springframework.messaging.Message Message} 的头部中。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see MetadataExtractorRegistry
 */
public interface MetadataExtractor {

	/**
	 * 分配给负载中提取出的 "route" 的键。
	 */
	String ROUTE_KEY = "route";


	/**
	 * 从给定的 {@link Payload} 元数据中提取值映射。
	 * 如果存在负载 "route"，应将其保存在 {@link #ROUTE_KEY} 下。
	 * @param payload 应读取其元数据的载荷
	 * @param metadataMimeType 该连接的元数据 MimeType。
	 * @return 从元数据中提取的名称-值对
	 * @throws DecodingException 如果元数据无法解码
	 * @throws IllegalArgumentException 如果路由元数据无法解码
	 */
	Map<String, Object> extract(Payload payload, MimeType metadataMimeType);

}
