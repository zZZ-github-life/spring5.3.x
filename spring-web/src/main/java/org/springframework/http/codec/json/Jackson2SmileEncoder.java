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

package org.springframework.http.codec.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 使用 Jackson 2.9 将 {@code Object} 流编码为 Smile 对象的字节流。
 * 对于非流式使用场景，出于性能考虑，{@link Flux} 元素会在序列化前
 * 收集到 {@link List} 中。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see Jackson2SmileDecoder
 */
public class Jackson2SmileEncoder extends AbstractJackson2Encoder {

	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
			new MimeType("application", "x-jackson-smile"),
			new MimeType("application", "*+x-jackson-smile")};

	private static final byte[] STREAM_SEPARATOR = new byte[0];


	public Jackson2SmileEncoder() {
		this(Jackson2ObjectMapperBuilder.smile().build(), DEFAULT_SMILE_MIME_TYPES);
	}

	public Jackson2SmileEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
		setStreamingMediaTypes(Collections.singletonList(new MediaType("application", "stream+x-jackson-smile")));
	}


	/**
	 * 返回用于给定 MIME 类型的分隔符。
	 * <p>默认情况下，如果给定的 MIME 类型是所配置的
	 * {@link #setStreamingMediaTypes(List) 流式} MIME 类型之一，
	 * 此方法返回单字节 0。
	 * @since 5.3
	 */
	@Nullable
	@Override
	protected byte[] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
		for (MediaType streamingMediaType : getStreamingMediaTypes()) {
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				return STREAM_SEPARATOR;
			}
		}
		return null;
	}
}
