/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.RMIClassLoader;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.lang.Nullable;

/**
 * 特殊的 ObjectInputStream 子类，在本地找不到类时会回退到指定的 codebase 加载类。与标准的 RMI 动态
 * 类下载约定不同，这里由客户端而不是服务器上的 "java.rmi.server.codebase" 系统属性决定 codebase
 * URL。
 *
 * <p>使用 JDK 的 RMIClassLoader 从指定的 codebase 加载类。codebase 可以由多个 URL 组成，以空格分隔。
 * 注意，RMIClassLoader 需要设置 SecurityManager，就像使用标准 RMI 的动态类下载时一样！（详情参见
 * RMI 文档。）
 *
 * <p>尽管位于 RMI 包中，此类<i>并不</i>用于 RmiClientInterceptor，后者使用标准的 RMI 基础设施，因
 * 此只能依赖 RMI 通过 "java.rmi.server.codebase" 提供的标准动态类下载。
 * CodebaseAwareObjectInputStream 由 HttpInvokerClientInterceptor 使用（参见其中的 "codebaseUrl" 属
 * 性）。
 *
 * <p>感谢 Lionel Mestre 提出该选项并提供了原型！
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see java.rmi.server.RMIClassLoader
 * @see RemoteInvocationSerializingExporter#createObjectInputStream
 * @see org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor#setCodebaseUrl
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class CodebaseAwareObjectInputStream extends ConfigurableObjectInputStream {

	private final String codebaseUrl;


	/**
	 * 为给定的 InputStream 和 codebase 创建一个新的 CodebaseAwareObjectInputStream。
	 * @param in 要从中读取的 InputStream
	 * @param codebaseUrl 在本地找不到类时用于加载类的 codebase URL
	 * （可由多个 URL 组成，以空格分隔）
	 * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
	 */
	public CodebaseAwareObjectInputStream(InputStream in, String codebaseUrl) throws IOException {
		this(in, null, codebaseUrl);
	}

	/**
	 * 为给定的 InputStream 和 codebase 创建一个新的 CodebaseAwareObjectInputStream。
	 * @param in 要从中读取的 InputStream
	 * @param classLoader 用于加载本地类的 ClassLoader
	 * （可以为 {@code null}，表示使用 RMI 的默认 ClassLoader）
	 * @param codebaseUrl 在本地找不到类时用于加载类的 codebase URL
	 * （可由多个 URL 组成，以空格分隔）
	 * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
	 */
	public CodebaseAwareObjectInputStream(
			InputStream in, @Nullable ClassLoader classLoader, String codebaseUrl) throws IOException {

		super(in, classLoader);
		this.codebaseUrl = codebaseUrl;
	}

	/**
	 * 为给定的 InputStream 和 codebase 创建一个新的 CodebaseAwareObjectInputStream。
	 * @param in 要从中读取的 InputStream
	 * @param classLoader 用于加载本地类的 ClassLoader
	 * （可以为 {@code null}，表示使用 RMI 的默认 ClassLoader）
	 * @param acceptProxyClasses 是否接受代理类的反序列化（可作为安全措施禁用）
	 * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
	 */
	public CodebaseAwareObjectInputStream(
			InputStream in, @Nullable ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {

		super(in, classLoader, acceptProxyClasses);
		this.codebaseUrl = null;
	}


	@Override
	protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex)
			throws IOException, ClassNotFoundException {

		// If codebaseUrl is set, try to load the class with the RMIClassLoader.
		// Else, propagate the ClassNotFoundException.
		if (this.codebaseUrl == null) {
			throw ex;
		}
		return RMIClassLoader.loadClass(this.codebaseUrl, className);
	}

	@Override
	protected ClassLoader getFallbackClassLoader() throws IOException {
		return RMIClassLoader.getClassLoader(this.codebaseUrl);
	}

}
