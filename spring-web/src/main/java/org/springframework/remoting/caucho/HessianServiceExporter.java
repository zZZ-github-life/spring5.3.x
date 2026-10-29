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

package org.springframework.remoting.caucho;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.util.NestedServletException;

/**
 * 基于 Servlet API 的 HTTP 请求处理器，将指定的服务 bean 导出为
 * Hessian 服务端点，可通过 Hessian 代理访问。
 *
 * <p>Hessian 是一种轻量级二进制 RPC 协议。
 * 有关 Hessian 的信息，请参阅
 * <a href="http://hessian.caucho.com">Hessian 网站</a>。
 * <b>注意：从 Spring 4.0 起，此导出器要求 Hessian 4.0 或更高版本。</b>
 *
 * <p>使用此类导出的 Hessian 服务可由任何 Hessian 客户端访问，
 * 因为其中不涉及任何特殊处理。
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see HessianClientInterceptor
 * @see HessianProxyFactoryBean
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 * @see org.springframework.remoting.rmi.RmiServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HessianServiceExporter extends HessianExporter implements HttpRequestHandler {

	/**
	 * 处理传入的 Hessian 请求并创建 Hessian 响应。
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (!"POST".equals(request.getMethod())) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(),
					new String[] {"POST"}, "HessianServiceExporter only supports POST requests");
		}

		response.setContentType(CONTENT_TYPE_HESSIAN);
		try {
			invoke(request.getInputStream(), response.getOutputStream());
		}
		catch (Throwable ex) {
			throw new NestedServletException("Hessian skeleton invocation failed", ex);
		}
	}

}
