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

import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * 框架或应用程序可用于将一组自定义配置预打包到 {@link WebTestClient.MockServerSpec}
 * 并作为快捷方式暴露的约定。
 *
 * <p>此接口的实现可以通过
 * {@link WebTestClient.MockServerSpec#apply} 插入，实例通常通过静态方法获取，例如：
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.securitySetup;
 *
 * // ...
 *
 * WebTestClient.bindToController(new TestController())
 * .apply(securitySetup("foo","bar"))
 * .build();
 * </pre>
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebTestClientConfigurer
 */
public interface MockServerConfigurer {

	/**
	 * 立即调用，即在此方法返回之前。
	 * @param serverSpec 添加该配置器的 serverSpec
	 */
	default void afterConfigureAdded(WebTestClient.MockServerSpec<?> serverSpec) {
	}

	/**
	 * 在构建模拟服务器之前调用。使用此钩子检查
	 * 和/或修改应用程序声明的过滤器和异常处理器。
	 * @param builder 用于处理请求（即模拟服务器）的 {@code HttpHandler} 的构建器
	 */
	default void beforeServerCreated(WebHttpHandlerBuilder builder) {
	}

}
