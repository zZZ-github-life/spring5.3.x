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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.util.FileCopyUtils;

/**
 * HTTP 请求处理器，将指定的服务 bean 导出为
 * Hessian 服务端点，可通过 Hessian 代理访问。
 * 专为 Sun JRE 1.6 HTTP 服务器设计，实现了
 * {@link com.sun.net.httpserver.HttpHandler} 接口。
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
 * @since 2.5.1
 * @see org.springframework.remoting.caucho.HessianClientInterceptor
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @deprecated as of Spring Framework 5.1, in favor of {@link HessianServiceExporter}
 */
@Deprecated
@org.springframework.lang.UsesSunHttpServer
public class SimpleHessianServiceExporter extends HessianExporter implements HttpHandler {

	/**
	 * 处理传入的 Hessian 请求并创建 Hessian 响应。
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Allow", "POST");
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		try {
			invoke(exchange.getRequestBody(), output);
		}
		catch (Throwable ex) {
			exchange.sendResponseHeaders(500, -1);
			logger.error("Hessian skeleton invocation failed", ex);
			return;
		}

		exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_HESSIAN);
		exchange.sendResponseHeaders(200, output.size());
		FileCopyUtils.copy(output.toByteArray(), exchange.getResponseBody());
	}

}
