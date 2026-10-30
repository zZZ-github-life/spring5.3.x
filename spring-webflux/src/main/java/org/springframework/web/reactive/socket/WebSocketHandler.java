/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.socket;

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * WebSocket 会话的处理器。
 *
 * <p>服务器端的 {@code WebSocketHandler} 通过
 * {@link org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
 * SimpleUrlHandlerMapping} 和
 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
 * WebSocketHandlerAdapter} 映射到请求。客户端的 {@code WebSocketHandler} 则传递给
 * {@link org.springframework.web.reactive.socket.client.WebSocketClient
 * WebSocketClient} 的 execute 方法。
 *
 * <p>使用 {@link WebSocketSession#receive() session.receive()} 组合入站消息流，
 * 使用 {@link WebSocketSession#send(Publisher) session.send(publisher)}
 * 处理出站消息流。下面是一个组合流程的示例，用于处理入站消息并发送出站消息：
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {
 *
 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Flux&lt;WebSocketMessage&gt; output = session.receive()
 *			.doOnNext(message -&gt; {
 * 				// ...
 * 			})
 * 			.concatMap(message -&gt; {
 * 				// ...
 * 			})
 * 			.map(value -&gt; session.textMessage("Echo " + value));
 *
 * 		return session.send(output);
 * 	}
 * }
 * </pre>
 *
 * <p>如果处理入站消息与发送出站消息是相互独立的流，可以使用 "zip" 操作符
 * 将它们组合起来：
 *
 * <pre class="code">
 * class ExampleHandler implements WebSocketHandler {
 *
 * 	&#064;Override
 * 	public Mono&lt;Void&gt; handle(WebSocketSession session) {
 *
 * 		Mono&lt;Void&gt; input = session.receive()
 *			.doOnNext(message -&gt; {
 * 				// ...
 * 			})
 * 			.concatMap(message -&gt; {
 * 				// ...
 * 			})
 * 			.then();
 *
 *		Flux&lt;String&gt; source = ... ;
 * 		Mono&lt;Void&gt; output = session.send(source.map(session::textMessage));
 *
 * 		return Mono.zip(input, output).then();
 * 	}
 * }
 * </pre>
 *
 * <p>{@code WebSocketHandler} 必须将入站流和出站流组合成一个统一的流，并返回一个
 * 反映该流完成情况的 {@code Mono<Void>}。这意味着无需检查连接是否打开，因为
 * Reactive Streams 信号会终止活动：入站流会收到完成/错误信号，出站流会收到
 * 取消信号。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketHandler {

	/**
	 * 返回此处理器支持的子协议列表。
	 * <p>默认返回空列表。
	 */
	default List<String> getSubProtocols() {
		return Collections.emptyList();
	}

	/**
	 * 在建立新的 WebSocket 连接时调用，允许处理该会话。
	 *
	 * <p>有关如何处理会话的更多详细信息和示例，请参见类级文档和参考手册。
	 * @param session 要处理的会话
	 * @return 指示应用对会话的处理何时完成，它应反映入站消息流的完成
	 * （即连接关闭），并可能反映出站消息流的完成以及消息的写入
	 */
	Mono<Void> handle(WebSocketSession session);

}
