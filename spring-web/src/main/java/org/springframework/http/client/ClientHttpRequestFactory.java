/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * 用于创建 {@link ClientHttpRequest} 对象的工厂。
 * 请求通过 {@link #createRequest(URI, HttpMethod)} 方法创建。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@FunctionalInterface
public interface ClientHttpRequestFactory {

	/**
	 * 为指定的 URI 和 HTTP 方法创建新的 {@link ClientHttpRequest}。
	 * <p>返回的请求可以写入，然后通过调用
	 * {@link ClientHttpRequest#execute()} 执行。
	 * @param uri the URI to create a request for
	 * @param httpMethod the HTTP method to execute
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
