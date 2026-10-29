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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

/**
 * 要写入 HTTP 响应的 {@link Resource} 扩展接口。
 * @author Brian Clozel
 * @since 5.0
 */
public interface HttpResource extends Resource {

	/**
	 * 要贡献给提供当前资源的 HTTP 响应的 HTTP 头。
	 * @return the HTTP response headers
	 */
	HttpHeaders getResponseHeaders();
}
