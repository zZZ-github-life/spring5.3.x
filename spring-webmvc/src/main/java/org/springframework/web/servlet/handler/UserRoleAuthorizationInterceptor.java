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

package org.springframework.web.servlet.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截器，通过 HttpServletRequest 的 isUserInRole 方法评估用户的角色，
 * 以此检查当前用户的授权。
 *
 * @author Juergen Hoeller
 * @since 20.06.2003
 * @see javax.servlet.http.HttpServletRequest#isUserInRole
 */
public class UserRoleAuthorizationInterceptor implements HandlerInterceptor {

	@Nullable
	private String[] authorizedRoles;


	/**
	 * 设置此拦截器应视为已授权的角色。
	 * @param authorizedRoles 角色名称数组
	 */
	public final void setAuthorizedRoles(String... authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}


	@Override
	public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		if (this.authorizedRoles != null) {
			for (String role : this.authorizedRoles) {
				if (request.isUserInRole(role)) {
					return true;
				}
			}
		}
		handleNotAuthorized(request, response, handler);
		return false;
	}

	/**
	 * 处理根据此拦截器未获授权的请求。
	 * 默认实现发送 HTTP 状态码 403（"forbidden"）。
	 * <p>可以重写此方法以写入自定义消息、转发或
	 * 重定向到某个错误页面或登录页面，或者抛出 ServletException。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler 选中的待执行处理器，用于类型和/或实例判断
	 * @throws javax.servlet.ServletException 发生内部错误时
	 * @throws java.io.IOException 写入响应时发生 I/O 错误的情况下
	 */
	protected void handleNotAuthorized(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
