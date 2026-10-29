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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 用于解析来自 URI 查询字符串参数、带有 @{@link RequestParam} 注解的方法参数的解析器。
 *
 * <p>此解析器也可以在默认解析模式下创建，此时未带 @{@link RequestParam} 注解的简单类型（int、long 等）也会被视为请求参数，参数名称从参数名派生。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称解析请求参数 String 值。然后假定已注册合适的 {@link Converter}，通过类型转换将该值转换为 {@link Map}。如果未指定请求参数名称，则改为使用 {@link RequestParamMapMethodArgumentResolver}，以映射形式提供对所有请求参数的访问。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	private final boolean useDefaultResolution;


	/**
	 * 带默认解析模式标志的类构造函数。
	 * @param factory a bean factory used for resolving  ${...} placeholder
	 * and #{...} SpEL expressions in default values, or {@code null} if default
	 * values are not expected to contain expressions
	 * @param registry for checking reactive type wrappers
	 * @param useDefaultResolution in default resolution mode a method argument
	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
	 * is treated as a request parameter even if it isn't annotated, the
	 * request parameter name is derived from the method parameter name.
	 */
	public RequestParamMethodArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry, boolean useDefaultResolution) {

		super(factory, registry);
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		if (checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::singleParam)) {
			return true;
		}
		else if (this.useDefaultResolution) {
			return checkParameterTypeNoReactiveWrapper(param, BeanUtils::isSimpleProperty) ||
					BeanUtils.isSimpleProperty(param.nestedIfOptional().getNestedParameterType());
		}
		return false;
	}

	private boolean singleParam(RequestParam requestParam, Class<?> type) {
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(requestParam.name());
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		List<String> paramValues = exchange.getRequest().getQueryParams().get(name);
		Object result = null;
		if (paramValues != null) {
			result = (paramValues.size() == 1 ? paramValues.get(0) : paramValues);
		}
		return result;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Required " + type + " parameter '" + name + "' is not present";
		throw new ServerWebInputException(reason, parameter);
	}


	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
