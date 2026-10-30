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
	 * @param url URL 地址
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 包装在 {@link Future} 中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步地通过对 URI 模板执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url URL 地址
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的 Map
	 * @return 包装在 {@link Future} 中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地通过对 URL 执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * @param url URL 地址
	 * @param responseType 返回值的类型
	 * @return 包装在 {@link Future} 中的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException;


	// HEAD

	/**
	 * 异步地检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param uriVariables 用于展开模板的变量
	 * @return 该资源的所有 HTTP 头，包装在 {@link Future} 中
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url URL 地址
	 * @param uriVariables 包含 URI 模板变量的 Map
	 * @return 该资源的所有 HTTP 头，包装在 {@link Future} 中
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步地检索由 URL 指定的资源的所有头。
	 * @param url URL 地址
	 * @return 该资源的所有 HTTP 头，包装在 {@link Future} 中
	 */
	ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return {@code Location} 头的值，包装在 {@link Future} 中
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return {@code Location} 头的值，包装在 {@link Future} 中
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，并异步地返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @return {@code Location} 头的值，包装在 {@link Future} 中
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(URI url, @Nullable HttpEntity<?> request) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return 包装在 {@link Future} 中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return 包装在 {@link Future} 中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，
	 * 并异步地返回响应作为 {@link ResponseEntity}。
	 * @param url URL 地址
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @return 包装在 {@link Future} 中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url, @Nullable HttpEntity<?> request,
			Class<T> responseType) throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象 PUT 到 URI 来创建或更新资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URI 模板来创建新资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URL 来创建新资源。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(URI url, @Nullable HttpEntity<?> request) throws RestClientException;


	// DELETE

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 * @param uriVariables 用于在模板中展开的变量
	 */
	ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 * @param uriVariables 用于在模板中展开的变量
	 */
	ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>Future 在完成时将返回 {@code null} 结果。
	 * @param url URL 地址
	 */
	ListenableFuture<?> delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 异步地返回给定 URI 的 Allow 头的值。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param uriVariables 用于在模板中展开的变量
	 * @return allow 头的值，包装在 {@link Future} 中
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地返回给定 URI 的 Allow 头的值。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url URL 地址
	 * @param uriVariables 用于在模板中展开的变量
	 * @return allow 头的值，包装在 {@link Future} 中
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步地返回给定 URL 的 Allow 头的值。
	 * @param url URL 地址
	 * @return allow 头的值，包装在 {@link Future} 中
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 包装在 {@link Future} 中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 包装在 {@link Future} 中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @return 包装在 {@link Future} 中的响应实体
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
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 包装在 {@link Future} 中的响应实体
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
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 包装在 {@link Future} 中的响应实体
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
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文）（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @return 包装在 {@link Future} 中的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// general execution

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
			Object... uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URI 模板执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量使用给定的 URI 变量映射进行展开。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步地对给定 URL 执行 HTTP 方法，使用 {@link AsyncRequestCallback}
	 * 准备请求，并使用 {@link ResponseExtractor} 读取响应。
	 * @param url URL 地址
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	<T> ListenableFuture<T> execute(URI url, HttpMethod method,
			@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor)
			throws RestClientException;

}
