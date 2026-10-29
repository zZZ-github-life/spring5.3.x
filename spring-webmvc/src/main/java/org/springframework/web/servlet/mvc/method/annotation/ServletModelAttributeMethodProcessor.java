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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Servlet 专用的 {@link ModelAttributeMethodProcessor}，通过
 * {@link ServletRequestDataBinder} 类型的 WebDataBinder 应用数据绑定。
 *
 * <p>还添加了回退策略：如果名称与模型属性名称匹配
 * 且存在合适的类型转换策略，则从 URI 模板变量或
 * 请求参数实例化模型属性。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {

	/**
	 * 类构造函数。
	 * @param annotationNotRequired if "true", non-simple method arguments and
	 * return values are considered model attributes with or without a
	 * {@code @ModelAttribute} annotation
	 */
	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
		super(annotationNotRequired);
	}


	/**
	 * 如果名称与模型属性名称匹配且存在合适的类型转换策略，
	 * 则从 URI 模板变量或请求参数实例化模型属性。
	 * 如果这些条件都不满足，则委托回基类。
	 * @see #createAttributeFromRequestValue
	 */
	@Override
	protected final Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest request) throws Exception {

		String value = getRequestValueForAttribute(attributeName, request);
		if (value != null) {
			Object attribute = createAttributeFromRequestValue(
					value, attributeName, parameter, binderFactory, request);
			if (attribute != null) {
				return attribute;
			}
		}

		return super.createAttribute(attributeName, parameter, binderFactory, request);
	}

	/**
	 * 从请求中获取可用于通过从 String 到目标类型的类型转换
	 * 来实例化模型属性的值。
	 * <p>默认实现首先查找与 URI 变量匹配的属性名称，
	 * 然后查找请求参数。
	 * @param attributeName the model attribute name
	 * @param request the current request
	 * @return the request value to try to convert, or {@code null} if none
	 */
	@Nullable
	protected String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
		Map<String, String> variables = getUriTemplateVariables(request);
		String variableValue = variables.get(attributeName);
		if (StringUtils.hasText(variableValue)) {
			return variableValue;
		}
		String parameterValue = request.getParameter(attributeName);
		if (StringUtils.hasText(parameterValue)) {
			return parameterValue;
		}
		return null;
	}

	protected final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, String> variables = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (variables != null ? variables : Collections.emptyMap());
	}

	/**
	 * 使用类型转换从 String 请求值（例如 URI 模板
	 * 变量、请求参数）创建模型属性。
	 * <p>仅当存在可执行转换的已注册 {@link Converter} 时，
	 * 默认实现才会进行转换。
	 * @param sourceValue the source value to create the model attribute from
	 * @param attributeName the name of the attribute (never {@code null})
	 * @param parameter the method parameter
	 * @param binderFactory for creating WebDataBinder instance
	 * @param request the current request
	 * @return the created model attribute, or {@code null} if no suitable
	 * conversion found
	 */
	@Nullable
	protected Object createAttributeFromRequestValue(String sourceValue, String attributeName,
			MethodParameter parameter, WebDataBinderFactory binderFactory, NativeWebRequest request)
			throws Exception {

		DataBinder binder = binderFactory.createBinder(request, null, attributeName);
		ConversionService conversionService = binder.getConversionService();
		if (conversionService != null) {
			TypeDescriptor source = TypeDescriptor.valueOf(String.class);
			TypeDescriptor target = new TypeDescriptor(parameter);
			if (conversionService.canConvert(source, target)) {
				return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
			}
		}
		return null;
	}

	/**
	 * 此实现在绑定之前将 {@link WebDataBinder} 向下转型为
	 * {@link ServletRequestDataBinder}。
	 * @see ServletRequestDataBinderFactory
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		Assert.state(servletRequest != null, "No ServletRequest");
		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
		servletBinder.bind(servletRequest);
	}

	@Override
	@Nullable
	public Object resolveConstructorArgument(String paramName, Class<?> paramType, NativeWebRequest request)
			throws Exception {

		Object value = super.resolveConstructorArgument(paramName, paramType, request);
		if (value != null) {
			return value;
		}
		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
		if (servletRequest != null) {
			String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
			@SuppressWarnings("unchecked")
			Map<String, String> uriVars = (Map<String, String>) servletRequest.getAttribute(attr);
			return uriVars.get(paramName);
		}
		return null;
	}

}
