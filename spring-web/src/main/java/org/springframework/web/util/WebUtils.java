/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Web 应用的各种工具方法。供多个框架类使用。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
public abstract class WebUtils {

	/**
	 * 标准 Servlet 2.3+ 规范中用于包含请求 URI 的请求属性。<p>如果通过 {@code RequestDispatcher} 包含，则当前资源将看到发起方的请求。其自身的请求 URI 作为请求属性公开。
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";

	/**
	 * 标准 Servlet 2.3+ 规范中用于包含上下文路径的请求属性。<p>如果通过 {@code RequestDispatcher} 包含，则当前资源将看到发起方的上下文路径。其自身的上下文路径作为请求属性公开。
	 */
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";

	/**
	 * 标准 Servlet 2.3+ 规范中用于包含 servlet 路径的请求属性。<p>如果通过 {@code RequestDispatcher} 包含，则当前资源将看到发起方的 servlet 路径。其自身的 servlet 路径作为请求属性公开。
	 */
	public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";

	/**
	 * 标准 Servlet 2.3+ 规范中用于包含路径信息的请求属性。<p>如果通过 {@code RequestDispatcher} 包含，则当前资源将看到发起方的路径信息。其自身的路径信息作为请求属性公开。
	 */
	public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";

	/**
	 * 标准 Servlet 2.3+ 规范中用于包含查询字符串的请求属性。<p>如果通过 {@code RequestDispatcher} 包含，则当前资源将看到发起方的查询字符串。其自身的查询字符串作为请求属性公开。
	 */
	public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";

	/**
	 * 标准 Servlet 2.4+ 规范中用于转发请求 URI 的请求属性。<p>如果通过 RequestDispatcher 转发到，则当前资源将看到自己的请求 URI。发起方的请求 URI 作为请求属性公开。
	 */
	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";

	/**
	 * 标准 Servlet 2.4+ 规范中用于转发上下文路径的请求属性。<p>如果通过 RequestDispatcher 转发到，则当前资源将看到自己的上下文路径。发起方的上下文路径作为请求属性公开。
	 */
	public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";

	/**
	 * 标准 Servlet 2.4+ 规范中用于转发 servlet 路径的请求属性。<p>如果通过 RequestDispatcher 转发到，则当前资源将看到自己的 servlet 路径。发起方的 servlet 路径作为请求属性公开。
	 */
	public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";

	/**
	 * 标准 Servlet 2.4+ 规范中用于转发路径信息的请求属性。<p>如果通过 RequestDispatcher 转发到，则当前资源将看到自己的路径信息。发起方的路径信息作为请求属性公开。
	 */
	public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";

	/**
	 * 标准 Servlet 2.4+ 规范中用于转发查询字符串的请求属性。<p>如果通过 RequestDispatcher 转发到，则当前资源将看到自己的查询字符串。发起方的查询字符串作为请求属性公开。
	 */
	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页状态码的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_STATUS_CODE_ATTRIBUTE = "javax.servlet.error.status_code";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页异常类型的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "javax.servlet.error.exception_type";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页消息的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_MESSAGE_ATTRIBUTE = "javax.servlet.error.message";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页异常的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_EXCEPTION_ATTRIBUTE = "javax.servlet.error.exception";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页请求 URI 的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";

	/**
	 * 标准 Servlet 2.3+ 规范中用于错误页 servlet 名称的请求属性。<p>当直接转发到标记为错误页的 JSP 而不是通过 Servlet 容器的错误页解析机制时，将其公开给这些 JSP。
	 */
	public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "javax.servlet.error.servlet_name";

	/**
	 * 内容类型 String 中字符集子句的前缀：";charset="。
	 */
	public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

	/**
	 * 根据 Servlet 规范，当 {@code request.getCharacterEncoding} 返回 {@code null} 时使用的默认字符编码。
	 * @see ServletRequest#getCharacterEncoding
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

	/**
	 * 标准 Servlet 规范上下文属性，用于指定当前 Web 应用的临时目录，类型为 {@code java.io.File}。
	 */
	public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "javax.servlet.context.tempdir";

	/**
	 * servlet 上下文级别的 HTML 转义参数（即 {@code web.xml} 中的 context-param）："defaultHtmlEscape"。
	 */
	public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

	/**
	 * 在 servlet 上下文级别（即 {@code web.xml} 中的 context-param）使用响应编码进行 HTML 转义的参数："responseEncodedHtmlEscape"。
	 * @since 4.1.2
	 */
	public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";

