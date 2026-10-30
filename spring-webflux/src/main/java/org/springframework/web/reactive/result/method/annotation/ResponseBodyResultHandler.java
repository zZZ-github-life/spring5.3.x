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

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler}，处理来自带有 {@code @ResponseBody} 注解的方法的返回值，并使用 {@link HttpMessageWriter} 将其写入请求或响应正文。
 *
 * <p>默认情况下，此结果处理器的顺序设置为 100。由于它会检测 {@code @ResponseBody} 是否存在，因此应排在查找特定返回类型的结果处理器之后。但请注意，此处理器确实会识别并显式忽略 {@code ResponseEntity} 返回类型。
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResponseBodyResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

	/**
	 * 使用默认 {@link ReactiveAdapterRegistry} 的基本构造函数。
	 * @param writers 用于序列化到响应正文的写入器
	 * @param resolver 用于确定请求的内容类型
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver) {
		this(writers, resolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 使用 {@link ReactiveAdapterRegistry} 实例的构造函数。
	 * @param writers 用于序列化到响应正文的写入器
	 * @param resolver 用于确定请求的内容类型
	 * @param registry 用于适配响应式类型
	 */
	public ResponseBodyResultHandler(List<HttpMessageWriter<?>> writers,
			RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {

		super(writers, resolver, registry);
		setOrder(100);
	}


	@Override
	public boolean supports(HandlerResult result) {
		MethodParameter returnType = result.getReturnTypeSource();
		Class<?> containingClass = returnType.getContainingClass();
		return (AnnotatedElementUtils.hasAnnotation(containingClass, ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class));
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Object body = result.getReturnValue();
		MethodParameter bodyTypeParameter = result.getReturnTypeSource();
		return writeBody(body, bodyTypeParameter, exchange);
	}

}
