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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 策略接口，用于在给定请求的上下文中将方法参数解析为参数值。
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see HandlerMethodReturnValueHandler
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 此解析器是否支持给定的{@linkplain MethodParameter 方法参数}。
	 * @param parameter 要检查的方法参数
	 * @return 如果此解析器支持提供的参数则返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 从给定请求中将方法参数解析为参数值。
	 * {@link ModelAndViewContainer} 提供对请求模型的访问。
	 * {@link WebDataBinderFactory} 提供在需要数据绑定和类型转换时创建
	 * {@link WebDataBinder} 实例的方式。
	 * @param parameter 要解析的方法参数。该参数之前必须已传递给
	 * {@link #supportsParameter}，且其必须返回 {@code true}。
	 * @param mavContainer 当前请求的 ModelAndViewContainer
	 * @param webRequest 当前请求
	 * @param binderFactory 用于创建 {@link WebDataBinder} 实例的工厂
	 * @return 解析后的参数值，如果无法解析则为 {@code null}
	 * @throws Exception 如果准备参数值时出错
	 */
	@Nullable
	Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception;

}
