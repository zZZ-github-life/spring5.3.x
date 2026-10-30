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

package org.springframework.web.method.annotation;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析标注了 {@code @RequestHeader} 的方法参数，{@link Map} 参数除外。
 * 关于标注了 {@code @RequestHeader} 的 {@link Map} 参数，请参阅
 * {@link RequestHeaderMapMethodArgumentResolver}。
 *
 * <p>{@code @RequestHeader} 是一种命名值，从请求头中解析得到。
 * 它具有 required 标志，以及当请求头不存在时可回退使用的默认值。
 *
 * <p>会调用 {@link WebDataBinder} 对解析出的、尚不匹配方法参数类型的
 * 请求头值应用类型转换。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link RequestHeaderMethodArgumentResolver} 实例。
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符
	 * 和 #{...} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计不包含表达式，则为 {@code null}
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				!Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		return new RequestHeaderNamedValueInfo(ann);
	}

	@Override
	@Nullable
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		String[] headerValues = request.getHeaderValues(name);
		if (headerValues != null) {
			return (headerValues.length == 1 ? headerValues[0] : headerValues);
		}
		else {
			return null;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingRequestHeaderException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingRequestHeaderException(name, parameter, true);
	}

	private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
