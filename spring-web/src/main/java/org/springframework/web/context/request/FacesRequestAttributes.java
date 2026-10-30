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

package org.springframework.web.context.request;

import java.lang.reflect.Method;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 用于 JSF {@link javax.faces.context.FacesContext} 的 {@link RequestAttributes} 适配器。
 * 在 JSF 环境中作为默认实现使用，封装当前的 FacesContext。
 *
 * <p><b>注意：</b>与 {@link ServletRequestAttributes} 不同，此变体<i>不</i>支持作用域属性的销毁回调，
 * 无论是请求作用域还是会话作用域均不支持。如果依赖此类隐式销毁回调，
 * 可考虑在 {@code web.xml} 中定义一个 Spring {@link RequestContextListener}。
 *
 * <p>自 Spring 4.0 起，要求 JSF 2.0 或更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see javax.faces.context.FacesContext#getExternalContext()
 * @see javax.faces.context.ExternalContext#getRequestMap()
 * @see javax.faces.context.ExternalContext#getSessionMap()
 * @see RequestContextHolder#currentRequestAttributes()
 */
public class FacesRequestAttributes implements RequestAttributes {

	/**
	 * 会创建大量此类对象，因此不应每次都新建 Logger。
	 */
	private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);

	private final FacesContext facesContext;


	/**
	 * 为给定的 FacesContext 创建一个新的 FacesRequestAttributes 适配器。
	 * @param facesContext 当前的 FacesContext
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesRequestAttributes(FacesContext facesContext) {
		Assert.notNull(facesContext, "FacesContext must not be null");
		this.facesContext = facesContext;
	}


	/**
	 * 返回此适配器所操作的 JSF FacesContext。
	 */
	protected final FacesContext getFacesContext() {
		return this.facesContext;
	}

	/**
	 * 返回此适配器所操作的 JSF ExternalContext。
	 * @see javax.faces.context.FacesContext#getExternalContext()
	 */
	protected final ExternalContext getExternalContext() {
		return getFacesContext().getExternalContext();
	}

	/**
	 * 返回指定作用域的 JSF 属性 Map。
	 * @param scope 指示请求或会话作用域的常量
	 * @return 指定作用域中属性的 Map 表示
	 * @see #SCOPE_REQUEST
	 * @see #SCOPE_SESSION
	 */
	protected Map<String, Object> getAttributeMap(int scope) {
		if (scope == SCOPE_REQUEST) {
			return getExternalContext().getRequestMap();
		}
		else {
			return getExternalContext().getSessionMap();
		}
	}


	@Override
	public Object getAttribute(String name, int scope) {
		return getAttributeMap(scope).get(name);
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		getAttributeMap(scope).put(name, value);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		getAttributeMap(scope).remove(name);
	}

	@Override
	public String[] getAttributeNames(int scope) {
		return StringUtils.toStringArray(getAttributeMap(scope).keySet());
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (logger.isWarnEnabled()) {
			logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name +
					"' because FacesRequestAttributes does not support such callbacks");
		}
	}

	@Override
	public Object resolveReference(String key) {
		if (REFERENCE_REQUEST.equals(key)) {
			return getExternalContext().getRequest();
		}
		else if (REFERENCE_SESSION.equals(key)) {
			return getExternalContext().getSession(true);
		}
		else if ("application".equals(key)) {
			return getExternalContext().getContext();
		}
		else if ("requestScope".equals(key)) {
			return getExternalContext().getRequestMap();
		}
		else if ("sessionScope".equals(key)) {
			return getExternalContext().getSessionMap();
		}
		else if ("applicationScope".equals(key)) {
			return getExternalContext().getApplicationMap();
		}
		else if ("facesContext".equals(key)) {
			return getFacesContext();
		}
		else if ("cookie".equals(key)) {
			return getExternalContext().getRequestCookieMap();
		}
		else if ("header".equals(key)) {
			return getExternalContext().getRequestHeaderMap();
		}
		else if ("headerValues".equals(key)) {
			return getExternalContext().getRequestHeaderValuesMap();
		}
		else if ("param".equals(key)) {
			return getExternalContext().getRequestParameterMap();
		}
		else if ("paramValues".equals(key)) {
			return getExternalContext().getRequestParameterValuesMap();
		}
		else if ("initParam".equals(key)) {
			return getExternalContext().getInitParameterMap();
		}
		else if ("view".equals(key)) {
			return getFacesContext().getViewRoot();
		}
		else if ("viewScope".equals(key)) {
			return getFacesContext().getViewRoot().getViewMap();
		}
		else if ("flash".equals(key)) {
			return getExternalContext().getFlash();
		}
		else if ("resource".equals(key)) {
			return getFacesContext().getApplication().getResourceHandler();
		}
		else {
			return null;
		}
	}

	@Override
	public String getSessionId() {
		Object session = getExternalContext().getSession(true);
		try {
			// HttpSession has a getId() method.
			Method getIdMethod = session.getClass().getMethod("getId");
			return String.valueOf(ReflectionUtils.invokeMethod(getIdMethod, session));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
		}
	}

	@Override
	public Object getSessionMutex() {
		// Enforce presence of a session first to allow listeners to create the mutex attribute
		ExternalContext externalContext = getExternalContext();
		Object session = externalContext.getSession(true);
		Object mutex = externalContext.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = (session != null ? session : externalContext);
		}
		return mutex;
	}

}
