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

package org.springframework.test.web.servlet;

import javax.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 构建 {@link MockHttpServletRequest}。
 *
 * <p>请参见
 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders MockMvcRequestBuilders} 中的静态工厂方法。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface RequestBuilder {

	/**
	 * 构建请求。
	 * @param servletContext 用于创建该请求的 {@link ServletContext}
	 * @return 请求
	 */
	MockHttpServletRequest buildRequest(ServletContext servletContext);

}
