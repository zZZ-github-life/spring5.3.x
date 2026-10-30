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

package org.springframework.web.context.support;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * HttpServletRequest 装饰器，通过在一次访问属性时进行延迟检查，使给定 WebApplicationContext 中的
 * 所有 Spring bean 可作为请求属性访问。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {

	private final WebApplicationContext webApplicationContext;

	@Nullable
	private final Set<String> exposedContextBeanNames;

	@Nullable
	private Set<String> explicitAttributes;


	/**
	 * 为给定的请求创建一个新的 ContextExposingHttpServletRequest。
	 * @param originalRequest 原始的 HttpServletRequest
	 * @param context 该请求运行所在的 WebApplicationContext
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
		this(originalRequest, context, null);
	}

	/**
	 * 为给定的请求创建一个新的 ContextExposingHttpServletRequest。
	 * @param originalRequest 原始的 HttpServletRequest
	 * @param context 该请求运行所在的 WebApplicationContext
	 * @param exposedContextBeanNames 上下文中有望暴露的 Bean 的名称（如果该值非 null，
	 * 则只有此 Set 中的 Bean 有资格作为属性暴露）
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context,
			@Nullable Set<String> exposedContextBeanNames) {

		super(originalRequest);
		Assert.notNull(context, "WebApplicationContext must not be null");
		this.webApplicationContext = context;
		this.exposedContextBeanNames = exposedContextBeanNames;
	}


	/**
	 * 返回此请求运行所在的 WebApplicationContext。
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	@Override
	@Nullable
	public Object getAttribute(String name) {
		if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) &&
				(this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) &&
				this.webApplicationContext.containsBean(name)) {
			return this.webApplicationContext.getBean(name);
		}
		else {
			return super.getAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);
		if (this.explicitAttributes == null) {
			this.explicitAttributes = new HashSet<>(8);
		}
		this.explicitAttributes.add(name);
	}

}
