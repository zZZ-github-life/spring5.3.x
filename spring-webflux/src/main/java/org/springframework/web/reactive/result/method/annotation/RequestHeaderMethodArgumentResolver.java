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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 解析带有 {@code @RequestHeader} 注解的方法参数，但 {@link Map} 参数除外。有关带有 {@code @RequestHeader} 注解的 {@link Map} 参数的详细信息，请参见 {@link RequestHeaderMapMethodArgumentResolver}。
 *
 * <p>{@code @RequestHeader} 是从请求头解析的命名值。它具有必需标志和默认值，在请求头不存在时作为回退。
 *
 * <p>会调用 {@link ConversionService} 对尚不匹配方法参数类型的已解析请求头值应用类型转换。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestHeaderMapMethodArgumentResolver
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建新的 {@link RequestHeaderMethodArgumentResolver} 实例。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符
	 * 和 {@code #{...}} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计没有表达式则为 {@code null}
	 * @param registry 用于检查响应式类型包装
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestHeader.class, this::singleParam);
	}

	private boolean singleParam(RequestHeader annotation, Class<?> type) {
		return !Map.class.isAssignableFrom(type);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		return new RequestHeaderNamedValueInfo(ann);
	}

	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		List<String> headerValues = exchange.getRequest().getHeaders().get(name);
		Object result = null;
		if (headerValues != null) {
			result = (headerValues.size() == 1 ? headerValues.get(0) : headerValues);
		}
		return result;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing request header '" + name + "' " +
				"for method parameter of type " + type, parameter);
	}


	private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
