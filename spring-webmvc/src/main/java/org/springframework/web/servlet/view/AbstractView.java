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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ContextExposingHttpServletRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;

/**
 * {@link org.springframework.web.servlet.View} 实现的抽象基类。
 * 子类应为 JavaBean，以便作为 Spring 管理的 bean 实例
 * 进行便捷配置。
 *
 * <p>提供对静态属性的支持，可通过多种方式指定这些属性
 * 使其可供视图使用。每次渲染操作时，静态属性将与
 * 给定的动态属性（控制器返回的模型）合并。
 *
 * <p>继承自 {@link WebApplicationObjectSupport}，这对某些视图会有帮助。
 * 子类只需实现实际的渲染逻辑。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setAttributes
 * @see #setAttributesMap
 * @see #renderMergedOutputModel
 */
public abstract class AbstractView extends WebApplicationObjectSupport implements View, BeanNameAware {

	/** 默认内容类型。可通过 bean 属性覆盖。 */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

	/** 临时输出字节数组（如有）的初始大小。 */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;


	@Nullable
	private String contentType = DEFAULT_CONTENT_TYPE;

	@Nullable
	private String requestContextAttribute;

	private final Map<String, Object> staticAttributes = new LinkedHashMap<>();

	private boolean exposePathVariables = true;

	private boolean exposeContextBeansAsAttributes = false;

	@Nullable
	private Set<String> exposedContextBeanNames;

	@Nullable
	private String beanName;



	/**
	 * 设置此视图的内容类型。
	 * 默认为 "text/html;charset=ISO-8859-1"。
	 * <p>如果假定视图本身会设置内容类型（例如 JSP 的情况），
	 * 子类可以忽略此设置。
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回此视图的内容类型。
	 */
	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置此视图的 RequestContext 属性名称。
	 * 默认不设置。
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回 RequestContext 属性的名称（如果有）。
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 以 CSV 字符串形式设置静态属性。
	 * 格式为：attname0={value1},attname1={value1}
	 * <p>"静态"属性是在 View 实例配置中指定的固定属性。
	 * 另一方面，"动态"属性是作为模型一部分
	 * 传入的值。
	 */
	public void setAttributesCSV(@Nullable String propString) throws IllegalArgumentException {
		if (propString != null) {
			StringTokenizer st = new StringTokenizer(propString, ",");
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				int eqIdx = tok.indexOf('=');
				if (eqIdx == -1) {
					throw new IllegalArgumentException(
							"Expected '=' in attributes CSV string '" + propString + "'");
				}
				if (eqIdx >= tok.length() - 2) {
					throw new IllegalArgumentException(
							"At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
				}
				String name = tok.substring(0, eqIdx);
				// Delete first and last characters of value: { and }
				int beginIndex = eqIdx + 2;
				int endIndex = tok.length() - 1;
				String value = tok.substring(beginIndex, endIndex);

				addStaticAttribute(name, value);
			}
		}
	}

	/**
	 * 从 {@code java.util.Properties} 对象为此视图设置静态属性。
	 * <p>"静态"属性是在 View 实例配置中指定的固定属性。
	 * 另一方面，"动态"属性是作为模型一部分
	 * 传入的值。
	 * <p>这是设置静态属性最便捷的方式。请注意，
	 * 如果模型中包含同名值，静态属性可以被动态属性覆盖。
	 * <p>可以使用 String 类型的 "value"（通过 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素进行填充。
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 */
	public void setAttributes(Properties attributes) {
		CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
	}

	/**
	 * 从 Map 为此视图设置静态属性。这允许设置
	 * 任意类型的属性值，例如 bean 引用。
	 * <p>"静态"属性是在 View 实例配置中指定的固定属性。
	 * 另一方面，"动态"属性是作为模型一部分
	 * 传入的值。
	 * <p>可以使用 XML bean 定义中的 "map" 或 "props" 元素进行填充。
	 * @param attributes a Map with name Strings as keys and attribute objects as values
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach(this::addStaticAttribute);
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问此视图的静态属性，
	 * 并可选择添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过
	 * {@code attributesMap[myKey]}。这在添加或覆盖
	 * 子视图定义中的条目时特别有用。
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 向此视图添加静态数据，并在每个视图中公开。
	 * <p>"静态"属性是在 View 实例配置中指定的固定属性。
	 * 另一方面，"动态"属性是作为模型一部分
	 * 传入的值。
	 * <p>必须在任何 {@code render} 调用之前调用。
	 * @param name the name of the attribute to expose
	 * @param value the attribute value to expose
	 * @see #render
	 */
	public void addStaticAttribute(String name, Object value) {
		this.staticAttributes.put(name, value);
	}

