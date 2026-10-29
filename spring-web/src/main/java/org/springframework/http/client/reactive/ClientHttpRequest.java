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

package org.springframework.http.client.reactive;

import java.net.URI;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;

/**
 * 表示客户端响应式 HTTP 请求。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public interface ClientHttpRequest extends ReactiveHttpOutputMessage {

	/**
	 * 返回请求的 HTTP 方法。
	 */
	HttpMethod getMethod();

	/**
	 * 返回请求的 URI。
	 */
	URI getURI();

	/**
	 * 返回要发送到服务器的请求 Cookie 的可变映射。
	 */
	MultiValueMap<String, HttpCookie> getCookies();

	/**
	 * 返回来自底层 HTTP 库的请求。
	 * @param <T> the expected type of the request to cast to
	 * @since 5.3
	 */
	<T> T getNativeRequest();

}
