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
 * 允许在读取请求体并将其转换为 Object 之前自定义请求，
 * 也允许在将得到的 Object 作为 {@code @RequestBody} 或
 * {@code HttpEntity} 方法参数传入控制器方法之前对其进行处理。
 *
 * <p>此约定的实现可以直接注册到
 * {@code RequestMappingHandlerAdapter}，更常见的是使用
 * {@code @ControllerAdvice} 注解，此时它们会被自动检测到。
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface RequestBodyAdvice {

	/**
	 * 最先调用，用于确定此拦截器是否适用。
	 * @param methodParameter 方法参数
	 * @param targetType 目标类型，不一定与方法参数类型相同，例如对于 {@code HttpEntity<String>}。
	 * @param converterType 选定的转换器类型
	 * @return 是否应调用该拦截器
	 */
	boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 第二个调用，在读取并转换请求体之前。
	 * @param inputMessage 请求
	 * @param parameter 目标方法参数
	 * @param targetType 目标类型，不一定与方法参数类型相同，例如对于 {@code HttpEntity<String>}。
	 * @param converterType 用于反序列化请求体的转换器
	 * @return 输入请求或新实例（永不为 {@code null}）
	 */
	HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException;

	/**
	 * 第三个（也是最后一个）调用，在请求体转换为 Object 之后。
	 * @param body 在调用第一个通知之前设置为转换器产生的对象
	 * @param inputMessage 请求
	 * @param parameter 目标方法参数
	 * @param targetType 目标类型，不一定与方法参数类型相同，例如对于 {@code HttpEntity<String>}。
	 * @param converterType 用于反序列化请求体的转换器
	 * @return 相同的请求体或新实例
	 */
	Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 如果请求体为空，则第二个（也是最后一个）调用。
	 * @param body 通常在调用第一个通知之前设置为 {@code null}
	 * @param inputMessage 请求
	 * @param parameter 方法参数
	 * @param targetType 目标类型，不一定与方法参数类型相同，例如对于 {@code HttpEntity<String>}。
	 * @param converterType 选定的转换器类型
	 * @return 要使用的值，或 {@code null}，
	 * 此时若参数是必需的可能会抛出 {@code HttpMessageNotReadableException}
	 */
	@Nullable
	Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType);


}
