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

package org.springframework.http.server;

import java.net.URI;

import org.springframework.lang.Nullable;

/**
 * {@link PathContainer} 的特化，将路径细分为
 * {@link #contextPath()} 和其余的 {@link #pathWithinApplication()}。
 * 后者通常用于应用程序内的请求映射，
 * 而前者在准备指向该应用程序的外部链接时很有用。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestPath extends PathContainer {

	/**
	 * 返回 URL 路径中表示应用程序的部分。
	 * 上下文路径始终位于路径开头，以 "/" 开始但不以 "/" 结尾。
	 * 同一应用程序的 URL 共享该上下文路径。
	 * <p>上下文路径可能来自底层运行时 API（例如作为 WAR 部署到
	 * Servlet 容器时），也可能在 WebFlux 应用程序中通过
	 * {@link org.springframework.http.server.reactive.ContextPathCompositeHandler
	 * ContextPathCompositeHandler} 指定。
	 */
	PathContainer contextPath();

	/**
	 * 请求路径中上下文路径之后的部分，通常用于应用程序内的
	 * 请求映射。
	 */
	PathContainer pathWithinApplication();

	/**
	 * 返回修改了上下文路径的新 {@code RequestPath} 实例。
	 * 新的上下文路径必须匹配开头处 0 个或多个路径段。
	 * @param contextPath the new context path
	 * @return a new {@code RequestPath} instance
	 */
	RequestPath modifyContextPath(String contextPath);


	/**
	 * 将请求的 URI 解析为 {@code RequestPath}。
	 * @param uri the URI of the request
	 * @param contextPath the contextPath portion of the URI path
	 */
	static RequestPath parse(URI uri, @Nullable String contextPath) {
		return parse(uri.getRawPath(), contextPath);
	}

	/**
	 * {@link #parse(URI, String)} 的变体，使用编码后的
	 * {@link URI#getRawPath() 原始路径}。
	 * @param rawPath the path
	 * @param contextPath the contextPath portion of the URI path
	 * @since 5.3
	 */
	static RequestPath parse(String rawPath, @Nullable String contextPath) {
		return new DefaultRequestPath(rawPath, contextPath);
	}

}
