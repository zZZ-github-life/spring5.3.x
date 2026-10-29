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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * 通过调用 {@link HttpHandler} 而不是向网络套接字发出实际请求来处理请求的连接器。
 *
 * <p>在内部，连接器使用并适配<br>
 * {@link MockClientHttpRequest} 和 {@link MockClientHttpResponse} 为<br>
 * {@link MockServerHttpRequest} 和 {@link MockServerHttpResponse}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHandlerConnector implements ClientHttpConnector {

	private static Log logger = LogFactory.getLog(HttpHandlerConnector.class);

	private final HttpHandler handler;


	/**
	 * 使用用于处理请求的 {@link HttpHandler} 的构造函数。
	 */
	public HttpHandlerConnector(HttpHandler handler) {
		Assert.notNull(handler, "HttpHandler is required");
		this.handler = handler;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod httpMethod, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		return Mono.defer(() -> doConnect(httpMethod, uri, requestCallback))
				.subscribeOn(Schedulers.parallel());
	}

	private Mono<ClientHttpResponse> doConnect(
			HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// unsafe(): we're intercepting, already serialized Publisher signals
		Sinks.Empty<Void> requestWriteSink = Sinks.unsafe().empty();
		Sinks.Empty<Void> handlerSink = Sinks.unsafe().empty();
		ClientHttpResponse[] savedResponse = new ClientHttpResponse[1];

		MockClientHttpRequest mockClientRequest = new MockClientHttpRequest(httpMethod, uri);
		MockServerHttpResponse mockServerResponse = new MockServerHttpResponse();

		mockClientRequest.setWriteHandler(requestBody -> {
			log("Invoking HttpHandler for ", httpMethod, uri);
			ServerHttpRequest mockServerRequest = adaptRequest(mockClientRequest, requestBody);
			ServerHttpResponse responseToUse = prepareResponse(mockServerResponse, mockServerRequest);
			this.handler.handle(mockServerRequest, responseToUse).subscribe(
					aVoid -> {},
					handlerSink::tryEmitError,  // Ignore result: signals cannot compete
					handlerSink::tryEmitEmpty);
			return Mono.empty();
		});

		mockServerResponse.setWriteHandler(responseBody ->
				Mono.fromRunnable(() -> {
					log("Creating client response for ", httpMethod, uri);
					savedResponse[0] = adaptResponse(mockServerResponse, responseBody);
				}));

		log("Writing client request for ", httpMethod, uri);
		requestCallback.apply(mockClientRequest).subscribe(
				aVoid -> {},
				requestWriteSink::tryEmitError,  // Ignore result: signals cannot compete
				requestWriteSink::tryEmitEmpty);

		return Mono.when(requestWriteSink.asMono(), handlerSink.asMono())
				.onErrorMap(ex -> {
					ClientHttpResponse response = savedResponse[0];
					return response != null ? new FailureAfterResponseCompletedException(response, ex) : ex;
				})
				.then(Mono.fromCallable(() -> savedResponse[0] != null ?
						savedResponse[0] : adaptResponse(mockServerResponse, Flux.empty())));
	}

	private void log(String message, HttpMethod httpMethod, URI uri) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("%s %s \"%s\"", message, httpMethod, uri));
		}
	}

	private ServerHttpRequest adaptRequest(MockClientHttpRequest request, Publisher<DataBuffer> body) {
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		HttpHeaders headers = request.getHeaders();
		MultiValueMap<String, HttpCookie> cookies = request.getCookies();
		return MockServerHttpRequest.method(method, uri).headers(headers).cookies(cookies).body(body);
	}

	private ServerHttpResponse prepareResponse(ServerHttpResponse response, ServerHttpRequest request) {
		return (request.getMethod() == HttpMethod.HEAD ? new HttpHeadResponseDecorator(response) : response);
	}

	private ClientHttpResponse adaptResponse(MockServerHttpResponse response, Flux<DataBuffer> body) {
		Integer status = response.getRawStatusCode();
		MockClientHttpResponse clientResponse = new MockClientHttpResponse((status != null) ? status : 200);
		clientResponse.getHeaders().putAll(response.getHeaders());
		clientResponse.getCookies().putAll(response.getCookies());
		clientResponse.setBody(body);
		return clientResponse;
	}


	/**
	 * 指示在服务器响应完成之后发生了错误，错误是通过 {@link ServerHttpResponse#writeWith}
	 * 或 {@link ServerHttpResponse#setComplete()} 发生的，此后响应无法再更改。此异常包装该错误，并提供对响应的
	 * {@link #getCompletedResponse() 访问}。
	 * <p>在实际运行的服务器上会发生什么取决于服务器何时提交响应，错误可能会也可能不会更改响应。
	 * 因此，在没有服务器的测试中，该异常会被包装并允许传播，以便应用程序收到警报。
	 * @since 5.2.2
	 */
	@SuppressWarnings("serial")
	public static final class FailureAfterResponseCompletedException extends RuntimeException {

		private final ClientHttpResponse completedResponse;


		private FailureAfterResponseCompletedException(ClientHttpResponse response, Throwable cause) {
			super("Error occurred after response was completed: " + response, cause);
			this.completedResponse = response;
		}


		public ClientHttpResponse getCompletedResponse() {
			return this.completedResponse;
		}
	}

}
