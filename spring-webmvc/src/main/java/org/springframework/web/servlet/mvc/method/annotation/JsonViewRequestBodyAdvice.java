/*
 * Copyright 2002-2017 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.util.Assert;

/**
 * {@link RequestBodyAdvice} 实现，用于增加对 Jackson 的
 * {@code @JsonView} 注解的支持，该注解声明在 Spring MVC 的 {@code @HttpEntity}
 * 或 {@code @RequestBody} 方法参数上。
 *
 * <p>注解中指定的反序列化视图将传递给
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}，
 * 后者将使用该视图来反序列化请求体。
 *
 * <p>请注意，尽管 {@code @JsonView} 允许指定多个类，
 * 但请求体通知仅支持恰好一个类参数。可考虑使用组合接口。
 * @author Sebastien Deleuze
 * @since 4.2
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see com.fasterxml.jackson.databind.ObjectMapper#readerWithView(Class)
 */
public class JsonViewRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return (AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType) &&
				methodParameter.getParameterAnnotation(JsonView.class) != null);
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> selectedConverterType) throws IOException {

		JsonView ann = methodParameter.getParameterAnnotation(JsonView.class);
		Assert.state(ann != null, "No JsonView annotation");

		Class<?>[] classes = ann.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
		}

		return new MappingJacksonInputMessage(inputMessage.getBody(), inputMessage.getHeaders(), classes[0]);
	}

}
