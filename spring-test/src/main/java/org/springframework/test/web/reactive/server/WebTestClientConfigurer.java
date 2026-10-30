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
	 * @param builder 要对其进行更改的 WebTestClient 构建器
	 * @param httpHandlerBuilder 用于“mock server” HttpHandler 的构建器，
	 * 前提是该客户端配置用于“mock server”测试
	 * @param connector 用于“live”集成测试的连接器，前提是该服务器
	 * 配置为 live 集成测试
	 */
	void afterConfigurerAdded(WebTestClient.Builder builder,
			@Nullable WebHttpHandlerBuilder httpHandlerBuilder,
			@Nullable ClientHttpConnector connector);

}
