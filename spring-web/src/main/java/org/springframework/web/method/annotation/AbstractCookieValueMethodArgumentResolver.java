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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于解析标注了 {@code @CookieValue} 的方法参数的基础抽象类。
 * 子类负责从请求中提取 Cookie 值。
 *
 * <p>{@code @CookieValue} 是一种命名值，从 Cookie 中解析得到。
 * 它具有 required 标志，以及当 Cookie 不存在时可回退使用的默认值。
 *
 * <p>可以调用 {@link WebDataBinder} 对解析出的 Cookie 值应用类型转换。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractCookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * 创建一个新的 {@link AbstractCookieValueMethodArgumentResolver} 实例。
	 * @param beanFactory 用于解析默认值中的 ${...} 占位符
	 * 和 #{...} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计不包含表达式，则为 {@code null}
	 */
	public AbstractCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CookieValue.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
		Assert.state(annotation != null, "No CookieValue annotation");
		return new CookieValueNamedValueInfo(annotation);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingRequestCookieException(name, parameter);
	}

	@Override
	protected void handleMissingValueAfterConversion(
			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		throw new MissingRequestCookieException(name, parameter, true);
	}

	private static final class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
