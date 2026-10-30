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

package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;

/**
 * 表示客户端 HTTP 响应。
 *
 * <p>通过调用 {@link ClientHttpRequest#execute()} 获得。
 *
 * <p>{@code ClientHttpResponse} 必须被 {@linkplain #close() 关闭}，
 * 通常在 {@code finally} 块中进行。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

	/**
	 * 以 {@link HttpStatus} 枚举值形式获取 HTTP 状态码。
	 * <p>对于 {@code HttpStatus} 不支持的状态码，请改用
	 * {@link #getRawStatusCode()}。
	 * @return 以 HttpStatus 枚举值表示的 HTTP 状态（永不为 {@code null}）
	 * @throws IOException 如果发生 I/O 错误
	 * @throws IllegalArgumentException 如果 HTTP 状态码未知
	 * @since #getRawStatusCode()
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus getStatusCode() throws IOException;

	/**
	 * 以整数形式获取 HTTP 状态码（可能非标准且
	 * 无法通过 {@link HttpStatus} 枚举解析）。
	 * @return 以整数值表示的 HTTP 状态
	 * @throws IOException 如果发生 I/O 错误
	 * @since 3.1.1
	 * @see #getStatusCode()
	 * @see HttpStatus#resolve(int)
	 */
	int getRawStatusCode() throws IOException;

	/**
	 * 获取响应的 HTTP 状态文本。
	 * @return HTTP 状态文本
	 * @throws IOException 如果发生 I/O 错误
	 */
	String getStatusText() throws IOException;

	/**
	 * 关闭此响应，释放所创建的所有资源。
	 */
	@Override
	void close();

}
