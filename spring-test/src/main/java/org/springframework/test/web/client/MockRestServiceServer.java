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

package org.springframework.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

/**
 * <strong>客户端 REST 测试的主要入口点</strong>。用于涉及直接或间接使用 {@link RestTemplate} 的测试。提供了一种设置将通过 {@code RestTemplate} 执行的预期请求以及要发回的模拟响应的方式，从而无需实际服务器。
 *
 * <p>下面是一个示例，假设已静态导入
 * {@code MockRestRequestMatchers}、{@code MockRestResponseCreators}
 * 和 {@code ExpectedCount}：
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate()
 * MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
 *
 * server.expect(manyTimes(), requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
 *     .andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
 *
 * Hotel hotel = restTemplate.getForObject("/hotels/{id}", Hotel.class, 42);
 * &#47;&#47; Use the hotel instance...
 *
 * // Verify all expectations met
 * server.verify();
 * </pre>
 *
 * <p>请注意，作为上述方式的替代，您还可以在 {@code RestTemplate} 上设置
 * {@link MockMvcClientHttpRequestFactory}，从而能够针对
 * {@link org.springframework.test.web.servlet.MockMvc MockMvc} 实例执行请求。
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("deprecation")
public final class MockRestServiceServer {

	private final RequestExpectationManager expectationManager;


	/**
	 * 使用 {@code RequestExpectationManager} 的私有构造函数。
	 * 请参见静态构建器方法和 {@code createServer} 快捷方法。
	 */
	private MockRestServiceServer(RequestExpectationManager expectationManager) {
		this.expectationManager = expectationManager;
	}


	/**
	 * 为单个 HTTP 请求设置预期。返回的
	 * {@link ResponseActions} 可用于设置更多预期以及定义响应。
	 * <p>在开始通过底层 {@code RestTemplate} 发出请求之前，可以多次调用此方法，
	 * 以设置所有预期请求。
	 * @param matcher 请求匹配器
	 * @return 该期望的表示
	 */
	public ResponseActions expect(RequestMatcher matcher) {
		return expect(ExpectedCount.once(), matcher);
	}

	/**
	 * 对 {@link #expect(RequestMatcher)} 的替代方法，同时指示预期请求的执行次数。
	 * <p>当请求预期的预期次数大于一时，只有第一次执行需要遵循声明顺序。后续
	 * 请求执行可以插入到其后的任意位置。
	 * @param count 期望的计数
	 * @param matcher 请求匹配器
	 * @return 该期望的表示
	 * @since 4.3
	 */
	public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
		return this.expectationManager.expectRequest(count, matcher);
	}

	/**
	 * 验证通过 {@link #expect(RequestMatcher)} 设置的所有预期请求确实都已执行。
	 * @throws AssertionError 如果并非所有期望都得到满足。
	 */
	public void verify() {
		this.expectationManager.verify();
	}

	/**
	 * {@link #verify()} 的变体，最多等待指定时间直到所有预期都得到满足。
	 * 这对于涉及异步请求的测试很有用。
	 * @param timeout 等待所有期望得到满足的时间
	 * @throws AssertionError 如果在指定超时时间内并非所有期望都得到满足，
	 * 或在此之前任何时刻有期望失败。
	 * @since 5.3.4
	 */
	public void verify(Duration timeout) {
		this.expectationManager.verify(timeout);
	}

	/**
	 * 重置内部状态，移除所有预期和已记录的请求。
	 */
	public void reset() {
		this.expectationManager.reset();
	}


	/**
	 * 返回一个用于应答给定 {@code RestTemplate} 的 {@code MockRestServiceServer} 构建器。
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder bindTo(RestTemplate restTemplate) {
		return new DefaultBuilder(restTemplate);
	}

	/**
	 * 返回一个用于应答给定 {@code AsyncRestTemplate} 的 {@code MockRestServiceServer} 构建器。
	 * @since 4.3
	 * @deprecated see deprecation notice on
	 * {@link org.springframework.web.client.AsyncRestTemplate} itself
	 */
	@Deprecated
	public static MockRestServiceServerBuilder bindTo(org.springframework.web.client.AsyncRestTemplate asyncRestTemplate) {
		return new DefaultBuilder(asyncRestTemplate);
	}

	/**
	 * 返回一个用于应答给定 {@code RestGatewaySupport} 的 {@code MockRestServiceServer} 构建器。
	 * @since 4.3
	 */
	public static MockRestServiceServerBuilder bindTo(RestGatewaySupport restGatewaySupport) {
		Assert.notNull(restGatewaySupport, "'restGatewaySupport' must not be null");
		return new DefaultBuilder(restGatewaySupport.getRestTemplate());
	}


	/**
	 * {@code bindTo(restTemplate).build()} 的快捷方式。
	 * @param restTemplate 要为其设置模拟测试的 RestTemplate
	 * @return 模拟服务器
	 */
	public static MockRestServiceServer createServer(RestTemplate restTemplate) {
		return bindTo(restTemplate).build();
	}

	/**
	 * {@code bindTo(asyncRestTemplate).build()} 的快捷方式。
	 * @param asyncRestTemplate 要为其设置模拟测试的 AsyncRestTemplate
	 * @return 创建的模拟服务器
	 * @deprecated see deprecation notice on
	 * {@link org.springframework.web.client.AsyncRestTemplate} itself
	 */
	@Deprecated
	public static MockRestServiceServer createServer(org.springframework.web.client.AsyncRestTemplate asyncRestTemplate) {
		return bindTo(asyncRestTemplate).build();
	}

	/**
	 * {@code bindTo(restGateway).build()} 的快捷方式。
	 * @param restGateway 要为其设置模拟测试的 REST 网关
	 * @return 创建的模拟服务器
	 */
	public static MockRestServiceServer createServer(RestGatewaySupport restGateway) {
		return bindTo(restGateway).build();
	}


	/**
	 * 用于创建 {@code MockRestServiceServer} 的构建器。
	 */
	public interface MockRestServiceServerBuilder {

		/**
		 * 是否允许预期请求按任意顺序执行，而不必与声明顺序一致。
		 * <p>实际上是以下代码的快捷方式：<br>
		 * {@code builder.build(new UnorderedRequestExpectationManager)}。
		 * <p>默认设置为 {@code false}。
		 * @param ignoreExpectOrder 是否忽略期望的顺序
		 */
		MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder);

		/**
		 * 使用 {@link BufferingClientHttpRequestFactory} 包装器缓冲输入流和输出流，
		 * 例如允许多次读取响应体。
		 * @since 5.0.5
		 */
		MockRestServiceServerBuilder bufferContent();

		/**
		 * 构建 {@code MockRestServiceServer}，并使用创建模拟请求的
		 * {@link ClientHttpRequestFactory} 设置底层
		 * {@code RestTemplate} 或 {@code AsyncRestTemplate}。
		 */
		MockRestServiceServer build();

		/**
		 * 接受自定义 {@link RequestExpectationManager} 的重载构建方法。
		 */
		MockRestServiceServer build(RequestExpectationManager manager);
	}


	private static class DefaultBuilder implements MockRestServiceServerBuilder {

		@Nullable
		private final RestTemplate restTemplate;

		@Nullable
		private final org.springframework.web.client.AsyncRestTemplate asyncRestTemplate;

		private boolean ignoreExpectOrder;

		private boolean bufferContent;


		public DefaultBuilder(RestTemplate restTemplate) {
			Assert.notNull(restTemplate, "RestTemplate must not be null");
			this.restTemplate = restTemplate;
			this.asyncRestTemplate = null;
		}

		public DefaultBuilder(org.springframework.web.client.AsyncRestTemplate asyncRestTemplate) {
			Assert.notNull(asyncRestTemplate, "AsyncRestTemplate must not be null");
			this.restTemplate = null;
			this.asyncRestTemplate = asyncRestTemplate;
		}

		@Override
		public MockRestServiceServerBuilder ignoreExpectOrder(boolean ignoreExpectOrder) {
			this.ignoreExpectOrder = ignoreExpectOrder;
			return this;
		}

		@Override
		public MockRestServiceServerBuilder bufferContent() {
			this.bufferContent = true;
			return this;
		}

		@Override
		public MockRestServiceServer build() {
			if (this.ignoreExpectOrder) {
				return build(new UnorderedRequestExpectationManager());
			}
			else {
				return build(new SimpleRequestExpectationManager());
			}
		}

		@Override
		public MockRestServiceServer build(RequestExpectationManager manager) {
			MockRestServiceServer server = new MockRestServiceServer(manager);
			MockClientHttpRequestFactory factory = server.new MockClientHttpRequestFactory();
			if (this.restTemplate != null) {
				if (this.bufferContent) {
					this.restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(factory));
				}
				else {
					this.restTemplate.setRequestFactory(factory);
				}
			}
			if (this.asyncRestTemplate != null) {
				this.asyncRestTemplate.setAsyncRequestFactory(factory);
			}
			return server;
		}
	}


	/**
	 * 模拟 ClientHttpRequestFactory，通过遍历预期 {@link DefaultRequestExpectation} 列表来创建请求。
	 */
	private class MockClientHttpRequestFactory implements ClientHttpRequestFactory,
			org.springframework.http.client.AsyncClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			return createRequestInternal(uri, httpMethod);
		}

		@Override
		public org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(
				URI uri, HttpMethod httpMethod) {

			return createRequestInternal(uri, httpMethod);
		}

		private org.springframework.mock.http.client.MockAsyncClientHttpRequest createRequestInternal(
				URI uri, HttpMethod httpMethod) {

			Assert.notNull(uri, "'uri' must not be null");
			Assert.notNull(httpMethod, "'httpMethod' must not be null");

			return new org.springframework.mock.http.client.MockAsyncClientHttpRequest(httpMethod, uri) {

				@Override
				protected ClientHttpResponse executeInternal() throws IOException {
					ClientHttpResponse response = expectationManager.validateRequest(this);
					setResponse(response);
					return response;
				}
			};
		}
	}

}
