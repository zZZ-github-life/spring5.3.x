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

package org.springframework.web.reactive.result.view;

import java.util.Locale;

import reactor.core.publisher.Mono;

/**
 * 将视图名称解析为 {@link View} 实例的契约。视图名称可以对应 HTML 模板，也可以是动态生成的。
 *
 * <p>视图解析过程由名为 {@link ViewResolutionResultHandler ViewResolutionResultHandler} 的基于 ViewResolver 的 {@code HandlerResultHandler} 实现驱动。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ViewResolutionResultHandler
 */
public interface ViewResolver {

	/**
	 * 将视图名称解析为 View 实例。
	 * @param viewName 要解析的视图名称
	 * @param locale 请求的 Locale
	 * @return 解析后的视图或空流
	 */
	Mono<View> resolveViewName(String viewName, Locale locale);

}
