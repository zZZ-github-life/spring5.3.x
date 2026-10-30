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

import org.springframework.core.codec.Encoder;

/**
 * {@link CodecConfigurer} 的扩展，提供服务器端相关的
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
 * <li>用于 multipart 数据的 {@link org.springframework.util.MultiValueMap
 * MultiValueMap&lt;String,Object&gt;}
 * <li>JSON 和 Smile（如果存在 Jackson）
 * <li>XML（如果存在 JAXB2）
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
 * <li>JSON 和 Smile（如果存在 Jackson）
 * <li>XML（如果存在 JAXB2）
 * <li>Server-Sent Events
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerCodecConfigurer extends CodecConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>在服务器端，内置默认值还包括与 SSE 编码器相关的定制。
	 */
	@Override
	ServerDefaultCodecs defaultCodecs();

	/**
	 * {@inheritDoc}。
	 */
	@Override
	ServerCodecConfigurer clone();


	/**
	 * {@code ServerCodecConfigurer} 的静态工厂方法。
	 */
	static ServerCodecConfigurer create() {
		return CodecConfigurerFactory.create(ServerCodecConfigurer.class);
	}


	/**
	 * {@link CodecConfigurer.DefaultCodecs} 的扩展，提供额外的服务器端选项。
	 */
	interface ServerDefaultCodecs extends DefaultCodecs {

		/**
		 * 配置用于 multipart 请求的 {@code HttpMessageReader}。
		 * <p>默认情况下，如果存在
		 * <a href="https://github.com/synchronoss/nio-multipart">Synchronoss NIO Multipart</a>，
		 * 则将其设置为使用
		 * {@link org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader
		 * SynchronossPartHttpMessageReader} 实例创建的
		 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageReader
		 * MultipartHttpMessageReader}。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)} 和/或
		 * {@link #enableLoggingRequestDetails(boolean)}，
		 * 它们将应用于给定的读取器（如适用）。
		 * @param reader 用于 multipart 请求的消息读取器。
		 * @since 5.1.11
		 */
		void multipartReader(HttpMessageReader<?> reader);

		/**
		 * 配置用于 Server-Sent Events 的 {@code Encoder}。
		 * <p>默认情况下，如果未设置此项且 Jackson 可用，
		 * 则会使用 {@link #jackson2JsonEncoder} 覆盖项。
		 * 使用此方法可以自定义 SSE 编码器。
		 */
		void serverSentEventEncoder(Encoder<?> encoder);
	}

}
