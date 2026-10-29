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
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析带有 {@code @RequestHeader} 注解的 {@link Map} 方法参数。对于带有 {@code @RequestHeader} 注解的单个头值，请改为参见 {@link RequestHeaderMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求头名称-值对。方法参数类型可以是 {@link MultiValueMap}，以接收某个头的所有值，而不仅仅是第一个值。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestHeaderMethodArgumentResolver
 */
public class RequestHeaderMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public RequestHeaderMapMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestHeader.class, this::allParams);
	}

	private boolean allParams(RequestHeader annotation, Class<?> type) {
		return Map.class.isAssignableFrom(type);
	}


	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		boolean isMultiValueMap = MultiValueMap.class.isAssignableFrom(methodParameter.getParameterType());
		HttpHeaders headers = exchange.getRequest().getHeaders();
		return (isMultiValueMap ? headers : headers.toSingleValueMap());
	}

}
