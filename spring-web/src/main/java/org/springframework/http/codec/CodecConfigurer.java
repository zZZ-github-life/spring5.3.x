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

package org.springframework.http.codec;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;

/**
 * 定义用于配置客户端或服务器 HTTP 消息读写器的通用接口。
 * 其使用方式如下：
 * <ul>
 * <li>使用 {@link ClientCodecConfigurer#create()} 或
 * {@link ServerCodecConfigurer#create()} 创建实例。
 * <li>使用 {@link #defaultCodecs()} 自定义默认注册的 HTTP 消息读写器。
 * <li>使用 {@link #customCodecs()} 添加自定义的 HTTP 消息读写器。
 * <li>使用 {@link #getReaders()} 和 {@link #getWriters()} 获取已配置的
 * HTTP 消息读取器和写入器列表。
 * </ul>
 *
 * <p>HTTP 消息读写器分为 3 类，顺序如下：
 * <ol>
 * <li>支持特定类型的类型化读写器，例如 byte[]、String。
 * <li>对象读写器，例如 JSON、XML。
 * <li>兜底读取器或写入器，例如支持任意媒体类型的 String。
 * </ol>
 *
 * <p>类型化读取器和对象读取器进一步细分并排序如下：
 * <ol>
 * <li>默认的 HTTP 读写器注册。
 * <li>自定义读取器和写入器。
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface CodecConfigurer {

	/**
	 * 提供一种自定义或替换默认注册的 HTTP 消息读写器的方式。
	 * @see #registerDefaults(boolean)
	 */
	DefaultCodecs defaultCodecs();

	/**
	 * 在默认注册的读写器之外，注册自定义的 HTTP 消息读写器。
	 */
	CustomCodecs customCodecs();

	/**
	 * 提供一种完全关闭默认 HTTP 消息读写器注册、仅依赖通过
	 * {@link #customCodecs()} 提供的读写器的方式。
	 * <p>默认值为 {@code "true"}，此时会进行默认注册；
	 * 将其设置为 {@code false} 则禁用默认注册。
	 */
	void registerDefaults(boolean registerDefaults);


	/**
	 * 获取已配置的 HTTP 消息读取器。
	 */
	List<HttpMessageReader<?>> getReaders();

	/**
	 * 获取已配置的 HTTP 消息写入器。
	 */
	List<HttpMessageWriter<?>> getWriters();

	/**
	 * 创建此 {@link CodecConfigurer} 的副本。返回的克隆对象拥有自己的
	 * 默认和自定义编解码器列表，通常可以独立配置。但请注意，
	 * 编解码器实例本身（如果配置了的话）不会被克隆。
	 * @since 5.1.12
	 */
	CodecConfigurer clone();


	/**
	 * 自定义或替换默认注册的 HTTP 消息读写器。这些选项进一步由
	 * {@link ClientCodecConfigurer.ClientDefaultCodecs ClientDefaultCodecs} 和
	 * {@link ServerCodecConfigurer.ServerDefaultCodecs ServerDefaultCodecs} 扩展。
	 */
	interface DefaultCodecs {

		/**
		 * 覆盖默认的 Jackson JSON {@code Decoder}。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)}，
		 * 它将被应用于给定的解码器。
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.json.Jackson2JsonDecoder
		 */
		void jackson2JsonDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Jackson JSON {@code Encoder}。
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.json.Jackson2JsonEncoder
		 */
		void jackson2JsonEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Jackson Smile {@code Decoder}。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)}，
		 * 它将被应用于给定的解码器。
		 * @param decoder 要使用的解码器实例
		 * @see org.springframework.http.codec.json.Jackson2SmileDecoder
		 */
		void jackson2SmileDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Jackson Smile {@code Encoder}。
		 * @param encoder 要使用的编码器实例
		 * @see org.springframework.http.codec.json.Jackson2SmileEncoder
		 */
		void jackson2SmileEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Protobuf {@code Decoder}。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)}，
		 * 它将被应用于给定的解码器。
		 * @param decoder 要使用的解码器实例
		 * @since 5.1
		 * @see org.springframework.http.codec.protobuf.ProtobufDecoder
		 */
		void protobufDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Protobuf {@code Encoder}。
		 * @param encoder 要使用的编码器实例
		 * @since 5.1
		 * @see org.springframework.http.codec.protobuf.ProtobufEncoder
		 * @see org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter
		 */
		void protobufEncoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 JAXB2 {@code Decoder}。
		 * <p>请注意，如果配置了 {@link #maxInMemorySize(int)}，
		 * 它将被应用于给定的解码器。
		 * @param decoder 要使用的解码器实例
		 * @since 5.1.3
		 * @see org.springframework.http.codec.xml.Jaxb2XmlDecoder
		 */
		void jaxb2Decoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 JABX2 {@code Encoder}。
		 * @param encoder 要使用的编码器实例
		 * @since 5.1.3
		 * @see org.springframework.http.codec.xml.Jaxb2XmlEncoder
		 */
		void jaxb2Encoder(Encoder<?> encoder);

		/**
		 * 覆盖默认的 Kotlin Serialization JSON {@code Decoder}。
		 * @param decoder 要使用的解码器实例
		 * @since 5.3
		 * @see org.springframework.http.codec.json.KotlinSerializationJsonDecoder
		 */
		void kotlinSerializationJsonDecoder(Decoder<?> decoder);

		/**
		 * 覆盖默认的 Kotlin Serialization JSON {@code Encoder}。
		 * @param encoder 要使用的编码器实例
		 * @since 5.3
		 * @see org.springframework.http.codec.json.KotlinSerializationJsonEncoder
		 */
		void kotlinSerializationJsonEncoder(Encoder<?> encoder);

		/**
		 * 注册一个应用于默认配置实例的消费者。这可用于配置而非替换
		 * 特定的编解码器或多个编解码器。该消费者会应用于每个默认的
		 * {@link Encoder}、{@link Decoder}、{@link HttpMessageReader} 和
		 * {@link HttpMessageWriter} 实例。
		 * @param codecConsumer 要应用的 consumer
		 * @since 5.3.4
		 */
		void configureDefaultCodec(Consumer<Object> codecConsumer);

		/**
		 * 配置当输入流需要聚合时可缓冲的字节数限制。这可能源于
		 * 解码为单个 {@code DataBuffer}、
		 * {@link java.nio.ByteBuffer ByteBuffer}、{@code byte[]}、
		 * {@link org.springframework.core.io.Resource Resource}、{@code String} 等。
		 * 在拆分输入流时（例如分隔文本）也可能发生，此时该限制
		 * 适用于分隔符之间缓冲的数据。
		 * <p>默认情况下不设置此项，此时将采用各编解码器的默认值。
		 * 默认情况下所有编解码器都限制为 256K。
		 * @param byteCount 要缓冲的最大字节数，-1 表示不限
		 * @since 5.1.11
		 */
		void maxInMemorySize(int byteCount);

		/**
		 * 是否以 DEBUG 级别记录表单数据，以 TRACE 级别记录头。
		 * 两者都可能包含敏感信息。
		 * <p>默认设置为 {@code false}，因此不会显示请求详情。
		 * @param enable 是否启用
		 * @since 5.1
		 */
		void enableLoggingRequestDetails(boolean enable);
	}


	/**
	 * 自定义 HTTP 消息读写器的注册表。
	 */
	interface CustomCodecs {

		/**
		 * 注册自定义编解码器。应为以下之一：
		 * <ul>
		 * <li>{@link HttpMessageReader}
		 * <li>{@link HttpMessageWriter}
		 * <li>{@link Encoder}（内部使用 {@link EncoderHttpMessageWriter} 包装）
		 * <li>{@link Decoder}（内部使用 {@link DecoderHttpMessageReader} 包装）
		 * </ul>
		 * @param codec 要注册的编解码器
		 * @since 5.1.13
		 */
		void register(Object codec);

		/**
		 * {@link #register(Object)} 的变体，还会通过 {@link #defaultCodecs()}
		 * 应用以下属性（如果已配置）：
		 * <ul>
		 * <li>{@link CodecConfigurer.DefaultCodecs#maxInMemorySize(int) maxInMemorySize}
		 * <li>{@link CodecConfigurer.DefaultCodecs#enableLoggingRequestDetails(boolean) enableLoggingRequestDetails}
		 * </ul>
		 * <p>每当使用 {@link #getReaders()} 或
		 * {@link #getWriters()} 获取已配置的读取器或写入器列表时，
		 * 都会应用这些属性。
		 * @param codec 要注册并应用默认配置的编解码器
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec);

		/**
		 * {@link #register(Object)} 的变体，还允许调用方将
		 * {@link DefaultCodecConfig} 中的属性应用于给定的编解码器。
		 * 如果希望应用所有属性，最好使用
		 * {@link #registerWithDefaultConfig(Object)}。
		 * <p>每当使用 {@link #getReaders()} 或
		 * {@link #getWriters()} 获取已配置的读取器或写入器列表时，
		 * 都会调用该消费者。
		 * @param codec 要注册的编解码器
		 * @param configConsumer 默认配置的 consumer
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer);

		/**
		 * 添加一个自定义 {@code Decoder}，内部使用
		 * {@link DecoderHttpMessageReader} 包装。
		 * @param decoder 要添加的解码器
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void decoder(Decoder<?> decoder);

		/**
		 * 添加一个自定义 {@code Encoder}，内部使用
		 * {@link EncoderHttpMessageWriter} 包装。
		 * @param encoder 要添加的编码器
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void encoder(Encoder<?> encoder);

		/**
		 * 添加一个自定义 {@link HttpMessageReader}。对于类型为
		 * {@link DecoderHttpMessageReader} 的读取器，可考虑改用快捷方法
		 * {@link #decoder(Decoder)}。
		 * @param reader 要添加的读取器
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void reader(HttpMessageReader<?> reader);

		/**
		 * 添加一个自定义 {@link HttpMessageWriter}。对于类型为
		 * {@link EncoderHttpMessageWriter} 的写入器，可考虑改用快捷方法
		 * {@link #encoder(Encoder)}。
		 * @param writer 要添加的写入器
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void writer(HttpMessageWriter<?> writer);

		/**
		 * 为应用于默认编解码器的 {@link DefaultCodecConfig 配置}
		 * 注册回调。这使自定义编解码器能够遵循应用于默认编解码器的
		 * 通用准则，例如记录详情和限制缓冲数据量。
		 * @param codecsConfigConsumer 默认编解码器配置回调
		 * @deprecated as of 5.1.13, use {@link #registerWithDefaultConfig(Object)}
		 * or {@link #registerWithDefaultConfig(Object, Consumer)} instead.
		 */
		@Deprecated
		void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer);
	}


	/**
	 * 公开通过 {@link #defaultCodecs()} 配置的、应用于默认编解码器的
	 * 属性值。此接口的主要目的是提供对这些值的访问，
	 * 以便在需要时也能将其应用于自定义编解码器。
	 * @since 5.1.12
	 * @see CustomCodecs#registerWithDefaultConfig(Object, Consumer)
	 */
	interface DefaultCodecConfig {

		/**
		 * 获取已配置的、当输入流需要聚合时可缓冲的字节数限制。
		 */
		@Nullable
		Integer maxInMemorySize();

		/**
		 * 是否以 DEBUG 级别记录表单数据，以 TRACE 级别记录头。
		 * 两者都可能包含敏感信息。
		 */
		@Nullable
		Boolean isEnableLoggingRequestDetails();
	}

}
