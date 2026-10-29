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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析带有 {@link MatrixVariable @MatrixVariable} 注解且注解未指定名称的 {@link Map} 类型参数。换句话说，此解析器的目的是提供对多个矩阵变量的访问，可以是全部矩阵变量，也可以是与特定路径变量关联的矩阵变量。
 *
 * <p>当指定名称时，Map 类型参数会被视为具有 Map 值的单个属性，改由 {@link MatrixVariableMethodArgumentResolver} 解析。
 * @author Rossen Stoyanchev
 * @since 5.0.1
 * @see MatrixVariableMethodArgumentResolver
 */
public class MatrixVariableMapMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public MatrixVariableMapMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		super(registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkAnnotatedParamNoReactiveWrapper(parameter, MatrixVariable.class,
				(ann, type) -> (Map.class.isAssignableFrom(type) && !StringUtils.hasText(ann.name())));
	}

	@Nullable
	@Override
	public Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		Map<String, MultiValueMap<String, String>> matrixVariables =
				exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(annotation != null, "No MatrixVariable annotation");
		String pathVariable = annotation.pathVar();

		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			if (mapForPathVariable == null) {
				return Collections.emptyMap();
			}
			map.putAll(mapForPathVariable);
		}
		else {
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				vars.forEach((name, values) -> {
					for (String value : values) {
						map.add(name, value);
					}
				});
			}
		}

		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}

	private boolean isSingleValueMap(MethodParameter parameter) {
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			if (genericTypes.length == 2) {
				return !List.class.isAssignableFrom(genericTypes[1].toClass());
			}
		}
		return false;
	}

}