	/**
	 * servlet 上下文级别（即 {@code web.xml} 中的 context-param）的 Web 应用根键参数："webAppRootKey"。
	 */
	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/** 默认 Web 应用根键："webapp.root"。 */
	public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

	/** 图像按钮情况下的名称后缀。 */
	public static final String[] SUBMIT_IMAGE_SUFFIXES = {".x", ".y"};

	/** 互斥会话属性的键。 */
	public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";


	/**
	 * 将系统属性设置为 Web 应用根目录。系统属性的键可以通过 {@code web.xml} 中的 "webAppRootKey" context-param 定义。默认为 "webapp.root"。<p>可用于支持以 {@code System.getProperty} 值进行替换的工具，例如 log4j 在日志文件位置中的 "${key}" 语法。
	 * @param servletContext the servlet context of the web application
	 * @throws IllegalStateException if the system property is already set,
	 * or if the WAR file is not expanded
	 * @see #WEB_APP_ROOT_KEY_PARAM
	 * @see #DEFAULT_WEB_APP_ROOT_KEY
	 * @see WebAppRootListener
	 */
	public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String root = servletContext.getRealPath("/");
		if (root == null) {
			throw new IllegalStateException(
					"Cannot set web app root system property when WAR file is not expanded");
		}
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		String oldValue = System.getProperty(key);
		if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
			throw new IllegalStateException("Web app root system property already set to different value: '" +
					key + "' = [" + oldValue + "] instead of [" + root + "] - " +
					"Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
		}
		System.setProperty(key, root);
		servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
	}

	/**
	 * 移除指向 Web 应用根目录的系统属性。应在 Web 应用关闭时调用。
	 * @param servletContext the servlet context of the web application
	 * @see #setWebAppRootSystemProperty
	 */
	public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		System.getProperties().remove(key);
	}

	/**
	 * 返回是否为 Web 应用启用了默认 HTML 转义，即 {@code web.xml} 中 "defaultHtmlEscape" context-param 的值（如果有）。<p>此方法区分完全未指定参数和指定了实际布尔值的情况，从而允许在全局级别未设置时使用上下文特定的默认值。
	 * @param servletContext the servlet context of the web application
	 * @return whether default HTML escaping is enabled for the given application
	 * ({@code null} = no explicit default)
	 */
	@Nullable
	public static Boolean getDefaultHtmlEscape(@Nullable ServletContext servletContext) {
		if (servletContext == null) {
			return null;
		}
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回在 HTML 转义字符时是否应使用响应编码，即在使用 UTF-* 编码时仅转义对 XML 标记有重要意义的字符。此选项通过 ServletContext 参数为 Web 应用启用，即 {@code web.xml} 中 "responseEncodedHtmlEscape" context-param 的值（如果有）。<p>此方法区分完全未指定参数和指定了实际布尔值的情况，从而允许在全局级别未设置时使用上下文特定的默认值。
	 * @param servletContext the servlet context of the web application
	 * @return whether response encoding is to be used for HTML escaping
	 * ({@code null} = no explicit default)
	 * @since 4.1.2
	 */
	@Nullable
	public static Boolean getResponseEncodedHtmlEscape(@Nullable ServletContext servletContext) {
		if (servletContext == null) {
			return null;
		}
		String param = servletContext.getInitParameter(RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM);
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回当前 Web 应用的临时目录，由 servlet 容器提供。
	 * @param servletContext the servlet context of the web application
	 * @return the File representing the temporary directory
	 */
	public static File getTempDir(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		return (File) servletContext.getAttribute(TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 返回 Web 应用中给定路径的真实路径，由 servlet 容器提供。<p>如果路径尚未以斜杠开头，则前置一个斜杠；如果无法将路径解析为资源（与 ServletContext 的 {@code getRealPath} 相对，后者返回 null），则抛出 FileNotFoundException。
	 * @param servletContext the servlet context of the web application
	 * @param path the path within the web application
	 * @return the corresponding real path
	 * @throws FileNotFoundException if the path cannot be resolved to a resource
	 * @see javax.servlet.ServletContext#getRealPath
	 */
	public static String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		// Interpret location as relative to the web application root directory.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String realPath = servletContext.getRealPath(path);
		if (realPath == null) {
			throw new FileNotFoundException(
					"ServletContext resource [" + path + "] cannot be resolved to absolute file path - " +
					"web application archive not expanded?");
		}
		return realPath;
	}

	/**
	 * 确定给定请求的会话 id（如果有）。
	 * @param request current HTTP request
	 * @return the session id, or {@code null} if none
	 */
	@Nullable
	public static String getSessionId(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getId() : null);
	}

	/**
	 * 检查给定请求是否具有给定名称的会话属性。如果没有会话或会话没有此属性，则返回 null。如果之前不存在会话，则不会创建新会话！
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 */
	@Nullable
	public static Object getSessionAttribute(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getAttribute(name) : null);
	}

	/**
	 * 检查给定请求是否具有给定名称的会话属性。如果没有会话或会话没有此属性，则抛出异常。如果之前不存在会话，则不会创建新会话！
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 * @throws IllegalStateException if the session attribute could not be found
	 */
	public static Object getRequiredSessionAttribute(HttpServletRequest request, String name)
			throws IllegalStateException {

		Object attr = getSessionAttribute(request, name);
		if (attr == null) {
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		return attr;
	}

	/**
	 * 将给定名称的会话属性设置为给定值。如果值为 null，则移除该会话属性（如果存在会话的话）。如无必要，不会创建新会话！
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @param value the value of the session attribute
	 */
	public static void setSessionAttribute(HttpServletRequest request, String name, @Nullable Object value) {
		Assert.notNull(request, "Request must not be null");
		if (value != null) {
			request.getSession().setAttribute(name, value);
		}
		else {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute(name);
			}
		}
	}

	/**
	 * 返回给定会话可用的最佳互斥对象：即用于对给定会话进行同步的对象。<p>如果可用，返回会话互斥属性；通常这意味着需要在 {@code web.xml} 中定义 HttpSessionMutexListener。如果未找到互斥属性，则回退到 HttpSession 本身。<p>会话互斥对象在整个会话生命周期内保证是同一个对象，可通过 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键获取。它可作为对当前会话加锁时进行同步的安全引用。<p>在许多情况下，HttpSession 引用本身也是安全的互斥对象，因为对于同一个活动逻辑会话，它始终是相同的对象引用。不过，这在不同的 servlet 容器之间并无保证；唯一 100% 安全的方式是使用会话互斥对象。
	 * @param session the HttpSession to find a mutex for
	 * @return the mutex object (never {@code null})
	 * @see #SESSION_MUTEX_ATTRIBUTE
	 * @see HttpSessionMutexListener
	 */
	public static Object getSessionMutex(HttpSession session) {
		Assert.notNull(session, "Session must not be null");
		Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}


	/**
	 * 如果可用，返回指定类型的适当请求对象，必要时对给定请求进行尽可能的解包。
	 * @param request the servlet request to introspect
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none
	 * of that type is available
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getNativeRequest(ServletRequest request, @Nullable Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(request)) {
				return (T) request;
			}
			else if (request instanceof ServletRequestWrapper) {
				return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		return null;
	}

	/**
	 * 如果可用，返回指定类型的适当响应对象，必要时对给定响应进行尽可能的解包。
	 * @param response the servlet response to introspect
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none
	 * of that type is available
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getNativeResponse(ServletResponse response, @Nullable Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			}
			else if (response instanceof ServletResponseWrapper) {
				return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}

	/**
	 * 确定给定请求是否为包含请求，即不是来自外部的顶层 HTTP 请求。<p>检查 "javax.servlet.include.request_uri" 请求属性是否存在。也可以检查任何仅在包含请求中存在的请求属性。
	 * @param request current servlet request
	 * @return whether the given request is an include request
	 */
	public static boolean isIncludeRequest(ServletRequest request) {
		return (request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null);
	}

	/**
	 * 按照 Servlet 2.3 规范定义的键，将 Servlet 规范的错误属性作为 {@link javax.servlet.http.HttpServletRequest} 属性公开，用于直接渲染的错误页，而不是通过 Servlet 容器的错误页解析机制：{@code javax.servlet.error.status_code}、{@code javax.servlet.error.exception_type}、{@code javax.servlet.error.message}、{@code javax.servlet.error.exception}、{@code javax.servlet.error.request_uri}、{@code javax.servlet.error.servlet_name}。<p>如果值已存在则不覆盖，以尊重之前显式公开的属性值。<p>默认公开状态码 200。可在（之前或之后）显式设置 "javax.servlet.error.status_code" 属性，以公开不同的状态码。
	 * @param request current servlet request
	 * @param ex the exception encountered
	 * @param servletName the name of the offending servlet
	 */
	public static void exposeErrorRequestAttributes(HttpServletRequest request, Throwable ex,
			@Nullable String servletName) {

		exposeRequestAttributeIfNotPresent(request, ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_OK);
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_TYPE_ATTRIBUTE, ex.getClass());
		exposeRequestAttributeIfNotPresent(request, ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_ATTRIBUTE, ex);
		exposeRequestAttributeIfNotPresent(request, ERROR_REQUEST_URI_ATTRIBUTE, request.getRequestURI());
		if (servletName != null) {
			exposeRequestAttributeIfNotPresent(request, ERROR_SERVLET_NAME_ATTRIBUTE, servletName);
		}
	}

	/**
	 * 如果指定请求属性尚不存在，则将其公开。
	 * @param request current servlet request
	 * @param name the name of the attribute
	 * @param value the suggested value of the attribute
	 */
	private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
		if (request.getAttribute(name) == null) {
			request.setAttribute(name, value);
		}
	}

	/**
	 * 按照 Servlet 2.3 规范定义的键，清除 Servlet 规范的错误属性：{@code javax.servlet.error.status_code}、{@code javax.servlet.error.exception_type}、{@code javax.servlet.error.message}、{@code javax.servlet.error.exception}、{@code javax.servlet.error.request_uri}、{@code javax.servlet.error.servlet_name}。
	 * @param request current servlet request
	 */
	public static void clearErrorRequestAttributes(HttpServletRequest request) {
		request.removeAttribute(ERROR_STATUS_CODE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_TYPE_ATTRIBUTE);
		request.removeAttribute(ERROR_MESSAGE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_ATTRIBUTE);
		request.removeAttribute(ERROR_REQUEST_URI_ATTRIBUTE);
		request.removeAttribute(ERROR_SERVLET_NAME_ATTRIBUTE);
	}

	/**
	 * 获取具有给定名称的第一个 cookie。请注意，多个 cookie 可以具有相同的名称但不同的路径或域。
	 * @param request current servlet request
	 * @param name cookie name
	 * @return the first cookie with the given name, or {@code null} if none is found
	 */
	@Nullable
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	/**
	 * 检查请求中是否发送了特定的 input type="submit" 参数，无论是通过按钮（直接使用名称）还是通过图像（名称 + ".x" 或名称 + ".y"）。
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return if the parameter was sent
	 * @see #SUBMIT_IMAGE_SUFFIXES
	 */
	public static boolean hasSubmitParameter(ServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		if (request.getParameter(name) != null) {
			return true;
		}
		for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
			if (request.getParameter(name + suffix) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从给定请求参数中获取指定名称的参数。<p>查找算法的说明请参见 {@link #findParameterValue(java.util.Map, String)}。
	 * @param request current HTTP request
	 * @param name the <i>logical</i> name of the request parameter
	 * @return the value of the parameter, or {@code null}
	 * if the parameter does not exist in given request
	 */
	@Nullable
	public static String findParameterValue(ServletRequest request, String name) {
		return findParameterValue(request.getParameterMap(), name);
	}

	/**
	 * 从给定请求参数中获取指定名称的参数。<p>此方法将尝试使用以下算法获取参数值：<ol> <li>尝试仅使用给定的<i>逻辑</i>名称获取参数值。这处理 {@code logicalName = value} 形式的参数。对于普通参数（例如通过隐藏 HTML 表单字段提交），这将返回所请求的值。</li> <li>尝试从参数名中获取参数值，其中请求中的参数名形式为 {@code logicalName_value = xyz}，"_" 为配置的分隔符。这处理通过 HTML 表单提交按钮提交的参数值。</li> <li>如果上一步获得的值带有 ".x" 或 ".y" 后缀，则将其移除。这处理通过 HTML 表单图像按钮提交值的情况。此时请求中的参数实际上形式为 {@code logicalName_value.x = 123}。</li> </ol>
	 * @param parameters the available parameter map
	 * @param name the <i>logical</i> name of the request parameter
	 * @return the value of the parameter, or {@code null}
	 * if the parameter does not exist in given request
	 */
	@Nullable
	public static String findParameterValue(Map<String, ?> parameters, String name) {
		// First try to get it as a normal name=value parameter
		Object value = parameters.get(name);
		if (value instanceof String[]) {
			String[] values = (String[]) value;
			return (values.length > 0 ? values[0] : null);
		}
		else if (value != null) {
			return value.toString();
		}
		// If no value yet, try to get it as a name_value=xyz parameter
		String prefix = name + "_";
		for (String paramName : parameters.keySet()) {
			if (paramName.startsWith(prefix)) {
				// Support images buttons, which would submit parameters as name_value.x=123
				for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
					if (paramName.endsWith(suffix)) {
						return paramName.substring(prefix.length(), paramName.length() - suffix.length());
					}
				}
				return paramName.substring(prefix.length());
			}
		}
		// We couldn't find the parameter value...
		return null;
	}

	/**
	 * 返回包含所有具有给定前缀的参数的 Map。单个值映射为 String，多个值映射为 String 数组。<p>例如，前缀为 "spring_" 时，"spring_param1" 和 "spring_param2" 会得到以 "param1" 和 "param2" 为键的 Map。
	 * @param request the HTTP request in which to look for parameters
	 * @param prefix the beginning of parameter names
	 * (if this is null or the empty string, all parameters will match)
	 * @return map containing request parameters <b>without the prefix</b>,
	 * containing either a String or a String array as values
	 * @see javax.servlet.ServletRequest#getParameterNames
	 * @see javax.servlet.ServletRequest#getParameterValues
	 * @see javax.servlet.ServletRequest#getParameterMap
	 */
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, @Nullable String prefix) {
		Assert.notNull(request, "Request must not be null");
		Enumeration<String> paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<>();
		if (prefix == null) {
			prefix = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			if (prefix.isEmpty() || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = request.getParameterValues(paramName);
				if (values == null || values.length == 0) {
					// Do nothing, no values found at all.
				}
				else if (values.length > 1) {
					params.put(unprefixed, values);
				}
				else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * 解析带矩阵变量的给定字符串。示例字符串如 {@code "q1=a;q1=b;q2=a,b,c"}。结果 Map 将包含键 {@code "q1"} 和 {@code "q2"}，值分别为 {@code ["a","b"]} 和 {@code ["a","b","c"]}。
	 * @param matrixVariables the unparsed matrix variables string
	 * @return a map with matrix variable names and values (never {@code null})
	 * @since 3.2
	 */
	public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		if (!StringUtils.hasText(matrixVariables)) {
			return result;
		}
		StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
		while (pairs.hasMoreTokens()) {
			String pair = pairs.nextToken();
			int index = pair.indexOf('=');
			if (index != -1) {
				String name = pair.substring(0, index);
				if (name.equalsIgnoreCase("jsessionid")) {
					continue;
				}
				String rawValue = pair.substring(index + 1);
				for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
					result.add(name, value);
				}
			}
			else {
				result.add(pair, "");
			}
		}
		return result;
	}

	/**
	 * 根据允许的来源列表检查给定请求来源。包含 "*" 的列表表示允许所有来源。空列表表示仅允许同源。<p><strong>注意：</strong>自 5.1 起，此方法忽略指定客户端发起地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。可考虑使用 {@code ForwardedHeaderFilter} 提取并使用这些头，或将其丢弃。
	 * @return {@code true} if the request origin is valid, {@code false} otherwise
	 * @since 4.1.5
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 */
	public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(allowedOrigins, "Allowed origins must not be null");

		String origin = request.getHeaders().getOrigin();
		if (origin == null || allowedOrigins.contains("*")) {
			return true;
		}
		else if (CollectionUtils.isEmpty(allowedOrigins)) {
			return isSameOrigin(request);
		}
		else {
			return allowedOrigins.contains(origin);
		}
	}

	/**
	 * 基于 {@code Origin}、{@code Host}、{@code Forwarded}、{@code X-Forwarded-Proto}、{@code X-Forwarded-Host} 和 {@code X-Forwarded-Port} 头，检查请求是否为同源请求。<p><strong>注意：</strong>自 5.1 起，此方法忽略指定客户端发起地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。可考虑使用 {@code ForwardedHeaderFilter} 提取并使用这些头，或将其丢弃。

	 * @return {@code true} if the request is a same-origin one, {@code false} in case
	 * of cross-origin request
	 * @since 4.2
	 */
	public static boolean isSameOrigin(HttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		String origin = headers.getOrigin();
		if (origin == null) {
			return true;
		}

		String scheme;
		String host;
		int port;
		if (request instanceof ServletServerHttpRequest) {
			// Build more efficiently if we can: we only need scheme, host, port for origin comparison
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			scheme = servletRequest.getScheme();
			host = servletRequest.getServerName();
			port = servletRequest.getServerPort();
		}
		else {
			URI uri = request.getURI();
			scheme = uri.getScheme();
			host = uri.getHost();
			port = uri.getPort();
		}

		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
		return (ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme()) &&
				ObjectUtils.nullSafeEquals(host, originUrl.getHost()) &&
				getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));
	}

	private static int getPort(@Nullable String scheme, int port) {
		if (port == -1) {
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			}
			else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		return port;
	}

}
