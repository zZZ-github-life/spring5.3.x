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
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应（如果有）会被转换并返回。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the map containing variables for the URI template
	 * @return the converted object
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应（如果有）会被转换并返回。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @return the converted object
	 */
	@Nullable
	<T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

	/**
	 * 通过对指定 URL 执行 GET 来检索实体。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the entity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the map containing variables for the URI template
	 * @return the converted object
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应会被转换并存储在 {@link ResponseEntity} 中。
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @return the converted object
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;


	// HEAD

	/**
	 * 检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand the template
	 * @return all HTTP headers of that resource
	 */
	HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 检索由 URI 模板指定的资源的所有头。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param uriVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource
	 */
	HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 检索由 URL 指定的资源的所有头。
	 * @param url the URL
	 * @return all HTTP headers of that resource
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @return the value for the {@code Location} header
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param responseType the type of the return value
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be POSTed (may be {@code null})
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URI 模板来创建新资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 PUT 到 URL 来创建新资源。
	 * <p>{@code request} 参数可以是 {@link HttpEntity}，以便向请求添加额外的 HTTP 头。
	 * @param url the URL
	 * @param request the Object to be PUT (may be {@code null})
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
	 * @param url the URL
	 * @param request the object to be PATCHed (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the object to be PATCHed (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
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
	 * @param url the URL
	 * @param request the object to be PATCHed (may be {@code null})
	 * @param responseType the type of the return value
	 * @return the converted object
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
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 */
	void delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 删除指定 URI 处的资源。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand the template
	 */
	void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 删除指定 URL 处的资源。
	 * @param url the URL
	 */
	void delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 返回给定 URI 的 {@code Allow} 头的值。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the {@code Allow} header
	 */
	Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 返回给定 URI 的 {@code Allow} 头的值。
	 * <p>URI 模板变量会使用给定的映射进行展开。
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the {@code Allow} header
	 */
	Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 返回给定 URL 的 {@code Allow} 头的值。
	 * @param url the URL
	 * @return the value of the {@code Allow} header
	 */
	Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，将给定请求实体写入请求，
	 * 并返回响应作为 {@link ResponseEntity}。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @return the response as entity
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
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the
	 * request (may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity
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
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity
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
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @return the response as entity
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
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @return the response as entity
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
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type to convert the response to, or {@code Void.class} for no body
	 * @return the response as entity
	 * @since 4.1
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// General execution

	/**
	 * 对给定 URI 模板执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量（如果有）进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables)
			throws RestClientException;

	/**
	 * 对给定 URI 模板执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * <p>URI 模板变量会使用给定的 URI 变量映射进行展开。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 对给定 URL 执行 HTTP 方法，使用 {@link RequestCallback} 准备请求，
	 * 并使用 {@link ResponseExtractor} 读取响应。
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	@Nullable
	<T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException;

}
