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

package org.springframework.remoting.httpinvoker;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.util.NestedServletException;

/**
 * 基于 Servlet API 的 HTTP 请求处理器，将指定的服务 bean 导出为
 * HTTP invoker 服务端点，可通过 HTTP invoker 代理访问。
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
 * @since 1.1
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerProxyFactoryBean
 * @see org.springframework.remoting.rmi.RmiServiceExporter
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HttpInvokerServiceExporter extends org.springframework.remoting.rmi.RemoteInvocationSerializingExporter implements HttpRequestHandler {

	/**
	 * 从请求中读取远程调用，执行该调用，
	 * 并将远程调用结果写入响应。
	 * @see #readRemoteInvocation(HttpServletRequest)
	 * @see #invokeAndCreateResult(org.springframework.remoting.support.RemoteInvocation, Object)
	 * @see #writeRemoteInvocationResult(HttpServletRequest, HttpServletResponse, RemoteInvocationResult)
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			RemoteInvocation invocation = readRemoteInvocation(request);
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			writeRemoteInvocationResult(request, response, result);
		}
		catch (ClassNotFoundException ex) {
			throw new NestedServletException("Class not found during deserialization", ex);
		}
	}

	/**
	 * 从给定的 HTTP 请求中读取 RemoteInvocation。
	 * <p>使用 {@link HttpServletRequest#getInputStream() servlet 请求的输入流}
	 * 委托给 {@link #readRemoteInvocation(HttpServletRequest, InputStream)}。
	 * @param request 当前 HTTP 请求
	 * @return RemoteInvocation 对象
	 * @throws IOException 发生 I/O 失败时抛出
	 * @throws ClassNotFoundException 如果由反序列化抛出则传播
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(request, request.getInputStream());
	}

	/**
	 * 从给定的 InputStream 中反序列化 RemoteInvocation 对象。
	 * <p>先给 {@link #decorateInputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。创建
	 * {@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}，
	 * 并调用 {@link #doReadRemoteInvocation} 实际读取对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param request 当前 HTTP 请求
	 * @param is 要读取的 InputStream
	 * @return RemoteInvocation 对象
	 * @throws IOException 发生 I/O 失败时抛出
	 * @throws ClassNotFoundException 如果在反序列化期间抛出则传播
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is)
			throws IOException, ClassNotFoundException {

		try (ObjectInputStream ois = createObjectInputStream(decorateInputStream(request, is))) {
			return doReadRemoteInvocation(ois);
		}
	}

	/**
	 * 返回用于读取远程调用的 InputStream，
	 * 可能会装饰给定的原始 InputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param request 当前 HTTP 请求
	 * @param is 原始 InputStream
	 * @return 可能已被装饰的 InputStream
	 * @throws IOException 发生 I/O 失败时抛出
	 */
	protected InputStream decorateInputStream(HttpServletRequest request, InputStream is) throws IOException {
		return is;
	}

	/**
	 * 将给定的 RemoteInvocationResult 写入给定的 HTTP 响应。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param result RemoteInvocationResult 对象
	 * @throws IOException 发生 I/O 失败时抛出
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result)
			throws IOException {

		response.setContentType(getContentType());
		writeRemoteInvocationResult(request, response, result, response.getOutputStream());
	}

	/**
	 * 将给定的 RemoteInvocation 序列化到给定的 OutputStream 中。
	 * <p>默认实现会先给 {@link #decorateOutputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。
	 * 为最终的流创建 {@link java.io.ObjectOutputStream}，并调用
	 * {@link #doWriteRemoteInvocationResult} 实际写入对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param result RemoteInvocationResult 对象
	 * @param os 要写入的 OutputStream
	 * @throws IOException 发生 I/O 失败时抛出
	 * @see #decorateOutputStream
	 * @see #doWriteRemoteInvocationResult
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result, OutputStream os)
			throws IOException {

		try (ObjectOutputStream oos =
					createObjectOutputStream(new FlushGuardedOutputStream(decorateOutputStream(request, response, os)))) {
			doWriteRemoteInvocationResult(result, oos);
		}
	}

	/**
	 * 返回用于写入远程调用结果的 OutputStream，
	 * 可能会装饰给定的原始 OutputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param os 原始 OutputStream
	 * @return 可能已被装饰的 OutputStream
	 * @throws IOException 发生 I/O 失败时抛出
	 */
	protected OutputStream decorateOutputStream(
			HttpServletRequest request, HttpServletResponse response, OutputStream os) throws IOException {

		return os;
	}


	/**
	 * 装饰 {@code OutputStream} 以防止 {@code flush()} 调用，
	 * 这些调用会被转换为空操作。
	 * <p>由于 {@link ObjectOutputStream#close()} 实际上会两次刷新/排空
	 * 底层流，此 {@link FilterOutputStream} 会防止单次 flush 调用。
	 * 多次 flush 调用可能导致性能问题，因为写入不会被按预期汇聚。
	 * @see <a href="https://jira.spring.io/browse/SPR-14040">SPR-14040</a>
	 */
	private static class FlushGuardedOutputStream extends FilterOutputStream {

		public FlushGuardedOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void flush() throws IOException {
			// Do nothing on flush
		}
	}

}
