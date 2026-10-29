/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * {@code ResponseBodyAdvice} 实现的便捷基类，
 * 用于在使用 {@link AbstractJackson2HttpMessageConverter} 的具体子类
 * 进行 JSON 序列化之前自定义响应。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.1
 */
public abstract class AbstractMappingJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	@Nullable
	public final Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
			MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		if (body == null) {
			return null;
		}
		MappingJacksonValue container = getOrCreateContainer(body);
		beforeBodyWriteInternal(container, contentType, returnType, request, response);
		return container;
	}

	/**
	 * 将主体包装到 {@link MappingJacksonValue} 值容器中（以提供
	 * 附加的序列化指令），或者如果已经包装则直接进行类型转换。
	 */
	protected MappingJacksonValue getOrCreateContainer(Object body) {
		return (body instanceof MappingJacksonValue ? (MappingJacksonValue) body : new MappingJacksonValue(body));
	}

	/**
	 * 仅当转换器类型为 {@code MappingJackson2HttpMessageConverter} 时才会调用。
	 */
	protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);

}
