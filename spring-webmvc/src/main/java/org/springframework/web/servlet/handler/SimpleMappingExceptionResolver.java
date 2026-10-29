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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} 实现，
 * 允许将异常类名称映射到视图名称，可用于一组
 * 给定处理器，也可用于 DispatcherServlet 中的所有处理器。
 *
 * <p>错误视图类似于错误页面 JSP，但可用于任何类型的
 * 异常（包括受检异常），并可为特定处理器提供细粒度的映射。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 22.11.2003
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class SimpleMappingExceptionResolver extends AbstractHandlerExceptionResolver {

	/** 异常属性的默认名称："exception"。 */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";


	@Nullable
	private Properties exceptionMappings;

	@Nullable
	private Class<?>[] excludedExceptions;

	@Nullable
	private String defaultErrorView;

	@Nullable
	private Integer defaultStatusCode;

	private Map<String, Integer> statusCodes = new HashMap<>();

	@Nullable
	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;


	/**
	 * 设置异常类名称与错误视图名称之间的映射。
	 * 异常类名称可以是子字符串，目前不支持通配符。
	 * 例如，值 "ServletException" 会匹配 {@code javax.servlet.ServletException}
	 * 及其子类。
	 * <p><b>注意：</b>请仔细考虑
	 * 模式的精确程度，以及是否包含包信息（包信息并非必需）。
	 * 例如，"Exception" 几乎会匹配任何内容，并且可能会遮蔽其他规则。
	 * 如果 "Exception" 是想为所有
	 * 受检异常定义规则，那么 "java.lang.Exception" 才是正确的。对于
	 * "BaseBusinessException" 这类较为特殊的异常名称，
	 * 则无需使用全限定名。
	 * @param mappings exception patterns (can also be fully qualified class names) as keys,
	 * and error view names as values
	 */
	public void setExceptionMappings(Properties mappings) {
		this.exceptionMappings = mappings;
	}

	/**
	 * 设置一个或多个要从异常映射中排除的异常。
	 * 会先检查被排除的异常，如果其中之一与实际
	 * 异常相等，则该异常将保持未解析状态。
	 * @param excludedExceptions one or more excluded exception types
	 */
	public void setExcludedExceptions(Class<?>... excludedExceptions) {
		this.excludedExceptions = excludedExceptions;
	}

	/**
	 * 设置默认错误视图的名称。
	 * 如果未找到特定映射，将返回此视图。
	 * <p>默认无。
	 */
	public void setDefaultErrorView(String defaultErrorView) {
		this.defaultErrorView = defaultErrorView;
	}

	/**
	 * 设置此异常解析器将为给定已解析错误视图应用的
	 * HTTP 状态码。键为视图名称；值为状态码。
	 * <p>请注意，此错误码仅在顶层请求的情况下才会应用。
	 * 它不会为 include 请求设置，因为无法在
	 * include 内部修改 HTTP 状态。
	 * <p>如果未指定，将应用默认状态码。
	 * @see #setDefaultStatusCode(int)
	 */
	public void setStatusCodes(Properties statusCodes) {
		for (Enumeration<?> enumeration = statusCodes.propertyNames(); enumeration.hasMoreElements();) {
			String viewName = (String) enumeration.nextElement();
			Integer statusCode = Integer.valueOf(statusCodes.getProperty(viewName));
			this.statusCodes.put(viewName, statusCode);
		}
	}

	/**
	 * {@link #setStatusCodes(Properties)} 的替代方案，供基于 Java 的
	 * 配置使用。
	 */
	public void addStatusCode(String viewName, int statusCode) {
		this.statusCodes.put(viewName, statusCode);
	}

	/**
	 * 返回通过 {@link #setStatusCodes(Properties)} 提供的 HTTP 状态码。
	 * 键为视图名称；值为状态码。
	 */
	public Map<String, Integer> getStatusCodesAsMap() {
		return Collections.unmodifiableMap(this.statusCodes);
	}

	/**
	 * 设置此异常解析器在解析错误视图且未定义状态码映射时将
	 * 应用的默认 HTTP 状态码。
	 * <p>请注意，此错误码仅在顶层请求的情况下才会应用。
	 * 它不会为 include 请求设置，因为无法在
	 * include 内部修改 HTTP 状态。
	 * <p>如果未指定，则不会应用任何状态码，而是将其留给
	 * 控制器或视图处理，或者保持 Servlet 引擎默认的 200（OK）。
	 * @param defaultStatusCode the HTTP status code value, for example 500
	 * ({@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}) or 404 ({@link HttpServletResponse#SC_NOT_FOUND})
	 * @see #setStatusCodes(Properties)
	 */
	public void setDefaultStatusCode(int defaultStatusCode) {
		this.defaultStatusCode = defaultStatusCode;
	}

	/**
	 * 设置异常应作为哪个模型属性暴露的名称。
	 * 默认为 "exception"。
	 * <p>可以将此设置为其他属性名称，或者设置为 {@code null}
	 * 以完全不暴露异常属性。
	 * @see #DEFAULT_EXCEPTION_ATTRIBUTE
	 */
	public void setExceptionAttribute(@Nullable String exceptionAttribute) {
		this.exceptionAttribute = exceptionAttribute;
	}


	/**
	 * 实际解析处理器执行期间抛出的给定异常，
	 * 如果合适，返回表示特定错误页面的 ModelAndView。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法会在检查此解析器是否适用（"mappedHandlers" 等）
	 * <i>之后</i>被调用，因此实现可以简单地继续执行
	 * 实际的异常处理。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time
	 * of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding {@code ModelAndView} to forward to,
	 * or {@code null} for default processing in the resolution chain
	 */
	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		// Expose ModelAndView for chosen error view.
		String viewName = determineViewName(ex, request);
		if (viewName != null) {
			// Apply HTTP status code for error views, if specified.
			// Only apply it if we're processing a top-level request.
			Integer statusCode = determineStatusCode(request, viewName);
			if (statusCode != null) {
				applyStatusCodeIfPossible(request, response, statusCode);
			}
			return getModelAndView(viewName, ex, request);
		}
		else {
			return null;
		}
	}

	/**
	 * 确定给定异常的视图名称：首先检查
	 * {@link #setExcludedExceptions(Class[]) "excludedExecptions"}，然后搜索
	 * {@link #setExceptionMappings "exceptionMappings"}，最后使用
	 * {@link #setDefaultErrorView "defaultErrorView"} 作为兜底。
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the resolved view name, or {@code null} if excluded or none found
	 */
	@Nullable
	protected String determineViewName(Exception ex, HttpServletRequest request) {
		String viewName = null;
		if (this.excludedExceptions != null) {
			for (Class<?> excludedEx : this.excludedExceptions) {
				if (excludedEx.equals(ex.getClass())) {
					return null;
				}
			}
		}
		// Check for specific exception mappings.
		if (this.exceptionMappings != null) {
			viewName = findMatchingViewName(this.exceptionMappings, ex);
		}
		// Return default error view else, if defined.
		if (viewName == null && this.defaultErrorView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving to default view '" + this.defaultErrorView + "'");
			}
			viewName = this.defaultErrorView;
		}
		return viewName;
	}

	/**
	 * 在给定异常映射中查找匹配的视图名称。
	 * @param exceptionMappings mappings between exception class names and error view names
	 * @param ex the exception that got thrown during handler execution
	 * @return the view name, or {@code null} if none found
	 * @see #setExceptionMappings
	 */
	@Nullable
	protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
		String viewName = null;
		String dominantMapping = null;
		int deepest = Integer.MAX_VALUE;
		for (Enumeration<?> names = exceptionMappings.propertyNames(); names.hasMoreElements();) {
			String exceptionMapping = (String) names.nextElement();
			int depth = getDepth(exceptionMapping, ex);
			if (depth >= 0 && (depth < deepest || (depth == deepest &&
					dominantMapping != null && exceptionMapping.length() > dominantMapping.length()))) {
				deepest = depth;
				dominantMapping = exceptionMapping;
				viewName = exceptionMappings.getProperty(exceptionMapping);
			}
		}
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' based on mapping [" + dominantMapping + "]");
		}
		return viewName;
	}

	/**
	 * 返回与超类匹配的深度。
	 * <p>0 表示 ex 完全匹配。如果没有匹配，则返回 -1。
	 * 否则返回深度。深度最低者胜出。
	 */
	protected int getDepth(String exceptionMapping, Exception ex) {
		return getDepth(exceptionMapping, ex.getClass(), 0);
	}

	private int getDepth(String exceptionMapping, Class<?> exceptionClass, int depth) {
		if (exceptionClass.getName().contains(exceptionMapping)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionClass == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
	}

	/**
	 * 确定要为给定错误视图应用的 HTTP 状态码。
	 * <p>默认实现返回给定视图名称的状态码（通过
	 * {@link #setStatusCodes(Properties) statusCodes} 属性指定），如果没有匹配，
	 * 则回退到 {@link #setDefaultStatusCode defaultStatusCode}。
	 * <p>可在自定义子类中重写此方法以自定义此行为。
	 * @param request current HTTP request
	 * @param viewName the name of the error view
	 * @return the HTTP status code to use, or {@code null} for the servlet container's default
	 * (200 in case of a standard error view)
	 * @see #setDefaultStatusCode
	 * @see #applyStatusCodeIfPossible
	 */
	@Nullable
	protected Integer determineStatusCode(HttpServletRequest request, String viewName) {
		if (this.statusCodes.containsKey(viewName)) {
			return this.statusCodes.get(viewName);
		}
		return this.defaultStatusCode;
	}

	/**
	 * 如果可能（即如果不在 include 请求中执行），将指定 HTTP 状态码
	 * 应用于给定响应。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param statusCode the status code to apply
	 * @see #determineStatusCode
	 * @see #setDefaultStatusCode
	 * @see HttpServletResponse#setStatus
	 */
	protected void applyStatusCodeIfPossible(HttpServletRequest request, HttpServletResponse response, int statusCode) {
		if (!WebUtils.isIncludeRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying HTTP status " + statusCode);
			}
			response.setStatus(statusCode);
			request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, statusCode);
		}
	}

	/**
	 * 为给定请求、视图名称和异常返回 ModelAndView。
	 * <p>默认实现委托给 {@link #getModelAndView(String, Exception)}。
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the ModelAndView instance
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
		return getModelAndView(viewName, ex);
	}

	/**
	 * 为给定视图名称和异常返回 ModelAndView。
	 * <p>默认实现会添加指定的异常属性。
	 * 可以在子类中重写。
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @return the ModelAndView instance
	 * @see #setExceptionAttribute
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex) {
		ModelAndView mv = new ModelAndView(viewName);
		if (this.exceptionAttribute != null) {
			mv.addObject(this.exceptionAttribute, ex);
		}
		return mv;
	}

}
