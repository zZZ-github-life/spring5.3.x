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

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.ui.Model;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于 {@link Model} 类型控制器方法参数的解析器，也可以解析为 {@link java.util.Map}。
 *
 * <p>Map 返回值可以根据 {@code @ModelAttribute} 或 {@code @ResponseBody} 等注解的存在以多种方式解释。从 5.2 开始，如果 {@code Map} 类型的参数还带有注解，此解析器返回 false。
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ModelMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ModelMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkParameterTypeNoReactiveWrapper(param, type ->
				Model.class.isAssignableFrom(type) ||
						(Map.class.isAssignableFrom(type) && param.getParameterAnnotations().length == 0));
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Class<?> type = parameter.getParameterType();
		if (Model.class.isAssignableFrom(type)) {
			return context.getModel();
		}
		else if (Map.class.isAssignableFrom(type)) {
			return context.getModel().asMap();
		}
		else {
			// Should never happen..
			throw new IllegalStateException("Unexpected method parameter type: " + type);
		}
	}

}
