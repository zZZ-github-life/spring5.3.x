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

package org.springframework.web.reactive.function.client;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * 提供在 {@link ExchangeFunction} 中使用的策略。
 *
 * <p>要创建实例，请参见静态方法 {@link #withDefaults()}、{@link #builder()} 和 {@link #empty()}。
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ExchangeStrategies {

	/**
	 * 返回用于读取和解码响应正文的 {@link HttpMessageReader HttpMessageReaders}。
	 * @return 消息读取器
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * 返回用于写入和编码请求正文的 {@link HttpMessageWriter HttpMessageWriters}。
	 * @return 消息写入器
	 */
	List<HttpMessageWriter<?>> messageWriters();

	/**
	 * 返回用于创建从当前实例复制的新的 {@link ExchangeStrategies} 实例的构建器。
	 * @since 5.1.12
	 */
	default Builder mutate() {
		throw new UnsupportedOperationException();
	}


	// Static builder methods

	/**
	 * 返回使用 {@link ClientCodecConfigurer} 提供的默认配置的 {@code ExchangeStrategies} 实例。
	 */
	static ExchangeStrategies withDefaults() {
		return DefaultExchangeStrategiesBuilder.DEFAULT_EXCHANGE_STRATEGIES;
	}

	/**
	 * 返回预先配置了默认配置的构建器作为起点。这与 {@link #withDefaults()} 相同，但返回可变构建器以便进一步自定义。
	 */
	static Builder builder() {
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * 返回以空配置开始的构建器。
	 */
	static Builder empty() {
		return new DefaultExchangeStrategiesBuilder();
	}


	/**
	 * 用于 {@link ExchangeStrategies} 的可变构建器。
	 */
	interface Builder {

		/**
		 * 自定义客户端 HTTP 消息读写器列表。
		 * @param consumer 用于自定义编解码器的消费者
		 * @return 此构建器
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> consumer);

		/**
		 * 构建 {@link ExchangeStrategies}。
		 * @return 构建的策略
		 */
		ExchangeStrategies build();
	}

}
