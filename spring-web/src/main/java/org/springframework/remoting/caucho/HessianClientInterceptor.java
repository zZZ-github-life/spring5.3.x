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

package org.springframework.remoting.caucho;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.MalformedURLException;

import com.caucho.hessian.HessianException;
import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.SerializerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;
import org.springframework.util.Assert;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}，用于访问 Hessian 服务。
 * 支持通过用户名和密码进行身份验证。
 * 服务 URL 必须是指向 Hessian 服务的 HTTP URL。
 *
 * <p>Hessian 是一种轻量级二进制 RPC 协议。
 * 有关 Hessian 的信息，请参阅
 * <a href="http://hessian.caucho.com">Hessian 网站</a>
 * <b>注意：从 Spring 4.0 起，此客户端要求 Hessian 4.0 或更高版本。</b>
 *
 * <p>注意：通过此代理工厂访问的服务无需使用 Spring 的 {@link HessianServiceExporter} 导出，因为此处不涉及任何特殊处理。因此，您也可以访问使用 Caucho 的 {@link com.caucho.hessian.server.HessianServlet} 导出的服务。
 *
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see #setUsername
 * @see #setPassword
 * @see HessianServiceExporter
 * @see HessianProxyFactoryBean
 * @see com.caucho.hessian.client.HessianProxyFactory
 * @see com.caucho.hessian.server.HessianServlet
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HessianClientInterceptor extends UrlBasedRemoteAccessor implements MethodInterceptor {

	private HessianProxyFactory proxyFactory = new HessianProxyFactory();

	@Nullable
	private Object hessianProxy;


	/**
	 * 设置要使用的 HessianProxyFactory 实例。
	 * 如果未指定，将创建默认的 HessianProxyFactory。
	 * <p>允许使用外部配置的工厂实例，
	 * 特别是自定义的 HessianProxyFactory 子类。
	 */
	public void setProxyFactory(@Nullable HessianProxyFactory proxyFactory) {
		this.proxyFactory = (proxyFactory != null ? proxyFactory : new HessianProxyFactory());
	}

	/**
	 * 指定要使用的 Hessian SerializerFactory。
	 * <p>通常将其作为类型为 {@code com.caucho.hessian.io.SerializerFactory} 的内部 bean 定义传入，
	 * 并应用自定义的 bean 属性值。
	 */
	public void setSerializerFactory(SerializerFactory serializerFactory) {
		this.proxyFactory.setSerializerFactory(serializerFactory);
	}

	/**
	 * 设置是否为每个被序列化的集合发送 Java 集合类型。默认为 "true"。
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.proxyFactory.getSerializerFactory().setSendCollectionType(sendCollectionType);
	}

	/**
	 * 设置是否允许将不可序列化的类型作为 Hessian 参数和返回值。默认为 "true"。
	 */
	public void setAllowNonSerializable(boolean allowNonSerializable) {
		this.proxyFactory.getSerializerFactory().setAllowNonSerializable(allowNonSerializable);
	}

	/**
	 * 设置是否应为远程调用启用重载方法。
	 * 默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setOverloadEnabled
	 */
	public void setOverloadEnabled(boolean overloadEnabled) {
		this.proxyFactory.setOverloadEnabled(overloadEnabled);
	}

	/**
	 * 设置此工厂访问远程服务时应使用的用户名。
	 * 默认为无。
	 * <p>用户名将由 Hessian 通过 HTTP Basic Authentication 发送。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setUser
	 */
	public void setUsername(String username) {
		this.proxyFactory.setUser(username);
	}

	/**
	 * 设置此工厂访问远程服务时应使用的密码。
	 * 默认为无。
	 * <p>密码将由 Hessian 通过 HTTP Basic Authentication 发送。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setPassword
	 */
	public void setPassword(String password) {
		this.proxyFactory.setPassword(password);
	}

	/**
	 * 设置是否启用 Hessian 的调试模式。
	 * 默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setDebug
	 */
	public void setDebug(boolean debug) {
		this.proxyFactory.setDebug(debug);
	}

	/**
	 * 设置是否使用分块 POST 发送 Hessian 请求。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setChunkedPost
	 */
	public void setChunkedPost(boolean chunkedPost) {
		this.proxyFactory.setChunkedPost(chunkedPost);
	}

	/**
	 * 指定用于 Hessian 客户端的自定义 HessianConnectionFactory。
	 */
	public void setConnectionFactory(HessianConnectionFactory connectionFactory) {
		this.proxyFactory.setConnectionFactory(connectionFactory);
	}

	/**
	 * 设置 Hessian 客户端使用的 socket 连接超时时间。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setConnectTimeout
	 */
	public void setConnectTimeout(long timeout) {
		this.proxyFactory.setConnectTimeout(timeout);
	}

	/**
	 * 设置等待 Hessian 服务响应时的超时时间。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setReadTimeout
	 */
	public void setReadTimeout(long timeout) {
		this.proxyFactory.setReadTimeout(timeout);
	}

	/**
	 * 设置解析请求和响应时是否使用 Hessian 协议版本 2。默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setHessian2Request
	 */
	public void setHessian2(boolean hessian2) {
		this.proxyFactory.setHessian2Request(hessian2);
		this.proxyFactory.setHessian2Reply(hessian2);
	}

	/**
	 * 设置解析请求时是否使用 Hessian 协议版本 2。默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setHessian2Request
	 */
	public void setHessian2Request(boolean hessian2) {
		this.proxyFactory.setHessian2Request(hessian2);
	}

	/**
	 * 设置解析响应时是否使用 Hessian 协议版本 2。默认为 "false"。
	 * @see com.caucho.hessian.client.HessianProxyFactory#setHessian2Reply
	 */
	public void setHessian2Reply(boolean hessian2) {
		this.proxyFactory.setHessian2Reply(hessian2);
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		prepare();
	}

	/**
	 * 为此拦截器初始化 Hessian 代理。
	 * @throws RemoteLookupFailureException if the service URL is invalid
	 */
	public void prepare() throws RemoteLookupFailureException {
		try {
			this.hessianProxy = createHessianProxy(this.proxyFactory);
		}
		catch (MalformedURLException ex) {
			throw new RemoteLookupFailureException("Service URL [" + getServiceUrl() + "] is invalid", ex);
		}
	}

	/**
	 * 创建由此拦截器包装的 Hessian 代理。
	 * @param proxyFactory the proxy factory to use
	 * @return the Hessian proxy
	 * @throws MalformedURLException if thrown by the proxy factory
	 * @see com.caucho.hessian.client.HessianProxyFactory#create
	 */
	protected Object createHessianProxy(HessianProxyFactory proxyFactory) throws MalformedURLException {
		Assert.notNull(getServiceInterface(), "'serviceInterface' is required");
		return proxyFactory.create(getServiceInterface(), getServiceUrl(), getBeanClassLoader());
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (this.hessianProxy == null) {
			throw new IllegalStateException("HessianClientInterceptor is not properly initialized - " +
					"invoke 'prepare' before attempting any operations");
		}

		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			return invocation.getMethod().invoke(this.hessianProxy, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			// Hessian 4.0 check: another layer of InvocationTargetException.
			if (targetEx instanceof InvocationTargetException) {
				targetEx = ((InvocationTargetException) targetEx).getTargetException();
			}
			if (targetEx instanceof HessianConnectionException) {
				throw convertHessianAccessException(targetEx);
			}
			else if (targetEx instanceof HessianException || targetEx instanceof HessianRuntimeException) {
				Throwable cause = targetEx.getCause();
				throw convertHessianAccessException(cause != null ? cause : targetEx);
			}
			else if (targetEx instanceof UndeclaredThrowableException) {
				UndeclaredThrowableException utex = (UndeclaredThrowableException) targetEx;
				throw convertHessianAccessException(utex.getUndeclaredThrowable());
			}
			else {
				throw targetEx;
			}
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException(
					"Failed to invoke Hessian proxy for remote service [" + getServiceUrl() + "]", ex);
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * 将给定的 Hessian 访问异常转换为相应的 Spring RemoteAccessException。
	 * @param ex the exception to convert
	 * @return the RemoteAccessException to throw
	 */
	protected RemoteAccessException convertHessianAccessException(Throwable ex) {
		if (ex instanceof HessianConnectionException || ex instanceof ConnectException) {
			return new RemoteConnectFailureException(
					"Cannot connect to Hessian remote service at [" + getServiceUrl() + "]", ex);
		}
		else {
			return new RemoteAccessException(
				"Cannot access Hessian remote service at [" + getServiceUrl() + "]", ex);
		}
	}

}
