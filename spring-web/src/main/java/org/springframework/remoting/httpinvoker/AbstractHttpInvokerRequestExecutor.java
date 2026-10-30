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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * HttpInvokerRequestExecutor 接口的抽象基类实现。
 *
 * <p>预先实现了 RemoteInvocation 对象的序列化以及
 * RemoteInvocationResults 对象的反序列化。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #doExecuteRequest
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public abstract class AbstractHttpInvokerRequestExecutor implements HttpInvokerRequestExecutor, BeanClassLoaderAware {

	/**
	 * 默认内容类型："application/x-java-serialized-object"。
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";

	private static final int SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE = 1024;


	protected static final String HTTP_METHOD_POST = "POST";

	protected static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	protected static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	protected static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	protected static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

	protected static final String ENCODING_GZIP = "gzip";


	protected final Log logger = LogFactory.getLog(getClass());

	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptGzipEncoding = true;

	@Nullable
	private ClassLoader beanClassLoader;


	/**
	 * 指定发送 HTTP invoker 请求时使用的内容类型。
	 * <p>默认为 "application/x-java-serialized-object"。
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * 返回发送 HTTP invoker 请求时使用的内容类型。
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否接受 GZIP 编码，即是否发送值为 "gzip" 的
	 * HTTP "Accept-Encoding" 头。
	 * <p>默认为 "true"。即使 HTTP 服务器启用了 GZIP 响应压缩，
	 * 如果不需要该功能，可将此标志关闭。
	 */
	public void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
		this.acceptGzipEncoding = acceptGzipEncoding;
	}

	/**
	 * 返回是否接受 GZIP 编码，即是否发送值为 "gzip" 的
	 * HTTP "Accept-Encoding" 头。
	 */
	public boolean isAcceptGzipEncoding() {
		return this.acceptGzipEncoding;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回此执行器应使用的 bean ClassLoader。
	 */
	@Nullable
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	@Override
	public final RemoteInvocationResult executeRequest(
			HttpInvokerClientConfiguration config, RemoteInvocation invocation) throws Exception {

		ByteArrayOutputStream baos = getByteArrayOutputStream(invocation);
		if (logger.isDebugEnabled()) {
			logger.debug("Sending HTTP invoker request for service at [" + config.getServiceUrl() +
					"], with size " + baos.size());
		}
		return doExecuteRequest(config, baos);
	}

	/**
	 * 将给定的 RemoteInvocation 序列化到 ByteArrayOutputStream 中。
	 * @param invocation RemoteInvocation 对象
	 * @return 包含序列化后 RemoteInvocation 的 ByteArrayOutputStream
	 * @throws IOException 如果由 I/O 方法抛出则传播
	 */
	protected ByteArrayOutputStream getByteArrayOutputStream(RemoteInvocation invocation) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE);
		writeRemoteInvocation(invocation, baos);
		return baos;
	}

	/**
	 * 将给定的 RemoteInvocation 序列化到给定的 OutputStream 中。
	 * <p>默认实现会先给 {@code decorateOutputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。
	 * 然后为最终的流创建 {@code ObjectOutputStream}，并调用
	 * {@code doWriteRemoteInvocation} 实际写入对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param invocation RemoteInvocation 对象
	 * @param os 要写入的 OutputStream
	 * @throws IOException 如果由 I/O 方法抛出则传播
	 * @see #decorateOutputStream
	 * @see #doWriteRemoteInvocation
	 */
	protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(decorateOutputStream(os))) {
			doWriteRemoteInvocation(invocation, oos);
		}
	}

	/**
	 * 返回用于写入远程调用的 OutputStream，
	 * 可能会装饰给定的原始 OutputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param os 原始 OutputStream
	 * @return 可能已被装饰的 OutputStream
	 */
	protected OutputStream decorateOutputStream(OutputStream os) throws IOException {
		return os;
	}

	/**
	 * 执行将给定调用对象实际写入给定 ObjectOutputStream 的操作。
	 * <p>默认实现仅调用 {@code writeObject}。
	 * 可重写，以便序列化自定义包装对象（而非普通调用），
	 * 例如加密感知的持有器。
	 * @param invocation RemoteInvocation 对象
	 * @param oos 要写入的 ObjectOutputStream
	 * @throws IOException 如果由 I/O 方法抛出则传播
	 * @see java.io.ObjectOutputStream#writeObject
	 */
	protected void doWriteRemoteInvocation(RemoteInvocation invocation, ObjectOutputStream oos) throws IOException {
		oos.writeObject(invocation);
	}


	/**
	 * 执行请求以发送给定的已序列化远程调用。
	 * <p>实现通常会调用 {@code readRemoteInvocationResult}
	 * 来反序列化返回的 RemoteInvocationResult 对象。
	 * @param config 指定目标服务的 HTTP invoker 配置
	 * @param baos 包含序列化后 RemoteInvocation 对象的
	 * ByteArrayOutputStream
	 * @return RemoteInvocationResult 对象
	 * @throws IOException 如果由 I/O 操作抛出则传播
	 * @throws ClassNotFoundException 如果在反序列化期间抛出则传播
	 * @throws Exception 发生一般错误时抛出
	 * @see #readRemoteInvocationResult(java.io.InputStream, String)
	 */
	protected abstract RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws Exception;

	/**
	 * 从给定的 InputStream 中反序列化 RemoteInvocationResult 对象。
	 * <p>先给 {@code decorateInputStream} 一次装饰流的机会
	 * （例如用于自定义加密或压缩）。通过 {@code createObjectInputStream}
	 * 创建 {@code ObjectInputStream}，并调用 {@code doReadRemoteInvocationResult}
	 * 实际读取对象。
	 * <p>可重写以自定义调用的序列化方式。
	 * @param is 要读取的 InputStream
	 * @param codebaseUrl 在本机找不到类时用于加载类的 codebase URL
	 * @return RemoteInvocationResult 对象
	 * @throws IOException 如果由 I/O 方法抛出则传播
	 * @throws ClassNotFoundException 如果在反序列化期间抛出则传播
	 * @see #decorateInputStream
	 * @see #createObjectInputStream
	 * @see #doReadRemoteInvocationResult
	 */
	protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, @Nullable String codebaseUrl)
			throws IOException, ClassNotFoundException {

		try (ObjectInputStream ois = createObjectInputStream(decorateInputStream(is), codebaseUrl)) {
			return doReadRemoteInvocationResult(ois);
		}
	}

	/**
	 * 返回用于读取远程调用结果的 InputStream，
	 * 可能会装饰给定的原始 InputStream。
	 * <p>默认实现按原样返回给定的流。
	 * 可重写，例如用于自定义加密或压缩。
	 * @param is 原始 InputStream
	 * @return 可能已被装饰的 InputStream
	 */
	protected InputStream decorateInputStream(InputStream is) throws IOException {
		return is;
	}

	/**
	 * 为给定的 InputStream 和 codebase 创建 ObjectInputStream。
	 * 默认实现创建 CodebaseAwareObjectInputStream。
	 * @param is 要读取的 InputStream
	 * @param codebaseUrl 在本机找不到类时用于加载类的 codebase URL
	 * （可以为 {@code null}）
	 * @return 要使用的新的 ObjectInputStream 实例
	 * @throws IOException 如果创建 ObjectInputStream 失败则抛出
	 * @see org.springframework.remoting.rmi.CodebaseAwareObjectInputStream
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is, @Nullable String codebaseUrl) throws IOException {
		return new org.springframework.remoting.rmi.CodebaseAwareObjectInputStream(is, getBeanClassLoader(), codebaseUrl);
	}

	/**
	 * 执行从给定 ObjectInputStream 实际读取调用对象的操作。
	 * <p>默认实现仅调用 {@code readObject}。
	 * 可重写，以便反序列化自定义包装对象（而非普通调用），
	 * 例如加密感知的持有器。
	 * @param ois 要读取的 ObjectInputStream
	 * @return RemoteInvocationResult 对象
	 * @throws IOException 如果由 I/O 方法抛出则传播
	 * @throws ClassNotFoundException 如果无法解析某个序列化对象的类名则抛出
	 * @see java.io.ObjectOutputStream#writeObject
	 */
	protected RemoteInvocationResult doReadRemoteInvocationResult(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocationResult)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocationResult.class.getName() + "]: " + ClassUtils.getDescriptiveType(obj));
		}
		return (RemoteInvocationResult) obj;
	}

}
