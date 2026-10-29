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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * 处理 {@link ModelAndView} 类型的返回值，将视图和模型
 * 信息复制到 {@link ModelAndViewContainer} 中。
 *
 * <p>如果返回值为 {@code null}，则将
 * {@link ModelAndViewContainer#setRequestHandled(boolean)} 标志设为
 * {@code true}，以表示请求已被直接处理。
 *
 * <p>{@link ModelAndView} 返回类型有明确的用途。因此，此
 * 处理器应配置在支持带有 {@code @ModelAttribute} 或 {@code @ResponseBody}
 * 注解的任意返回值类型的处理器之前，
 * 以确保它们不会越俎代庖。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Nullable
	private String[] redirectPatterns;


	/**
	 * 配置一个或多个简单模式（如 {@link PatternMatchUtils#simpleMatch} 所述），
	 * 用于在 "redirect:" 之外识别自定义的重定向前缀。
	 * <p>请注意，仅配置此属性并不会使自定义重定向前缀生效。
	 * 还必须有一个能够识别该前缀的自定义 {@link View}。
	 * @since 4.1
	 */
	public void setRedirectPatterns(@Nullable String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 返回已配置的重定向模式（如果有）。
	 * @since 4.1
	 */
	@Nullable
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ModelAndView.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		ModelAndView mav = (ModelAndView) returnValue;
		if (mav.isReference()) {
			String viewName = mav.getViewName();
			mavContainer.setViewName(viewName);
			if (viewName != null && isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else {
			View view = mav.getView();
			mavContainer.setView(view);
			if (view instanceof SmartView && ((SmartView) view).isRedirectView()) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		mavContainer.setStatus(mav.getStatus());
		mavContainer.addAllAttributes(mav.getModel());
	}

	/**
	 * 给定的视图名称是否为重定向视图引用。
	 * 默认实现会检查已配置的重定向模式，
	 * 以及视图名称是否以 "redirect:" 前缀开头。
	 * @param viewName the view name to check, never {@code null}
	 * @return "true" if the given view name is recognized as a redirect view
	 * reference; "false" otherwise.
	 */
	protected boolean isRedirectViewName(String viewName) {
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
