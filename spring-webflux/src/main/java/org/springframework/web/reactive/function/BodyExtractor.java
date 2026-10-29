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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * 可从 {@link ReactiveHttpInputMessage} 正文中提取数据的函数。
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of data to extract
 * @param <M> the type of {@link ReactiveHttpInputMessage} this extractor can be applied to
 * @see BodyExtractors
 */
@FunctionalInterface
public interface BodyExtractor<T, M extends ReactiveHttpInputMessage> {

	/**
	 * 从给定的输入消息中提取。
	 * @param inputMessage the request to extract from
	 * @param context the configuration to use
	 * @return the extracted data
	 */
	T extract(M inputMessage, Context context);


	/**
	 * 定义提取过程中使用的上下文。
	 */
	interface Context {

		/**
		 * 返回用于正文提取的 {@link HttpMessageReader HttpMessageReaders}。
		 * @return the stream of message readers
		 */
		List<HttpMessageReader<?>> messageReaders();

		/**
		 * 可选地返回 {@link ServerHttpResponse}（如果存在）。
		 */
		Optional<ServerHttpResponse> serverResponse();

		/**
		 * 返回用于自定义正文提取的提示映射。
		 */
		Map<String, Object> hints();
	}

}
