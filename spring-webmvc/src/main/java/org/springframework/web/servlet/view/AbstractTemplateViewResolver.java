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

package org.springframework.web.servlet.view;

/**
 * 模板视图解析器的抽象基类，特别针对 FreeMarker 视图。
 *
 * <p>提供了一种便捷方式来指定 {@link AbstractTemplateView} 对请求属性、
 * 会话属性以及 Spring 宏辅助对象的公开标志。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see AbstractTemplateView
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver
 */
public abstract class AbstractTemplateViewResolver extends UrlBasedViewResolver {

	private boolean exposeRequestAttributes = false;

	private boolean allowRequestOverride = false;

	private boolean exposeSessionAttributes = false;

	private boolean allowSessionOverride = false;

	private boolean exposeSpringMacroHelpers = true;


	@Override
	protected Class<?> requiredViewClass() {
		return AbstractTemplateView.class;
	}

	/**
	 * 设置是否在与模板合并之前将所有请求属性添加到
	 * 模型中。默认为 "false"。
	 * @see AbstractTemplateView#setExposeRequestAttributes
	 */
	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
	 * 设置是否允许 HttpServletRequest 属性覆盖（隐藏）
	 * 控制器生成的同名模型属性。默认为 "false"，
	 * 这会在发现与模型属性同名的请求属性时抛出异常。
	 * @see AbstractTemplateView#setAllowRequestOverride
	 */
	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
	 * 设置是否在与模板合并之前将所有 HttpSession 属性添加到
	 * 模型中。默认为 "false"。
	 * @see AbstractTemplateView#setExposeSessionAttributes
	 */
	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
	 * 设置是否允许 HttpSession 属性覆盖（隐藏）
	 * 控制器生成的同名模型属性。默认为 "false"，
	 * 这会在发现与模型属性同名的会话属性时抛出异常。
	 * @see AbstractTemplateView#setAllowSessionOverride
	 */
	public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	/**
	 * 设置是否以 "springMacroRequestContext" 为名称公开一个 RequestContext 供
	 * Spring 的宏库使用。默认为 "true"。
	 * @see AbstractTemplateView#setExposeSpringMacroHelpers
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractTemplateView view = (AbstractTemplateView) super.buildView(viewName);
		view.setExposeRequestAttributes(this.exposeRequestAttributes);
		view.setAllowRequestOverride(this.allowRequestOverride);
		view.setExposeSessionAttributes(this.exposeSessionAttributes);
		view.setAllowSessionOverride(this.allowSessionOverride);
		view.setExposeSpringMacroHelpers(this.exposeSpringMacroHelpers);
		return view;
	}

}
