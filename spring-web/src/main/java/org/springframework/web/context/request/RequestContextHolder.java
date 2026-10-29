/*
 * Copyright 2002-2016 the original author or authors.
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

import javax.faces.context.FacesContext;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 持有类，以线程绑定的 {@link RequestAttributes} 对象形式暴露 Web 请求。如果 {@code inheritable}
 * 标志设置为 {@code true}，则由当前线程创建的任何子线程都会继承该请求。
 *
 * <p>使用 {@link RequestContextListener} 或
 * {@link org.springframework.web.filter.RequestContextFilter} 来暴露
 * 当前的 Web 请求。注意，
 * {@link org.springframework.web.servlet.DispatcherServlet}
 * 默认情况下已经暴露了当前请求。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see RequestContextListener
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public abstract class RequestContextHolder  {

	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());

	private static final ThreadLocal<RequestAttributes> requestAttributesHolder =
			new NamedThreadLocal<>("Request attributes");

	private static final ThreadLocal<RequestAttributes> inheritableRequestAttributesHolder =
			new NamedInheritableThreadLocal<>("Request context");


	/**
	 * 重置当前线程的 RequestAttributes。
	 */
	public static void resetRequestAttributes() {
		requestAttributesHolder.remove();
		inheritableRequestAttributesHolder.remove();
	}

	/**
	 * 将给定的 RequestAttributes 绑定到当前线程，<i>不</i>将其暴露为可被子线程继承。
	 * @param attributes the RequestAttributes to expose
	 * @see #setRequestAttributes(RequestAttributes, boolean)
	 */
	public static void setRequestAttributes(@Nullable RequestAttributes attributes) {
		setRequestAttributes(attributes, false);
	}

	/**
	 * 将给定的 RequestAttributes 绑定到当前线程。
	 * @param attributes the RequestAttributes to expose,
	 * or {@code null} to reset the thread-bound context
	 * @param inheritable whether to expose the RequestAttributes as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 */
	public static void setRequestAttributes(@Nullable RequestAttributes attributes, boolean inheritable) {
		if (attributes == null) {
			resetRequestAttributes();
		}
		else {
			if (inheritable) {
				inheritableRequestAttributesHolder.set(attributes);
				requestAttributesHolder.remove();
			}
			else {
				requestAttributesHolder.set(attributes);
				inheritableRequestAttributesHolder.remove();
			}
		}
	}

	/**
	 * 返回当前绑定到该线程的 RequestAttributes。
	 * @return the RequestAttributes currently bound to the thread,
	 * or {@code null} if none bound
	 */
	@Nullable
	public static RequestAttributes getRequestAttributes() {
		RequestAttributes attributes = requestAttributesHolder.get();
		if (attributes == null) {
			attributes = inheritableRequestAttributesHolder.get();
		}
		return attributes;
	}

	/**
	 * 返回当前绑定到该线程的 RequestAttributes。
	 * <p>如果存在先前绑定的 RequestAttributes 实例，则将其暴露出来。否则回退到当前的 JSF
	 * FacesContext（如果有）。
	 * @return the RequestAttributes currently bound to the thread
	 * @throws IllegalStateException if no RequestAttributes object
	 * is bound to the current thread
	 * @see #setRequestAttributes
	 * @see ServletRequestAttributes
	 * @see FacesRequestAttributes
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public static RequestAttributes currentRequestAttributes() throws IllegalStateException {
		RequestAttributes attributes = getRequestAttributes();
		if (attributes == null) {
			if (jsfPresent) {
				attributes = FacesRequestAttributesFactory.getFacesRequestAttributes();
			}
			if (attributes == null) {
				throw new IllegalStateException("No thread-bound request found: " +
						"Are you referring to request attributes outside of an actual web request, " +
						"or processing a request outside of the originally receiving thread? " +
						"If you are actually operating within a web request and still receive this message, " +
						"your code is probably running outside of DispatcherServlet: " +
						"In this case, use RequestContextListener or RequestContextFilter to expose the current request.");
			}
		}
		return attributes;
	}


	/**
	 * 内部类，用于避免硬编码的 JSF 依赖。
 	 */
	private static class FacesRequestAttributesFactory {

		@Nullable
		public static RequestAttributes getFacesRequestAttributes() {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			return (facesContext != null ? new FacesRequestAttributes(facesContext) : null);
		}
	}

}
