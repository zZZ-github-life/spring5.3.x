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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * 一个可以执行 SockJS "Info" 请求的组件，该请求需要在
 * SockJS 会话启动之前执行，以检查服务器端点的能力，
 * 例如端点是否允许使用 WebSocket。
 *
 * <p>通常 {@link XhrTransport} 实现也是此约定的实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see AbstractXhrTransport
 */
public interface InfoReceiver {

	/**
	 * 对 SockJS "Info" URL 执行 HTTP 请求，
	 * 并返回得到的 JSON 响应内容，或引发异常。
	 * <p>请注意，自 4.2 起，此方法接受 {@code headers} 参数。
	 * @param infoUrl 用于获取 SockJS 服务器信息的 URL
	 * @param headers 用于该请求的头
	 * @return 响应的正文
	 */
	String executeInfoRequest(URI infoUrl, @Nullable HttpHeaders headers);

}
