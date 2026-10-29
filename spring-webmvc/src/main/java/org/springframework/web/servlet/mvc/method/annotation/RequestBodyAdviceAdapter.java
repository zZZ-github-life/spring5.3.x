/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;

/**
 * 实现
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice
 * RequestBodyAdvice} 的便捷起点，提供默认的方法实现。
 *
 * <p>子类必须根据通知的适用时机实现 {@link #supports} 并返回 true。
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public abstract class RequestBodyAdviceAdapter implements RequestBodyAdvice {

	/**
	 * 默认实现返回传入的 InputMessage。
	 */
	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {

		return inputMessage;
	}

	/**
	 * 默认实现返回传入的请求体。
	 */
	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

	/**
	 * 默认实现返回传入的请求体。
	 */
	@Override
	@Nullable
	public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

}
