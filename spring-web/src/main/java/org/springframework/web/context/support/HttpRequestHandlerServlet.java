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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.WebApplicationContext;

/**
 * 简单的 HttpServlet，将请求委托给定义在 Spring 根 Web 应用上下文中的 {@link HttpRequestHandler}
 * bean。目标 bean 名称必须与 {@code web.xml} 中定义的 HttpRequestHandlerServlet servlet-name 一
 * 致。
 *
 * <p>例如，可以用它在每个 HttpRequestHandlerServlet 定义中暴露单个 Spring 远程导出器，
 * 例如 {@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 * 或 {@link org.springframework.remoting.caucho.HessianServiceExporter}。
 * 与在 DispatcherServlet 上下文中将远程导出器定义为 bean 相比，这是一种最小的替代方案
 * （后者提供了高级的映射和拦截功能）。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.DispatcherServlet
 */
@SuppressWarnings("serial")
public class HttpRequestHandlerServlet extends HttpServlet {

	@Nullable
	private HttpRequestHandler target;


	@Override
	public void init() throws ServletException {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		this.target = wac.getBean(getServletName(), HttpRequestHandler.class);
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.target != null, "No HttpRequestHandler available");

		LocaleContextHolder.setLocale(request.getLocale());
		try {
			this.target.handleRequest(request, response);
		}
		catch (HttpRequestMethodNotSupportedException ex) {
			String[] supportedMethods = ex.getSupportedMethods();
			if (supportedMethods != null) {
				response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
			}
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

}
