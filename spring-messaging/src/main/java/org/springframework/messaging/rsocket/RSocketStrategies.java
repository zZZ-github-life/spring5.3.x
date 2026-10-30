/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.List;
import java.util.function.Consumer;

import io.rsocket.Payload;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeType;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;

/**
 * 提供供 RSocket 请求者和响应者组件使用的策略的访问入口。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface RSocketStrategies {

	/**
	 * 返回配置的 {@link Builder#encoder(Encoder[]) encoders}。
	 * @see #encoder(ResolvableType, MimeType)
	 */
	List<Encoder<?>> encoders();

	/**
	 * 查找与给定元素类型兼容的 Encoder。
	 * @param elementType 要匹配的元素类型
	 * @param mimeType 要匹配的 MimeType
	 * @param <T> 用于将 Encoder 转换为预期的元素类型
	 * @return 匹配的 Encoder
	 * @throws IllegalArgumentException 如果找不到匹配的 Encoder
	 */
	@SuppressWarnings("unchecked")
	default <T> Encoder<T> encoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Encoder<?> encoder : encoders()) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<T>) encoder;
			}
		}
		throw new IllegalArgumentException("No encoder for " + elementType);
	}

	/**
	 * 返回配置的 {@link Builder#decoder(Decoder[]) decoders}。
	 * @see #decoder(ResolvableType, MimeType)
	 */
	List<Decoder<?>> decoders();

	/**
	 * 查找与给定元素类型兼容的 Decoder。
	 * @param elementType 要匹配的元素类型
	 * @param mimeType 要匹配的 MimeType
	 * @param <T> 用于将 Decoder 转换为预期的元素类型
	 * @return 匹配的 Decoder
	 * @throws IllegalArgumentException 如果找不到匹配的 Decoder
	 */
	@SuppressWarnings("unchecked")
	default <T> Decoder<T> decoder(ResolvableType elementType, @Nullable MimeType mimeType) {
		for (Decoder<?> decoder : decoders()) {
			if (decoder.canDecode(elementType, mimeType)) {
				return (Decoder<T>) decoder;
			}
		}
		throw new IllegalArgumentException("No decoder for " + elementType);
	}

	/**
	 * 返回配置的 {@link Builder#routeMatcher(RouteMatcher)}。
	 */
	RouteMatcher routeMatcher();

	/**
	 * 返回配置的
	 * {@link Builder#reactiveAdapterStrategy(ReactiveAdapterRegistry) reactiveAdapterRegistry}。
	 */
	ReactiveAdapterRegistry reactiveAdapterRegistry();

	/**
	 * 返回配置的
	 * {@link Builder#dataBufferFactory(DataBufferFactory) dataBufferFactory}。
	 */
	DataBufferFactory dataBufferFactory();

	/**
	 * 返回配置的 {@link Builder#metadataExtractor(MetadataExtractor)}。
	 */
	MetadataExtractor metadataExtractor();

	/**
	 * 返回一个构建器，用于基于当前实例复制创建新的 {@link RSocketStrategies} 实例。
	 */
	default Builder mutate() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder(this);
	}


	/**
	 * 使用默认设置创建 {@code RSocketStrategies} 实例。
	 * 等价于 {@code RSocketStrategies.builder().build()}。有关默认设置的详细信息，
	 * 请参阅各个构建器方法。
	 */
	static RSocketStrategies create() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder().build();
	}

	/**
	 * 返回用于准备新的 {@code RSocketStrategies} 实例的构建器。
	 * 该构建器会应用默认设置，详情请参阅各个构建器方法。
	 */
	static Builder builder() {
		return new DefaultRSocketStrategies.DefaultRSocketStrategiesBuilder();
	}


	/**
	 * 用于创建 {@code RSocketStrategies} 的构建器选项。
	 */
	interface Builder {

		/**
		 * 追加用于将对象序列化为 {@link Payload} 的数据或元数据的编码器列表。
		 * <p>默认情况下，它使用针对 {@code String}、{@code byte[]}、
		 * {@code ByteBuffer} 和 {@code DataBuffer} 的编码器进行初始化。
		 */
		Builder encoder(Encoder<?>... encoder);

		/**
		 * 立即将 consumer 应用于已配置的编码器列表。
		 */
		Builder encoders(Consumer<List<Encoder<?>>> consumer);

		/**
		 * 追加用于从 {@link Payload} 的数据或元数据反序列化对象的解码器列表。
		 * <p>默认情况下，它使用针对 {@code String}、{@code byte[]}、
		 * {@code ByteBuffer} 和 {@code DataBuffer} 的解码器进行初始化。
		 */
		Builder decoder(Decoder<?>... decoder);

		/**
		 * 立即将 consumer 应用于已配置的解码器列表。
		 */
		Builder decoders(Consumer<List<Decoder<?>>> consumer);

		/**
		 * 配置 {@code RouteMatcher}，用于根据路由模式将路由匹配到消息处理器。
		 * 此选项适用于客户端或服务器响应方。
		 * <p>默认使用 {@link SimpleRouteMatcher}，其底层是使用 "." 作为分隔符的
		 * {@link AntPathMatcher}。为了获得更好的效率，可以考虑改用
		 * {@code spring-web} 中的 {@code PathPatternRouteMatcher}。
		 */
		Builder routeMatcher(@Nullable RouteMatcher routeMatcher);

		/**
		 * 配置响应式类型支持的注册表。可用于适配和/或确定给定
		 * {@link org.reactivestreams.Publisher Publisher} 的语义。
		 * <p>默认使用 {@link ReactiveAdapterRegistry#getSharedInstance()}。
		 */
		Builder reactiveAdapterStrategy(@Nullable ReactiveAdapterRegistry registry);

		/**
		 * 配置用于在准备请求或创建响应时分配缓冲区的 DataBufferFactory。
		 * <p>默认设置为使用池化分配缓冲区以支持零拷贝的
		 * {@link NettyDataBufferFactory}。RSocket 也必须
		 * <a href="https://github.com/rsocket/rsocket-java#zero-copy">进行相应配置</a>
		 * 才能实现零拷贝。对于客户端设置，{@link RSocketRequester.Builder}
		 * 会自动适配此处配置的 {@code DataBufferFactory}，并相应地设置
		 * {@link io.rsocket.core.RSocketConnector RSocketConnector} 中的帧解码器。
		 * 对于服务器设置，{@link io.rsocket.core.RSocketServer RSocketServer}
		 * 也必须进行相应配置以支持零拷贝。
		 * <p>如果改用 {@link DefaultDataBufferFactory}，则无需在 RSocket 中
		 * 进行相关配置更改。
		 */
		Builder dataBufferFactory(@Nullable DataBufferFactory bufferFactory);

		/**
		 * 配置 {@link MetadataExtractor}，以提取路由以及其他元数据。
		 * 此选项适用于客户端或服务器响应方。
		 * <p>默认使用 {@link DefaultMetadataExtractor}，它使用
		 * {@link #decoder(Decoder[]) configured} 的解码器，并从
		 * {@code "message/x.rsocket.routing.v0"} 元数据中提取路由。
		 */
		Builder metadataExtractor(@Nullable MetadataExtractor metadataExtractor);

		/**
		 * 将 consumer 应用于 {@link MetadataExtractorRegistry}，
		 * 以便注册额外的元数据条目提取器。
		 */
		Builder metadataExtractorRegistry(Consumer<MetadataExtractorRegistry> consumer);

		/**
		 * 构建 {@code RSocketStrategies} 实例。
		 */
		RSocketStrategies build();
	}

}
