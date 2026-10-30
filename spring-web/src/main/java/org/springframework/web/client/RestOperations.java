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

package org.springframework.web.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

/**
 * 指定一组基本 RESTful 操作的接口。
 * 由 {@link RestTemplate} 实现。通常不直接使用，但它是增强可测试性的有用选项，
 * 因为可以很容易地被模拟或打桩。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see RestTemplate
 */
public interface RestOperations {

	// GET

	/**
	 * 通过对指定 URL 执行 GET 来检索表示。
	 * 响应（如果有）会被转换并返回。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应（如果有）会被转换并返回。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应（如果有）会被转换并返回。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

	/**
	 * 通过对指定 URL 执行 GET 来检索实体。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 实体
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 转换后的对象
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * @param url 该 URL
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;


	// HEAD

	/**
	 * 检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param uriVariables 用于展开模板的变量
	 * @return 该资源的所有 HTTP 头
	 */
	HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url 该 URL
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 该资源的所有 HTTP 头
	 */
	HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 检索由 URL 指定的资源的所有头。
	 * @param url 该 URL
	 * @return 该资源的所有 HTTP 头
	 */
	HttpHeaders headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return {@code Location} 头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，并返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return {@code Location} 头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，并返回
	 * {@code Location} 头的值。该头通常指示新资源的存储位置。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @return {@code Location} 头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(URI url, @Nullable Object request) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并返回响应中找到的表示。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并返回响应中找到的表示。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，
	 * 并返回响应中找到的表示。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @since 3.0.2
	 * @see HttpEntity
	 */
	<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板来创建新资源，
	 * 并返回响应作为 {@link HttpEntity}。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @since 3.0.2
	 * @see HttpEntity
	 */
	<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 来创建新资源，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p>实体的主体或 {@code request} 本身可以是
	 * {@link org.springframework.util.MultiValueMap MultiValueMap}，以创建多部分请求。
	 * {@code MultiValueMap} 中的值可以是表示部分主体的任何 Object，
	 * 也可以是表示带有主体和头的部分的
	 * {@link org.springframework.http.HttpEntity HttpEntity}。
	 * @param url 该 URL
	 * @param request 要 POST 的对象（可能为 {@code null}）
	 * @return 转换后的对象
	 * @since 3.0.2
	 * @see HttpEntity
	 */
	<T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象 PUT 到 URI 来创建或更新资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * @param url 该 URL
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URI 模板来创建新资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * @param url 该 URL
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @param uriVariables 用于展开模板的变量
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URL 来创建新资源。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * @param url 该 URL
	 * @param request 要 PUT 的对象（可能为 {@code null}）
	 * @see HttpEntity
	 */
	void put(URI url, @Nullable Object request) throws RestClientException;


	// PATCH

	/**
	 * 通过将给定对象 PATCH 到 URI 模板来更新资源，
	 * 并返回响应中找到的表示。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p><b>注意：标准 JDK HTTP 库不支持 HTTP PATCH。
	 * 你需要使用 Apache HttpComponents 或 OkHttp 请求工厂。</b>
	 * @param url 该 URL
	 * @param request 要 PATCH 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @since 4.3.5
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 */
	@Nullable
	<T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象 PATCH 到 URI 模板来更新资源，
	 * 并返回响应中找到的表示。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p><b>注意：标准 JDK HTTP 库不支持 HTTP PATCH。
	 * 你需要使用 Apache HttpComponents 或 OkHttp 请求工厂。</b>
	 * @param url 该 URL
	 * @param request 要 PATCH 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于展开模板的变量
	 * @return 转换后的对象
	 * @since 4.3.5
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 */
	@Nullable
	<T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 PATCH 到 URL 来更新资源，
	 * 并返回响应中找到的表示。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * <p><b>注意：标准 JDK HTTP 库不支持 HTTP PATCH。
	 * 你需要使用 Apache HttpComponents 或 OkHttp 请求工厂。</b>
	 * @param url 该 URL
	 * @param request 要 PATCH 的对象（可能为 {@code null}）
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @since 4.3.5
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 */
	@Nullable
	<T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException;



	// DELETE

	/**
	 * 删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param uriVariables 用于在模板中展开的变量
	 */
	void delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url 该 URL
	 * @param uriVariables 用于展开模板的变量
	 */
	void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 删除指定 URL 处的资源。
	 * @param url 该 URL
	 */
	void delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 返回给定 URI 的 {@code Allow} 头的值。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param uriVariables 用于在模板中展开的变量
	 * @return {@code Allow} 头的值
	 */
	Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 返回给定 URI 的 {@code Allow} 头的值。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url 该 URL
	 * @param uriVariables 用于在模板中展开的变量
	 * @return {@code Allow} 头的值
	 */
	Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 返回给定 URL 的 {@code Allow} 头的值。
	 * @param url 该 URL
	 * @return {@code Allow} 头的值
	 */
	Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 作为实体返回的响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 作为实体返回的响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @return 作为实体返回的响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 作为实体返回的响应
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(String url,HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 作为实体返回的响应
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（头和/或正文，可能为 {@code null}）
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @return 作为实体返回的响应
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) throws RestClientException;

	/**
	 * 执行给定 {@link RequestEntity} 中指定的请求，并返回
	 * 响应作为 {@link ResponseEntity}。通常与 {@code RequestEntity} 上的
	 * 静态构建器方法结合使用，例如：
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity
	 *     .post(new URI(&quot;https://example.com/foo&quot;))
	 *     .accept(MediaType.APPLICATION_JSON)
	 *     .body(body);
	 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
	 * </pre>
	 * @param requestEntity 要写入请求的实体
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @return 作为实体返回的响应
	 * @since 4.1
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * 执行给定 {@link RequestEntity} 中指定的请求，并返回
	 * 响应作为 {@link ResponseEntity}。给定的
	 * {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity
	 *     .post(new URI(&quot;https://example.com/foo&quot;))
	 *     .accept(MediaType.APPLICATION_JSON)
	 *     .body(body);
	 * ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
	 * </pre>
	 * @param requestEntity 要写入请求的实体
	 * @param responseType 要将响应转换成的类型；若没有正文则为 {@code Void.class}
	 * @return 作为实体返回的响应
	 * @since 4.1
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// General execution

	/**
	 * 对给定 URI 模板执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables)
			throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量映射进行展开。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables 用于在模板中展开的变量
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 对给定 URL 执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * @param url 该 URL
	 * @param method HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	@Nullable
	<T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException;

}
