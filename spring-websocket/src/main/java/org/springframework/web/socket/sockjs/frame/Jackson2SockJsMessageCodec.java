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

package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于编码和解码 SockJS 消息的 Jackson 2.6+ 编解码器。
 *
 * <p>它使用以下属性自定义 Jackson 的默认属性：
 * <ul>
 * <li>禁用 {@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>请注意，Jackson 的 JSR-310 和 Joda-Time 支持模块将在可用时自动注册
 * （分别在 Java 8 和 Joda-Time 本身可用时）。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class Jackson2SockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final ObjectMapper objectMapper;


	public Jackson2SockJsMessageCodec() {
		this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
	}

	public Jackson2SockJsMessageCodec(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}


	@Override
	@Nullable
	public String[] decode(String content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	@Nullable
	public String[] decodeInputStream(InputStream content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsString(content);
	}

}
