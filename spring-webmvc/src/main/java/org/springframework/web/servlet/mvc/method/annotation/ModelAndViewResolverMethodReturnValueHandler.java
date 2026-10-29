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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * 此返回值处理器应当排在其他所有处理器之后，因为它
 * 尝试处理_任意_返回值类型（即对所有返回类型
 * 都返回 {@code true}）。
 *
 * <p>返回值要么通过 {@link ModelAndViewResolver} 处理，
 * 要么在它是非简单类型时被视为模型属性。
 * 如果这两种方式都不成功（本质上即除 String 之外的简单类型），
 * 则抛出 {@link UnsupportedOperationException}。
 *
 * <p><strong>注意：</strong>此类主要是为了支持
 * {@link ModelAndViewResolver}，遗憾的是后者无法被恰当地
 * 适配到 {@link HandlerMethodReturnValueHandler} 约定，因为
 * {@link HandlerMethodReturnValueHandler#supportsReturnType} 方法
 * 无法实现。因此，{@code ModelAndViewResolver} 只能在所有其他返回值
 * 处理器都获得机会之后，最后被调用。建议将
 * {@code ModelAndViewResolver} 重新实现为 {@code HandlerMethodReturnValueHandler}，
 * 后者还能更好地访问返回类型和方法信息。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewResolverMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Nullable
	private final List<ModelAndViewResolver> mavResolvers;

	private final ModelAttributeMethodProcessor modelAttributeProcessor =
			new ServletModelAttributeMethodProcessor(true);


	/**
	 * 创建一个新实例。
	 */
	public ModelAndViewResolverMethodReturnValueHandler(@Nullable List<ModelAndViewResolver> mavResolvers) {
		this.mavResolvers = mavResolvers;
	}


	/**
	 * 始终返回 {@code true}。请参见类级说明。
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (this.mavResolvers != null) {
			for (ModelAndViewResolver mavResolver : this.mavResolvers) {
				Class<?> handlerType = returnType.getContainingClass();
				Method method = returnType.getMethod();
				Assert.state(method != null, "No handler method");
				ExtendedModelMap model = (ExtendedModelMap) mavContainer.getModel();
				ModelAndView mav = mavResolver.resolveModelAndView(method, handlerType, returnValue, model, webRequest);
				if (mav != ModelAndViewResolver.UNRESOLVED) {
					mavContainer.addAllAttributes(mav.getModel());
					mavContainer.setViewName(mav.getViewName());
					if (!mav.isReference()) {
						mavContainer.setView(mav.getView());
					}
					return;
				}
			}
		}

		// No suitable ModelAndViewResolver...
		if (this.modelAttributeProcessor.supportsReturnType(returnType)) {
			this.modelAttributeProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
		}
		else {
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
