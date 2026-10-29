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

import java.io.IOException;
import java.io.InvalidClassException;
import java.net.ConnectException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}，用于访问
 * HTTP invoker 服务。服务 URL 必须是公开
 * HTTP invoker 服务的 HTTP URL。
 *
 * <p>序列化远程调用对象，并反序列化远程调用结果对象。
 * 与 RMI 一样使用 Java 序列化，但提供了与 Caucho 基于 HTTP 的
 * Hessian 协议同样简单的配置方式。
 *
 * <P>HTTP invoker 是一种极具扩展性和可定制性的协议。
 * 它支持 RemoteInvocationFactory 机制（与 RMI invoker 类似），
 * 允许包含额外的调用属性（例如安全上下文）。此外，它还允许
 * 通过 {@link HttpInvokerRequestExecutor} 策略自定义请求执行。
 *
 * <p>可以使用 JDK 的 {@link java.rmi.server.RMIClassLoader} 从给定的
 * {@link #setCodebaseUrl codebase} 加载类，实现从远程位置按需动态
 * 下载代码。codebase 可以由多个 URL 组成，以空格分隔。请注意，
 * RMIClassLoader 要求设置 SecurityManager，这与使用标准 RMI 进行
 * 动态类下载时的情况类似！（有关详细信息，请参阅 RMI 文档。）
 *
 * <p><b>警告：请注意不安全的 Java 反序列化所带来的漏洞：
 * 被篡改的输入流可能导致在反序列化步骤中于服务器上执行非预期代码。
 * 因此，不要将 HTTP invoker 端点暴露给不受信任的客户端，
 * 而应仅在自己内部的服务之间使用。</b>
 * 一般而言，我们强烈建议改用其他任何消息格式（例如 JSON）。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceUrl
 * @see #setCodebaseUrl
 * @see #setRemoteInvocationFactory
 * @see #setHttpInvokerRequestExecutor
 * @see HttpInvokerServiceExporter
 * @see HttpInvokerProxyFactoryBean
 * @see java.rmi.server.RMIClassLoader
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HttpInvokerClientInterceptor extends RemoteInvocationBasedAccessor
		implements MethodInterceptor, HttpInvokerClientConfiguration {

	@Nullable
	private String codebaseUrl;

	@Nullable
	private HttpInvokerRequestExecutor httpInvokerRequestExecutor;


	/**
	 * 设置在本地找不到类时用于下载类的 codebase URL。
	 * 可以由多个 URL 组成，以空格分隔。
	 * <p>遵循 RMI 的动态类下载 codebase 约定。
	 * 与 RMI 不同（RMI 中由服务器通过 "java.rmi.server.codebase"
	 * 系统属性决定类下载的 URL），此处 codebase URL 由客户端决定。
	 * 服务器通常与服务 URL 相同，只是指向其中的不同路径。
	 * @see #setServiceUrl
	 * @see org.springframework.remoting.rmi.CodebaseAwareObjectInputStream
	 * @see java.rmi.server.RMIClassLoader
	 */
	public void setCodebaseUrl(@Nullable String codebaseUrl) {
		this.codebaseUrl = codebaseUrl;
	}

	/**
	 * 返回在本地找不到类时用于下载类的 codebase URL。
	 */
	@Override
	@Nullable
	public String getCodebaseUrl() {
		return this.codebaseUrl;
	}

	/**
	 * 设置用于执行远程调用的 HttpInvokerRequestExecutor 实现。
	 * <p>默认为 {@link SimpleHttpInvokerRequestExecutor}。对于更复杂的需求，
	 * 可考虑使用 {@link HttpComponentsHttpInvokerRequestExecutor}。
	 * @see SimpleHttpInvokerRequestExecutor
	 * @see HttpComponentsHttpInvokerRequestExecutor
	 */
	public void setHttpInvokerRequestExecutor(HttpInvokerRequestExecutor httpInvokerRequestExecutor) {
		this.httpInvokerRequestExecutor = httpInvokerRequestExecutor;
	}

	/**
	 * 返回此远程访问器使用的 HttpInvokerRequestExecutor。
	 * <p>如果尚未初始化任何执行器，则创建默认的
	 * SimpleHttpInvokerRequestExecutor。
	 */
	public HttpInvokerRequestExecutor getHttpInvokerRequestExecutor() {
		if (this.httpInvokerRequestExecutor == null) {
			SimpleHttpInvokerRequestExecutor executor = new SimpleHttpInvokerRequestExecutor();
			executor.setBeanClassLoader(getBeanClassLoader());
			this.httpInvokerRequestExecutor = executor;
		}
		return this.httpInvokerRequestExecutor;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Eagerly initialize the default HttpInvokerRequestExecutor, if needed.
		getHttpInvokerRequestExecutor();
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "HTTP invoker proxy for service URL [" + getServiceUrl() + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;

		try {
			result = executeRequest(invocation, methodInvocation);
		}
		catch (Throwable ex) {
			RemoteAccessException rae = convertHttpInvokerAccessException(ex);
			throw (rae != null ? rae : ex);
		}

		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
			}
		}
	}

	/**
	 * 通过 {@link HttpInvokerRequestExecutor} 执行给定的远程调用。
	 * <p>此实现委托给 {@link #executeRequest(RemoteInvocation)}。
	 * 可重写以针对特定的原始 MethodInvocation 做出反应。
	 * @param invocation the RemoteInvocation to execute
	 * @param originalInvocation the original MethodInvocation (can e.g. be cast
	 * to the ProxyMethodInvocation interface for accessing user attributes)
	 * @return the RemoteInvocationResult object
	 * @throws Exception in case of errors
	 */
	protected RemoteInvocationResult executeRequest(
			RemoteInvocation invocation, MethodInvocation originalInvocation) throws Exception {

		return executeRequest(invocation);
	}

	/**
	 * 通过 {@link HttpInvokerRequestExecutor} 执行给定的远程调用。
	 * <p>可在子类中重写，以向执行器传递不同的配置对象。
	 * 或者，在此访问器的子类中添加更多配置属性：默认情况下，
	 * 访问器将自身作为配置对象传递给执行器。
	 * @param invocation the RemoteInvocation to execute
	 * @return the RemoteInvocationResult object
	 * @throws IOException if thrown by I/O operations
	 * @throws ClassNotFoundException if thrown during deserialization
	 * @throws Exception in case of general errors
	 * @see #getHttpInvokerRequestExecutor
	 * @see HttpInvokerClientConfiguration
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws Exception {
		return getHttpInvokerRequestExecutor().executeRequest(this, invocation);
	}

	/**
	 * 将给定的 HTTP invoker 访问异常转换为相应的
	 * Spring {@link RemoteAccessException}。
	 * @param ex the exception to convert
	 * @return the RemoteAccessException to throw, or {@code null} to have the
	 * original exception propagated to the caller
	 */
	@Nullable
	protected RemoteAccessException convertHttpInvokerAccessException(Throwable ex) {
		if (ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Could not connect to HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
		}

		if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError ||
				ex instanceof InvalidClassException) {
			return new RemoteAccessException(
					"Could not deserialize result from HTTP invoker remote service [" + getServiceUrl() + "]", ex);
		}

		if (ex instanceof Exception) {
			return new RemoteAccessException(
					"Could not access HTTP invoker remote service at [" + getServiceUrl() + "]", ex);
		}

		// For any other Throwable, e.g. OutOfMemoryError: let it get propagated as-is.
		return null;
	}

}
