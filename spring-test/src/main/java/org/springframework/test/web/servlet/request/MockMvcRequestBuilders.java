/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.request;

import java.net.URI;

import javax.servlet.DispatcherType;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * 用于创建 {@link RequestBuilder RequestBuilders} 的静态工厂方法。
 *
 * <h3>与 Spring TestContext Framework 的集成</h3>
 * <p>此类中的方法将重用由 Spring TestContext Framework 创建的
 * {@link org.springframework.mock.web.MockServletContext MockServletContext}。
 *
 * <h3>Eclipse 用户</h3>
 * <p>建议将此添加为 Java 编辑器收藏项。要导航到此设置，
 * 请打开 Preferences 并输入 “favorites”。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Greg Turnquist
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 3.2
 */
public abstract class MockMvcRequestBuilders {

	/**
	 * 为 GET 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder get(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, urlTemplate, uriVariables);
	}

	/**
	 * 为 GET 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder get(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, uri);
	}

	/**
	 * 为 POST 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder post(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, urlTemplate, uriVariables);
	}

	/**
	 * 为 POST 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder post(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, uri);
	}

	/**
	 * 为 PUT 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder put(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, urlTemplate, uriVariables);
	}

	/**
	 * 为 PUT 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder put(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, uri);
	}

	/**
	 * 为 PATCH 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder patch(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, urlTemplate, uriVariables);
	}

	/**
	 * 为 PATCH 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder patch(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH, uri);
	}

	/**
	 * 为 DELETE 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder delete(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, urlTemplate, uriVariables);
	}

	/**
	 * 为 DELETE 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder delete(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, uri);
	}

	/**
	 * 为 OPTIONS 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder options(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, urlTemplate, uriVariables);
	}

	/**
	 * 为 OPTIONS 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder options(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, uri);
	}

	/**
	 * 为 HEAD 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, urlTemplate, uriVariables);
	}

	/**
	 * 为 HEAD 请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD, uri);
	}

	/**
	 * 为具有给定 HTTP 方法的请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param method the HTTP method (GET, POST, etc)
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod method, String urlTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(method, urlTemplate, uriVariables);
	}

	/**
	 * 为具有给定 HTTP 方法的请求创建 {@link MockHttpServletRequestBuilder}。
	 * @param httpMethod the HTTP method (GET, POST, etc)
	 * @param uri the URL
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * 允许自定义 HTTP 动词（如 WebDAV）的替代工厂方法。
	 * @param httpMethod the HTTP method
	 * @param uri the URL
	 * @since 4.3
	 */
	public static MockHttpServletRequestBuilder request(String httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * 为 multipart 请求创建 {@link MockMultipartHttpServletRequestBuilder}，
	 * 使用 POST 作为 HTTP 方法。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 5.0
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(String urlTemplate, Object... uriVariables) {
		return new MockMultipartHttpServletRequestBuilder(urlTemplate, uriVariables);
	}

	/**
	 * {@link #multipart(String, Object...)} 的变体，
	 * 还接受 {@link HttpMethod}。
	 * @param httpMethod the HTTP method to use
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 5.3.22
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, String urlTemplate, Object... uriVariables) {
		return new MockMultipartHttpServletRequestBuilder(httpMethod, urlTemplate, uriVariables);
	}

	/**
	 * {@link #multipart(String, Object...)} 的变体，使用 {@link URI}。
	 * @param uri the URL
	 * @since 5.0
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(URI uri) {
		return new MockMultipartHttpServletRequestBuilder(uri);
	}

	/**
	 * {@link #multipart(String, Object...)} 的变体，
	 * 使用 {@link URI} 和 {@link HttpMethod}。
	 * @param httpMethod the HTTP method to use
	 * @param uri the URL
	 * @since 5.3.21
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, URI uri) {
		return new MockMultipartHttpServletRequestBuilder(httpMethod, uri);
	}

	/**
	 * 为 multipart 请求创建 {@link MockMultipartHttpServletRequestBuilder}。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 * @deprecated in favor of {@link #multipart(String, Object...)}
	 */
	@Deprecated
	public static MockMultipartHttpServletRequestBuilder fileUpload(String urlTemplate, Object... uriVariables) {
		return new MockMultipartHttpServletRequestBuilder(urlTemplate, uriVariables);
	}

	/**
	 * 为 multipart 请求创建 {@link MockMultipartHttpServletRequestBuilder}。
	 * @param uri the URL
	 * @since 4.0.3
	 * @deprecated in favor of {@link #multipart(URI)}
	 */
	@Deprecated
	public static MockMultipartHttpServletRequestBuilder fileUpload(URI uri) {
		return new MockMultipartHttpServletRequestBuilder(uri);
	}


	/**
	 * 基于启动异步处理的请求的 {@link MvcResult} 创建用于异步分派的 {@link RequestBuilder}。
	 * <p>用法包括先执行一个启动异步处理的请求：
	 * <pre class="code">
	 * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
	 *	.andExpect(request().asyncStarted())
	 *	.andReturn();
	 *  </pre>
	 * <p>然后重用该 {@code MvcResult} 执行异步分派：
	 * <pre class="code">
	 * this.mockMvc.perform(asyncDispatch(mvcResult))
	 *	.andExpect(status().isOk())
	 *	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 *	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	 * </pre>
	 * @param mvcResult the result from the request that started async processing
	 */
	public static RequestBuilder asyncDispatch(MvcResult mvcResult) {

		// There must be an async result before dispatching
		mvcResult.getAsyncResult();

		return servletContext -> {
			MockHttpServletRequest request = mvcResult.getRequest();
			request.setDispatcherType(DispatcherType.ASYNC);
			request.setAsyncStarted(false);
			return request;
		};
	}

}
