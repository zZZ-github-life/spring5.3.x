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

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析带有 @{@link PathVariable} 注解的方法参数。
 *
 * <p>@{@link PathVariable} 是从 URI 模板变量解析的命名值。它始终是必需的，并且没有可回退的默认值。有关命名值处理方式的更多信息，请参见基类 {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称解析 URI 变量 String 值。然后假定存在合适的 {@link Converter}，通过类型转换将该值转换为 {@link Map}。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 * @see PathVariableMapMethodArgumentResolver
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * 创建新的 {@link PathVariableMethodArgumentResolver}。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符
	 * 和 {@code #{...}} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计不包含表达式则为 {@code null}
	 * @param registry 用于检查响应式类型包装
	 */
	public PathVariableMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkAnnotatedParamNoReactiveWrapper(parameter, PathVariable.class, this::singlePathVariable);
	}

	private boolean singlePathVariable(PathVariable pathVariable, Class<?> type) {
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(pathVariable.name());
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		Assert.state(ann != null, "No PathVariable annotation");
		return new PathVariableNamedValueInfo(ann);
	}

	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		String attributeName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(attributeName, Collections.emptyMap()).get(name);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new ServerErrorException(name, parameter, null);
	}

	@Override
	protected void handleResolvedValue(
			@Nullable Object arg, String name, MethodParameter parameter, Model model, ServerWebExchange exchange) {

		// TODO: View.PATH_VARIABLES ?
	}


	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
