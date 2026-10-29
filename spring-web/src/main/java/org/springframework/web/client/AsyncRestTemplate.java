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

package org.springframework.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * <strong>Spring 用于异步客户端 HTTP 访问的核心类。</strong>
 * 提供与 {@link RestTemplate} 类似的方法，但返回的是 {@link ListenableFuture}
 * 包装对象，而不是具体的结果。
 *
 * <p>{@code AsyncRestTemplate} 通过 {@link #getRestOperations()} 方法暴露一个同步的
 * {@link RestTemplate}，并与该 {@code RestTemplate} 共享
 * {@linkplain #setErrorHandler 错误处理器}和
 * {@linkplain #setMessageConverters 消息转换器}。
 *
 * <p><strong>注意：</strong>默认情况下，{@code AsyncRestTemplate} 依赖
 * 标准 JDK 设施来建立 HTTP 连接。你可以通过使用接受
 * {@link org.springframework.http.client.AsyncClientHttpRequestFactory} 的构造函数，
 * 切换到其他 HTTP 库，例如 Apache HttpComponents、Netty 和 OkHttp。
 *
 * <p>更多信息请参考 {@link RestTemplate} API 文档。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see RestTemplate
 * @deprecated as of Spring 5.0, in favor of {@link org.springframework.web.reactive.function.client.WebClient}
 */
@Deprecated
public class AsyncRestTemplate extends org.springframework.http.client.support.InterceptingAsyncHttpAccessor
		implements AsyncRestOperations {

	private final RestTemplate syncTemplate;


	/**
	 * 使用默认设置创建一个新的 {@code AsyncRestTemplate} 实例。
	 * <p>此构造函数结合使用 {@link SimpleClientHttpRequestFactory} 和
	 * {@link SimpleAsyncTaskExecutor} 进行异步执行。
	 */
	public AsyncRestTemplate() {
		this(new SimpleAsyncTaskExecutor());
	}

	/**
	 * 使用给定的 {@link AsyncTaskExecutor} 创建一个新的 {@code AsyncRestTemplate} 实例。
	 * <p>此构造函数结合使用 {@link SimpleClientHttpRequestFactory} 和给定的
	 * {@code AsyncTaskExecutor} 进行异步执行。
	 */
	public AsyncRestTemplate(AsyncListenableTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "AsyncTaskExecutor must not be null");
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setTaskExecutor(taskExecutor);
		this.syncTemplate = new RestTemplate(requestFactory);
		setAsyncRequestFactory(requestFactory);
	}

	/**
	 * 使用给定的 {@link org.springframework.http.client.AsyncClientHttpRequestFactory}
	 * 创建一个新的 {@code AsyncRestTemplate} 实例。
	 * <p>此构造函数会将给定的异步 {@code AsyncClientHttpRequestFactory}
	 * 强制转换为 {@link ClientHttpRequestFactory}。由于 Spring 中提供的所有
	 * {@code ClientHttpRequestFactory} 实现同时也实现了
	 * {@code AsyncClientHttpRequestFactory}，因此不应导致
	 * {@code ClassCastException}。
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory) {
		this(asyncRequestFactory, (ClientHttpRequestFactory) asyncRequestFactory);
	}

	/**
	 * 使用给定的异步和同步请求工厂创建一个新的 {@code AsyncRestTemplate} 实例。
	 * @param asyncRequestFactory the asynchronous request factory
	 * @param syncRequestFactory the synchronous request factory
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory,
			ClientHttpRequestFactory syncRequestFactory) {

		this(asyncRequestFactory, new RestTemplate(syncRequestFactory));
	}

	/**
	 * 使用给定的 {@link org.springframework.http.client.AsyncClientHttpRequestFactory}
	 * 和同步 {@link RestTemplate} 创建一个新的 {@code AsyncRestTemplate} 实例。
	 * @param requestFactory the asynchronous request factory to use
	 * @param restTemplate the synchronous template to use
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory requestFactory,
			RestTemplate restTemplate) {

		Assert.notNull(restTemplate, "RestTemplate must not be null");
		this.syncTemplate = restTemplate;
		setAsyncRequestFactory(requestFactory);
	}


	/**
	 * 设置错误处理器。
	 * <p>默认情况下，AsyncRestTemplate 使用
	 * {@link org.springframework.web.client.DefaultResponseErrorHandler}。
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.syncTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * 返回错误处理器。
	 */
	public ResponseErrorHandler getErrorHandler() {
		return this.syncTemplate.getErrorHandler();
	}

	/**
	 * 配置默认 URI 变量值。这是以下代码的快捷方式：
	 * <pre class="code">
	 * DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
	 * handler.setDefaultUriVariables(...);
	 *
	 * AsyncRestTemplate restTemplate = new AsyncRestTemplate();
	 * restTemplate.setUriTemplateHandler(handler);
	 * </pre>
	 * @param defaultUriVariables the default URI variable values
	 * @since 4.3
	 */
	@SuppressWarnings("deprecation")
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		UriTemplateHandler handler = this.syncTemplate.getUriTemplateHandler();
		if (handler instanceof DefaultUriBuilderFactory) {
			((DefaultUriBuilderFactory) handler).setDefaultUriVariables(defaultUriVariables);
		}
		else if (handler instanceof org.springframework.web.util.AbstractUriTemplateHandler) {
			((org.springframework.web.util.AbstractUriTemplateHandler) handler)
					.setDefaultUriVariables(defaultUriVariables);
		}
		else {
			throw new IllegalArgumentException(
					"This property is not supported with the configured UriTemplateHandler.");
		}
	}

	/**
	 * 此属性与 {@code RestTemplate} 上的对应属性具有相同的用途。
	 * 更多详情请参见 {@link RestTemplate#setUriTemplateHandler}。
	 * @param handler the URI template handler to use
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		this.syncTemplate.setUriTemplateHandler(handler);
	}

	/**
	 * 返回配置的 URI 模板处理器。
	 */
	public UriTemplateHandler getUriTemplateHandler() {
		return this.syncTemplate.getUriTemplateHandler();
	}

	@Override
	public RestOperations getRestOperations() {
		return this.syncTemplate;
	}

	/**
	 * 设置要使用的消息主体转换器。
	 * <p>这些转换器用于在 HTTP 请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.syncTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * 返回消息主体转换器。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.syncTemplate.getMessageConverters();
	}


	// GET

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}


	// HEAD

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException {

		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException {

		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor);
	}


	// POST

	@Override
	public ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Object... uriVars)
			throws RestClientException {

		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVars)
			throws RestClientException {

		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(URI url, @Nullable HttpEntity<?> request)
			throws RestClientException {

		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor);
		return adaptToLocationHeader(future);
	}

	private static ListenableFuture<URI> adaptToLocationHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<URI, HttpHeaders>(future) {
			@Override
			@Nullable
			protected URI adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getLocation();
			}
		};
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url,
			@Nullable HttpEntity<?> request, Class<T> responseType) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}


	// PUT

	@Override
	public ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Object... uriVars)
			throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVars);
	}

	@Override
	public ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVars)
			throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVars);
	}

	@Override
	public ListenableFuture<?> put(URI url, @Nullable HttpEntity<?> request) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null);
	}


	// DELETE

	@Override
	public ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(URI url) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null);
	}


	// OPTIONS

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVars)
			throws RestClientException {

		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVars)
			throws RestClientException {

		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor);
		return adaptToAllowHeader(future);
	}

	private static ListenableFuture<Set<HttpMethod>> adaptToAllowHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<Set<HttpMethod>, HttpHeaders>(future) {
			@Override
			protected Set<HttpMethod> adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getAllow();
			}
		};
	}

	// exchange

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... uriVariables) throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor);
	}


	// general execution

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method, @Nullable AsyncRequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
			Map<String, ?> uriVariables) throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(URI url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * 在给定的 URI 上执行指定的方法。
	 * {@link org.springframework.http.client.ClientHttpRequest}
	 * 使用 {@link RequestCallback} 进行处理；响应则使用
	 * {@link ResponseExtractor} 进行处理。
	 * @param url the fully-expanded URL to connect to
	 * @param method the HTTP method to execute (GET, POST, etc.)
	 * @param requestCallback object that prepares the request (can be {@code null})
	 * @param responseExtractor object that extracts the return value from the response (can
	 * be {@code null})
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	protected <T> ListenableFuture<T> doExecute(URI url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "'url' must not be null");
		Assert.notNull(method, "'method' must not be null");
		try {
			org.springframework.http.client.AsyncClientHttpRequest request = createAsyncRequest(url, method);
			if (requestCallback != null) {
				requestCallback.doWithRequest(request);
			}
			ListenableFuture<ClientHttpResponse> responseFuture = request.executeAsync();
			return new ResponseExtractorFuture<>(method, url, responseFuture, responseExtractor);
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + url + "\":" + ex.getMessage(), ex);
		}
	}

	private void logResponseStatus(HttpMethod method, URI url, ClientHttpResponse response) {
		if (logger.isDebugEnabled()) {
			try {
				logger.debug("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + ")");
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	private void handleResponseError(HttpMethod method, URI url, ClientHttpResponse response) throws IOException {
		if (logger.isDebugEnabled()) {
			try {
				logger.debug("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + "); invoking error handler");
			}
			catch (IOException ex) {
				// ignore
			}
		}
		getErrorHandler().handleError(url, method, response);
	}

	/**
	 * 返回一个请求回调实现，它根据给定的响应类型和配置的
	 * {@linkplain #getMessageConverters() 消息转换器}准备请求的
	 * {@code Accept} 头。
	 */
	protected <T> AsyncRequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.acceptHeaderRequestCallback(responseType));
	}

	/**
	 * 返回一个请求回调实现，用于将给定对象写入请求流。
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(@Nullable HttpEntity<T> requestBody) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(requestBody));
	}

	/**
	 * 返回一个请求回调实现，用于将给定对象写入请求流。
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(@Nullable HttpEntity<T> request, Type responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(request, responseType));
	}

	/**
	 * 返回用于 {@link ResponseEntity} 的响应提取器。
	 */
	protected <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return this.syncTemplate.responseEntityExtractor(responseType);
	}

	/**
	 * 返回用于 {@link HttpHeaders} 的响应提取器。
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.syncTemplate.headersExtractor();
	}


	/**
	 * 从
	 * {@link #doExecute(URI, HttpMethod, AsyncRequestCallback, ResponseExtractor)} 返回的 Future。
	 */
	private class ResponseExtractorFuture<T> extends ListenableFutureAdapter<T, ClientHttpResponse> {

		private final HttpMethod method;

		private final URI url;

		@Nullable
		private final ResponseExtractor<T> responseExtractor;

		public ResponseExtractorFuture(HttpMethod method, URI url,
				ListenableFuture<ClientHttpResponse> clientHttpResponseFuture,
				@Nullable ResponseExtractor<T> responseExtractor) {

			super(clientHttpResponseFuture);
			this.method = method;
			this.url = url;
			this.responseExtractor = responseExtractor;
		}

		@Override
		@Nullable
		protected final T adapt(ClientHttpResponse response) throws ExecutionException {
			try {
				if (!getErrorHandler().hasError(response)) {
					logResponseStatus(this.method, this.url, response);
				}
				else {
					handleResponseError(this.method, this.url, response);
				}
				return convertResponse(response);
			}
			catch (Throwable ex) {
				throw new ExecutionException(ex);
			}
			finally {
				response.close();
			}
		}

		@Nullable
		protected T convertResponse(ClientHttpResponse response) throws IOException {
			return (this.responseExtractor != null ? this.responseExtractor.extractData(response) : null);
		}
	}


	/**
	 * 将 {@link RequestCallback} 适配到 {@link AsyncRequestCallback} 接口。
	 */
	private static class AsyncRequestCallbackAdapter implements AsyncRequestCallback {

		private final RequestCallback adaptee;

		/**
		 * 根据给定的 {@link RequestCallback} 创建一个新的
		 * {@code AsyncRequestCallbackAdapter}。
		 * @param requestCallback the callback to base this adapter on
		 */
		public AsyncRequestCallbackAdapter(RequestCallback requestCallback) {
			this.adaptee = requestCallback;
		}

		@Override
		public void doWithRequest(final org.springframework.http.client.AsyncClientHttpRequest request)
				throws IOException {

			this.adaptee.doWithRequest(new ClientHttpRequest() {
				@Override
				public ClientHttpResponse execute() throws IOException {
					throw new UnsupportedOperationException("execute not supported");
				}
				@Override
				public OutputStream getBody() throws IOException {
					return request.getBody();
				}
				@Override
				@Nullable
				public HttpMethod getMethod() {
					return request.getMethod();
				}
				@Override
				public String getMethodValue() {
					return request.getMethodValue();
				}
				@Override
				public URI getURI() {
					return request.getURI();
				}
				@Override
				public HttpHeaders getHeaders() {
					return request.getHeaders();
				}
			});
		}
	}

}
