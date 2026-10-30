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

package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 表示 HTTP 输出消息，由 {@linkplain #getHeaders() headers} 和可写的 {@linkplain #getBody() body} 组成。
 * <p>通常由客户端的 HTTP 请求处理器或服务器端的 HTTP 响应处理器实现。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpOutputMessage extends HttpMessage {

	/**
	 * 以输出流的形式返回消息正文。
	 * @return 输出流正文（永不为 {@code null}）
	 * @throws IOException 如果发生 I/O 错误
	 */
	OutputStream getBody() throws IOException;

}
