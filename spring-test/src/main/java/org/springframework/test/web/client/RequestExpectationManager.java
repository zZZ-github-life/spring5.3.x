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
import java.time.Duration;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 封装实现 {@link MockRestServiceServer} 所需的行为，包括其公共 API（创建预期 + 验证/重置）以及用于验证实际请求的额外方法。
 *
 * <p>应用程序不会直接使用此约定，但可以通过 {@code MockRestServiceServer} 构建器
 * {@link org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder#build(RequestExpectationManager)
 * 插入} 自定义实现。
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public interface RequestExpectationManager {

	/**
	 * 设置新的请求预期。返回的 {@link ResponseActions} 用于添加更多预期并定义响应。
	 * <p>这是 {@link MockRestServiceServer#expect(ExpectedCount, RequestMatcher)} 的委托。
	 * @param requestMatcher 一个请求期望
	 * @return 用于设置更多期望并定义响应
	 * @see MockRestServiceServer#expect(RequestMatcher)
	 * @see MockRestServiceServer#expect(ExpectedCount, RequestMatcher)
	 */
	ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher);

	/**
	 * 验证所有预期都已满足。
	 * <p>这是 {@link MockRestServiceServer#verify()} 的委托。
	 * @throws AssertionError 如果并非所有期望都得到满足。
	 * @see MockRestServiceServer#verify()
	 */
	void verify();

	/**
	 * {@link #verify()} 的变体，最多等待指定时间直到所有预期都得到满足。
	 * 这对于涉及异步请求的测试很有用。
	 * @param timeout 等待所有期望得到满足的时间
	 * @throws AssertionError 如果在指定超时时间内并非所有期望都得到满足，
	 * 或在此之前任何时刻有期望失败。
	 * @since 5.3.4
	 */
	void verify(Duration timeout);

	/**
	 * 重置内部状态，移除所有预期和已记录的请求。
	 * <p>这是 {@link MockRestServiceServer#reset()} 的委托。
	 * @see MockRestServiceServer#reset()
	 */
	void reset();


	/**
	 * 根据已声明的预期验证给定的实际请求。如果验证成功，则返回要使用的模拟响应，否则抛出错误。
	 * <p>在 {@link MockRestServiceServer} 中用于处理实际请求。
	 * @param request 请求
	 * @return 如果请求校验通过，则返回响应。
	 * @throws AssertionError 当某些期望未得到满足时。
	 * @throws IOException 发生任何校验错误时。
	 */
	ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException;
}
