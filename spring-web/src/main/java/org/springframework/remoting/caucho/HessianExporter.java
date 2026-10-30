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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianDebugOutputStream;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import org.apache.commons.logging.Log;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteExporter;
import org.springframework.util.Assert;
import org.springframework.util.CommonsLogWriter;

/**
 * 用于 Hessian 端点的通用基于流的协议导出器。
 *
 * <p>Hessian 是一种轻量级二进制 RPC 协议。
 * 有关 Hessian 的信息，请参阅
 * <a href="http://hessian.caucho.com">Hessian 网站</a>。
 * <b>注意：从 Spring 4.0 起，此导出器要求 Hessian 4.0 或更高版本。</b>
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see #invoke(java.io.InputStream, java.io.OutputStream)
 * @see HessianServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HessianExporter extends RemoteExporter implements InitializingBean {

	/**
	 * hessian 的内容类型（{@code application/x-hessian}）。
	 */
	public static final String CONTENT_TYPE_HESSIAN = "application/x-hessian";


	private SerializerFactory serializerFactory = new SerializerFactory();

	@Nullable
	private HessianRemoteResolver remoteResolver;

	@Nullable
	private Log debugLogger;

	@Nullable
	private HessianSkeleton skeleton;


	/**
	 * 指定要使用的 Hessian SerializerFactory。
	 * <p>通常将其作为类型为 {@code com.caucho.hessian.io.SerializerFactory} 的内部 bean 定义传入，
	 * 并应用自定义的 bean 属性值。
	 */
	public void setSerializerFactory(@Nullable SerializerFactory serializerFactory) {
		this.serializerFactory = (serializerFactory != null ? serializerFactory : new SerializerFactory());
	}

	/**
	 * 设置是否为每个被序列化的集合发送 Java 集合类型。默认为 "true"。
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.serializerFactory.setSendCollectionType(sendCollectionType);
	}

	/**
	 * 设置是否允许将不可序列化的类型作为 Hessian 参数和返回值。默认为 "true"。
	 */
	public void setAllowNonSerializable(boolean allowNonSerializable) {
		this.serializerFactory.setAllowNonSerializable(allowNonSerializable);
	}

	/**
	 * 指定用于解析远程对象引用的自定义 HessianRemoteResolver。
	 */
	public void setRemoteResolver(HessianRemoteResolver remoteResolver) {
		this.remoteResolver = remoteResolver;
	}

	/**
	 * 设置是否启用 Hessian 的调试模式，并将日志输出到此导出器的 Commons Logging 日志。默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setDebug
	 */
	public void setDebug(boolean debug) {
		this.debugLogger = (debug ? logger : null);
	}


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * 初始化此导出器。
	 */
	public void prepare() {
		checkService();
		checkServiceInterface();
		this.skeleton = new HessianSkeleton(getProxyForService(), getServiceInterface());
	}


	/**
	 * 对导出的对象执行调用。
	 * @param inputStream 请求流
	 * @param outputStream 响应流
	 * @throws Throwable 如果调用失败则抛出
	 */
	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		Assert.notNull(this.skeleton, "Hessian exporter has not been initialized");
		doInvoke(this.skeleton, inputStream, outputStream);
	}

	/**
	 * 使用给定的流实际调用骨架（skeleton）。
	 * @param skeleton 要调用的 skeleton
	 * @param inputStream 请求流
	 * @param outputStream 响应流
	 * @throws Throwable 如果调用失败则抛出
	 */
	protected void doInvoke(HessianSkeleton skeleton, InputStream inputStream, OutputStream outputStream)
			throws Throwable {

		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			InputStream isToUse = inputStream;
			OutputStream osToUse = outputStream;

			if (this.debugLogger != null && this.debugLogger.isDebugEnabled()) {
				try (PrintWriter debugWriter = new PrintWriter(new CommonsLogWriter(this.debugLogger))){
					@SuppressWarnings("resource")
					HessianDebugInputStream dis = new HessianDebugInputStream(inputStream, debugWriter);
					@SuppressWarnings("resource")
					HessianDebugOutputStream dos = new HessianDebugOutputStream(outputStream, debugWriter);
					dis.startTop2();
					dos.startTop2();
					isToUse = dis;
					osToUse = dos;
				}
			}

			if (!isToUse.markSupported()) {
				isToUse = new BufferedInputStream(isToUse);
				isToUse.mark(1);
			}

			int code = isToUse.read();
			int major;
			int minor;

			AbstractHessianInput in;
			AbstractHessianOutput out;

			if (code == 'H') {
				// Hessian 2.0 stream
				major = isToUse.read();
				minor = isToUse.read();
				if (major != 0x02) {
					throw new IOException("Version " + major + '.' + minor + " is not understood");
				}
				in = new Hessian2Input(isToUse);
				out = new Hessian2Output(osToUse);
				in.readCall();
			}
			else if (code == 'C') {
				// Hessian 2.0 call... for some reason not handled in HessianServlet!
				isToUse.reset();
				in = new Hessian2Input(isToUse);
				out = new Hessian2Output(osToUse);
				in.readCall();
			}
			else if (code == 'c') {
				// Hessian 1.0 call
				major = isToUse.read();
				minor = isToUse.read();
				in = new HessianInput(isToUse);
				if (major >= 2) {
					out = new Hessian2Output(osToUse);
				}
				else {
					out = new HessianOutput(osToUse);
				}
			}
			else {
				throw new IOException("Expected 'H'/'C' (Hessian 2.0) or 'c' (Hessian 1.0) in hessian input at " + code);
			}

			in.setSerializerFactory(this.serializerFactory);
			out.setSerializerFactory(this.serializerFactory);
			if (this.remoteResolver != null) {
				in.setRemoteResolver(this.remoteResolver);
			}

			try {
				skeleton.invoke(in, out);
			}
			finally {
				try {
					in.close();
					isToUse.close();
				}
				catch (IOException ex) {
					// ignore
				}
				try {
					out.close();
					osToUse.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

}
