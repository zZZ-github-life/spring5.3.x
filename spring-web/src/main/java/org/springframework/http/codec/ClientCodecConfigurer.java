/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

/**
 * {@link CodecConfigurer} 的扩展，提供客户端相关的
 * HTTP 消息读写器选项。
 *
 * <p>默认注册以下 HTTP 消息读取器：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>用于表单数据的 {@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,String&gt;}
 * <li>JSON 和 Smile（如果存在 Jackson）
 * <li>XML（如果存在 JAXB2）
 * <li>Server-Sent Events
 * </ul>
 *
 * <p>默认注册的 HTTP 消息写入器：
 * <ul>{@code byte[]}
 * <li>{@link java.nio.ByteBuffer}
 * <li>{@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
 * <li>{@link org.springframework.core.io.Resource Resource}
 * <li>{@link String}
 * <li>用于表单数据的 {@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,String&gt;}
 * <li>用于 multipart 数据的 {@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,Object&gt;}
 * <li>JSON 和 Smile（如果存在 Jackson）
 * <li>XML（如果存在 JAXB2）
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ClientCodecConfigurer extends CodecConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>在客户端，内置默认值还包括与 multipart 读写器相关的定制，
	 * 以及用于 SSE 的解码器。
	 */
	@Override
	ClientDefaultCodecs defaultCodecs();

	/**
	 * {@inheritDoc}。
	 */
	@Override
	ClientCodecConfigurer clone();


	/**
	 * {@code ClientCodecConfigurer} 的静态工厂方法。
	 */
	static ClientCodecConfigurer create() {
		return CodecConfigurerFactory.create(ClientCodecConfigurer.class);
	}


	/**
	 * {@link CodecConfigurer.DefaultCodecs} 的扩展，提供额外的客户端选项。
	 */
	interface ClientDefaultCodecs extends DefaultCodecs {

		/**
		 * 配置供
		 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageWriter
		 * MultipartHttpMessageWriter} 使用的编码器或写入器。
		 */
		MultipartCodecs multipartCodecs();

		/**
		 * 配置用于 Server-Sent Events 的 {@code Decoder}。
		 * <p>默认情况下，如果未设置此项且 Jackson 可用，
		 * 则会使用 {@link #jackson2JsonDecoder} 覆盖项。
		 * 使用此方法可以自定义 SSE 解码器。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)}，
		 * 它将被应用于给定的解码器。
		 * @param decoder the decoder to use
		 */
		void serverSentEventDecoder(Decoder<?> decoder);
	}


	/**
	 * multipart HTTP 消息写入器的注册表和容器。
	 */
	interface MultipartCodecs {

		/**
		 * 添加一个 Part {@code Encoder}，内部使用
		 * {@link EncoderHttpMessageWriter} 包装。
		 * @param encoder the encoder to add
		 */
		MultipartCodecs encoder(Encoder<?> encoder);

		/**
		 * 添加一个 Part {@link HttpMessageWriter}。对于类型为
		 * {@link EncoderHttpMessageWriter} 的写入器，可考虑改用快捷方法
		 * {@link #encoder(Encoder)}。
		 * @param writer the writer to add
		 */
		MultipartCodecs writer(HttpMessageWriter<?> writer);
	}

}
