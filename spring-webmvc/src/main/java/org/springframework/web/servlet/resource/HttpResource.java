/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

/**
 * 扩展接口，表示要写入
 * HTTP 响应的 {@link Resource}。
 * @author Brian Clozel
 * @since 5.0
 */
public interface HttpResource extends Resource {

	/**
	 * 要添加到为当前资源提供服务的 HTTP 响应中的
	 * HTTP 头。
	 * @return HTTP 响应头
	 */
	HttpHeaders getResponseHeaders();

}
