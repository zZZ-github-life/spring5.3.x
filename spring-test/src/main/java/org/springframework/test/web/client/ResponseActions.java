/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * 用于设置请求预期并定义响应的约定。
 * 实现可通过 {@link MockRestServiceServer#expect(RequestMatcher)} 获取。
 * @author Craig Walls
 * @since 3.2
 */
public interface ResponseActions {

	/**
	 * 添加请求预期。
	 * @return 该期望
	 */
	ResponseActions andExpect(RequestMatcher requestMatcher);

	/**
	 * 定义响应。
	 * @param responseCreator 响应的创建器
	 */
	void andRespond(ResponseCreator responseCreator);

}
