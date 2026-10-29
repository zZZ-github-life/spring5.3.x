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

package org.springframework.mock.web.server;

import reactor.core.publisher.Mono;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * {@link DefaultServerWebExchange} 的扩展，与
 * {@link MockServerHttpRequest} 和 {@link MockServerHttpResponse} 一起用于测试。
 *
 * <p>创建实例请参见静态工厂方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerWebExchange extends DefaultServerWebExchange {

	private MockServerWebExchange(MockServerHttpRequest request, WebSessionManager sessionManager) {
		super(request, new MockServerHttpResponse(), sessionManager,
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}


	@Override
	public MockServerHttpResponse getResponse() {
		return (MockServerHttpResponse) super.getResponse();
	}


	/**
	 * 根据给定的 mock 请求创建一个 {@link MockServerWebExchange}。
	 * @param request the request to use.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest request) {
		return builder(request).build();
	}

	/**
	 * {@link #from(MockServerHttpRequest)} 的变体，使用 mock 请求构建器。
	 * @param requestBuilder the builder for the mock request.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
		return builder(requestBuilder).build();
	}

	/**
	 * 从给定的 mock 请求开始创建一个 {@link Builder}。
	 * @param request the request to use.
	 * @return the exchange builder
	 * @since 5.1
	 */
	public static MockServerWebExchange.Builder builder(MockServerHttpRequest request) {
		return new MockServerWebExchange.Builder(request);
	}

	/**
	 * {@link #builder(MockServerHttpRequest)} 的变体，使用 mock 请求构建器。
	 * @param requestBuilder the builder for the mock request.
	 * @return the exchange builder
	 * @since 5.1
	 */
	public static MockServerWebExchange.Builder builder(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
		return new MockServerWebExchange.Builder(requestBuilder.build());
	}


	/**
	 * {@link MockServerWebExchange} 的构建器。
	 * @since 5.1
	 */
	public static class Builder {

		private final MockServerHttpRequest request;

		@Nullable
		private WebSessionManager sessionManager;

		public Builder(MockServerHttpRequest request) {
			this.request = request;
		}

		/**
		 * 设置用于该交换的会话。
		 * <p>此方法与
		 * {@link #sessionManager(WebSessionManager)} 互斥。
		 * @param session the session to use
		 * @see MockWebSession
		 */
		public Builder session(WebSession session) {
			this.sessionManager = exchange -> Mono.just(session);
			return this;
		}

		/**
		 * 提供用于该交换的 {@code WebSessionManager} 实例。
		 * <p>此方法与 {@link #session(WebSession)} 互斥。
		 * @param sessionManager the session manager to use
		 */
		public Builder sessionManager(WebSessionManager sessionManager) {
			this.sessionManager = sessionManager;
			return this;
		}

		/**
		 * 构建 {@code MockServerWebExchange} 实例。
		 */
		public MockServerWebExchange build() {
			return new MockServerWebExchange(this.request,
					this.sessionManager != null ? this.sessionManager : new DefaultWebSessionManager());
		}
	}

}
