/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 通过兼容的 {@code HttpMessageReader} 读取请求正文，解析带有 {@code @RequestBody} 注解的方法参数。
 *
 * <p>如果 {@code @RequestBody} 方法参数带有 {@code @javax.validation.Valid} 或 {@link org.springframework.validation.annotation.Validated} 注解，也会进行校验。校验失败会导致 {@link ServerWebInputException}。
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RequestBodyMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public RequestBodyMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter param, BindingContext bindingContext, ServerWebExchange exchange) {

		RequestBody ann = param.getParameterAnnotation(RequestBody.class);
		Assert.state(ann != null, "No RequestBody annotation");
		return readBody(param, ann.required(), bindingContext, exchange);
	}

}
