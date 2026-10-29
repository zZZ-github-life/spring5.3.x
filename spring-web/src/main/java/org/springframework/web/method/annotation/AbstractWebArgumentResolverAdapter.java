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

package org.springframework.web.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 一个抽象基类，用于将 {@link WebArgumentResolver} 适配到
 * {@link HandlerMethodArgumentResolver} 契约。
 *
 * <p><strong>注意：</strong>提供此类是为了向后兼容。
 * 不过，建议将 {@code WebArgumentResolver} 重写为
 * {@code HandlerMethodArgumentResolver}。由于 {@link #supportsParameter}
 * 只能通过实际解析值并检查结果是否为 {@code WebArgumentResolver#UNRESOLVED} 来实现，
 * 因此抛出的任何异常都必须被吞掉并忽略，因为无法确定是适配器不支持该参数，
 * 还是由于内部原因而失败。
 * {@code HandlerMethodArgumentResolver} 契约还提供了对模型属性以及
 * {@code WebDataBinderFactory}（用于类型转换）的访问。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractWebArgumentResolverAdapter implements HandlerMethodArgumentResolver {

	private final Log logger = LogFactory.getLog(getClass());

	private final WebArgumentResolver adaptee;


	/**
	 * 创建一个新的实例。
	 */
	public AbstractWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		Assert.notNull(adaptee, "'adaptee' must not be null");
		this.adaptee = adaptee;
	}


	/**
	 * 实际解析值，并检查解析结果不是
	 * {@link WebArgumentResolver#UNRESOLVED}，同时吞掉<em>任何</em>异常。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		try {
			NativeWebRequest webRequest = getWebRequest();
			Object result = this.adaptee.resolveArgument(parameter, webRequest);
			if (result == WebArgumentResolver.UNRESOLVED) {
				return false;
			}
			else {
				return ClassUtils.isAssignableValue(parameter.getParameterType(), result);
			}
		}
		catch (Exception ex) {
			// ignore (see class-level doc)
			if (logger.isDebugEnabled()) {
				logger.debug("Error in checking support for parameter [" + parameter + "]: " + ex.getMessage());
			}
			return false;
		}
	}

	/**
	 * 委托给 {@link WebArgumentResolver} 实例。
	 * @throws IllegalStateException if the resolved value is not assignable
	 * to the method parameter.
	 */
	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();
		Object result = this.adaptee.resolveArgument(parameter, webRequest);
		if (result == WebArgumentResolver.UNRESOLVED || !ClassUtils.isAssignableValue(paramType, result)) {
			throw new IllegalStateException(
					"Standard argument type [" + paramType.getName() + "] in method " + parameter.getMethod() +
					"resolved to incompatible value of type [" + (result != null ? result.getClass() : null) +
					"]. Consider declaring the argument type in a less specific fashion.");
		}
		return result;
	}


	/**
	 * 在 {@link #supportsParameter} 中访问 NativeWebRequest 所必需。
	 */
	protected abstract NativeWebRequest getWebRequest();

}
