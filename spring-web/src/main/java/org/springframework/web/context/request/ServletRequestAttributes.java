/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 基于 Servlet 的 {@link RequestAttributes} 接口实现。
 *
 * <p>从 Servlet 请求作用域和 HTTP 会话作用域访问对象，不区分“session”和“global session”。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.servlet.ServletRequest#getAttribute
 * @see javax.servlet.http.HttpSession#getAttribute
 */
public class ServletRequestAttributes extends AbstractRequestAttributes {

	/**
	 * 标识销毁回调存储在 {@link HttpSession} 中时其名称前缀的 {@link String} 常量。
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			ServletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";

	protected static final Set<Class<?>> immutableValueTypes = new HashSet<>(16);

	static {
		immutableValueTypes.addAll(NumberUtils.STANDARD_NUMBER_TYPES);
		immutableValueTypes.add(Boolean.class);
		immutableValueTypes.add(Character.class);
		immutableValueTypes.add(String.class);
	}


	private final HttpServletRequest request;

	@Nullable
	private HttpServletResponse response;

	@Nullable
	private volatile HttpSession session;

	private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<>(1);


	/**
	 * 为给定的请求创建一个新的 ServletRequestAttributes 实例。
	 * @param request 当前 HTTP 请求
	 */
	public ServletRequestAttributes(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
	}

	/**
	 * 为给定的请求创建一个新的 ServletRequestAttributes 实例。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应（用于可选暴露）
	 */
	public ServletRequestAttributes(HttpServletRequest request, @Nullable HttpServletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * 暴露我们正在包装的原生 {@link HttpServletRequest}。
	 */
	public final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 暴露我们正在包装的原生 {@link HttpServletResponse}（如果有）。
	 */
	@Nullable
	public final HttpServletResponse getResponse() {
		return this.response;
	}

	/**
	 * 暴露我们正在包装的 {@link HttpSession}。
	 * @param allowCreate 在尚不存在会话时是否允许创建新会话
	 */
	@Nullable
	protected final HttpSession getSession(boolean allowCreate) {
		if (isRequestActive()) {
			HttpSession session = this.request.getSession(allowCreate);
			this.session = session;
			return session;
		}
		else {
			// Access through stored session reference, if any...
			HttpSession session = this.session;
			if (session == null) {
				if (allowCreate) {
					throw new IllegalStateException(
							"No session found and request already completed - cannot create new session!");
				}
				else {
					session = this.request.getSession(false);
					this.session = session;
				}
			}
			return session;
		}
	}

	private HttpSession obtainSession() {
		HttpSession session = getSession(true);
		Assert.state(session != null, "No HttpSession");
		return session;
	}


	@Override
	public Object getAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attribute - request is not active anymore!");
			}
			return this.request.getAttribute(name);
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					Object value = session.getAttribute(name);
					if (value != null) {
						this.sessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			return null;
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot set request attribute - request is not active anymore!");
			}
			this.request.setAttribute(name, value);
		}
		else {
			HttpSession session = obtainSession();
			this.sessionAttributesToUpdate.remove(name);
			session.setAttribute(name, value);
		}
	}

	@Override
	public void removeAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (isRequestActive()) {
				removeRequestDestructionCallback(name);
				this.request.removeAttribute(name);
			}
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				this.sessionAttributesToUpdate.remove(name);
				try {
					session.removeAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
					session.removeAttribute(name);
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
		}
	}

	@Override
	public String[] getAttributeNames(int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attributes - request is not active anymore!");
			}
			return StringUtils.toStringArray(this.request.getAttributeNames());
		}
		else {
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					return StringUtils.toStringArray(session.getAttributeNames());
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			return new String[0];
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (scope == SCOPE_REQUEST) {
			registerRequestDestructionCallback(name, callback);
		}
		else {
			registerSessionDestructionCallback(name, callback);
		}
	}

	@Override
	public Object resolveReference(String key) {
		if (REFERENCE_REQUEST.equals(key)) {
			return this.request;
		}
		else if (REFERENCE_SESSION.equals(key)) {
			return getSession(true);
		}
		else {
			return null;
		}
	}

	@Override
	public String getSessionId() {
		return obtainSession().getId();
	}

	@Override
	public Object getSessionMutex() {
		return WebUtils.getSessionMutex(obtainSession());
	}


	/**
	 * 通过 {@code session.setAttribute} 调用更新所有已访问的会话属性，明确告知容器这些属性可能已被修
	 * 改。
	 */
	@Override
	protected void updateAccessedSessionAttributes() {
		if (!this.sessionAttributesToUpdate.isEmpty()) {
			// Update all affected session attributes.
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name);
						if (oldValue == newValue && !isImmutableSessionAttribute(name, newValue)) {
							session.setAttribute(name, newValue);
						}
					}
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			this.sessionAttributesToUpdate.clear();
		}
	}

	/**
	 * 确定给定值是否应被视为不可变的会话属性，即无需通过 {@code session.setAttribute} 重新设置，因为
	 * 其值在内部不会发生有意义的改变。
	 * <p>对于 {@code String}、{@code Character}、{@code Boolean} 和标准 {@code Number} 值，默认实现
	 * 返回 {@code true}。
	 * @param name 属性的名称
	 * @param value 要检查的相应值
	 * @return 如果该值在会话属性管理意义上应视为不可变，
	 * 则返回 {@code true}；否则返回 {@code false}
	 * @see #updateAccessedSessionAttributes()
	 */
	protected boolean isImmutableSessionAttribute(String name, @Nullable Object value) {
		return (value == null || immutableValueTypes.contains(value.getClass()));
	}

	/**
	 * 注册给定的回调，使其在会话终止后执行。
	 * <p>注意：回调对象应可序列化，以便在 Web 应用重启后仍然有效。
	 * @param name 为其注册回调的属性的名称
	 * @param callback 销毁时要执行的回调
	 */
	protected void registerSessionDestructionCallback(String name, Runnable callback) {
		HttpSession session = obtainSession();
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name,
				new DestructionCallbackBindingListener(callback));
	}


	@Override
	public String toString() {
		return this.request.toString();
	}

}
