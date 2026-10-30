/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.response.MockRestResponseCreators;

/**
 * 用于创建 {@link ClientHttpResponse} 的约定。
 * 实现可通过 {@link MockRestResponseCreators} 获取。
 * @author Craig Walls
 * @since 3.2
 */
@FunctionalInterface
public interface ResponseCreator {

	/**
	 * 为给定请求创建响应。
	 * @param request 请求
	 */
	ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException;

}
