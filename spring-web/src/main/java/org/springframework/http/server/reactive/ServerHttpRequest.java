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

package org.springframework.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 表示响应式服务器端 HTTP 请求。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public interface ServerHttpRequest extends HttpRequest, ReactiveHttpInputMessage {

	/**
	 * 返回表示底层连接的 id（如果可用），
	 * 或返回请求本身的 id，用于关联日志消息。
	 * @since 5.1
	 * @see org.springframework.web.server.ServerWebExchange#getLogPrefix()
	 */
	String getId();

	/**
	 * 返回完整请求路径的结构化表示，该路径截至但不包括
	 * {@link #getQueryParams() 查询}部分。
	 * <p>返回的路径细分为
	 * {@link RequestPath#contextPath()} 部分和其余的
	 * {@link RequestPath#pathWithinApplication() pathWithinApplication} 部分。
	 * 后者可以传入
	 * {@link org.springframework.web.util.pattern.PathPattern} 的方法
	 * 以进行路径匹配。
	 */
	RequestPath getPath();

	/**
	 * 返回包含已解析并解码的查询参数值的只读 Map。
	 */
	MultiValueMap<String, String> getQueryParams();

	/**
	 * 返回客户端发送的 cookie 的只读 Map。
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * 返回接收请求的本地地址（如果可用）。
	 * @since 5.2.3
	 */
	@Nullable
	default InetSocketAddress getLocalAddress() {
		return null;
	}

	/**
	 * 返回此请求所连接的远程地址（如果可用）。
	 */
	@Nullable
	default InetSocketAddress getRemoteAddress() {
		return null;
	}

	/**
	 * 如果请求是通过安全协议传输的，返回 SSL 会话信息，
	 * 包括 SSL 证书（如果可用）。
	 * @return 会话信息，如果没有可用会话则为 {@code null}
	 * @since 5.0.2
	 */
	@Nullable
	default SslInfo getSslInfo() {
		return null;
	}

	/**
	 * 返回一个构建器，通过将请求包装在 {@link ServerHttpRequestDecorator} 中
	 * 来修改此请求的属性，并返回修改后的值或委托回此实例。
	 */
	default ServerHttpRequest.Builder mutate() {
		return new DefaultServerHttpRequestBuilder(this);
	}


	/**
	 * 用于修改现有 {@link ServerHttpRequest} 的构建器。
	 */
	interface Builder {

		/**
		 * 设置要返回的 HTTP 方法。
		 */
		Builder method(HttpMethod httpMethod);

		/**
		 * 设置要使用的 URI，需满足以下条件：
		 * <ul>
		 * <li>如果还设置了 {@link #path(String) path}，它将覆盖此处
		 * 提供的 URI 的路径。
		 * <li>如果还设置了 {@link #contextPath(String) contextPath}，
		 * 或已存在该值，则它必须与此处提供的 URI 路径的开头匹配。
		 * </ul>
		 */
		Builder uri(URI uri);

		/**
		 * 设置要使用的路径，以替代请求 URI 的 {@code "rawPath"}，
		 * 需满足以下条件：
		 * <ul>
		 * <li>如果还设置了 {@link #uri(URI) uri}，此处给出的路径
		 * 将覆盖给定 URI 的路径。
		 * <li>如果还设置了 {@link #contextPath(String) contextPath}，
		 * 或已存在该值，则它必须与此处给出的路径的开头匹配。
		 * <li>给定的值必须以斜杠开头。
		 * </ul>
		 */
		Builder path(String path);

		/**
		 * 设置要使用的 contextPath。
		 * <p>给定的值必须是有效的 {@link RequestPath#contextPath()
		 * contextPath}，并且必须与请求 URI 路径的开头匹配。
		 * 这意味着更改 contextPath 也意味着需要通过
		 * {@link #path(String)} 更改路径。
		 */
		Builder contextPath(String contextPath);

		/**
		 * 设置或覆盖给定名称下指定的头值。
		 * <p>如果需要添加头值、删除头等，请使用
		 * {@link #headers(Consumer)} 以获得更强的控制能力。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @since 5.1.9
		 * @see #headers(Consumer)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 操作请求头。提供的 {@code HttpHeaders} 包含当前请求头，
		 * 因此 {@code Consumer} 可以{@linkplain HttpHeaders#set(String, String) 覆盖}
		 * 或{@linkplain HttpHeaders#remove(Object) 删除}现有值，
		 * 或使用任何其他 {@link HttpHeaders} 方法。
		 * @see #header(String, String...)
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置 SSL 会话信息。这在路由器处完成 TLS 终止，
		 * 但 SSL 信息通过其他方式（例如通过某个头）提供时可能很有用。
		 * @since 5.0.7
		 */
		Builder sslInfo(SslInfo sslInfo);

		/**
		 * 设置远程客户端的地址。
		 * @since 5.3
		 */
		Builder remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 使用修改后的属性构建 {@link ServerHttpRequest} 装饰器。
		 */
		ServerHttpRequest build();
	}

}
