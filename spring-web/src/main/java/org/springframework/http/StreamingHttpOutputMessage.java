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
 * 表示允许设置流式正文的 HTTP 输出消息。请注意，此类消息通常不支持 {@link #getBody()} 访问。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see #setBody
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

	/**
	 * 为此消息设置流式正文回调。
	 * @param body 流式正文回调
	 */
	void setBody(Body body);


	/**
	 * 定义可直接写入 {@link OutputStream} 的正文的约定。适用于通过回调机制间接提供 {@link OutputStream} 访问的 HTTP 客户端库。
	 */
	@FunctionalInterface
	interface Body {

		/**
		 * 将此正文写入给定的 {@link OutputStream}。
		 * @param outputStream 要写入的输出流
		 * @throws IOException 如果发生 I/O 错误
		 */
		void writeTo(OutputStream outputStream) throws IOException;
	}

}
