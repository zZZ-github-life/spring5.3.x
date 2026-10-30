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

package org.springframework.web.method.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;

/**
 * 记录在调用控制器方法过程中，由
 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 和
 * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}
 * 所做出的与模型和视图相关的决策。
 *
 * <p>{@link #setRequestHandled} 标志可用于表示请求已被直接处理，
 * 无需进行视图解析。
 *
 * <p>实例化时会自动创建一个默认的 {@link Model}。在重定向场景中，
 * 可以通过 {@link #setRedirectModel} 提供一个替代的模型实例。
 * 当 {@link #setRedirectModelScenario} 设置为 {@code true} 表示重定向场景时，
 * {@link #getModel()} 将返回重定向模型而不是默认模型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ModelAndViewContainer {

	private boolean ignoreDefaultModelOnRedirect = false;

	@Nullable
	private Object view;

	private final ModelMap defaultModel = new BindingAwareModelMap();

	@Nullable
	private ModelMap redirectModel;

	private boolean redirectModelScenario = false;

	@Nullable
	private HttpStatus status;

	private final Set<String> noBinding = new HashSet<>(4);

	private final Set<String> bindingDisabled = new HashSet<>(4);

	private final SessionStatus sessionStatus = new SimpleSessionStatus();

	private boolean requestHandled = false;


	/**
	 * 默认情况下，“默认”模型的内容既用于渲染场景，也用于重定向场景。
	 * 或者，控制器方法可以声明一个 {@code RedirectAttributes} 类型的参数，
	 * 并通过它提供用于准备重定向 URL 的属性。
	 * <p>将此标志设为 {@code true} 可确保即使在未声明 RedirectAttributes 参数的情况下，
	 * “默认”模型也绝不会在重定向场景中使用。设为 {@code false} 则表示
	 * 如果控制器方法未声明 RedirectAttributes 参数，重定向时可以使用“默认”模型。
	 * <p>默认设置为 {@code false}。
	 */
	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	/**
	 * 设置视图名称，供 DispatcherServlet 通过 ViewResolver 解析。
	 * 将覆盖任何已有的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回供 DispatcherServlet 通过 ViewResolver 解析的视图名称，
	 * 如果已设置 View 对象则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置供 DispatcherServlet 使用的 View 对象。
	 * 将覆盖任何已有的视图名称或 View。
	 */
	public void setView(@Nullable Object view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象；如果使用的是由 DispatcherServlet 通过 ViewResolver 解析的
	 * 视图名称，则返回 {@code null}。
	 */
	@Nullable
	public Object getView() {
		return this.view;
	}

	/**
	 * 视图是否是通过名称指定的视图引用，需由 DispatcherServlet 通过 ViewResolver 解析。
	 */
	public boolean isViewReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回要使用的模型——“默认”模型或“重定向”模型。
	 * 如果 {@code redirectModelScenario=false}，或者没有重定向模型
	 * （即未将 RedirectAttributes 声明为方法参数）且
	 * {@code ignoreDefaultModelOnRedirect=false}，则使用默认模型。
	 */
	public ModelMap getModel() {
		if (useDefaultModel()) {
			return this.defaultModel;
		}
		else {
			if (this.redirectModel == null) {
				this.redirectModel = new ModelMap();
			}
			return this.redirectModel;
		}
	}

	/**
	 * 是使用默认模型还是重定向模型。
	 */
	private boolean useDefaultModel() {
		return (!this.redirectModelScenario || (this.redirectModel == null && !this.ignoreDefaultModelOnRedirect));
	}

	/**
	 * 返回实例化时创建的“默认”模型。
	 * <p>通常建议改用 {@link #getModel()}，它会返回“默认”模型（模板渲染）
	 * 或“重定向”模型（重定向 URL 准备）。在无论如何都需要访问“默认”模型的高级场景中
	 * （例如保存通过 {@code @SessionAttributes} 指定的模型属性），可能需要使用此方法。
	 * @return 默认模型（永不为 {@code null}）
	 * @since 4.1.4
	 */
	public ModelMap getDefaultModel() {
		return this.defaultModel;
	}

	/**
	 * 提供一个单独使用的模型实例，用于重定向场景。
	 * <p>不过，除非将 {@link #setRedirectModelScenario} 设置为 {@code true}
	 * 以表示实际的重定向场景，否则不会使用所提供的附加模型。
	 */
	public void setRedirectModel(ModelMap redirectModel) {
		this.redirectModel = redirectModel;
	}

	/**
	 * 控制器是否返回了重定向指令，例如以 "redirect:" 为前缀的视图名称、
	 * RedirectView 实例等。
	 */
	public void setRedirectModelScenario(boolean redirectModelScenario) {
		this.redirectModelScenario = redirectModelScenario;
	}

	/**
	 * 提供一个 HTTP 状态，该状态将随用于视图渲染目的的
	 * {@code ModelAndView} 一起传递。
	 * @since 4.3
	 */
	public void setStatus(@Nullable HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回已配置的 HTTP 状态（如果有）。
	 * @since 4.3
	 */
	@Nullable
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * 以编程方式注册一个不应进行数据绑定的属性，
	 * 即使后续声明了 {@code @ModelAttribute} 也不例外。
	 * @param attributeName 属性的名称
	 * @since 4.3
	 */
	public void setBindingDisabled(String attributeName) {
		this.bindingDisabled.add(attributeName);
	}

	/**
	 * 给定模型属性的绑定是否被禁用。
	 * @since 4.3
	 */
	public boolean isBindingDisabled(String name) {
		return (this.bindingDisabled.contains(name) || this.noBinding.contains(name));
	}

	/**
	 * 注册对应的模型属性是否应进行数据绑定，
	 * 对应于 {@code @ModelAttribute(binding=true/false)} 声明。
	 * <p>注意：虽然 {@link #isBindingDisabled} 会考虑此标志，
	 * 但 {@link #setBindingDisabled} 的强制声明始终会覆盖它。
	 * @param attributeName 属性的名称
	 * @since 4.3.13
	 */
	public void setBinding(String attributeName, boolean enabled) {
		if (!enabled) {
			this.noBinding.add(attributeName);
		}
		else {
			this.noBinding.remove(attributeName);
		}
	}

	/**
	 * 返回要使用的 {@link SessionStatus} 实例，可用于表示会话处理已完成。
	 */
	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}

	/**
	 * 请求是否已在处理器内完全处理（例如 {@code @ResponseBody} 方法），
	 * 因此无需进行视图解析。当控制器方法声明了
	 * {@code ServletResponse} 或 {@code OutputStream} 类型的参数时，也可以设置此标志。
	 * <p>默认值为 {@code false}。
	 */
	public void setRequestHandled(boolean requestHandled) {
		this.requestHandled = requestHandled;
	}

	/**
	 * 请求是否已在处理器内完全处理。
	 */
	public boolean isRequestHandled() {
		return this.requestHandled;
	}

	/**
	 * 将提供的属性添加到基础模型中。
	 * 是 {@code getModel().addAttribute(String, Object)} 的快捷方式。
	 */
	public ModelAndViewContainer addAttribute(String name, @Nullable Object value) {
		getModel().addAttribute(name, value);
		return this;
	}

	/**
	 * 将提供的属性添加到基础模型中。
	 * 是 {@code getModel().addAttribute(Object)} 的快捷方式。
	 */
	public ModelAndViewContainer addAttribute(Object value) {
		getModel().addAttribute(value);
		return this;
	}

	/**
	 * 将所有属性复制到基础模型中。
	 * 是 {@code getModel().addAllAttributes(Map)} 的快捷方式。
	 */
	public ModelAndViewContainer addAllAttributes(@Nullable Map<String, ?> attributes) {
		getModel().addAllAttributes(attributes);
		return this;
	}

	/**
	 * 复制所提供 {@code Map} 中的属性，同名已有对象优先（即不会被替换）。
	 * 是 {@code getModel().mergeAttributes(Map<String, ?>)} 的快捷方式。
	 */
	public ModelAndViewContainer mergeAttributes(@Nullable Map<String, ?> attributes) {
		getModel().mergeAttributes(attributes);
		return this;
	}

	/**
	 * 从模型中移除给定的属性。
	 */
	public ModelAndViewContainer removeAttributes(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				getModel().remove(key);
			}
		}
		return this;
	}

	/**
	 * 基础模型是否包含给定的属性名称。
	 * 是 {@code getModel().containsAttribute(String)} 的快捷方式。
	 */
	public boolean containsAttribute(String name) {
		return getModel().containsAttribute(name);
	}


	/**
	 * 返回诊断信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
		if (!isRequestHandled()) {
			if (isViewReference()) {
				sb.append("reference to view with name '").append(this.view).append('\'');
			}
			else {
				sb.append("View is [").append(this.view).append(']');
			}
			if (useDefaultModel()) {
				sb.append("; default model ");
			}
			else {
				sb.append("; redirect model ");
			}
			sb.append(getModel());
		}
		else {
			sb.append("Request handled directly");
		}
		return sb.toString();
	}

}
