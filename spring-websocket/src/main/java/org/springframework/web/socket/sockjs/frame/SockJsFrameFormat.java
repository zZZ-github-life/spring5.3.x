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

package org.springframework.web.socket.sockjs.frame;

/**
 * 对 SockJS 帧内容应用特定于传输的格式，从而生成可以写出的内容。
 * 主要用于推送数据的 HTTP 服务器端传输。
 *
 * <p>格式化方式可能各不相同，从为 XHR 轮询和流式传输简单地追加换行符，
 * 到 jsonp 风格的回调函数、包裹 script 标签等等。
 *
 * <p>有关使用的各种 SockJS 帧格式，请参见
 * {@link  org.springframework.web.socket.sockjs.transport.handler.AbstractHttpSendingTransportHandler#getFrameFormat(org.springframework.http.server.ServerHttpRequest) AbstractHttpSendingTransportHandler.getFrameFormat}
 * 的实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsFrameFormat {

	String format(SockJsFrame frame);

}
