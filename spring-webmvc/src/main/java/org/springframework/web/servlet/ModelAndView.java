/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

/**
 * Web MVC 框架中 Model 和 View 的持有者。
 * 请注意，二者是完全不同的概念。本类只是同时持有它们，
 * 使控制器能够在单个返回值中同时返回模型和视图。
 *
 * <p>表示处理器返回的模型和视图，由 DispatcherServlet 进行解析。
 * 视图可以是 String 类型的视图名称，需要由 ViewResolver 对象解析；
 * 也可以直接指定 View 对象。模型是一个 Map，允许使用按名称索引的多个对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @see DispatcherServlet
 * @see ViewResolver
 * @see HandlerAdapter#handle
 * @see org.springframework.web.servlet.mvc.Controller#handleRequest
 */
public class ModelAndView {

	/** View 实例或视图名称字符串。 */
	@Nullable
	private Object view;

	/** 模型 Map。 */
	@Nullable
	private ModelMap model;

	/** 可选的响应 HTTP 状态。 */
	@Nullable
	private HttpStatus status;

	/** 指示此实例是否已通过调用 {@link #clear()} 被清空。 */
	private boolean cleared = false;


	/**
	 * 用于 bean 风格用法的默认构造函数：通过填充 bean 属性
	 * 而不是传入构造参数来构建对象。
	 * @see #setView(View)
	 * @see #setViewName(String)
	 */
	public ModelAndView() {
	}

	/**
	 * 当没有模型数据需要暴露时使用的便捷构造函数。
	 * 也可以与 {@code addObject} 配合使用。
	 * @param viewName name of the View to render, to be resolved
	 * by the DispatcherServlet's ViewResolver
	 * @see #addObject
	 */
	public ModelAndView(String viewName) {
		this.view = viewName;
	}

	/**
	 * 当没有模型数据需要暴露时使用的便捷构造函数。
	 * 也可以与 {@code addObject} 配合使用。
	 * @param view the View object to render
	 * @see #addObject
	 */
	public ModelAndView(View view) {
		this.view = view;
	}

