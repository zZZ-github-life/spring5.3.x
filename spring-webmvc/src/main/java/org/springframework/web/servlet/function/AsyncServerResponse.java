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

package org.springframework.web.servlet.function;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapterRegistry;

/**
 * {@link ServerResponse} 的异步子类型，暴露未来的响应。
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 * @see ServerResponse#async(Object)
 */
public interface AsyncServerResponse extends ServerResponse {

	/**
	 * 无限期阻塞，直到获得未来的响应。
	 */
	ServerResponse block();


	// Static creation methods

	/**
	 * 使用给定的异步响应创建 {@code AsyncServerResponse}。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或任何可通过
	 * {@link ReactiveAdapterRegistry} 适配的单个 {@code ServerResponse} 异步生产者）。
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @return the asynchronous response
	 */
	static AsyncServerResponse create(Object asyncResponse) {
		return DefaultAsyncServerResponse.create(asyncResponse, null);
	}

	/**
	 * 使用给定的异步响应创建（已构建的）响应。
	 * 参数 {@code asyncResponse} 可以是
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} 或
	 * {@link Publisher Publisher&lt;ServerResponse&gt;}（或任何可通过
	 * {@link ReactiveAdapterRegistry} 适配的单个 {@code ServerResponse} 异步生产者）。
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @param timeout maximum time period to wait for before timing out
	 * @return the asynchronous response
	 */
	static AsyncServerResponse create(Object asyncResponse, Duration timeout) {
		return DefaultAsyncServerResponse.create(asyncResponse, timeout);
	}

}

