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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;

/**
 * 处理 {@code void} 和 {@code String} 类型的返回值，将其
 * 解释为视图名称引用。从 4.2 开始，它还会将通用的 {@code CharSequence}
 * 类型（例如 {@code StringBuilder} 或 Groovy 的 {@code GString}）作为视图名称处理。
 *
 * <p>由于返回类型为 {@code void} 或实际返回值本身为
 * {@code null} 时，返回值会被原样保留，从而允许已配置的
 * {@link RequestToViewNameTranslator} 按约定选择视图名称。
 *
 * <p>String 返回值可以有多种解释方式，具体取决于
 * 是否存在 {@code @ModelAttribute} 或 {@code @ResponseBody} 等注解。
 * 因此，此处理器应配置在支持这些注解的处理器之后。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Nullable
	private String[] redirectPatterns;


	/**
	 * 配置一个或多个简单模式（如 {@link PatternMatchUtils#simpleMatch} 所述），
	 * 用于在 "redirect:" 之外识别自定义的重定向前缀。
	 * <p>请注意，仅配置此属性并不会使自定义重定向前缀生效。
	 * 还必须有一个能够识别该前缀的自定义 View。
	 * @since 4.1
	 */
	public void setRedirectPatterns(@Nullable String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 已配置的重定向模式（如果有）。
	 */
	@Nullable
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue instanceof CharSequence) {
			String viewName = returnValue.toString();
			mavContainer.setViewName(viewName);
			if (isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else if (returnValue != null) {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

	/**
	 * 给定的视图名称是否为重定向视图引用。
	 * 默认实现会检查已配置的重定向模式，
	 * 以及视图名称是否以 "redirect:" 前缀开头。
	 * @param viewName 要检查的视图名称，永不为 {@code null}
	 * @return 如果给定的视图名称被识别为重定向视图引用，
	 * 则返回 “true”；否则返回 “false”。
	 */
	protected boolean isRedirectViewName(String viewName) {
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
