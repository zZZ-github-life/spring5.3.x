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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 简单的控制器，始终返回预先配置的视图，并可选地设置响应状态码。
 * 视图和状态可通过提供的配置属性进行配置。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rossen Stoyanchev
 */
public class ParameterizableViewController extends AbstractController {

	@Nullable
	private Object view;

	@Nullable
	private HttpStatus statusCode;

	private boolean statusOnly;


	public ParameterizableViewController() {
		super(false);
		setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}

	/**
	 * 设置要由 ModelAndView 返回的视图名称，将由 DispatcherServlet
	 * 通过 ViewResolver 解析。将覆盖任何已存在的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要委托的视图名称；如果使用的是 View 实例，则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		if (this.view instanceof String) {
			String viewName = (String) this.view;
			if (getStatusCode() != null && getStatusCode().is3xxRedirection()) {
				return viewName.startsWith("redirect:") ? viewName : "redirect:" + viewName;
			}
			else {
				return viewName;
			}
		}
		return null;
	}

	/**
	 * 设置要由 ModelAndView 返回的 View 对象。
	 * 将覆盖任何已存在的视图名称或 View。
	 * @since 4.1
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象；如果我们使用的是由 DispatcherServlet 通过
	 * ViewResolver 解析的视图名称，则返回 {@code null}。
	 * @since 4.1
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 配置此控制器应在响应上设置的 HTTP 状态码。
	 * <p>当配置了以 "redirect:" 为前缀的视图名称时，无需设置此属性，
	 * 因为 RedirectView 会完成该操作。不过此属性仍可用于覆盖
	 * {@code RedirectView} 的 3xx 状态码。若需完全控制重定向，
	 * 请提供 {@code RedirectView} 实例。
	 * <p>如果状态码为 204 且未配置视图，则请求将完全在控制器内部处理。
	 * @since 4.1
	 */
	public void setStatusCode(@Nullable HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 返回配置的 HTTP 状态码，或 {@code null}。
	 * @since 4.1
	 */
	@Nullable
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}


	/**
	 * 该属性可用于指示请求被视为已在控制器内部完全处理，
	 * 不应使用任何视图进行渲染。与 {@link #setStatusCode} 结合使用时很有用。
	 * <p>默认值为 {@code false}。
	 * @since 4.1
	 */
	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	/**
	 * 请求是否在控制器内部完全处理。
	 */
	public boolean isStatusOnly() {
		return this.statusOnly;
	}


	/**
	 * 返回带有指定视图名称的 ModelAndView 对象。
	 * <p>{@link RequestContextUtils#getInputFlashMap
	 * "input" FlashMap} 的内容也会添加到模型中。
	 * @see #getViewName()
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String viewName = getViewName();

		if (getStatusCode() != null) {
			if (getStatusCode().is3xxRedirection()) {
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, getStatusCode());
			}
			else {
				response.setStatus(getStatusCode().value());
				if (getStatusCode().equals(HttpStatus.NO_CONTENT) && viewName == null) {
					return null;
				}
			}
		}

		if (isStatusOnly()) {
			return null;
		}

		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(RequestContextUtils.getInputFlashMap(request));
		if (viewName != null) {
			modelAndView.setViewName(viewName);
		}
		else {
			modelAndView.setView(getView());
		}
		return modelAndView;
	}

	@Override
	public String toString() {
		return "ParameterizableViewController [" + formatStatusAndView() + "]";
	}

	private String formatStatusAndView() {
		StringBuilder sb = new StringBuilder();
		if (this.statusCode != null) {
			sb.append("status=").append(this.statusCode);
		}
		if (this.view != null) {
			sb.append(sb.length() != 0 ? ", " : "");
			String viewName = getViewName();
			sb.append("view=").append(viewName != null ? "\"" + viewName + "\"" : this.view);
		}
		return sb.toString();
	}
}
