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

package org.springframework.remoting.httpinvoker;

import java.io.IOException;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * 用于实际执行 HTTP invoker 请求的策略接口。
 * 由 HttpInvokerClientInterceptor 及其子类
 * HttpInvokerProxyFactoryBean 使用。
 *
 * <p>开箱即用地提供了两个实现：
 * <ul>
 * <li><b>{@code SimpleHttpInvokerRequestExecutor}：</b>
 * 使用 JDK 设施执行 POST 请求，不支持
 * HTTP 身份验证或高级配置选项。
 * <li><b>{@code HttpComponentsHttpInvokerRequestExecutor}：</b>
 * 使用 Apache 的 Commons HttpClient 执行 POST 请求，
 * 允许使用预先配置的 HttpClient 实例
 * （可能带有身份验证、HTTP 连接池等）。
 * </ul>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see HttpInvokerClientInterceptor#setHttpInvokerRequestExecutor
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
@FunctionalInterface
public interface HttpInvokerRequestExecutor {

	/**
	 * 执行请求以发送给定的远程调用。
	 * @param config the HTTP invoker configuration that specifies the
	 * target service
	 * @param invocation the RemoteInvocation to execute
	 * @return the RemoteInvocationResult object
	 * @throws IOException if thrown by I/O operations
	 * @throws ClassNotFoundException if thrown during deserialization
	 * @throws Exception in case of general errors
	 */
	RemoteInvocationResult executeRequest(HttpInvokerClientConfiguration config, RemoteInvocation invocation)
			throws Exception;

}
