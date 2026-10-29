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

package org.springframework.web.servlet.mvc.method;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy;

/**
 * 用于基于 {@code RequestMappingInfo} 的处理器方法映射的
 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
 * HandlerMethodMappingNamingStrategy}。
 *
 * 如果设置了 {@code RequestMappingInfo} 的 name 属性，则使用其值。
 * 否则，名称由类名中的大写字母组成，
 * 后跟作为分隔符的 "#"，以及方法名。例如，对于名为 TestController
 * 且方法为 getFoo 的类，名称为 "TC#getFoo"。
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategy
		implements HandlerMethodMappingNamingStrategy<RequestMappingInfo> {

	/** HandlerMethod 映射名称中类型级别部分与方法级别部分之间的分隔符。 */
	public static final String SEPARATOR = "#";


	@Override
	public String getName(HandlerMethod handlerMethod, RequestMappingInfo mapping) {
		if (mapping.getName() != null) {
			return mapping.getName();
		}
		StringBuilder sb = new StringBuilder();
		String simpleTypeName = handlerMethod.getBeanType().getSimpleName();
		for (int i = 0; i < simpleTypeName.length(); i++) {
			if (Character.isUpperCase(simpleTypeName.charAt(i))) {
				sb.append(simpleTypeName.charAt(i));
			}
		}
		sb.append(SEPARATOR).append(handlerMethod.getMethod().getName());
		return sb.toString();
	}

}
