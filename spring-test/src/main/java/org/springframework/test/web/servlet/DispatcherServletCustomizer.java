/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.web.servlet.DispatcherServlet;

/**
 * 用于自定义由 {@link MockMvc} 管理的 {@link DispatcherServlet} 实例的策略接口。
 * @author Stephane Nicoll
 * @since 4.3.4
 */
@FunctionalInterface
public interface DispatcherServletCustomizer {

	/**
	 * 在提供的 {@link DispatcherServlet} 初始化<em>之前</em>对其进行自定义。
	 * @param dispatcherServlet the dispatcher servlet to customize
	 */
	void customize(DispatcherServlet dispatcherServlet);

}
