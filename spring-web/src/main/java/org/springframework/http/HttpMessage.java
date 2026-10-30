/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http;

/**
 * 表示 HTTP 请求和响应消息的基础接口。由 {@link HttpHeaders} 组成，可通过 {@link #getHeaders()} 获取。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpMessage {

	/**
	 * 返回此消息的头。
	 * @return 对应的 HttpHeaders 对象（永不为 {@code null}）
	 */
	HttpHeaders getHeaders();

}
