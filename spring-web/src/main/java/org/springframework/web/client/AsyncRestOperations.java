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

package org.springframework.web.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 指定一组基本异步 RESTful 操作的接口。
 * 由 {@link AsyncRestTemplate} 实现。通常不直接使用，但它是增强可测试性的有用选项，
 * 因为可以很容易地被模拟或打桩。
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see AsyncRestTemplate
 * @see RestOperations
 * @deprecated as of Spring 5.0, in favor of {@link org.springframework.web.reactive.function.client.WebClient}
 */
@Deprecated
public interface AsyncRestOperations {

	/**
	 * 公开同步的 Spring RestTemplate，以允许同步调用。
	 */
	RestOperations getRestOperations();


	// GET

	/**
	 * 异步地通过对指定 URL 执行 GET 来检索实体。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步地通过对 URI 模板执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the map containing variables for the URI template
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地通过对 URL 执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException;


	// HEAD

	/**
	 * 异步地检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand the template
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param uriVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步地检索由 URL 指定的资源的所有头。
	 * @param url the URL
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(URI url, @Nullable HttpEntity<?> request) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url, @Nullable HttpEntity<?> request,
			Class<T> responseType) throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象 PUT 到 URI 来创建或更新资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URI 模板来创建新资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URL 来创建新资源。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(URI url, @Nullable HttpEntity<?> request) throws RestClientException;


	// DELETE

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 */
	ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 */
	ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url the URL
	 */
	ListenableFuture<?> delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 异步地返回给定 URI 的 Allow 头的值。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地返回给定 URI 的 Allow 头的值。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步地返回给定 URL 的 Allow 头的值。
	 * @param url the URL
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the
	 * request (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// general execution

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量使用给定的 URI 变量映射进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URL 执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> ListenableFuture<T> execute(URI url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor)
			throws RestClientException;

}
