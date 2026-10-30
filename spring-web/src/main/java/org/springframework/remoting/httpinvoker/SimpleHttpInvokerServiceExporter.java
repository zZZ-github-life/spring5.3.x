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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * HTTP 请求处理器，将指定的服务 bean 导出为
 * HTTP invoker 服务端点，可通过 HTTP invoker 代理访问。
 * 专为 Sun JRE 1.6 HTTP 服务器设计，实现了
 * {@link com.sun.net.httpserver.HttpHandler} 接口。
 *
 * <p>反序列化远程调用对象，并序列化远程调用结果对象。
 * 与 RMI 一样使用 Java 序列化，但提供了与 Caucho 基于 HTTP 的
 * Hessian 协议同样简单的配置方式。
 *
 * <p><b>HTTP invoker 是 Java 到 Java 远程调用的推荐协议。</b>
 * 它比 Hessian 更强大、更具扩展性，代价是与 Java 绑定。
 * 不过，它的配置与 Hessian 一样简单，这是它相对于 RMI 的主要优势。
 *
 * <p><b>警告：请注意不安全的 Java 反序列化所带来的漏洞：
 * 被篡改的输入流可能导致在反序列化步骤中于服务器上执行非预期代码。
 * 因此，不要将 HTTP invoker 端点暴露给不受信任的客户端，
 * 而应仅在自己内部的服务之间使用。</b>
 * 一般而言，我们强烈建议改用其他任何消息格式（例如 JSON）。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor
 * @see org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean
 * @deprecated as of Spring Framework 5.1, in favor of {@link HttpInvokerServiceExporter}
 */
@Deprecated
@org.springframework.lang.UsesSunHttpServer
public class SimpleHttpInvokerServiceExporter extends org.springframework.remoting.rmi.RemoteInvocationSerializingExporter implements HttpHandler {

	/**
	 * 从请求中读取远程调用，执行该调用，
	 * 并将远程调用结果写入响应。
	 * @see #readRemoteInvocation(HttpExchange)
	 * @see #invokeAndCreateResult(RemoteInvocation, Object)
	 * @see #writeRemoteInvocationResult(HttpExchange, RemoteInvocationResult)
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			RemoteInvocation invocation = readRemoteInvocation(exchange);
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			writeRemoteInvocationResult(exchange, result);
			exchange.close();
		}
		catch (ClassNotFoundException ex) {
			exchange.sendResponseHeaders(500, -1);
			logger.error("Class not found during deserialization", ex);
		}
	}

	/**
	 * 从给定的 HTTP 请求中读取 RemoteInvocation。
	 * <p>使用 {@link HttpExchange#getRequestBody()} 请求的输入流}
	 * 委托给 {@link #readRemoteInvocation(HttpExchange, InputStream)}。
	 * @param exchange 当前 HTTP 请求/响应
	 * @return RemoteInvocation 对象
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 * @throws ClassNotFoundException 如果由反序列化抛出则传播
	 */
	protected RemoteInvocation readRemoteInvocation(HttpExchange exchange)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(exchange, exchange.getRequestBody());
	}

	/**
	 * 从给定的 InputStream 中反序列化 RemoteInvocation 对象。
	 * <p>先给 {@link #decorateInputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。创建
	 * {@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}，
	 * 并调用 {@link #doReadRemoteInvocation} 实际读取对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param exchange 当前 HTTP 请求/响应
	 * @param is 要读取的 InputStream
	 * @return RemoteInvocation 对象
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 * @throws ClassNotFoundException 如果在反序列化期间抛出则传播
	 */
	protected RemoteInvocation readRemoteInvocation(HttpExchange exchange, InputStream is)
			throws IOException, ClassNotFoundException {

		ObjectInputStream ois = createObjectInputStream(decorateInputStream(exchange, is));
		return doReadRemoteInvocation(ois);
	}

	/**
	 * 返回用于读取远程调用的 InputStream，
	 * 可能会装饰给定的原始 InputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param exchange 当前 HTTP 请求/响应
	 * @param is 原始 InputStream
	 * @return 可能已被装饰的 InputStream
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 */
	protected InputStream decorateInputStream(HttpExchange exchange, InputStream is) throws IOException {
		return is;
	}

	/**
	 * 将给定的 RemoteInvocationResult 写入给定的 HTTP 响应。
	 * @param exchange 当前 HTTP 请求/响应
	 * @param result RemoteInvocationResult 对象
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 */
	protected void writeRemoteInvocationResult(HttpExchange exchange, RemoteInvocationResult result)
			throws IOException {

		exchange.getResponseHeaders().set("Content-Type", getContentType());
		exchange.sendResponseHeaders(200, 0);
		writeRemoteInvocationResult(exchange, result, exchange.getResponseBody());
	}

	/**
	 * 将给定的 RemoteInvocation 序列化到给定的 OutputStream 中。
	 * <p>默认实现会先给 {@link #decorateOutputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。
	 * 为最终的流创建 {@link java.io.ObjectOutputStream}，并调用
	 * {@link #doWriteRemoteInvocationResult} 实际写入对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param exchange 当前 HTTP 请求/响应
	 * @param result RemoteInvocationResult 对象
	 * @param os 要写入的 OutputStream
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 * @see #decorateOutputStream
	 * @see #doWriteRemoteInvocationResult
	 */
	protected void writeRemoteInvocationResult(
			HttpExchange exchange, RemoteInvocationResult result, OutputStream os) throws IOException {

		ObjectOutputStream oos = createObjectOutputStream(decorateOutputStream(exchange, os));
		doWriteRemoteInvocationResult(result, oos);
		oos.flush();
	}

	/**
	 * 返回用于写入远程调用结果的 OutputStream，
	 * 可能会装饰给定的原始 OutputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param exchange 当前 HTTP 请求/响应
	 * @param os 原始 OutputStream
	 * @return 可能已被装饰的 OutputStream
	 * @throws java.io.IOException 发生 I/O 失败时抛出
	 */
	protected OutputStream decorateOutputStream(HttpExchange exchange, OutputStream os) throws IOException {
		return os;
	}

}
