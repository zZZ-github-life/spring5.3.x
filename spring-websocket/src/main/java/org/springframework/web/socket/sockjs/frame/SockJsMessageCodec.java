/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;

/**
 * 将消息编码为 SockJS 消息帧以及从 SockJS 消息帧解码，SockJS 消息帧本质上是一个
 * JSON 编码消息的数组。例如：
 *
 * <pre class="code">
 * a["message1","message2"]
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsMessageCodec {

	/**
	 * 将给定消息编码为 SockJS 消息帧。除了对每条消息应用标准
	 * JSON 引用外，还有一些额外的 JSON Unicode 转义
	 * 规则。请参阅 SockJS 协议的 "JSON Unicode Encoding" 部分（即
	 * 协议测试套件）。
	 * @param messages 要编码的消息
	 * @return SockJS 消息帧的内容（永不为 {@code null}）
	 */
	String encode(String... messages);

	/**
	 * 解码给定的 SockJS 消息帧。
	 * @param content SockJS 消息帧
	 * @return 消息数组；如果没有则为 {@code null}
	 * @throws IOException 如果无法解析该内容
	 */
	@Nullable
	String[] decode(String content) throws IOException;

	/**
	 * 解码给定的 SockJS 消息帧。
	 * @param content SockJS 消息帧
	 * @return 消息数组；如果没有则为 {@code null}
	 * @throws IOException 如果无法解析该内容
	 */
	@Nullable
	String[] decodeInputStream(InputStream content) throws IOException;

}