	/**
	 * 根据视图名称和模型创建一个新的 ModelAndView。
	 * @param viewName name of the View to render, to be resolved
	 * by the DispatcherServlet's ViewResolver
	 * @param model a Map of model names (Strings) to model objects
	 * (Objects). Model entries may not be {@code null}, but the
	 * model Map may be {@code null} if there is no model data.
	 */
	public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 根据 View 对象和模型创建一个新的 ModelAndView。
	 * <em>注意：所提供的模型数据会被复制到本类的内部存储中。
	 * 将 Map 提供给本类之后，不应再修改它</em>
	 * @param view the View object to render
	 * @param model a Map of model names (Strings) to model objects
	 * (Objects). Model entries may not be {@code null}, but the
	 * model Map may be {@code null} if there is no model data.
	 */
	public ModelAndView(View view, @Nullable Map<String, ?> model) {
		this.view = view;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 根据视图名称和 HTTP 状态创建一个新的 ModelAndView。
	 * @param viewName name of the View to render, to be resolved
	 * by the DispatcherServlet's ViewResolver
	 * @param status an HTTP status code to use for the response
	 * (to be set just prior to View rendering)
	 * @since 4.3.8
	 */
	public ModelAndView(String viewName, HttpStatus status) {
		this.view = viewName;
		this.status = status;
	}

	/**
	 * 根据视图名称、模型和 HTTP 状态创建一个新的 ModelAndView。
	 * @param viewName name of the View to render, to be resolved
	 * by the DispatcherServlet's ViewResolver
	 * @param model a Map of model names (Strings) to model objects
	 * (Objects). Model entries may not be {@code null}, but the
	 * model Map may be {@code null} if there is no model data.
	 * @param status an HTTP status code to use for the response
	 * (to be set just prior to View rendering)
	 * @since 4.3
	 */
	public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatus status) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
		this.status = status;
	}

	/**
	 * 用于接收单个模型对象的便捷构造函数。
	 * @param viewName name of the View to render, to be resolved
	 * by the DispatcherServlet's ViewResolver
	 * @param modelName name of the single entry in the model
	 * @param modelObject the single model object
	 */
	public ModelAndView(String viewName, String modelName, Object modelObject) {
		this.view = viewName;
		addObject(modelName, modelObject);
	}

	/**
	 * 用于接收单个模型对象的便捷构造函数。
	 * @param view the View object to render
	 * @param modelName name of the single entry in the model
	 * @param modelObject the single model object
	 */
	public ModelAndView(View view, String modelName, Object modelObject) {
		this.view = view;
		addObject(modelName, modelObject);
	}


	/**
	 * 为此 ModelAndView 设置视图名称，由 DispatcherServlet
	 * 通过 ViewResolver 解析。将覆盖任何已有的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要由 DispatcherServlet 通过 ViewResolver 解析的视图名称，
	 * 如果使用的是 View 对象则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 为此 ModelAndView 设置 View 对象。将覆盖任何已有的视图名称或 View。
	 */
	public void setView(@Nullable View view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象；如果使用的是需要由 DispatcherServlet
	 * 通过 ViewResolver 解析的视图名称，则返回 {@code null}。
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 指示此 {@code ModelAndView} 是否具有视图，
	 * 无论是作为视图名称还是作为直接的 {@link View} 实例。
	 */
	public boolean hasView() {
		return (this.view != null);
	}

	/**
	 * 返回是否使用的是视图引用，即如果视图是通过名称指定的，
	 * 需要由 DispatcherServlet 通过 ViewResolver 解析，则返回 {@code true}。
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回模型 map。可能返回 {@code null}。
	 * 由 DispatcherServlet 调用以评估模型。
	 */
	@Nullable
	protected Map<String, Object> getModelInternal() {
		return this.model;
	}

	/**
	 * 返回底层的 {@code ModelMap} 实例（永不为 {@code null}）。
	 */
	public ModelMap getModelMap() {
		if (this.model == null) {
			this.model = new ModelMap();
		}
		return this.model;
	}

	/**
	 * 返回模型 map。永不返回 {@code null}。
	 * 供应用代码调用以修改模型。
	 */
	public Map<String, Object> getModel() {
		return getModelMap();
	}

	/**
	 * 设置用于响应的 HTTP 状态。
	 * <p>响应状态会在渲染 View 之前设置。
	 * @since 4.3
	 */
	public void setStatus(@Nullable HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回为响应配置的 HTTP 状态（如果有）。
	 * @since 4.3
	 */
	@Nullable
	public HttpStatus getStatus() {
		return this.status;
	}


	/**
	 * 向模型中添加一个属性。
	 * @param attributeName name of the object to add to the model (never {@code null})
	 * @param attributeValue object to add to the model (can be {@code null})
	 * @see ModelMap#addAttribute(String, Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
		getModelMap().addAttribute(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用参数名生成方式向模型中添加一个属性。
	 * @param attributeValue the object to add to the model (never {@code null})
	 * @see ModelMap#addAttribute(Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(Object attributeValue) {
		getModelMap().addAttribute(attributeValue);
		return this;
	}

	/**
	 * 将所提供的 Map 中包含的所有属性添加到模型中。
	 * @param modelMap a Map of attributeName &rarr; attributeValue pairs
	 * @see ModelMap#addAllAttributes(Map)
	 * @see #getModelMap()
	 */
	public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
		getModelMap().addAllAttributes(modelMap);
		return this;
	}


	/**
	 * 清除此 ModelAndView 对象的状态。
	 * 之后该对象将为空。
	 * <p>可用于在 HandlerInterceptor 的 {@code postHandle} 方法中
	 * 抑制对给定 ModelAndView 对象的渲染。
	 * @see #isEmpty()
	 * @see HandlerInterceptor#postHandle
	 */
	public void clear() {
		this.view = null;
		this.model = null;
		this.cleared = true;
	}

	/**
	 * 返回此 ModelAndView 对象是否为空，
	 * 即它是否不持有任何视图且不包含模型。
	 */
	public boolean isEmpty() {
		return (this.view == null && CollectionUtils.isEmpty(this.model));
	}

	/**
	 * 返回此 ModelAndView 对象是否因调用 {@link #clear} 而为空，
	 * 即它是否不持有任何视图且不包含模型。
	 * <p>如果在对 {@link #clear} 的调用<strong>之后</strong>又向该实例添加了
	 * 任何额外状态，则返回 {@code false}。
	 * @see #clear()
	 */
	public boolean wasCleared() {
		return (this.cleared && isEmpty());
	}


	/**
	 * 返回有关此模型和视图的诊断信息。
	 */
	@Override
	public String toString() {
		return "ModelAndView [view=" + formatView() + "; model=" + this.model + "]";
	}

	private String formatView() {
		return isReference() ? "\"" + this.view + "\"" : "[" + this.view + "]";
	}

}
