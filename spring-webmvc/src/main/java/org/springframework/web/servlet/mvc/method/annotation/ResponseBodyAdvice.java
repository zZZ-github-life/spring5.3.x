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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * 允许在 {@code @ResponseBody} 或 {@code ResponseEntity} 控制器方法
 * 执行之后、但主体被 {@code HttpMessageConverter} 写出之前
 * 自定义响应。
 *
 * <p>实现可以直接注册到
 * {@code RequestMappingHandlerAdapter} 和 {@code ExceptionHandlerExceptionResolver}，
 * 更常见的是使用 {@code @ControllerAdvice} 注解，此时它们
 * 会被两者自动检测到。
 * @author Rossen Stoyanchev
 * @since 4.1
 * @param <T> 请求体类型
 */
public interface ResponseBodyAdvice<T> {

	/**
	 * 此组件是否支持给定的控制器方法返回类型
	 * 以及所选的 {@code HttpMessageConverter} 类型。
	 * @param returnType 返回类型
	 * @param converterType 选定的转换器类型
	 * @return 如果应调用 {@link #beforeBodyWrite} 则为 {@code true}；否则为 {@code false}
	 */
	boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType);

	/**
	 * 在选择 {@code HttpMessageConverter} 之后、调用其 write 方法
	 * 之前调用。
	 * @param body 要写入的请求体
	 * @param returnType 控制器方法的返回类型
	 * @param selectedContentType 通过内容协商选定的内容类型
	 * @param selectedConverterType 选定用于写入响应的转换器类型
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 传入的请求体，或修改后的（可能是新的）实例
	 */
	@Nullable
	T beforeBodyWrite(@Nullable T body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response);

}
