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

package org.springframework.test.web.servlet.request;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 供希望在使用 {@link MockHttpServletRequestBuilder} 或其子类
 * {@link MockMultipartHttpServletRequestBuilder} 构建
 * {@link MockHttpServletRequest} 实例后进一步初始化该实例的应用或第三方库使用的扩展点。
 *
 * <p>此接口的实现可以在即将构造请求时提供给
 * {@link MockHttpServletRequestBuilder#with(RequestPostProcessor)}。
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
@FunctionalInterface
public interface RequestPostProcessor {

	/**
	 * 在通过 {@code MockHttpServletRequestBuilder} 创建并初始化给定的
	 * {@code MockHttpServletRequest} 之后对其进行后处理。
	 * @param request 要初始化的请求
	 * @return 要使用的请求，可以是传入的请求，也可以是包装后的请求
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