	/**
	 * 返回此视图的静态属性。便于测试。
	 * <p>返回一个不可修改的 Map，因为其目的不是
	 * 操作该 Map，而只是检查其内容。
	 * @return the static attributes in this view
	 */
	public Map<String, Object> getStaticAttributes() {
		return Collections.unmodifiableMap(this.staticAttributes);
	}

	/**
	 * 指定是否将路径变量添加到模型中。
	 * <p>路径变量通常通过 {@code @PathVariable}
	 * 注解绑定到 URI 模板变量。它们实际上是 URI 模板变量，并对其应用类型转换
	 * 以得到有类型的 Object 值。在视图中构建指向同一 URL 及其他 URL 的链接时，
	 * 经常需要这类值。
	 * <p>添加到模型中的路径变量会覆盖静态属性（参见 {@link #setAttributes(Properties)}），
	 * 但不会覆盖模型中已存在的属性。
	 * <p>默认情况下此标志设为 {@code true}。具体的视图类型可以覆盖此设置。
	 * @param exposePathVariables {@code true} to expose path variables, and {@code false} otherwise
	 */
	public void setExposePathVariables(boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回是否将路径变量添加到模型中。
	 */
	public boolean isExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否通过一次属性访问时的惰性检查，使应用上下文中的
	 * 所有 Spring bean 可作为请求属性访问。
	 * <p>这将使所有此类 bean 可在 JSP 2.0 页面的普通 {@code ${...}}
	 * 表达式中访问，也可在 JSTL 的 {@code c:out}
	 * 值表达式中访问。
	 * <p>默认为 "false"。开启此标志即可透明地在请求属性命名空间中
	 * 公开所有 Spring bean。
	 * <p><b>注意：</b>上下文 bean 将覆盖手动添加的任何同名自定义请求或会话
	 * 属性。但是，同名的模型属性（明确公开给此视图的属性）
	 * 将始终覆盖上下文 bean。
	 * @see #getRequestToExpose
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应被公开的 bean 名称。
	 * 如果此值非 null，则只有指定的 bean 才有资格作为
	 * 属性公开。
	 * <p>如果你希望公开应用上下文中的所有 Spring bean，请开启
	 * {@link #setExposeContextBeansAsAttributes "exposeContextBeansAsAttributes"}
	 * 标志，但不要为此属性列出具体的 bean 名称。
	 */
	public void setExposedContextBeanNames(String... exposedContextBeanNames) {
		this.exposedContextBeanNames = new HashSet<>(Arrays.asList(exposedContextBeanNames));
	}

	/**
	 * 设置视图的名称。便于追踪。
	 * <p>框架代码在构造视图时必须调用此方法。
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回视图的名称。如果视图配置正确，
	 * 则绝不应为 {@code null}。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}


	/**
	 * 根据指定的模型准备视图，必要时将其与静态属性
	 * 及 RequestContext 属性合并。
	 * 实际渲染委托给 renderMergedOutputModel。
	 * @see #renderMergedOutputModel
	 */
	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()) +
					(this.staticAttributes.isEmpty() ? "" : ", static attributes " + this.staticAttributes));
		}

		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}

	/**
	 * 创建一个合并后的输出 Map（绝不为 {@code null}），其中包含动态值和静态属性。
	 * 动态值优先于静态属性。
	 */
	protected Map<String, Object> createMergedOutputModel(@Nullable Map<String, ?> model,
			HttpServletRequest request, HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// Consolidate static and dynamic model attributes.
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		Map<String, Object> mergedModel = CollectionUtils.newLinkedHashMap(size);
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

	/**
	 * 创建一个 RequestContext 并在指定属性名称下公开。
	 * <p>默认实现为给定请求和模型创建一个标准的 RequestContext 实例。
	 * 可在子类中重写以使用自定义实例。
	 * @param request current HTTP request
	 * @param model combined output Map (never {@code null}),
	 * with dynamic values taking precedence over static attributes
	 * @return the RequestContext instance
	 * @see #setRequestContextAttribute
	 * @see org.springframework.web.servlet.support.RequestContext
	 */
	protected RequestContext createRequestContext(
			HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {

		return new RequestContext(request, response, getServletContext(), model);
	}

	/**
	 * 为渲染准备给定的响应。
	 * <p>默认实现针对通过 HTTPS 发送下载内容时
	 * 的 IE 缺陷应用了一个变通方案。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		if (generatesDownloadContent()) {
			response.setHeader("Pragma", "private");
			response.setHeader("Cache-Control", "private, must-revalidate");
		}
	}

	/**
	 * 返回此视图是否生成下载内容
	 * （通常是 PDF 或 Excel 文件之类的二进制内容）。
	 * <p>默认实现返回 {@code false}。如果子类知道自己
	 * 生成的是需要在客户端进行临时缓存的下载内容，
	 * 通常通过响应的 OutputStream 输出，则鼓励在此返回 {@code true}。
	 * @see #prepareResponse
	 * @see javax.servlet.http.HttpServletResponse#getOutputStream()
	 */
	protected boolean generatesDownloadContent() {
		return false;
	}

	/**
	 * 获取要公开给 {@link #renderMergedOutputModel}（即视图）的请求句柄。
	 * <p>默认实现会包装原始请求，以便按要求将 Spring bean
	 * 作为请求属性公开。
	 * @param originalRequest the original servlet request as provided by the engine
	 * @return the wrapped request, or the original request if no wrapping is necessary
	 * @see #setExposeContextBeansAsAttributes
	 * @see #setExposedContextBeanNames
	 * @see org.springframework.web.context.support.ContextExposingHttpServletRequest
	 */
	protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest) {
		if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
			WebApplicationContext wac = getWebApplicationContext();
			Assert.state(wac != null, "No WebApplicationContext");
			return new ContextExposingHttpServletRequest(originalRequest, wac, this.exposedContextBeanNames);
		}
		return originalRequest;
	}

	/**
	 * 子类必须实现此方法以实际渲染视图。
	 * <p>第一步是准备请求：在 JSP 的情况下，
	 * 这意味着将模型对象设置为请求属性。
	 * 第二步是视图的实际渲染，
	 * 例如通过 RequestDispatcher 包含 JSP。
	 * @param model combined output Map (never {@code null}),
	 * with dynamic values taking precedence over static attributes
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if rendering failed
	 */
	protected abstract void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;


	/**
	 * 将给定 Map 中的模型对象作为请求属性公开。
	 * 名称取自模型 Map。
	 * 此方法适用于 {@link javax.servlet.RequestDispatcher} 可访问的所有资源。
	 * @param model a Map of model objects to expose
	 * @param request current HTTP request
	 */
	protected void exposeModelAsRequestAttributes(Map<String, Object> model,
			HttpServletRequest request) throws Exception {

		model.forEach((name, value) -> {
			if (value != null) {
				request.setAttribute(name, value);
			}
			else {
				request.removeAttribute(name);
			}
		});
	}

	/**
	 * 为此视图创建一个临时 OutputStream。
	 * <p>这通常用作 IE 的变通方案，以便在实际将内容写入 HTTP 响应之前，
	 * 根据临时流设置内容长度标头。
	 */
	protected ByteArrayOutputStream createTemporaryOutputStream() {
		return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
	}

	/**
	 * 将给定的临时 OutputStream 写入 HTTP 响应。
	 * @param response current HTTP response
	 * @param baos the temporary OutputStream to write
	 * @throws IOException if writing/flushing failed
	 */
	protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
		// Write content type and also length (determined via byte array).
		response.setContentType(getContentType());
		response.setContentLength(baos.size());

		// Flush byte array to servlet output stream.
		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();
	}

	/**
	 * 将响应的内容类型设置为所配置的
	 * {@link #setContentType(String) content type}，除非
	 * {@link View#SELECTED_CONTENT_TYPE} 请求属性存在且
	 * 设置为具体的媒体类型。
	 */
	protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
		MediaType mediaType = (MediaType) request.getAttribute(View.SELECTED_CONTENT_TYPE);
		if (mediaType != null && mediaType.isConcrete()) {
			response.setContentType(mediaType.toString());
		}
		else {
			response.setContentType(getContentType());
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + formatViewName();
	}

	protected String formatViewName() {
		return (getBeanName() != null ? "name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
	}

}
