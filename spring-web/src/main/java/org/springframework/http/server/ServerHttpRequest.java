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

package org.springframework.http.server;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;

/**
 * 表示服务器端 HTTP 请求。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

	/**
	 * 返回包含已认证用户名的
	 * {@link java.security.Principal} 实例。
	 * <p>如果用户尚未通过认证，该方法返回 <code>null</code>。
	 */
	@Nullable
	Principal getPrincipal();

	/**
	 * 返回接收请求的地址。
	 */
	InetSocketAddress getLocalAddress();

	/**
	 * 返回远程客户端的地址。
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * 返回一个控制对象，可将请求置于异步模式，
	 * 使响应保持打开状态，直到从当前线程或其他线程显式关闭。
	 */
	ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response);

}
