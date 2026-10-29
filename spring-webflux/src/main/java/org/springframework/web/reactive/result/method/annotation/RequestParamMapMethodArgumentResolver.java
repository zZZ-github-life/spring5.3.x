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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于带有 {@link RequestParam @RequestParam} 注解且注解未指定请求参数名称的 {@link Map} 方法参数的解析器。有关解析带请求参数名称的 {@link Map} 方法参数，请参见 {@link RequestParamMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求参数名称-值对。如果方法参数类型为 {@link MultiValueMap}，则创建的映射包含所有请求参数及其所有值，以应对请求参数具有多个值的情况。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RequestParamMethodArgumentResolver
 */
public class RequestParamMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public RequestParamMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::allParams);
	}

	private boolean allParams(RequestParam requestParam, Class<?> type) {
		return (Map.class.isAssignableFrom(type) && !StringUtils.hasText(requestParam.name()));
	}


	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		boolean isMultiValueMap = MultiValueMap.class.isAssignableFrom(methodParameter.getParameterType());
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		return (isMultiValueMap ? queryParams : queryParams.toSingleValueMap());
	}

}
