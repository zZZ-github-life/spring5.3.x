/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.support.RequestContext;

/**
 * 基于模板的视图技术（如 FreeMarker）的适配器基类，
 * 支持在其模型中使用请求属性和会话属性，
 * 并可选择为 Spring 的 FreeMarker 宏库公开辅助对象。
 *
 * <p>JSP/JSTL 及其他视图技术自动拥有对
 * HttpServletRequest 对象的访问能力，从而可访问当前用户的
 * 请求/会话属性。此外，它们还能够自行创建并缓存
 * 作为请求属性的辅助对象。
 *
 * @author Juergen Hoeller
 * @author Darren Davison
 * @since 1.0.2
 * @see AbstractTemplateViewResolver
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView
 */
public abstract class AbstractTemplateView extends AbstractUrlBasedView {

	/**
	 * 模板模型中 RequestContext 实例的变量名称，
	 * 可供 Spring 的宏使用：例如用于创建 BindStatus 对象。
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";


	private boolean exposeRequestAttributes = false;

	private boolean allowRequestOverride = false;

	private boolean exposeSessionAttributes = false;

	private boolean allowSessionOverride = false;

	private boolean exposeSpringMacroHelpers = true;


	/**
	 * 设置是否在与模板合并之前将所有请求属性添加到
	 * 模型中。默认为 "false"。
	 * <p>请注意，某些模板可能会自行使请求属性可见，
	 * 例如 FreeMarker，而无需在 MVC 模型中公开它们。
	 */
	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
	 * 设置是否允许 HttpServletRequest 属性覆盖（隐藏）
	 * 控制器生成的同名模型属性。默认为 "false"，
	 * 这会在发现与模型属性同名的请求属性时抛出异常。
	 */
	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
	 * 设置是否在与模板合并之前将所有 HttpSession 属性添加到
	 * 模型中。默认为 "false"。
	 */
	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
	 * 设置是否允许 HttpSession 属性覆盖（隐藏）
	 * 控制器生成的同名模型属性。默认为 "false"，
	 * 这会在发现与模型属性同名的会话属性时抛出异常。
	 */
	public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	/**
	 * 设置是否以 "springMacroRequestContext" 为名称公开一个 RequestContext 供
	 * Spring 的宏库使用。默认为 "true"。
	 * <p>目前 Spring 的 FreeMarker 默认宏需要此项。
	 * 请注意，对于使用 HTML 表单的模板，这<i>并非</i>必需，
	 * <i>除非</i>你希望利用 Spring 的辅助宏。
	 * @see #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (this.exposeRequestAttributes) {
			Map<String, Object> exposed = null;
			for (Enumeration<String> en = request.getAttributeNames(); en.hasMoreElements();) {
				String attribute = en.nextElement();
				if (model.containsKey(attribute) && !this.allowRequestOverride) {
					throw new ServletException("Cannot expose request attribute '" + attribute +
							"' because of an existing model object of the same name");
				}
				Object attributeValue = request.getAttribute(attribute);
				if (logger.isDebugEnabled()) {
					exposed = (exposed != null ? exposed : new LinkedHashMap<>());
					exposed.put(attribute, attributeValue);
				}
				model.put(attribute, attributeValue);
			}
			if (logger.isTraceEnabled() && exposed != null) {
				logger.trace("Exposed request attributes to model: " + exposed);
			}
		}

		if (this.exposeSessionAttributes) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Map<String, Object> exposed = null;
				for (Enumeration<String> en = session.getAttributeNames(); en.hasMoreElements();) {
					String attribute = en.nextElement();
					if (model.containsKey(attribute) && !this.allowSessionOverride) {
						throw new ServletException("Cannot expose session attribute '" + attribute +
								"' because of an existing model object of the same name");
					}
					Object attributeValue = session.getAttribute(attribute);
					if (logger.isDebugEnabled()) {
						exposed = (exposed != null ? exposed : new LinkedHashMap<>());
						exposed.put(attribute, attributeValue);
					}
					model.put(attribute, attributeValue);
				}
				if (logger.isTraceEnabled() && exposed != null) {
					logger.trace("Exposed session attributes to model: " + exposed);
				}
			}
		}

		if (this.exposeSpringMacroHelpers) {
			if (model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new ServletException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
						"' because of an existing model object of the same name");
			}
			// Expose RequestContext instance for Spring macros.
			model.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE,
					new RequestContext(request, response, getServletContext(), model));
		}

		applyContentType(response);

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering [" + getUrl() + "]");
		}

		renderMergedTemplateModel(model, request, response);
	}

	/**
	 * 将此视图在 "contentType" bean 属性中指定的内容类型
	 * 应用到给定的响应。
	 * <p>仅在响应尚未设置内容类型时才应用视图的 contentType。
	 * 这允许处理器事先覆盖默认的内容类型。
	 * @param response 当前 HTTP 响应
	 * @see #setContentType
	 */
	protected void applyContentType(HttpServletResponse response)	{
		if (response.getContentType() == null) {
			response.setContentType(getContentType());
		}
	}

	/**
	 * 子类必须实现此方法以实际渲染视图。
	 * @param model 合并后的输出 Map，必要时已合并请求属性和会话属性
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
