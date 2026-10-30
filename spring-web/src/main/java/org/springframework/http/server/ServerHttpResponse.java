/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;

/**
 * 表示服务器端 HTTP 响应。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

	/**
	 * 设置响应的 HTTP 状态码。
	 * @param status 作为 HttpStatus 枚举值的 HTTP 状态
	 */
	void setStatusCode(HttpStatus status);

	/**
	 * 确保响应的头和内容都被写出。
	 * <p>首次刷新之后，头将无法再更改。
	 * 只能继续写入内容和刷新内容。
	 */
	@Override
	void flush() throws IOException;

	/**
	 * 关闭此响应，释放所创建的资源。
	 */
	@Override
	void close();

}
