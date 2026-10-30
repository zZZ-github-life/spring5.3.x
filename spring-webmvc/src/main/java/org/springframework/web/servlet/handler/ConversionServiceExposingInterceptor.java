/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截器，将所配置的 {@link ConversionService} 放入请求作用域，
 * 使其在请求处理期间可用。请求属性名称为
 * "org.springframework.core.convert.ConversionService"，即
 * {@code ConversionService.class.getName()} 的值。
 *
 * <p>主要用于 JSP 标签（例如 spring:eval 标签）中。
 *
 * @author Keith Donald
 * @since 3.0.1
 */
public class ConversionServiceExposingInterceptor implements HandlerInterceptor {

	private final ConversionService conversionService;


	/**
	 * 创建新的 {@link ConversionServiceExposingInterceptor}。
	 * @param conversionService 调用该拦截器时要导出到请求作用域的转换服务
	 */
	public ConversionServiceExposingInterceptor(ConversionService conversionService) {
		Assert.notNull(conversionService, "The ConversionService may not be null");
		this.conversionService = conversionService;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		request.setAttribute(ConversionService.class.getName(), this.conversionService);
		return true;
	}

}
