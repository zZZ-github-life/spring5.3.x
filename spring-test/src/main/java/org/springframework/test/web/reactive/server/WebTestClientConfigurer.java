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

package org.springframework.test.web.reactive.server;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * 框架或应用程序可用于将一组自定义配置预打包到 {@link WebTestClient.Builder}
 * 并作为快捷方式暴露的约定。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see MockServerConfigurer
 */
public interface WebTestClientConfigurer {

	/**
	 * 仅调用一次，且立即调用（即在此方法返回之前）。
	 * @param builder the WebTestClient builder to make changes to
	 * @param httpHandlerBuilder the builder for the "mock server" HttpHandler
	 * this client was configured for "mock server" testing
	 * @param connector the connector for "live" integration tests if this
	 * server was configured for live integration testing
	 */
	void afterConfigurerAdded(WebTestClient.Builder builder,
			@Nullable WebHttpHandlerBuilder httpHandlerBuilder,
			@Nullable ClientHttpConnector connector);

}
