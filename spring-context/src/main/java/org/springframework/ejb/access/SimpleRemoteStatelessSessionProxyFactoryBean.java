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

package org.springframework.ejb.access;

import javax.naming.NamingException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 用于远程 SLSB 代理的便捷 {@link FactoryBean}。面向 EJB 2.x 设计，但也适用于 EJB 3 会话 Bean。
 *
 * <p>关于如何指定目标 EJB 的 JNDI 位置，请参见 {@link org.springframework.jndi.JndiObjectLocator}。
 *
 * <p>如果需要控制拦截器链，请使用带有 SimpleRemoteSlsbInvokerInterceptor 的 AOP ProxyFactoryBean，
 * 而不要依赖本类。
 *
 * <p>在 Bean 容器中，此类通常最好以单例方式使用。但是，如果该 Bean 容器会预实例化单例（XML
 * ApplicationContext 的各个变体便是如此），而 Bean 容器又先于 EJB 容器加载目标 EJB，则可能遇到问题。
 * 这是因为默认情况下，JNDI 查找会在本类的 init 方法中执行并缓存，但此时 EJB 尚未绑定到目标位置。
 * 最好的解决办法是将 lookupHomeOnStartup 属性设为 false，这样 home 将在首次访问该 EJB 时获取。（出
 * 于向后兼容的考虑，该标志的默认值才为 true）。
 *
 * <p>此代理工厂通常与 RMI 业务接口一起使用，该接口充当 EJB 组件接口的父接口。或者，此工厂也可以用
 * 一个匹配的非 RMI 业务接口来代理远程 SLSB，即一个镜像 EJB 业务方法但不声明 RemoteExceptions 的接
 * 口。在后一种情况下，EJB 桩抛出的 RemoteExceptions 会自动转换为 Spring 的非受检
 * RemoteAccessException。
 *
 * @author Rod Johnson
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 09.05.2003
 * @see org.springframework.remoting.RemoteAccessException
 * @see AbstractSlsbInvokerInterceptor#setLookupHomeOnStartup
 * @see AbstractSlsbInvokerInterceptor#setCacheHome
 * @see AbstractRemoteSlsbInvokerInterceptor#setRefreshHomeOnConnectFailure
 */
public class SimpleRemoteStatelessSessionProxyFactoryBean extends SimpleRemoteSlsbInvokerInterceptor
	implements FactoryBean<Object>, BeanClassLoaderAware {

	/** 我们要代理的 EJB 的业务接口。 */
	@Nullable
	private Class<?> businessInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** EJBObject. */
	@Nullable
	private Object proxy;


	/**
	 * 设置我们要代理的 EJB 的业务接口。这通常是 EJB 远程组件接口的父接口。在实现 EJB 时，使用业务方法
	 * 接口是一种最佳实践。
	 * <p>也可以指定一个匹配的非 RMI 业务接口，即一个镜像 EJB 业务方法但不声明 RemoteExceptions 的接口。
	 * 在这种情况下，EJB 桩抛出的 RemoteExceptions 会自动转换为 Spring 的通用 RemoteAccessException。
	 * @param businessInterface 该 EJB 的业务接口
	 */
	public void setBusinessInterface(@Nullable Class<?> businessInterface) {
		this.businessInterface = businessInterface;
	}

	/**
	 * 返回我们要代理的 EJB 的业务接口。
	 */
	@Nullable
	public Class<?> getBusinessInterface() {
		return this.businessInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.businessInterface == null) {
			throw new IllegalArgumentException("businessInterface is required");
		}
		this.proxy = new ProxyFactory(this.businessInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.businessInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
