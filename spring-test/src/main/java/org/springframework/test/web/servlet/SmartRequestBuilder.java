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

package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 扩展的 {@link RequestBuilder} 变体，它将
 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor org.springframework.test.web.servlet.request.RequestPostProcessors}
 * 作为 {@link #buildRequest} 方法的单独步骤应用。
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface SmartRequestBuilder extends RequestBuilder {

	/**
	 * 应用请求后处理。通常意味着调用一个或多个
	 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor org.springframework.test.web.servlet.request.RequestPostProcessors}。
	 * @param request 要初始化的请求
	 * @return 要使用的请求，可以是传入的请求，也可以是包装后的请求
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
