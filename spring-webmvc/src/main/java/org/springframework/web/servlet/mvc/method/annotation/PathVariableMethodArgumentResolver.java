/*
 * Copyright 2002-2021 the original author or authors.
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

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 解析带有 @{@link PathVariable} 注解的方法参数。
 *
 * <p>@{@link PathVariable} 是一个具名值，从 URI 模板变量中解析得到。
 * 它始终是必需的，且没有可供回退的默认值。有关具名值如何处理的更多信息，
 * 请参见基类
 * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}。
 *
 * <p>如果方法参数类型为 {@link Map}，则使用注解中指定的名称
 * 来解析 URI 变量字符串值。随后假定已注册了合适的 {@link Converter} 或 {@link PropertyEditor}，
 * 该值会通过类型转换转换为 {@link Map}。
 *
 * <p>会调用 {@link WebDataBinder} 对尚不匹配方法参数类型的已解析路径变量值
 * 应用类型转换。
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (!parameter.hasParameterAnnotation(PathVariable.class)) {
			return false;
		}
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
			return (pathVariable != null && StringUtils.hasText(pathVariable.value()));
		}
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		Assert.state(ann != null, "No PathVariable annotation");
		return new PathVariableNamedValueInfo(ann);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingPathVariableException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingPathVariableException(name, parameter, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer, NativeWebRequest request) {

		String key = View.PATH_VARIABLES;
		int scope = RequestAttributes.SCOPE_REQUEST;
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
		if (pathVars == null) {
			pathVars = new HashMap<>();
			request.setAttribute(key, pathVars, scope);
		}
		pathVars.put(name, arg);
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			return;
		}

		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		String name = (ann != null && StringUtils.hasLength(ann.value()) ? ann.value() : parameter.getParameterName());
		String formatted = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
		uriVariables.put(name, formatted);
	}

	@Nullable
	protected String formatUriValue(@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		else if (cs != null) {
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		}
		else {
			return value.toString();
		}
	}


	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
