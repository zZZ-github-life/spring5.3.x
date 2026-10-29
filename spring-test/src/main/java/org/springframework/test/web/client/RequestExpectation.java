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

package org.springframework.test.web.client;

/**
 * 对 {@code ResponseActions} 的扩展，同时还实现了
 * {@code RequestMatcher} 和 {@code ResponseCreator}。
 *
 * <p>虽然 {@code ResponseActions} 是定义预期的 API，但此子接口是用于将预期匹配到实际请求并创建响应的内部 SPI。
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

	/**
	 * 此预期是否还有剩余的调用次数。
	 */
	boolean hasRemainingCount();

	/**
	 * 增加已匹配请求计数，并检查是否未超过最大计数。
	 * @since 5.0.3
	 */
	void incrementAndValidate();

	/**
	 * 此请求预期的要求是否已满足。
	 */
	boolean isSatisfied();

}
