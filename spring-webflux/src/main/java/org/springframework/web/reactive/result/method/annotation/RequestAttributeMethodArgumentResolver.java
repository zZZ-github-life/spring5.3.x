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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 解析带有 @{@link RequestAttribute} 注解的方法参数。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see SessionAttributeMethodArgumentResolver
 */
public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建新的 {@link RequestAttributeMethodArgumentResolver} 实例。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符
	 * 和 {@code #{...}} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计没有表达式则为 {@code null}
	 * @param registry 用于检查响应式类型包装
	 */
	public RequestAttributeMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return param.hasParameterAnnotation(RequestAttribute.class);
	}


	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute ann = parameter.getParameterAnnotation(RequestAttribute.class);
		Assert.state(ann != null, "No RequestAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		Object value = exchange.getAttribute(name);
		ReactiveAdapter toAdapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		if (toAdapter != null) {
			if (value == null) {
				Assert.isTrue(toAdapter.supportsEmpty(),
						() -> "No request attribute '" + name + "' and target type " +
								parameter.getGenericParameterType() + " doesn't support empty values.");
				return toAdapter.fromPublisher(Mono.empty());
			}
			if (parameter.getParameterType().isAssignableFrom(value.getClass())) {
				return value;
			}
			ReactiveAdapter fromAdapter = getAdapterRegistry().getAdapter(value.getClass());
			Assert.isTrue(fromAdapter != null,
					() -> getClass().getSimpleName() + " doesn't support " +
							"reactive type wrapper: " + parameter.getGenericParameterType());
			return toAdapter.fromPublisher(fromAdapter.toPublisher(value));
		}
		return value;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing request attribute '" + name + "' of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}

}
